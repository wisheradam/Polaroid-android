package com.instantroom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import java.io.File
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.instantroom.core.*
import com.instantroom.data.*
import com.instantroom.ui.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35],qualifiers="w480dp-h1000dp") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class AnalogCameraUiTest {
 @get:Rule val compose=createComposeRule()
 private fun render(remaining: Int=10, countdown: Int=0, height: Int=800) {
  compose.setContent { InstantTheme {
   Box(Modifier.width(360.dp).height(height.dp)) {
    OneAndOnlyCamera(Settings(),CartridgeEntity("c","CLASSIC","COLOR",remaining,true),
     if(countdown>0)CaptureUi(CapturePhase.COUNTDOWN,countdown=countdown)else CaptureUi(),true,true,true,true,false,null,
     {},{},{},{},{},{},{},{},{},{}) {Box(Modifier.fillMaxSize().background(Color(0xFF547C65)),contentAlignment=androidx.compose.ui.Alignment.Center){CountdownOverlay(countdown,0,false)}}
   }
  }}
 }
 @Test fun responsiveCameraRendersAtPhoneSizesAndLargeText() {
  var width by mutableStateOf(360)
  var height by mutableStateOf(800)
  var scale by mutableStateOf(1f)
  compose.setContent { InstantTheme {
   val density=LocalDensity.current
   CompositionLocalProvider(LocalDensity provides Density(density.density,scale)) {
    Box(Modifier.width(width.dp).height(height.dp)) {
     OneAndOnlyCamera(Settings(cameraColor=CameraColor.SPECTRUM),CartridgeEntity("c","CLASSIC","COLOR",10,true),CaptureUi(),true,true,true,true,false,null,{},{},{},{},{},{},{},{},{},{}) {
      Box(Modifier.fillMaxSize().background(Color(0xFF547C65)))
     }
    }
   }
  }}
  for((w,h,font) in listOf(Triple(360,800,1f),Triple(393,852,1f),Triple(412,915,1f),Triple(360,800,2f))) {
   compose.runOnIdle{width=w;height=h;scale=font}
   compose.onNodeWithContentDescription("Check2GO").assertIsDisplayed()
   val dir=File("build/verification/design-system").apply{mkdirs()}
   File(dir,"camera-${w}x${h}-${font}.png").outputStream().use {
    compose.onNodeWithTag("one-and-only-camera").captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
   }
   compose.onNodeWithContentDescription("Settings").performScrollTo().assertIsDisplayed()
   compose.onNodeWithContentDescription("Gallery").assertIsDisplayed()
   val target=compose.onNodeWithContentDescription("Settings").fetchSemanticsNode().boundsInRoot
   assertTrue("Settings must expose its full touch target",target.height>=with(compose.density){76.dp.toPx()}-.5f)
   if(font>1.4f)File("build/verification/design-system/camera-large-text-navigation.png").outputStream().use {
    compose.onNodeWithTag("one-and-only-camera").captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
   }
  }
 }
 @Test fun emptyCartridgeDisablesCaptureButKeepsReplacementAccessible() {
  render(0)
  compose.onNodeWithContentDescription("Take photograph").assertIsNotEnabled()
  compose.onNodeWithContentDescription("Film: 0 shots remaining. Replace cartridge").assertIsEnabled()
 }
 @Test fun timerMenuUpdatesThroughTheActualControlCallback() {
  var selected=SelfTimer.OFF
  compose.setContent { InstantTheme {Box(Modifier.width(360.dp).height(800.dp)) {
   OneAndOnlyCamera(Settings(),null,CaptureUi(),true,true,true,true,false,null,{},{},{},{},{},{},{selected=it},{},{},{}) {}
  }}}
  compose.onNodeWithContentDescription("Self-timer").performClick()
  compose.onNodeWithText("15 seconds").performClick()
  compose.runOnIdle{assertEquals(SelfTimer.FIFTEEN,selected)}
 }
 @Test fun compactCameraKeepsBrandAndCountdownInThePreview() {
  render(countdown=3,height=560)
  compose.onNodeWithContentDescription("Check2GO").assertIsDisplayed()
  compose.onNodeWithText("3").assertIsDisplayed()
  val bounds=compose.onNodeWithTag("analog-live-aperture").fetchSemanticsNode().boundsInRoot
  assertEquals(bounds.width,bounds.height,.1f)
 }
}
