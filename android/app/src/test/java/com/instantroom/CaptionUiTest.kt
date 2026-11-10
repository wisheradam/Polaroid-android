package com.instantroom

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.instantroom.core.*
import com.instantroom.data.PhotoEntity
import com.instantroom.ui.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35]) @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CaptionUiTest {
 @get:Rule val compose=createComposeRule()
 private fun photo(caption: String=""): PhotoEntity {
  val context=androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
  val preview=File(context.cacheDir,"caption-ui-preview.jpg")
  val b=android.graphics.Bitmap.createBitmap(300,365,android.graphics.Bitmap.Config.ARGB_8888)
  b.eraseColor(android.graphics.Color.rgb(247,242,226))
  preview.outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.JPEG,94,it)};b.recycle()
  return PhotoEntity("p","original",preview.path,123,"CLASSIC","COLOR","c",1f,false,caption=caption)
 }
 @Test fun logoIsLegibleWithCameraSwitchInBothThemesAtSmallWidth() {
  var theme by mutableStateOf(ThemeMode.LIGHT)
  compose.setContent {InstantTheme(theme){Row(Modifier.width(280.dp)){BrandLogo(Modifier.weight(1f));CameraSwitchControl(false,true){}}}}
  for(mode in listOf(ThemeMode.LIGHT,ThemeMode.DARK)) {
   compose.runOnIdle{theme=mode}
   compose.onNodeWithContentDescription("Check2GO").assertIsDisplayed()
   compose.onNodeWithText("Instant Room").assertDoesNotExist()
   val logo=compose.onNodeWithTag("camera-brand-logo").fetchSemanticsNode().boundsInRoot
   val switch=compose.onNodeWithContentDescription("Switch to front camera").fetchSemanticsNode().boundsInRoot
   assertTrue(logo.right<=switch.left)
   val dir=File("build/verification/ui").apply{mkdirs()}
   File(dir,"logo-${mode.name}.png").outputStream().use{compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
  }
 }
 @Test fun userCanEnterCaptionSelectFontAndSave() {
  var result="";var font=CaptionFont.SOL_KOL
  compose.setContent{InstantTheme{CaptionEditor(photo(),{}, {text,style,done->result=text;font=style;done(true)})}}
  compose.onNodeWithText("Your handwritten note").performTextInput("A memory by the sea")
  compose.onNodeWithText("Denistina",substring=false).performScrollTo().performClick()
  compose.onNodeWithText("Save caption").performClick()
  compose.runOnIdle{assertEquals("A memory by the sea",result);assertEquals(CaptionFont.DENISTINA,font)}
 }
 @Test fun existingCaptionCanBeEditedOrRemoved() {
  var result="unchanged"
  compose.setContent{InstantTheme{CaptionEditor(photo("Old note"),{}, {text,_,done->result=text;done(true)})}}
  compose.onNodeWithText("Old note").performTextReplacement("New note")
  compose.onNodeWithText("Save caption").performClick();compose.runOnIdle{assertEquals("New note",result)}
  compose.onNodeWithText("Remove caption").performClick();compose.runOnIdle{assertEquals("",result)}
 }
 @Test fun overlongCaptionDisablesSaveAndShowsExplanation() {
  compose.setContent{InstantTheme{CaptionEditor(photo(),{}, {_,_,_->error("Invalid caption saved")})}}
  compose.onNodeWithText("Your handwritten note").performTextInput("a".repeat(81))
  compose.onNodeWithText("Save caption").assertIsNotEnabled()
  compose.onNodeWithText("Use at most 80 characters").assertExists()
 }
}
