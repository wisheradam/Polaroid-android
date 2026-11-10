package com.instantroom

import android.app.Application
import android.graphics.Bitmap
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.instantroom.core.*
import com.instantroom.data.*
import com.instantroom.device.CapturePort
import com.instantroom.imaging.FilmProcessor
import com.instantroom.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class TimedCaptureIntegrationTest {
 private class Clock: CaptureClock {var time=0L;var blocked=false;override fun nowMs()=time;override suspend fun sleep(ms:Long){if(blocked)awaitCancellation();time+=ms;yield()}}
 private lateinit var db: InstantDatabase
 private lateinit var repo: PhotoRepository
 private lateinit var vm: RoomViewModel
 private val clock=Clock()
 private val requests=mutableListOf<Long>()
 private val lighting=mutableListOf<Pair<Boolean,Boolean>>()
 private var failAt=0
 private var cancelAt=0
 private val events=mutableListOf<String>()
 @Before fun setup()=runBlocking<Unit>{
  val app=ApplicationProvider.getApplicationContext<Application>()
  db=Room.inMemoryDatabaseBuilder(app,InstantDatabase::class.java).build();repo=PhotoRepository(app,db,FilmProcessor(Preferences(app)));repo.replace(FilmFormat.CLASSIC,Appearance.COLOR)
  val camera=object:CapturePort {
   override val hasFlash=true
   override suspend fun capture(file:File,flash:Boolean,onShutter:()->Unit)=captureWithLighting(file,flash,false,onShutter)
   override suspend fun captureWithLighting(file:File,flash:Boolean,screenFlash:Boolean,onShutter:()->Unit) {
    requests+=clock.time;lighting+=flash to screenFlash
    if(requests.size==failAt)error("Simulated camera failure")
    onShutter();val b=Bitmap.createBitmap(80,60,Bitmap.Config.ARGB_8888);file.outputStream().use{b.compress(Bitmap.CompressFormat.JPEG,95,it)};b.recycle()
    if(requests.size==cancelAt)vm.cancelCapture()
   }
  }
  val feedback=object:FeedbackPort {
   override fun charge(durationMs:Long){events+="charge"};override fun shutter(){events+="shutter"};override fun eject(){events+="eject"};override fun pin()=Unit;override fun stop()=Unit
   override fun exposed(){events+="exposed"};override fun countdown(finalSeconds:Boolean){events+=if(finalSeconds)"final-tick" else "tick"}
  }
  vm=RoomViewModel(app,repo,camera,feedback,clock);vm.foreground=true;vm.updateSettings(Settings())
  drainUntil{vm.initialized.value}
 }
 @After fun close(){vm.viewModelScope.cancel();db.close()}
 private suspend fun drainUntil(done:()->Boolean){withTimeout(20000){while(!done()){shadowOf(Looper.getMainLooper()).idle();delay(5)}}}
 private suspend fun finish(){drainUntil{!repo.captureLock.isLocked}}
 @Test fun immediateAndDelayedSingleCaptureIntegrateChargeFlashAndEjection()=runBlocking<Unit>{
  vm.updateSettings(Settings(timer=SelfTimer.FIVE));vm.takePicture(true);finish()
  assertEquals(listOf(5000L),requests);assertEquals(listOf(true to false),lighting);assertEquals(9,repo.active()!!.remaining);assertNotNull(vm.capture.value.photoId);assertEquals(1,events.count{it=="shutter"});assertEquals(1,events.count{it=="eject"});assertTrue(events.contains("final-tick"))
 }
 @Test fun boothCapturesExactlyFourWithFiveSecondSpacingAndSingleEjection()=runBlocking<Unit>{
  vm.startBooth(true);vm.startBooth(true);finish()
  assertEquals(listOf(5000L,10000L,15000L,20000L),requests);assertEquals(6,repo.active()!!.remaining);assertEquals(4,events.count{it=="shutter"});assertEquals(4,events.count{it=="exposed"});assertEquals(1,events.count{it=="eject"});val p=repo.photo(vm.capture.value.photoId!!);assertEquals(0f,p.progress,0f);assertEquals("GRID",p.boothLayout)
 }
 @Test fun frontBoothUsesScreenFlashAndSelectedStrip()=runBlocking<Unit>{
  vm.updateSettings(Settings(front=true,selfieLight=SelfieLightMode.BOTH,boothLayout=BoothLayout.STRIP));vm.startBooth(false);finish()
  assertEquals(List(4){false to true},lighting);assertTrue(vm.settings.value.front);assertEquals("STRIP",repo.photo(vm.capture.value.photoId!!).boothLayout)
 }
 @Test fun cancellationBeforeDeadlineConsumesNothingAndReleasesCaptureLock()=runBlocking<Unit>{
  clock.blocked=true;vm.updateSettings(Settings(timer=SelfTimer.THIRTY));vm.takePicture(true)
  shadowOf(Looper.getMainLooper()).idle();vm.cancelCapture();finish()
  assertTrue(requests.isEmpty());assertEquals(10,repo.active()!!.remaining);assertEquals(CapturePhase.IDLE,vm.capture.value.phase)
 }
 @Test fun cancellationDuringSecondCaptureRetainsBothSuccessfulShots()=runBlocking<Unit>{
  cancelAt=2;vm.startBooth(false);finish()
  assertEquals(2,requests.size);assertEquals(8,repo.active()!!.remaining);assertEquals(2,repo.photos.first().size);assertTrue(repo.photos.first().all{File(it.original).isFile});assertEquals(0,events.count{it=="eject"})
 }
 @Test fun failedSecondCaptureStopsSessionWithoutChargingForFailure()=runBlocking<Unit>{
  failAt=2;vm.startBooth(false);finish();assertEquals(2,requests.size);assertEquals(9,repo.active()!!.remaining);assertEquals(1,repo.photos.first().size);assertEquals(0,events.count{it=="eject"})
 }
 @Test fun cameraLossAndCameraSwitchGuardApplyDuringCountdown()=runBlocking<Unit>{
  clock.blocked=true;vm.startBooth(false);assertFalse(vm.switchCamera(true,true));assertFalse(vm.settings.value.front);vm.cancelCapture();finish();assertTrue(requests.isEmpty());assertEquals(10,repo.active()!!.remaining)
 }
 @Test fun offRetainsOriginalChargeSequenceWithoutAdditionalTimer()=runBlocking<Unit>{vm.takePicture(false);finish();assertEquals(listOf(650L),requests);assertNotNull(vm.capture.value.photoId)}
 @Test fun insufficientFilmCannotStartBoothOrReloadAutomatically()=runBlocking<Unit>{val id=repo.active()!!.id;repeat(7){db.dao().consume(id)};vm.startBooth(false);finish();assertTrue(requests.isEmpty());assertEquals(id,repo.active()!!.id);assertEquals(3,repo.active()!!.remaining);assertTrue(vm.message.value!!.contains("four exposures"))}
 @Test fun cancellingFourthCaptureStillRetainsFourShotsAndOneComposite()=runBlocking<Unit>{cancelAt=4;vm.startBooth(false);finish();assertEquals(4,requests.size);assertEquals(6,repo.active()!!.remaining);assertEquals(5,repo.photos.first().size);assertEquals(0f,repo.photos.first().single{it.boothLayout.isNotEmpty()}.progress,0f);assertEquals(0,events.count{it=="eject"})}
}
