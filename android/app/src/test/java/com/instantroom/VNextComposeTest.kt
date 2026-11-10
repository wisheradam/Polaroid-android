package com.instantroom

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.instantroom.core.*
import com.instantroom.data.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import com.instantroom.ui.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class VNextComposeTest {
 @get:Rule val compose=createComposeRule()
 @Test fun cameraSwitchSupportsBothDirections(){var front by mutableStateOf(false);compose.setContent{InstantTheme{CameraSwitchControl(front,true){front=!front}}};compose.onNodeWithContentDescription("Switch to front camera").performClick();compose.onNodeWithContentDescription("Switch to rear camera").assertIsDisplayed().performClick();compose.onNodeWithContentDescription("Switch to front camera").assertIsDisplayed()}
 @Test fun disabledSwitchCannotBeUsed(){compose.setContent{InstantTheme{CameraSwitchControl(false,false){error("Should not switch")}}};compose.onNodeWithContentDescription("Switch to front camera").assertIsNotEnabled()}
 @Test fun selfieLightingOptionsAreContextual(){var mode by mutableStateOf(SelfieLightMode.OFF);compose.setContent{InstantTheme{SelfieLightControl(mode,true){mode=it}}};compose.onNodeWithText("Selfie light: Off ▾").performClick();compose.onNodeWithText("Edge light",substring=false).performClick();compose.runOnIdle{assertEquals(SelfieLightMode.EDGE,mode)}}
 @Test fun inactiveFlashOverlayDoesNotBlockCameraControls(){
  var clicked=false
  compose.setContent{InstantTheme{
   val context=LocalContext.current
   val host=remember{SelfieLightingHost(context,context.activity().window)}
   Box { Button(onClick={clicked=true}){Text("Continue")};SelfieLightingOverlay(host,false,Settings(),false) }
  }}
  compose.onNodeWithText("Continue").performClick();compose.runOnIdle{assertTrue(clicked)}
 }
 @Test fun themeSelectionUpdatesTheWholeColorScheme(){
  var s by mutableStateOf(Settings(theme=ThemeMode.DARK));var luminance=0f
  compose.setContent{InstantTheme(s.theme){val surface=MaterialTheme.colorScheme.surface;SideEffect{luminance=surface.luminance()};SettingsScreen(s){s=it}}}
  compose.runOnIdle{assertTrue(luminance<.1f)}
  compose.onNodeWithText("Light",substring=false).performClick()
  compose.runOnIdle{assertEquals(ThemeMode.LIGHT,s.theme);assertTrue(luminance>.8f)}
 }
 @Test fun shareMenuExposesSocialAndFallbackTargets(){compose.setContent{InstantTheme{ShareActions{}}};compose.onNodeWithText("Share").performClick();compose.onNodeWithText("Instagram").assertIsDisplayed();compose.onNodeWithText("Facebook").assertIsDisplayed();compose.onNodeWithText("Other apps").assertIsDisplayed()}
}
