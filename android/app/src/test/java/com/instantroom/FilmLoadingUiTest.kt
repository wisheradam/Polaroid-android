package com.instantroom

import android.app.Application
import android.os.Looper
import org.robolectric.Shadows.shadowOf
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.instantroom.core.*
import com.instantroom.data.*
import com.instantroom.imaging.FilmProcessor
import com.instantroom.ui.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35],qualifiers="w360dp-h800dp") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class FilmLoadingUiTest {
 @get:Rule val compose=createComposeRule()
 @Test fun intermediateLoadingPhasesKeepTheLedgerAndRequireClosure()=runBlocking<Unit> {
  val app=ApplicationProvider.getApplicationContext<Application>()
  val db=Room.inMemoryDatabaseBuilder(app,InstantDatabase::class.java).build()
  val repo=PhotoRepository(app,db,FilmProcessor(Preferences(app)))
  repo.replace(FilmFormat.CLASSIC,Appearance.COLOR)
  val initial=repo.active()!!
  repeat(3){db.dao().consume(initial.id)}
  val cartridge=repo.active()!!
  val vm=RoomViewModel(app,repo)
  var exited=false
  try {
   compose.setContent{InstantTheme{FilmLoadingScreen(vm,cartridge){exited=true}}}
   compose.onNodeWithText("Open film door").performScrollTo().performClick()
   compose.onNodeWithText("Replace this cartridge?").assertIsDisplayed()
   compose.onNodeWithText("Keep film").performClick()
   compose.onNodeWithText("Back").assertIsEnabled()
   compose.onNodeWithText("Open film door").performScrollTo().performClick()
   compose.onAllNodesWithText("Open film door").onLast().performClick()
   compose.onNodeWithText("Back").assertIsNotEnabled()
   compose.onNodeWithText("Remove cartridge ↓").performScrollTo().performClick()
   compose.onNodeWithText("Insert cartridge ↑").performScrollTo().performClick()
   compose.onNodeWithText("Close film door").performScrollTo().assertIsEnabled()
   assertFalse(exited)
   assertEquals(cartridge.id,repo.active()!!.id)
   assertEquals(7,repo.active()!!.remaining)
  }finally{vm.viewModelScope.cancel();db.close()}
 }
 @Test fun appearanceRemainsQueuedUntilTheActualLoadingSequenceCloses()=runBlocking<Unit>{
  val app=ApplicationProvider.getApplicationContext<Application>();val db=Room.inMemoryDatabaseBuilder(app,InstantDatabase::class.java).build();val repo=PhotoRepository(app,db,FilmProcessor(Preferences(app)));repo.replace(FilmFormat.CLASSIC,Appearance.COLOR);val cartridge=repo.active()!!;val vm=RoomViewModel(app,repo);vm.updateSettings(Settings());var exited=false
  try{
   compose.setContent{InstantTheme{FilmLoadingScreen(vm,cartridge){exited=true}}}
   compose.onNodeWithText("Open film door").performScrollTo().performClick();compose.onAllNodesWithText("Open film door").onLast().performClick()
   compose.runOnIdle{vm.requestCameraAppearance(CameraStyle.OLD_FASION,CameraColor.OLIVE);assertEquals(CameraStyle.ONE_AND_ONLY,vm.settings.value.cameraStyle)}
   compose.onNodeWithText("Remove cartridge ↓").performScrollTo().performClick();compose.onNodeWithText("Insert cartridge ↑").performScrollTo().performClick()
   compose.runOnIdle{assertEquals(CameraStyle.ONE_AND_ONLY,vm.settings.value.cameraStyle)}
   compose.onNodeWithText("Close film door").performScrollTo().performClick();compose.waitUntil(20000){shadowOf(Looper.getMainLooper()).idle();exited || vm.message.value?.startsWith("Appearance queued")==false};assertTrue("Closure failed: ${vm.message.value}",exited)
   assertEquals(CameraStyle.OLD_FASION,vm.settings.value.cameraStyle);assertEquals(CameraColor.OLIVE,vm.settings.value.cameraColor);assertNull(vm.pendingAppearance.value);assertEquals(10,repo.active()!!.remaining);assertNotEquals(cartridge.id,repo.active()!!.id)
  }finally{vm.viewModelScope.cancel();db.close()}
 }

}
