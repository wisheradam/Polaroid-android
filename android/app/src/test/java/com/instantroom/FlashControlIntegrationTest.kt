package com.instantroom

import android.app.Application
import android.content.Context
import android.os.VibratorManager
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.instantroom.core.*
import com.instantroom.data.*
import com.instantroom.device.*
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
class FlashControlIntegrationTest {
 private lateinit var db: InstantDatabase
 private lateinit var repo: PhotoRepository
 private lateinit var vm: RoomViewModel
 private var clicks=0
 @Before fun setup()=runBlocking<Unit>{
  val app=ApplicationProvider.getApplicationContext<Application>();db=Room.inMemoryDatabaseBuilder(app,InstantDatabase::class.java).build();repo=PhotoRepository(app,db,FilmProcessor(Preferences(app)));repo.replace(FilmFormat.GO,Appearance.WARM)
  db.dao().insertPhoto(PhotoEntity("p","original","film",1,"GO","WARM",repo.active()!!.id,.4f,false))
  val camera=object:CapturePort{override val hasFlash=true;override suspend fun capture(file:File,flash:Boolean,onShutter:()->Unit){error("A toggle must not capture")}}
  val feedback=object:FeedbackPort {override fun charge(durationMs:Long)=Unit;override fun shutter()=Unit;override fun eject()=Unit;override fun pin()=Unit;override fun stop()=Unit;override fun controlClick(){clicks++}}
  vm=RoomViewModel(app,repo,camera,feedback);vm.updateSettings(Settings())
 }
 @After fun close(){vm.viewModelScope.cancel();db.close()}
 @Test fun togglesBothDirectionsOnceAndPreservesFilmArchiveAndDevelopment()=runBlocking<Unit>{val cart=repo.active();val photos=repo.photos.first();assertTrue(vm.setRearFlash(true,true));assertTrue(vm.settings.value.rearFlash);assertFalse(vm.setRearFlash(true,true));assertTrue(vm.setRearFlash(false,true));assertEquals(2,clicks);assertEquals(cart,repo.active());assertEquals(photos,repo.photos.first())}
 @Test fun unavailableOrBusyFlashDoesNotToggleOrVibrate(){assertFalse(vm.setRearFlash(true,false));for(phase in CapturePhase.entries.filter{it!=CapturePhase.IDLE}){vm.capture.value=CaptureUi(phase);assertFalse(vm.setRearFlash(true,true))};assertEquals(0,clicks);assertFalse(vm.settings.value.rearFlash)}
 @Test fun soundAndHapticsAreIndependent(){vm.updateSettings(vm.settings.value.copy(haptics=false));assertTrue(vm.setRearFlash(true,true));assertEquals(0,clicks);vm.updateSettings(vm.settings.value.copy(haptics=true,sound=false));assertTrue(vm.setRearFlash(false,true));assertEquals(1,clicks)}
 @Test fun styleChangeDoesNotInvokeFlashFeedbackOrAlterCaptureState(){val before=vm.capture.value;vm.updateSettings(vm.settings.value.copy(cameraStyle=CameraStyle.INSTA_STYLE,cameraColor=CameraColor.MINT));assertEquals(0,clicks);assertEquals(before,vm.capture.value);assertFalse(vm.settings.value.rearFlash)}
 @Test fun physicalFeedbackAdapterHonorsGlobalHapticsPreference(){val c=ApplicationProvider.getApplicationContext<Context>();val vibrator=(c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator;val shadow=shadowOf(vibrator);shadow.setHasVibrator(true);val p=Preferences(c);p.set(Settings(haptics=true,sound=false));val f=Feedback(c,p);try{f.controlClick();assertTrue(shadow.isVibrating);f.stop();p.set(p.state.value.copy(haptics=false));f.controlClick();assertFalse(shadow.isVibrating)}finally{f.release()}}
}
