package com.instantroom

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
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
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35]) @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CameraStyleUiTest {
 @get:Rule val compose=createComposeRule()
 @Test fun allThirteenFinishesRenderDistinctPreviewsInBothAppModes(){
  var s by mutableStateOf(Settings());compose.setContent{InstantTheme(s.theme,s.cameraStyle,s.cameraColor){Box(Modifier.width(320.dp)){CameraStylePreview(s.cameraStyle,s.cameraColor)}}}
  val hashes=mutableSetOf<Int>()
  for(mode in listOf(ThemeMode.LIGHT,ThemeMode.DARK))for(style in CameraStyle.entries)for(color in style.colors){
   compose.runOnIdle{s=s.copy(theme=mode,cameraStyle=style,cameraColor=color)}
   compose.onNodeWithContentDescription("${style.label}, ${color.label} camera preview").assertIsDisplayed()
   val b=compose.onRoot().captureToImage().asAndroidBitmap();val pixels=IntArray(b.width*b.height);b.getPixels(pixels,0,b.width,0,0,b.width,b.height)
   if(mode==ThemeMode.LIGHT)hashes+=pixels.contentHashCode()
   val dir=File("build/verification/styles").apply{mkdirs()};File(dir,"${style.name}-${color.name}-${mode.name}.png").outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
  }
  assertEquals(13,hashes.size)
 }
 @Test fun settingsPickerChangesStyleAndOnlyOffersItsValidColors(){
  var s by mutableStateOf(Settings());compose.setContent{InstantTheme{SettingsScreen(s){s=it}}}
  compose.onNodeWithContentDescription("Camera style Insta Style").performScrollTo().performClick();compose.onNodeWithContentDescription("Camera color Pink").performScrollTo().performClick();compose.runOnIdle{assertEquals(CameraColor.PINK,s.cameraColor)}
  compose.onNodeWithContentDescription("Camera style Old Fasion").performScrollTo().performClick();compose.onNodeWithContentDescription("Camera color Pink").assertDoesNotExist();compose.runOnIdle{assertEquals(CameraStyle.OLD_FASION,s.cameraStyle);assertEquals(CameraColor.BLACK,s.cameraColor)}
  compose.onNodeWithContentDescription("Camera style One and Only").performScrollTo().performClick();compose.onNodeWithContentDescription("Camera color Cream / Spectrum").performScrollTo().performClick();compose.runOnIdle{assertEquals(CameraColor.SPECTRUM,s.cameraColor)}
 }
 @Test fun appAppearanceAndPhysicalCameraColorAreIndependent(){
  var mode by mutableStateOf(ThemeMode.LIGHT);val style=CameraStyle.INSTA_STYLE;val color=CameraColor.MINT
  compose.setContent{InstantTheme(mode,style,color){CameraStylePreview(style,color)}}
  val before=compose.onRoot().captureToImage().asAndroidBitmap();compose.runOnIdle{mode=ThemeMode.DARK};val after=compose.onRoot().captureToImage().asAndroidBitmap();assertTrue(before.sameAs(after))
 }
 @Test fun actualCameraSupportsThreeStylesAndKeepsLogoCountdownAndCancelVisible()=runBlocking<Unit>{
  val app=ApplicationProvider.getApplicationContext<Application>();val db=Room.inMemoryDatabaseBuilder(app,InstantDatabase::class.java).build();val repo=PhotoRepository(app,db,FilmProcessor(Preferences(app)));repo.replace(FilmFormat.CLASSIC,Appearance.COLOR);val cart=repo.active();val vm=RoomViewModel(app,repo)
  var s by mutableStateOf(Settings())
  try {
   compose.setContent{InstantTheme(s.theme,s.cameraStyle,s.cameraColor){Box(Modifier.width(320.dp).height(600.dp)){val cap by vm.capture.collectAsState();CameraScreen(vm,cart,cap,s,true,{})}}}
   for(style in CameraStyle.entries){
    compose.runOnIdle{s=s.copy(cameraStyle=style,cameraColor=if(style==CameraStyle.INSTA_STYLE)CameraColor.PINK else style.defaultColor);vm.capture.value=CaptureUi(CapturePhase.COUNTDOWN,countdown=3,shot=2,booth=true)}
    compose.onNodeWithContentDescription("Check2GO").assertIsDisplayed();compose.onNodeWithText("3").assertIsDisplayed();compose.onNodeWithContentDescription("Cancel session").assertIsDisplayed()
    val dir=File("build/verification/styles").apply{mkdirs()};File(dir,"camera-${style.name}.png").outputStream().use{compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
   }
  }finally{vm.viewModelScope.cancel();db.close()}
 }
 @Test fun newCaptionUsesGlobalFontButExistingCaptionKeepsItsFont(){
  var selected=CaptionFont.SOL_KOL;var existing by mutableStateOf(false)
  val blank=PhotoEntity("p","original","",1,"CLASSIC","COLOR","c",1f,false)
  compose.setContent{InstantTheme{key(existing){CaptionEditor(if(existing)blank.copy(caption="Keep me",captionFont="DENISTINA") else blank,{}, {_,font,done->selected=font;done(true)},defaultFont=CaptionFont.HISTORIC)}}}
  compose.onNodeWithText("Save caption").performClick();compose.runOnIdle{assertEquals(CaptionFont.HISTORIC,selected);existing=true}
  compose.onNodeWithText("Save caption").performClick();compose.runOnIdle{assertEquals(CaptionFont.DENISTINA,selected)}
 }
}
