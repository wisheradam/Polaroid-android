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
class AppearanceSwitchIntegrationTest {
 private class Clock: CaptureClock {var time=0L;var block=true;override fun nowMs()=time;override suspend fun sleep(ms:Long){if(block)awaitCancellation();time+=ms;yield()}}
 private lateinit var db: InstantDatabase;private lateinit var repo: PhotoRepository;private lateinit var vm: RoomViewModel
 private val clock=Clock();private var captures=0;private var ejects=0;private var queueAtEjection=false;private var shutterObserved=false;private var shutterFeedback=0
 @Before fun setup()=runBlocking<Unit>{
  val app=ApplicationProvider.getApplicationContext<Application>();db=Room.inMemoryDatabaseBuilder(app,InstantDatabase::class.java).build();repo=PhotoRepository(app,db,FilmProcessor(Preferences(app)));repo.replace(FilmFormat.SPECTRA,Appearance.MONO)
  db.dao().insertPhoto(PhotoEntity("developing","original","print",1,"SPECTRA","MONO",repo.active()!!.id,.4f,false))
  val camera=object:CapturePort {override val hasFlash=true;override suspend fun capture(file:File,flash:Boolean,onShutter:()->Unit){captures++;assertFalse(vm.capture.value.shutterFired);onShutter();shutterObserved=vm.capture.value.shutterFired;val b=Bitmap.createBitmap(80,60,Bitmap.Config.ARGB_8888);file.outputStream().use{b.compress(Bitmap.CompressFormat.JPEG,95,it)};b.recycle()}}
  val feedback=object:FeedbackPort {override fun charge(durationMs:Long)=Unit;override fun shutter(){shutterFeedback++};override fun pin()=Unit;override fun stop()=Unit;override fun eject(){ejects++;if(queueAtEjection){vm.requestCameraAppearance(CameraStyle.OLD_FASION,CameraColor.OLIVE);assertEquals(CameraStyle.ONE_AND_ONLY,vm.settings.value.cameraStyle)}}}
  vm=RoomViewModel(app,repo,camera,feedback,clock);vm.foreground=true;vm.updateSettings(Settings(front=true,theme=ThemeMode.DARK,timer=SelfTimer.FIFTEEN,captionFont=CaptionFont.HISTORIC,selfieLight=SelfieLightMode.BOTH,boothLayout=BoothLayout.STRIP));drain{vm.initialized.value}
 }
 @After fun close(){vm.viewModelScope.cancel();db.close()}
 private suspend fun drain(done:()->Boolean){withTimeout(20000){while(!done()){shadowOf(Looper.getMainLooper()).idle();delay(5)}}}
 @Test fun immediateSelectionPreservesCartridgePhotosAndEveryIndependentPreference()=runBlocking<Unit>{val before=vm.settings.value;val cart=repo.active();val photos=repo.photos.first();vm.requestCameraAppearance(CameraStyle.OLD_FASION,CameraColor.OLIVE);assertEquals(before.copy(cameraStyle=CameraStyle.OLD_FASION,cameraColor=CameraColor.OLIVE),vm.settings.value);assertEquals(cart,repo.active());assertEquals(photos,repo.photos.first());assertEquals(vm.settings.value,Preferences(ApplicationProvider.getApplicationContext()).state.value);assertEquals(0,captures)}
 @Test fun allBusyCapturePhasesQueueTheLatestChoice(){for(phase in CapturePhase.entries.filter{it!=CapturePhase.IDLE}){vm.capture.value=CaptureUi(phase);vm.requestCameraAppearance(CameraStyle.OLD_FASION,CameraColor.PINK);assertEquals(CameraStyle.ONE_AND_ONLY,vm.settings.value.cameraStyle);assertEquals(CameraColor.BLACK,vm.pendingAppearance.value!!.color);vm.requestCameraAppearance(CameraStyle.INSTA_STYLE,CameraColor.MINT);assertEquals(CameraStyle.ONE_AND_ONLY,vm.settings.value.cameraStyle);vm.cancelPendingAppearance()};assertEquals(0,captures)}
 @Test fun deferredAppearanceNeverReplaysStaleTimerOrThemePreferences(){assertTrue(repo.captureLock.tryLock());vm.requestCameraAppearance(CameraStyle.OLD_FASION,CameraColor.BRASS);vm.updateSettings(vm.settings.value.copy(timer=SelfTimer.THIRTY,theme=ThemeMode.LIGHT));assertEquals(CameraStyle.ONE_AND_ONLY,vm.settings.value.cameraStyle);repo.captureLock.unlock();vm.setFilmLoadingPhase(LoadingPhase.CLOSED);assertEquals(CameraColor.BRASS,vm.settings.value.cameraColor);assertEquals(SelfTimer.THIRTY,vm.settings.value.timer);assertEquals(ThemeMode.LIGHT,vm.settings.value.theme)}
 @Test fun incompleteLoadingBlocksCaptureAndAppearanceUntilClosure()=runBlocking<Unit>{for(phase in listOf(LoadingPhase.OPEN,LoadingPhase.REMOVED,LoadingPhase.INSERTED)){vm.setFilmLoadingPhase(phase);vm.requestCameraAppearance(CameraStyle.OLD_FASION,CameraColor.IVORY);vm.takePicture(false);assertEquals(CameraStyle.ONE_AND_ONLY,vm.settings.value.cameraStyle);assertFalse(repo.captureLock.isLocked);assertEquals(10,repo.active()!!.remaining)};vm.setFilmLoadingPhase(LoadingPhase.CLOSED);assertEquals(CameraColor.IVORY,vm.settings.value.cameraColor);assertNull(vm.pendingAppearance.value);assertEquals(0,captures)}
 @Test fun queuedChoiceCanBeCancelledOrRevertedToTheCommittedAppearance(){vm.setFilmLoadingPhase(LoadingPhase.OPEN);vm.requestCameraAppearance(CameraStyle.OLD_FASION,CameraColor.BLACK);vm.cancelPendingAppearance();assertNull(vm.pendingAppearance.value);vm.requestCameraAppearance(CameraStyle.INSTA_STYLE,CameraColor.PINK);vm.requestCameraAppearance(CameraStyle.ONE_AND_ONLY,CameraColor.WHITE);assertNull(vm.pendingAppearance.value);vm.setFilmLoadingPhase(LoadingPhase.CLOSED);assertEquals(CameraStyle.ONE_AND_ONLY,vm.settings.value.cameraStyle)}
 @Test fun timerCancellationSafelyAppliesQueuedAppearanceWithoutSpendingFilm()=runBlocking<Unit>{vm.takePicture(false);drain{repo.captureLock.isLocked && vm.capture.value.phase!=CapturePhase.IDLE};vm.requestCameraAppearance(CameraStyle.OLD_FASION,CameraColor.OLIVE);assertEquals(CameraStyle.ONE_AND_ONLY,vm.settings.value.cameraStyle);vm.cancelCapture();drain{!repo.captureLock.isLocked && vm.pendingAppearance.value==null};assertEquals(CameraColor.OLIVE,vm.settings.value.cameraColor);assertEquals(10,repo.active()!!.remaining);assertEquals(0,captures)}
 @Test fun queuedAppearanceWaitsForTheCompleteMechanicalEjection()=runBlocking<Unit>{clock.block=false;queueAtEjection=true;vm.updateSettings(vm.settings.value.copy(timer=SelfTimer.OFF));vm.takePicture(false);drain{!repo.captureLock.isLocked};assertEquals(1,captures);assertEquals(1,ejects);assertTrue(clock.time>=1800);assertEquals(CameraStyle.OLD_FASION,vm.settings.value.cameraStyle);assertEquals(9,repo.active()!!.remaining);assertEquals(0f,repo.photo(vm.capture.value.photoId!!).progress,0f);assertEquals(.4f,repo.photo("developing").progress,0f)}
 @Test fun cameraStartCallbackMarksFiringWithoutAddingFeedbackOrSpendingExtraFilm()=runBlocking<Unit>{clock.block=false;vm.updateSettings(vm.settings.value.copy(timer=SelfTimer.OFF));vm.takePicture(false);drain{!repo.captureLock.isLocked};assertTrue(shutterObserved);assertEquals(1,shutterFeedback);assertEquals(1,captures);assertEquals(9,repo.active()!!.remaining)}
 @Test fun allThirteenChoicesPersistAndNormalizeUsingTheLegacyStorageIds(){for(style in CameraStyle.entries)for(color in style.colors){vm.requestCameraAppearance(style,color);val restored=Preferences(ApplicationProvider.getApplicationContext()).state.value;assertEquals(style,restored.cameraStyle);assertEquals(color,restored.cameraColor)};vm.requestCameraAppearance(CameraStyle.OLD_FASION,CameraColor.PINK);assertEquals(CameraColor.BLACK,vm.settings.value.cameraColor);assertEquals("OLD_FASION",vm.settings.value.cameraStyle.name)}
}
