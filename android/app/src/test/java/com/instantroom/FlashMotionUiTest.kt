package com.instantroom

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import com.instantroom.core.*
import com.instantroom.ui.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35]) @GraphicsMode(GraphicsMode.Mode.NATIVE)
class FlashMotionUiTest {
 @get:Rule val compose=createComposeRule()
 @Test fun boltRemainsVisibleAtIntermediateFramesInBothDirections(){
  var on by mutableStateOf(false)
  compose.mainClock.autoAdvance=false
  compose.setContent{InstantTheme(ThemeMode.LIGHT){FlashToggle(on,true,true){on=it}}}
  val control=compose.onNodeWithContentDescription("Rear camera flash")
  fun verify(name:String){
   compose.onAllNodesWithTag("flash-bolt",useUnmergedTree=true).assertCountEquals(1)
   val b=compose.onNodeWithTag("flash-bolt",useUnmergedTree=true).captureToImage().asAndroidBitmap()
   val ink=Color(b.getPixel((b.width*.52f).toInt(),(b.height*.35f).toInt()))
   assertTrue("Bolt disappeared at $name",contrast(ink,Color(0xFFFAF3E6))>1.8f)
   val dir=File("build/verification/flash").apply{mkdirs()};File(dir,"$name.png").outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
  }
  verify("off")
  control.performClick();compose.runOnIdle{assertTrue(on)};compose.mainClock.advanceTimeBy(32);compose.waitForIdle();control.assertIsOn()
  for(i in 1..6){compose.mainClock.advanceTimeBy(50);verify("on-$i")}
  control.performClick();compose.runOnIdle{assertFalse(on)};compose.mainClock.advanceTimeBy(32);compose.waitForIdle();control.assertIsOff()
  for(i in 1..6){compose.mainClock.advanceTimeBy(50);verify("off-$i")}
 }
 @Test fun rapidReversalsNeverRemoveBoltOrChangeBounds(){
  var on by mutableStateOf(false);var clicks=0;compose.mainClock.autoAdvance=false
  compose.setContent{InstantTheme(ThemeMode.DARK){FlashToggle(on,true,true){on=it;clicks++}}}
  val node=compose.onNodeWithContentDescription("Rear camera flash");val bounds=node.fetchSemanticsNode().boundsInRoot
  repeat(5){node.performClick();compose.mainClock.advanceTimeBy(65);assertEquals(bounds,node.fetchSemanticsNode().boundsInRoot);compose.onAllNodesWithTag("flash-bolt",useUnmergedTree=true).assertCountEquals(1)}
  compose.runOnIdle{assertEquals(5,clicks);assertTrue(on)}
 }
 @Test fun unavailableFlashRetainsIconButCannotToggle(){compose.setContent{InstantTheme{FlashToggle(false,false,true){error("Unsupported flash toggled")}}};compose.onNodeWithContentDescription("Rear camera flash").assertIsNotEnabled();compose.onNodeWithTag("flash-bolt",useUnmergedTree=true).assertExists()}
 @Test fun busyFlashIsLockedDuringCapture(){compose.setContent{InstantTheme{FlashToggle(true,true,false){error("Busy flash toggled")}}};compose.onNodeWithContentDescription("Rear camera flash").assertIsOn().assertIsNotEnabled()}
 @Test fun restoredFlashStateDoesNotEmitClickCallback(){var calls=0;compose.setContent{InstantTheme{FlashToggle(true,true,true){calls++}}};compose.runOnIdle{assertEquals(0,calls)};compose.onNodeWithContentDescription("Rear camera flash").assertIsOn()}
}
