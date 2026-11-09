package com.instantroom.data

import android.content.Context
import androidx.room.withTransaction
import com.instantroom.core.*
import com.instantroom.imaging.FilmProcessor
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
 fun originalFile(id: String) = File(context.filesDir, "photos/$id-original.jpg").also { it.parentFile!!.mkdirs() }
 /** A successful CameraX JPEG is journalled, then consumed+archived in ONE transaction.
  * Recovery replays the journal if process death occurred before Room commit. */
 suspend fun prepareCapture(id: String, cart: CartridgeEntity): PhotoEntity = withContext(Dispatchers.IO) {
  val file=originalFile(id)
  val p=PhotoEntity(id,file.absolutePath,capturedAt=System.currentTimeMillis(),format=cart.format,appearance=cart.appearance,cartridgeId=cart.id)
  val payload=org.json.JSONObject().put("id",id).put("cartridge",cart.id).put("format",cart.format).put("appearance",cart.appearance).put("time",p.capturedAt).toString()
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
   PhotoEntity(id,file.absolutePath,capturedAt=j.getLong("time"),format=j.getString("format"),appearance=j.getString("appearance"),cartridgeId=j.getString("cartridge"))
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
   archiveSuccessful(PhotoEntity(id,originalFile(id).absolutePath,capturedAt=j.getLong("time"),format=j.getString("format"),appearance=j.getString("appearance"),cartridgeId=j.getString("cartridge")))
   journal.delete()
  }
  dao.pending().forEach { process(it) }
 }
 suspend fun process(p: PhotoEntity) = withContext(Dispatchers.Default) {
  val path = processor.process(p)
  dao.processed(p.id, path)
 }
 suspend fun replace(format: FilmFormat, appearance: Appearance) = captureLock.withLock {
  db.withTransaction {
   dao.deactivate()
   dao.insertCartridge(CartridgeEntity(UUID.randomUUID().toString(), format.name, appearance.name))
  }
 }
 suspend fun active() = dao.active()
 suspend fun develop(id: String, strength: Float, accessible: Boolean = false): Unit = mutationLock.withLock {
  val p = dao.photo(id) ?: return@withLock
  if (p.processing || p.progress >= 1f) return@withLock
  dao.progress(id, if(accessible) Development.accessiblePress(p.progress, true) else Development.acceptedShake(p.progress, strength))
 }
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
