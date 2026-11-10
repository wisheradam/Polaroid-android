package com.instantroom

import android.app.Application
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
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class CameraSwitchIntegrationTest {
 private val camera=object:CapturePort {override val hasFlash=false;override suspend fun capture(file:File,flash:Boolean,onShutter:()->Unit){error("Switching must not capture")}}
 private val feedback=object:FeedbackPort {override fun charge(durationMs:Long)=Unit;override fun shutter()=Unit;override fun eject()=Unit;override fun pin()=Unit;override fun stop()=Unit}
 @Test fun switchingBothDirectionsPreservesCartridgeAndArchive()=runBlocking<Unit> {
  val app=ApplicationProvider.getApplicationContext<Application>();val db=Room.inMemoryDatabaseBuilder(app,InstantDatabase::class.java).build()
  val repo=PhotoRepository(app,db,FilmProcessor(Preferences(app)));repo.replace(FilmFormat.SPECTRA,Appearance.WARM)
  val cart=repo.active()!!
  db.dao().insertPhoto(PhotoEntity("saved","original","processed",42,"SPECTRA","WARM",cart.id,.43f,false))
  val vm=RoomViewModel(app,repo,camera,feedback)
  try {
   vm.updateSettings(vm.settings.value.copy(front=false))
   val photoBefore=repo.photos.first()
   assertTrue(vm.switchCamera(true,true));assertTrue(vm.settings.value.front)
   assertTrue(vm.switchCamera(true,true));assertFalse(vm.settings.value.front)
   assertEquals(cart,repo.active());assertEquals(photoBefore,repo.photos.first())
  } finally {vm.viewModelScope.cancel();db.close()}
 }
 @Test fun captureAndEjectionAndCaptureLockRejectSwitching()=runBlocking<Unit> {
  val app=ApplicationProvider.getApplicationContext<Application>();val db=Room.inMemoryDatabaseBuilder(app,InstantDatabase::class.java).build();val repo=PhotoRepository(app,db,FilmProcessor(Preferences(app)));val vm=RoomViewModel(app,repo,camera,feedback)
  try {
   vm.updateSettings(vm.settings.value.copy(front=false))
   for(phase in CapturePhase.entries.filter{it!=CapturePhase.IDLE}){vm.capture.value=CaptureUi(phase);assertFalse(vm.switchCamera(true,true));assertFalse(vm.settings.value.front)}
   vm.capture.value=CaptureUi();repo.captureLock.lock();try{assertFalse(vm.switchCamera(true,true))}finally{repo.captureLock.unlock()}
  }finally{vm.viewModelScope.cancel();db.close()}
 }
 @Test fun missingCameraLeavesSelectionUnchanged()=runBlocking<Unit> {
  val app=ApplicationProvider.getApplicationContext<Application>();val db=Room.inMemoryDatabaseBuilder(app,InstantDatabase::class.java).build();val repo=PhotoRepository(app,db,FilmProcessor(Preferences(app)));val vm=RoomViewModel(app,repo,camera,feedback)
  try{vm.updateSettings(vm.settings.value.copy(front=false));assertFalse(vm.switchCamera(false,true));assertFalse(vm.settings.value.front)}finally{vm.viewModelScope.cancel();db.close()}
 }
}
