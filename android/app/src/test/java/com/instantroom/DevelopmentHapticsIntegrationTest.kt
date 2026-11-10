package com.instantroom

import android.app.Application
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.instantroom.core.*
import com.instantroom.data.*
import com.instantroom.device.CapturePort
import com.instantroom.imaging.FilmProcessor
import com.instantroom.ui.RoomViewModel
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class DevelopmentHapticsIntegrationTest {
 private lateinit var db: InstantDatabase
 private lateinit var repo: PhotoRepository
 private lateinit var vm: RoomViewModel
 private var pulses=0
 private var stops=0
 @Before fun setup()=runBlocking<Unit> {
  val app=ApplicationProvider.getApplicationContext<Application>()
  db=Room.inMemoryDatabaseBuilder(app,InstantDatabase::class.java).build();repo=PhotoRepository(app,db,FilmProcessor(Preferences(app)))
  val camera=object:CapturePort{override val hasFlash=false;override suspend fun capture(file:File,flash:Boolean,onShutter:()->Unit)=Unit}
  val feedback=object:FeedbackPort {
   override fun charge(durationMs:Long)=Unit;override fun shutter()=Unit;override fun eject()=Unit;override fun pin()=Unit;override fun stop()=Unit
   override fun develop(){pulses++};override fun stopDevelopment(){stops++}
  }
  db.dao().insertPhoto(PhotoEntity("p","original","film",1,"CLASSIC","COLOR","c",.3f,false))
  vm=RoomViewModel(app,repo,camera,feedback);vm.foreground=true;vm.updateSettings(Settings(haptics=true));vm.startDevelopment("p")
 }
 @After fun close(){vm.viewModelScope.cancel();db.close()}
 private suspend fun drain(){repeat(30){shadowOf(Looper.getMainLooper()).idle();delay(10)}}
 @Test fun acceptedProgressPulsesButRestingAndInvalidInputDoNot()=runBlocking<Unit> {
  vm.develop("p",0f);drain();assertEquals(0,pulses);assertEquals(.3f,repo.photo("p").progress,0f)
  vm.develop("p",1f);drain();assertEquals(1,pulses);val progress=repo.photo("p").progress
  drain();assertEquals(1,pulses);assertEquals(progress,repo.photo("p").progress,0f)
 }
 @Test fun hapticsSettingAndBackgroundAndCompletedStateAreRespected()=runBlocking<Unit> {
  vm.updateSettings(vm.settings.value.copy(haptics=false));vm.develop("p",1f);drain();assertEquals(0,pulses);assertTrue(repo.photo("p").progress>.3f)
  vm.updateSettings(vm.settings.value.copy(haptics=true));vm.foreground=false;val before=repo.photo("p").progress;vm.develop("p",1f);drain();assertEquals(before,repo.photo("p").progress,0f)
  vm.foreground=true;db.dao().progress("p",1f);vm.develop("p",1f);drain();assertEquals(0,pulses)
 }
 @Test fun pausedOrDisposedSessionRejectsQueuedEventsAndCancelsFeedback()=runBlocking<Unit> {
  vm.stopDevelopment();assertTrue(stops>0);vm.develop("p",1f);drain();assertEquals(0,pulses);assertEquals(.3f,repo.photo("p").progress,0f)
 }
 @Test fun manualAccessibilityDevelopmentHasNoShakePulse()=runBlocking<Unit> {
  vm.updateSettings(vm.settings.value.copy(accessible=true));vm.accessibleDevelop("p");drain();assertTrue(repo.photo("p").progress>.3f);assertEquals(0,pulses)
 }
}
