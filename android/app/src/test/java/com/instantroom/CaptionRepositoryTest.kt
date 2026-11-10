package com.instantroom

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.instantroom.core.*
import com.instantroom.data.*
import com.instantroom.imaging.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35]) @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CaptionRepositoryTest {
 private val context get()=ApplicationProvider.getApplicationContext<Context>()
 private lateinit var db: InstantDatabase
 private lateinit var repo: PhotoRepository
 @Before fun setup(){db=Room.inMemoryDatabaseBuilder(context,InstantDatabase::class.java).build();repo=PhotoRepository(context,db,FilmProcessor(Preferences(context)))}
 @After fun close(){db.close()}
 private suspend fun photo(format: FilmFormat=FilmFormat.CLASSIC,progress: Float=1f): PhotoEntity {
  if(repo.active()==null)repo.replace(format,Appearance.COLOR)
  val id=UUID.randomUUID().toString()
  val original=repo.originalFile(id).apply{writeBytes(byteArrayOf(1,2,3,4))}
  val base=File(original.parentFile,"$id-film.jpg")
  val b=Bitmap.createBitmap(600,(600/format.aspect).toInt(),Bitmap.Config.ARGB_8888);b.eraseColor(Color.rgb(247,242,226))
  base.outputStream().use{b.compress(Bitmap.CompressFormat.JPEG,94,it)};b.recycle()
  return PhotoEntity(id,original.path,base.path,123,format.name,"COLOR",repo.active()!!.id,progress,false).also{db.dao().insertPhoto(it)}
 }
 @Test fun addEditRemovePreserveOriginalBaseFilmAndCorkboard()=runBlocking<Unit> {
  val p=photo();repo.attach(p.id);repo.pin(p.id,123,.5f,.1f)
  val pins=repo.placements.first();val cart=repo.active();val baseBytes=File(p.processed).readBytes()
  repo.setCaption(p.id,"First note",CaptionFont.SOL_KOL)
  val added=repo.photo(p.id);assertEquals("First note",added.caption);assertEquals(p.processed,added.baseProcessed);assertNotEquals(p.processed,added.processed)
  val oldRendered=File(added.processed).readBytes()
  repo.setCaption(p.id,"Second note",CaptionFont.DENISTINA)
  val edited=repo.photo(p.id);assertEquals("Second note",edited.caption);assertEquals("DENISTINA",edited.captionFont);assertEquals(p.processed,edited.baseProcessed);assertNotEquals(added.processed,edited.processed)
  // Prior URI remains valid if a share recipient is still reading it.
  assertArrayEquals(oldRendered,File(added.processed).readBytes())
  repo.setCaption(p.id,"  ",CaptionFont.HISTORIC)
  val removed=repo.photo(p.id);assertEquals("",removed.caption);assertEquals(p.processed,removed.processed)
  assertArrayEquals(baseBytes,File(removed.processed).readBytes());assertArrayEquals(byteArrayOf(1,2,3,4),File(p.original).readBytes())
  assertEquals(cart,repo.active());assertEquals(pins,repo.placements.first());assertEquals(1f,removed.progress,0f)
 }
 @Test fun unfinishedAndProcessingPhotosCannotBeCaptioned()=runBlocking<Unit> {
  val p=photo(progress=.6f);assertTrue(runCatching{repo.setCaption(p.id,"No",CaptionFont.SOL_KOL)}.isFailure)
  db.dao().insertPhoto(p.copy(id="processing",progress=1f,processing=true))
  assertTrue(runCatching{repo.setCaption("processing","No",CaptionFont.SOL_KOL)}.isFailure)
  assertEquals(p,repo.photo(p.id))
 }
 @Test fun invalidCaptionAndMissingBaseLeavePriorPrintIntact()=runBlocking<Unit> {
  val p=photo();assertTrue(runCatching{repo.setCaption(p.id,"x".repeat(81),CaptionFont.SOL_KOL)}.isFailure);assertEquals(p,repo.photo(p.id))
  File(p.processed).delete();assertTrue(runCatching{repo.setCaption(p.id,"No",CaptionFont.SOL_KOL)}.isFailure);assertEquals(p,repo.photo(p.id))
 }
 @Test fun captionMetadataAndRenderedReferenceSurviveDatabaseReopen()=runBlocking<Unit> {
  val name="caption-${UUID.randomUUID()}.db"
  var disk=Room.databaseBuilder(context,InstantDatabase::class.java,name).build()
  try {
   val p=photo();disk.dao().insertCartridge(repo.active()!!);disk.dao().insertPhoto(p)
   val r=PhotoRepository(context,disk,FilmProcessor(Preferences(context)));r.setCaption(p.id,"A moment kept",CaptionFont.HISTORIC)
   val before=r.photo(p.id);disk.close();disk=Room.databaseBuilder(context,InstantDatabase::class.java,name).build()
   assertEquals(before,disk.dao().photo(p.id));assertTrue(File(before.processed).isFile)
  }finally{disk.close();context.deleteDatabase(name)}
 }
 @Test fun shareAndGalleryReceiveCaptionedJpegAcrossAllFormats()=runBlocking<Unit> {
  org.robolectric.util.ReflectionHelpers.getStaticField<MutableMap<String,Any>>(androidx.core.content.FileProvider::class.java,"sCache").clear()
  for(format in FilmFormat.entries) {
   val p=photo(format);repo.setCaption(p.id,"Netanya memories",CaptionFont.SOL_KOL)
   val final=repo.photo(p.id);val bytes=File(final.processed).readBytes();val export=PhotoExport(context)
   val chooser=export.shareIntent(final)
   val send=chooser.getParcelableExtra(Intent.EXTRA_INTENT,Intent::class.java)!!
   val uri=send.getParcelableExtra(Intent.EXTRA_STREAM,Uri::class.java)!!
   context.contentResolver.openInputStream(uri)!!.use{assertArrayEquals(bytes,it.readBytes())}
   assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION!=0)
   export.save(final)
   val collection=android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
   context.contentResolver.query(collection,arrayOf("_id"),"_display_name = ?",arrayOf("InstantRoom-${p.id}.jpg"),null)!!.use{cursor->
    assertTrue(cursor.moveToFirst())
    val saved=android.content.ContentUris.withAppendedId(collection,cursor.getLong(0))
    context.contentResolver.openInputStream(saved)!!.use{assertArrayEquals(bytes,it.readBytes())}
   }
   val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size);assertEquals(format.aspect,bitmap.width.toFloat()/bitmap.height,.01f);bitmap.recycle()
  }
 }
 @Test fun concurrentEditsAlwaysUseTheSameUncaptionedBase()=runBlocking<Unit> {
  val p=photo();coroutineScope {listOf("One","Two").map{text->async{repo.setCaption(p.id,text,CaptionFont.SOL_KOL)}}.awaitAll()}
  val final=repo.photo(p.id);assertTrue(final.caption in listOf("One","Two"));assertEquals(p.processed,final.baseProcessed);assertTrue(File(final.processed).length()>0)
 }
 @Test fun regenerationUsesRetainedBaseEvenAfterProcessingSettingsChange()=runBlocking<Unit> {
  val p=photo();repo.setCaption(p.id,"Our story",CaptionFont.HISTORIC)
  val before=File(repo.photo(p.id).processed).readBytes()
  Preferences(context).set(Settings(lowMemory=true,imperfections=false))
  repo.setCaption(p.id,"Our story",CaptionFont.HISTORIC)
  assertArrayEquals(before,File(repo.photo(p.id).processed).readBytes())
 }
}
