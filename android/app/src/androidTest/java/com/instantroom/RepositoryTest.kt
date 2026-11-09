package com.instantroom

import android.content.Context
import android.graphics.*
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.instantroom.core.*
import com.instantroom.data.*
import com.instantroom.imaging.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
 private lateinit var db: InstantDatabase
 private lateinit var context: Context
 private lateinit var repo: PhotoRepository
 @Before fun setUp(){context=ApplicationProvider.getApplicationContext();db=Room.inMemoryDatabaseBuilder(context,InstantDatabase::class.java).build();repo=PhotoRepository(context,db,FilmProcessor(Preferences(context)))}
 @After fun tearDown(){db.close()}
 private suspend fun expose(process: Boolean=false): PhotoEntity {
  val id=UUID.randomUUID().toString();val image=Bitmap.createBitmap(160,120,Bitmap.Config.ARGB_8888);Canvas(image).drawColor(Color.CYAN)
  repo.originalFile(id).outputStream().use{image.compress(Bitmap.CompressFormat.JPEG,95,it)};image.recycle()
  val p=repo.commitCapture(id,repo.active()!!)
  if(process)repo.process(p)
  return p
 }
 @Test fun tenSuccessesExhaustAndReplacementPreservesArchive()=runBlocking<Unit> {
  repo.replace(FilmFormat.GO,Appearance.WARM);repeat(10){expose()}
  assertEquals(0,repo.active()!!.remaining);assertEquals(10,repo.photos.first().size)
  val id=UUID.randomUUID().toString();repo.originalFile(id).writeBytes(byteArrayOf(1))
  assertTrue(runCatching{repo.commitCapture(id,repo.active()!!)}.isFailure)
  // A rejected attempt must not leave a recovery journal.
  java.io.File(repo.originalFile(id).parent,"$id.pending").delete()
  repo.replace(FilmFormat.ROUND,Appearance.MONO);assertEquals(10,repo.active()!!.remaining);assertEquals(10,repo.photos.first().size)
 }
 @Test fun failedCaptureDoesNotConsume()=runBlocking<Unit> {
  repo.replace(FilmFormat.CLASSIC,Appearance.COLOR)
  val result=runCatching{repo.commitCapture(UUID.randomUUID().toString(),repo.active()!!)}
  assertTrue(result.isFailure);assertEquals(10,repo.active()!!.remaining)
 }
 @Test fun duplicateCompletionDoesNotDoubleConsume()=runBlocking<Unit> {
  repo.replace(FilmFormat.CLASSIC,Appearance.COLOR);val cart=repo.active()!!;val p=expose();repo.commitCapture(p.id,cart)
  assertEquals(9,repo.active()!!.remaining);assertEquals(1,repo.photos.first().size)
 }
 @Test fun formatComesFromInstalledCartridge()=runBlocking<Unit> {
  repo.replace(FilmFormat.SPECTRA,Appearance.MONO);val p=expose();assertEquals("SPECTRA",p.format);assertEquals("MONO",p.appearance)
 }
 @Test fun developmentAndPinLockSurviveRepositoryRecreation()=runBlocking<Unit> {
  repo.replace(FilmFormat.CLASSIC,Appearance.COLOR);val p=expose(true)
  repeat(8){repo.develop(p.id,1f)};val progress=db.dao().photo(p.id)!!.progress
  val restored=PhotoRepository(context,db,FilmProcessor(Preferences(context)))
  assertEquals(progress,restored.photos.first().first().progress,0f)
  repeat(100){restored.develop(p.id,1.4f)};restored.attach(p.id);restored.pin(p.id,0xFFB7543D,.5f,.1f)
  val before=restored.placements.first().first();restored.move(p.id,400f,300f,30f);assertEquals(before,restored.placements.first().first())
  restored.unpin(p.id);restored.move(p.id,400f,300f,30f);assertEquals(400f,restored.placements.first().first().x,0f)
  restored.detach(p.id);assertEquals(1,restored.photos.first().size)
 }
 @Test fun interruptedJournalBeforeCameraDoesNotBlockRecovery()=runBlocking<Unit> {
  repo.replace(FilmFormat.CLASSIC,Appearance.COLOR)
  val id=UUID.randomUUID().toString();val journal=java.io.File(repo.originalFile(id).parent,"$id.pending")
  journal.writeText("{truncated")
  repo.recover()
  assertFalse(journal.exists());assertEquals(10,repo.active()!!.remaining);assertEquals(0,repo.photos.first().size)
 }
 @Test fun interruptedCaptureJournalIsRecoveredExactlyOnce()=runBlocking<Unit> {
  repo.replace(FilmFormat.CLASSIC,Appearance.COLOR)
  val id=UUID.randomUUID().toString();repo.prepareCapture(id,repo.active()!!)
  val b=Bitmap.createBitmap(80,80,Bitmap.Config.ARGB_8888)
  repo.originalFile(id).outputStream().use{b.compress(Bitmap.CompressFormat.JPEG,90,it)};b.recycle()
  repo.recover();repo.recover()
  assertEquals(9,repo.active()!!.remaining);assertEquals(1,repo.photos.first().size);assertFalse(repo.photos.first().first().processing)
 }
 @Test fun durableDatabaseRetainsProgressAfterClosing()=runBlocking<Unit> {
  val name="test-${UUID.randomUUID()}.db"
  var disk=Room.databaseBuilder(context,InstantDatabase::class.java,name).build()
  try {
   disk.dao().insertCartridge(CartridgeEntity("c","CLASSIC","COLOR"))
   disk.dao().insertPhoto(PhotoEntity("p","original", "film",123,"CLASSIC","COLOR","c",.43f,false))
   disk.close();disk=Room.databaseBuilder(context,InstantDatabase::class.java,name).build()
   assertEquals(.43f,disk.dao().photo("p")!!.progress,0f)
   assertEquals(10,disk.dao().active()!!.remaining)
  } finally {disk.close();context.deleteDatabase(name)}
 }
 @Test fun originalAndProcessedImagesAreIndependent()=runBlocking<Unit> {
  repo.replace(FilmFormat.ROUND,Appearance.WARM);val p=expose(true);val saved=db.dao().photo(p.id)!!
  assertTrue(java.io.File(saved.original).isFile);assertTrue(java.io.File(saved.processed).isFile);assertNotEquals(saved.original,saved.processed)
  val b=BitmapFactory.decodeFile(saved.processed);assertEquals(FilmFormat.ROUND.aspect,b.width.toFloat()/b.height,.01f);b.recycle()
 }
 @Test fun incompletePhotoCannotBeExported()=runBlocking<Unit> {
  repo.replace(FilmFormat.CLASSIC,Appearance.COLOR);val p=expose(true)
  assertTrue(runCatching {PhotoExport(context).save(db.dao().photo(p.id)!!)}.isFailure)
 }
 @Test fun completedPhotoExportsOnModernAndroid()=runBlocking<Unit> {
  Assume.assumeTrue(android.os.Build.VERSION.SDK_INT>=29)
  repo.replace(FilmFormat.CLASSIC,Appearance.COLOR);val p=expose(true);repeat(100){repo.develop(p.id,1.4f)}
  PhotoExport(context).save(db.dao().photo(p.id)!!)
  context.contentResolver.query(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,arrayOf(android.provider.MediaStore.Images.Media._ID),"${android.provider.MediaStore.Images.Media.DISPLAY_NAME} = ?",arrayOf("InstantRoom-${p.id}.jpg"),null).use { cursor -> assertTrue(cursor != null && cursor.moveToFirst()) }
 }
}
