package com.instantroom

import android.app.Application
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.instantroom.core.*
import com.instantroom.data.*
import com.instantroom.device.CapturePort
import com.instantroom.imaging.FilmProcessor
import com.instantroom.ui.RoomViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

/** Fake hardware verifies coordination, not physical sound/flash/vibration. */
@RunWith(AndroidJUnit4::class)
class ExposureWorkflowTest {
 private class FakeCamera(var fail: Boolean=false): CapturePort {
  override val hasFlash=true
  var calls=0
  override suspend fun capture(file: File,flash: Boolean,onShutter: ()->Unit) {
   calls++;if(fail)error("Simulated camera failure")
   onShutter()
   val image=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888)
   file.outputStream().use{image.compress(Bitmap.CompressFormat.JPEG,90,it)};image.recycle()
  }
 }
 private class Trace: FeedbackPort {
  val events=java.util.Collections.synchronizedList(mutableListOf<String>())
  var ejectAt=0L
  override fun charge(durationMs:Long){events.add("charge")}
  override fun shutter(){events.add("shutter")}
  override fun eject(){events.add("eject");ejectAt=android.os.SystemClock.elapsedRealtime()}
  override fun pin(){events.add("pin")}
  override fun stop()=Unit
 }
 @Test fun captureIsSingleFlightAndEjectionUsesTheSameClock()=runBlocking<Unit> {
  val app=ApplicationProvider.getApplicationContext<Application>()
  val db=Room.inMemoryDatabaseBuilder(app,InstantDatabase::class.java).build()
  try {
   val repo=PhotoRepository(app,db,FilmProcessor(Preferences(app)))
   repo.replace(FilmFormat.GO,Appearance.COLOR)
   val camera=FakeCamera();val trace=Trace();val vm=RoomViewModel(app,repo,camera,trace)
   withTimeout(10000){vm.initialized.first{it}}
   vm.foreground=true;vm.takePicture(false);vm.takePicture(false)
   withTimeout(15000){vm.capture.first{it.phase==CapturePhase.IDLE && it.photoId!=null}}
   assertEquals(1,camera.calls);assertEquals(9,repo.active()!!.remaining)
   assertEquals(listOf("charge","shutter","eject"),trace.events.toList())
   assertTrue(android.os.SystemClock.elapsedRealtime()-trace.ejectAt>=EjectionTimeline.durationMs)
  } finally{db.close()}
 }
 @Test fun failingCameraLeavesFilmAndArchiveUntouched()=runBlocking<Unit> {
  val app=ApplicationProvider.getApplicationContext<Application>();val db=Room.inMemoryDatabaseBuilder(app,InstantDatabase::class.java).build()
  try {
   val repo=PhotoRepository(app,db,FilmProcessor(Preferences(app)));repo.replace(FilmFormat.CLASSIC,Appearance.COLOR)
   val vm=RoomViewModel(app,repo,FakeCamera(true),Trace());withTimeout(10000){vm.initialized.first{it}}
   vm.takePicture(false);withTimeout(10000){vm.message.first{it!=null}}
   assertEquals(10,repo.active()!!.remaining);assertEquals(0,repo.photos.first().size)
  }finally{db.close()}
 }
}
