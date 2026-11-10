package com.instantroom

import android.content.Context
import android.graphics.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import com.instantroom.core.*
import com.instantroom.data.*
import com.instantroom.ui.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35]) @GraphicsMode(GraphicsMode.Mode.NATIVE)
class BoothUiTest {
 @get:Rule val compose=createComposeRule()
 @Test fun timerMenuExposesOffAndAllThreeDurations(){var timer by mutableStateOf(SelfTimer.OFF);compose.setContent{InstantTheme{TimerControl(timer,true){timer=it}}};compose.onNodeWithContentDescription("Self-timer").performClick();listOf("Off","5 seconds","15 seconds","30 seconds").forEach{compose.onNodeWithText(it).assertExists()};compose.onNodeWithText("15 seconds").performClick();compose.runOnIdle{assertEquals(SelfTimer.FIFTEEN,timer)}}
 @Test fun layoutSelectionIsAvailableBeforeCaptureAndLockedDuringCapture(){var layout by mutableStateOf(BoothLayout.GRID);var enabled by mutableStateOf(true);compose.setContent{InstantTheme{BoothLayoutControl(layout,enabled){layout=it}}};compose.onNodeWithText("Vertical Strip 1×4").performClick();compose.runOnIdle{assertEquals(BoothLayout.STRIP,layout);enabled=false};compose.onNodeWithText("Collage 2×2").assertIsNotEnabled()}
 @Test fun countdownShowsLargeFinalDigitAndCurrentShot(){compose.setContent{InstantTheme{CountdownOverlay(3,2,true)}};compose.onNodeWithText("3").assertIsDisplayed();compose.onNodeWithText("PHOTO 2 OF 4").assertIsDisplayed()}
 @Test fun compositeMaskDoesNotDevelopOnTimerAndPreservesTallProportions(){
  val context=ApplicationProvider.getApplicationContext<Context>()
  val layout=BoothLayout.STRIP;val g=layout.geometry
  val file=File(context.cacheDir,"booth-mask.jpg");val b=Bitmap.createBitmap(300,(300/g.aspect).toInt(),Bitmap.Config.ARGB_8888);b.eraseColor(Color.CYAN);file.outputStream().use{b.compress(Bitmap.CompressFormat.JPEG,94,it)};b.recycle()
  var photo by mutableStateOf(PhotoEntity("mask","original",file.path,1,"CLASSIC","COLOR","c",0f,false,boothSessionId="s",boothLayout="STRIP"))
  compose.setContent{InstantTheme{FilmPrint(photo,Modifier.width(80.dp))}}
  compose.waitUntil(5000){compose.onRoot().fetchSemanticsNode().size.height>0}
  compose.waitUntil(5000){compose.onAllNodesWithTag("film-print-ready").fetchSemanticsNodes().isNotEmpty()}
  val first=compose.onRoot().captureToImage().asAndroidBitmap()
  compose.mainClock.advanceTimeBy(10000);compose.waitForIdle()
  val resting=compose.onRoot().captureToImage().asAndroidBitmap()
  assertTrue(first.sameAs(resting))
  val bounds=compose.onNodeWithContentDescription("Undeveloped Vertical Strip 1×4 photograph; shake to reveal").fetchSemanticsNode().boundsInRoot
  assertEquals(g.aspect,bounds.width/bounds.height,.01f)
  compose.runOnIdle{photo=photo.copy(progress=1f)};compose.waitForIdle()
  val developed=compose.onRoot().captureToImage().asAndroidBitmap();assertFalse(first.sameAs(developed))
  val dir=File("build/verification/booth").apply{mkdirs()};File(dir,"strip-developed-ui.png").outputStream().use{developed.compress(Bitmap.CompressFormat.PNG,100,it)}
 }
 @Test fun mainCameraKeepsBoothCountdownAndCancelVisibleOnSmallScreen()=kotlinx.coroutines.runBlocking<Unit>{
  val app=ApplicationProvider.getApplicationContext<android.app.Application>()
  val db=androidx.room.Room.inMemoryDatabaseBuilder(app,InstantDatabase::class.java).build()
  val repo=PhotoRepository(app,db,com.instantroom.imaging.FilmProcessor(Preferences(app)))
  repo.replace(FilmFormat.CLASSIC,Appearance.COLOR);val cart=repo.active()
  val vm=RoomViewModel(app,repo)
  try {
   compose.setContent{InstantTheme{Box(Modifier.width(320.dp).height(560.dp)){val cap by vm.capture.collectAsState();CameraScreen(vm,cart,cap,Settings(),true,{})}}}
   compose.onNodeWithText("Photo Booth",substring=false).performClick()
   compose.runOnIdle{vm.capture.value=CaptureUi(phase=CapturePhase.COUNTDOWN,countdown=3,shot=2,booth=true)}
   compose.onNodeWithText("3").assertIsDisplayed()
   compose.onNodeWithContentDescription("Cancel session").assertIsDisplayed()
   compose.onNodeWithContentDescription("Check2GO").assertIsDisplayed()
   val dir=File("build/verification/booth").apply{mkdirs()};File(dir,"camera-countdown-ui.png").outputStream().use{compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
  }finally{vm.viewModelScope.cancel();db.close()}
 }
}
