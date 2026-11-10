package com.instantroom.data

import android.content.Context
import androidx.room.withTransaction
import com.instantroom.core.*
import com.instantroom.imaging.FilmProcessor
import com.instantroom.imaging.CaptionRenderer
import com.instantroom.imaging.BoothRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class PhotoRepository(private val context: Context, private val db: InstantDatabase, private val processor: FilmProcessor) {
 private val dao = db.dao()
 val photos = dao.photos()
 val cartridge = dao.activeFlow()
 val placements = dao.placements()
 val captureLock = Mutex()
 private val mutationLock = Mutex()
 private val boothLock = Mutex()
 fun originalFile(id: String) = File(context.filesDir, "photos/$id-original.jpg").also { it.parentFile!!.mkdirs() }
 /** A successful CameraX JPEG is journalled, then consumed+archived in ONE transaction.
  * Recovery replays the journal if process death occurred before Room commit. */
 suspend fun prepareCapture(id: String, cart: CartridgeEntity, sessionId: String="", shotIndex: Int=-1): PhotoEntity = withContext(Dispatchers.IO) {
  val file=originalFile(id)
  val p=PhotoEntity(id,file.absolutePath,capturedAt=System.currentTimeMillis(),format=cart.format,appearance=cart.appearance,cartridgeId=cart.id,boothSessionId=sessionId,boothIndex=shotIndex)
  val payload=org.json.JSONObject().put("id",id).put("cartridge",cart.id).put("format",cart.format).put("appearance",cart.appearance).put("time",p.capturedAt).put("session",sessionId).put("shot",shotIndex).toString()
  java.io.FileOutputStream(File(file.parent,"$id.pending")).use { it.write(payload.toByteArray());it.fd.sync() }
  p
 }
 suspend fun abandonCapture(id: String) = withContext(Dispatchers.IO) {
  originalFile(id).delete();File(originalFile(id).parent,"$id.pending").delete();Unit
 }
 suspend fun commitCapture(id: String, cart: CartridgeEntity): PhotoEntity = withContext(Dispatchers.IO) {
  val file=originalFile(id)
  check(file.length()>0) { "Camera did not create an image" }
  val journal=File(file.parent,"$id.pending")
  val existing=dao.photo(id)
  if(existing!=null){journal.delete();return@withContext existing}
  val p=if(journal.exists()) {
   val j=org.json.JSONObject(journal.readText())
   PhotoEntity(id,file.absolutePath,capturedAt=j.getLong("time"),format=j.getString("format"),appearance=j.getString("appearance"),cartridgeId=j.getString("cartridge"),boothSessionId=j.optString("session",""),boothIndex=j.optInt("shot",-1))
  } else prepareCapture(id,cart)
  archiveSuccessful(p)
  journal.delete()
  p
 }
 private suspend fun archiveSuccessful(p: PhotoEntity) = db.withTransaction {
  if (dao.photo(p.id) == null) {
   check(dao.consume(p.cartridgeId) == 1) { "Film changed before exposure was saved" }
   dao.insertPhoto(p)
  }
 }
 suspend fun recover() = withContext(Dispatchers.IO) {
  val directory=File(context.filesDir,"photos"); directory.mkdirs()
  directory.listFiles()?.filter { it.extension=="pending" }?.forEach { journal ->
   val id=journal.nameWithoutExtension
   if(!originalFile(id).isFile || originalFile(id).length()==0L) {journal.delete();return@forEach}
   val j=org.json.JSONObject(journal.readText())
   archiveSuccessful(PhotoEntity(id,originalFile(id).absolutePath,capturedAt=j.getLong("time"),format=j.getString("format"),appearance=j.getString("appearance"),cartridgeId=j.getString("cartridge"),boothSessionId=j.optString("session",""),boothIndex=j.optInt("shot",-1)))
   journal.delete()
  }
  dao.pending().forEach { process(it) }
  dao.unfinishedSessions().forEach { session->
   if(dao.sources(session.id).size==4)finishBooth(session.id) else dao.sessionState(session.id,"INTERRUPTED")
  }
 }
 suspend fun process(p: PhotoEntity) = withContext(Dispatchers.Default) {
  val path = if(p.boothLayout.isEmpty())processor.process(p) else BoothRenderer(context).render(dao.sources(p.boothSessionId),BoothLayout.valueOf(p.boothLayout),p.id)
  dao.processed(p.id, path)
 }
 suspend fun replace(format: FilmFormat, appearance: Appearance) = captureLock.withLock {
  db.withTransaction {
   dao.deactivate()
   dao.insertCartridge(CartridgeEntity(UUID.randomUUID().toString(), format.name, appearance.name))
  }
 }
 suspend fun startBooth(layout: BoothLayout, cart: CartridgeEntity): BoothSessionEntity {
  check(cart.remaining>=4){"Photo Booth needs four exposures. Manually load a fresh cartridge."}
  check(dao.active()?.id==cart.id)
  val s=BoothSessionEntity(UUID.randomUUID().toString(),cart.id,layout.name,System.currentTimeMillis())
  dao.insertSession(s);return s
 }
 suspend fun boothSources(id: String) = dao.sources(id)
 suspend fun interruptBooth(id: String) {dao.sessionState(id,"INTERRUPTED")}
 suspend fun finishBooth(id: String): PhotoEntity = boothLock.withLock {
  val session=checkNotNull(dao.session(id));val sources=dao.sources(id)
  check(sources.size==4 && sources.map{it.boothIndex}==listOf(0,1,2,3)){"All four source photographs are required"}
  val compositeId="$id-composite"
  val composite=db.withTransaction {
   dao.photo(compositeId) ?: sources.first().copy(id=compositeId,processed="",progress=0f,processing=true,caption="",baseProcessed="",boothIndex=-1,boothLayout=session.layout).also{dao.insertPhoto(it)}
  }
  if(composite.processing)process(composite)
  dao.sessionState(id,"COMPLETE")
  checkNotNull(dao.photo(compositeId))
 }
 /** Recomposition creates a new undeveloped print and never consumes film. */
 suspend fun recompose(id: String, layout: BoothLayout): PhotoEntity = boothLock.withLock {
  val sources=dao.sources(id);check(sources.size==4)
  val p=sources.first().copy(id=UUID.randomUUID().toString(),processed="",progress=0f,processing=true,caption="",baseProcessed="",boothIndex=-1,boothLayout=layout.name)
  dao.insertPhoto(p);process(p);checkNotNull(dao.photo(p.id))
 }
 suspend fun active() = dao.active()
 suspend fun develop(id: String, strength: Float, accessible: Boolean = false, allowed: ()->Boolean = {true}): Boolean = mutationLock.withLock {
  val p = dao.photo(id) ?: return@withLock false
  if (p.processing || p.progress >= 1f || !allowed()) return@withLock false
  val next=if(accessible) Development.accessiblePress(p.progress, true) else Development.acceptedShake(p.progress, strength)
  if(next<=p.progress)return@withLock false
  dao.progress(id,next)
  true
 }
 suspend fun setCaption(id: String, text: String, font: CaptionFont): Unit = mutationLock.withLock {
  val normalized=CaptionRules.normalize(text)
  val p=checkNotNull(dao.photo(id)){"Photograph is unavailable"}
  check(!p.processing && p.progress>=1f){"Develop this photograph first"}
  val base=p.baseProcessed.ifEmpty{p.processed}
  check(File(base).isFile){"The uncaptioned print is unavailable"}
  val rendered=withContext(Dispatchers.Default){CaptionRenderer(context).render(File(base),p.geometry,normalized,font)}
  // A crash before this transaction leaves the previously committed print intact.
  // Unique output paths also invalidate image caches without mutating a shared URI.
  check(dao.caption(id,normalized,font.name,base,rendered)==1)
 }
 suspend fun photo(id: String): PhotoEntity = mutationLock.withLock {checkNotNull(dao.photo(id))}
 suspend fun attach(id: String) = mutationLock.withLock {
  val p=dao.photo(id) ?: return@withLock
  check(p.progress >= 1f && !p.processing)
  if(dao.placement("main",id)==null) dao.putPlacement(PlacementEntity(photoId=id,z=dao.nextZ()))
 }
 suspend fun move(id: String, x: Float, y: Float, rotation: Float) = mutationLock.withLock {
  val p=dao.placement("main",id) ?: return@withLock
  if(!p.pinned) dao.putPlacement(p.copy(x=x.coerceIn(0f,1800f), y=y.coerceIn(0f,1400f), rotation=rotation.coerceIn(-180f,180f), z=dao.nextZ()))
 }
 suspend fun pin(id: String, color: Long, x: Float, y: Float) = mutationLock.withLock {
  val p=dao.placement("main",id) ?: return@withLock
  dao.putPlacement(p.copy(pinned=true,pinColor=color,pinX=x.coerceIn(.05f,.95f),pinY=y.coerceIn(.02f,.9f)))
 }
 suspend fun unpin(id: String) = mutationLock.withLock { dao.placement("main",id)?.let { dao.putPlacement(it.copy(pinned=false)) } }
 suspend fun detach(id: String) = mutationLock.withLock { dao.detach(id) }
}
