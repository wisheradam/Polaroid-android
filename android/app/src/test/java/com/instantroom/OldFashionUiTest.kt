package com.instantroom

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
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

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35],qualifiers="w480dp-h1000dp-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class OldFashionUiTest {
 @get:Rule val c=createComposeRule()
 private var settings by mutableStateOf(Settings(cameraStyle=CameraStyle.OLD_FASION,cameraColor=CameraColor.BLACK))
 private var cap by mutableStateOf(CaptureUi())
 private var remaining by mutableStateOf(10)
 private var booth by mutableStateOf(false)
 private var available by mutableStateOf(true)
 private var captures=0;private var switches=0;private var loads=0;private var gallery=0;private var openedSettings=0;private var cancelled=0
 private val paper=PhotoEntity("paper","source","",1,"CLASSIC","COLOR","c",0f,false)
 @Composable private fun Face(){OldFashionCamera(settings,CartridgeEntity("c","CLASSIC","COLOR",remaining,true),cap,true,true,true,available,booth,paper,
  {captures++},{loads++},{gallery++},{openedSettings++},{switches++;settings=settings.copy(front=!settings.front)},
  {settings=settings.copy(rearFlash=it)},{settings=settings.copy(timer=it)},{booth=it},{settings=settings.copy(boothLayout=it)},
  {settings=settings.copy(selfieLight=it)},{cancelled++;cap=CaptureUi()}) {
   Box(Modifier.fillMaxSize().background(Color(0xFF547C65)).semantics{contentDescription="Preview test double"},contentAlignment=Alignment.Center){Text("TEST PREVIEW",color=Color.White,fontSize=8.sp)}
  }}
 private fun show(){c.setContent{InstantTheme(settings.theme,settings.cameraStyle,settings.cameraColor){Box(Modifier.size(393.dp,852.dp).background(OldFashionTokens.Chrome).padding(top=44.dp,bottom=34.dp)){Face()}}}}
 private fun shot(name:String){val dir=File("build/verification/old-fashion").apply{mkdirs()};File(dir,"$name.png").outputStream().use{c.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}
 @Test fun canonicalHierarchyPreviewGeometryAndAccessibleTargets(){show();c.onNodeWithTag("old-fashion-camera").assertExists();c.onNodeWithTag("old-fashion-flash-housing").assertExists();c.onNodeWithContentDescription("Preview test double").assertIsDisplayed();val a=c.onNodeWithTag("old-fashion-live-aperture").fetchSemanticsNode().boundsInRoot;assertEquals(119f,a.width,.1f);assertEquals(132f,a.height,.1f);val flash=c.onNodeWithTag("old-fashion-flash-housing").fetchSemanticsNode().boundsInRoot;assertEquals(52f,flash.top,.1f);val shutter=c.onNodeWithContentDescription("Take photograph").fetchSemanticsNode().boundsInRoot;assertEquals(48f,shutter.width,.1f);assertEquals(80f,shutter.height,.1f);assertEquals(448f,shutter.top,.1f);c.onNodeWithTag("old-fashion-gold-mesh",useUnmergedTree=true).assert(!hasClickAction());c.onNodeWithContentDescription("Settings").assertIsDisplayed();shot("black-393x852")}
 @Test fun shutterPressAndCancellationDoNotEmitCaptureFeedback(){c.mainClock.autoAdvance=false;show();val target=c.onNodeWithContentDescription("Take photograph");target.performTouchInput{down(center)};c.mainClock.advanceTimeBy(250);c.waitForIdle();target.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Pressed"));shot("shutter-pressed");target.performTouchInput{cancel()};c.mainClock.advanceTimeBy(160);c.waitForIdle();target.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Ready"));c.runOnIdle{assertEquals(0,captures)}}
 @Test fun fourFinishesKeepPhysicalColorsAcrossAppModes(){show();for(color in CameraStyle.OLD_FASION.colors){c.runOnIdle{settings=settings.copy(cameraColor=color,theme=ThemeMode.LIGHT)};val a=c.onRoot().captureToImage().asAndroidBitmap();shot("finish-${color.name}");c.runOnIdle{settings=settings.copy(theme=ThemeMode.DARK)};val b=c.onRoot().captureToImage().asAndroidBitmap();assertTrue("Physical finish changed with app mode: $color",a.sameAs(b))};assertTrue(contrast(OldFashionTokens.Shutter,inkOn(OldFashionTokens.Shutter))>=4.5f)}
 @Test fun shutterAndControlCallbacksRemainSingleAndStateDriven(){show();c.onNodeWithContentDescription("Take photograph").performTouchInput{click()};c.runOnIdle{assertEquals(1,captures)};c.onNodeWithContentDescription("Self-timer").performClick();c.onNodeWithText("15 seconds").performClick();c.runOnIdle{assertEquals(SelfTimer.FIFTEEN,settings.timer)};c.onNodeWithContentDescription("Switch to front camera").performTouchInput{click()};c.onNodeWithContentDescription("Selfie lighting").performClick();c.onNodeWithText(SelfieLightMode.BOTH.label).performClick();c.runOnIdle{assertEquals(SelfieLightMode.BOTH,settings.selfieLight)};c.onNodeWithContentDescription("Switch to rear camera").performClick();c.onNodeWithContentDescription("Rear camera flash").performClick();c.runOnIdle{assertTrue(settings.rearFlash);assertEquals(2,switches)};shot("flash-active");c.onNodeWithContentDescription("Photo Booth").performScrollTo().performClick();c.onNodeWithText(BoothLayout.STRIP.label).performClick();c.onNodeWithText("Use Photo Booth").performClick();c.runOnIdle{assertTrue(booth);assertEquals(BoothLayout.STRIP,settings.boothLayout);cap=CaptureUi(CapturePhase.CHARGING,booth=true)};c.onNodeWithContentDescription("Start Photo Booth").assertIsNotEnabled();c.onNodeWithContentDescription("Self-timer").assertIsNotEnabled();c.onNodeWithContentDescription("Cancel session").performClick();c.runOnIdle{assertEquals(1,cancelled);available=false};c.onNodeWithContentDescription("Rear camera flash").assertIsNotEnabled();c.runOnIdle{remaining=0};c.onNodeWithContentDescription("Start Photo Booth").assertIsNotEnabled();c.onNodeWithContentDescription("Film: 0 shots remaining. Replace cartridge").performClick();c.runOnIdle{assertEquals(1,loads);assertEquals(1,captures)}}
 @Test fun responsiveMatrixKeepsPreviewUniformAndEssentialActionsReachable(){var width by mutableStateOf(360);var height by mutableStateOf(800);var scale by mutableStateOf(1f);var top by mutableStateOf(28);var bottom by mutableStateOf(24)
  c.setContent{InstantTheme{CompositionLocalProvider(LocalDensity provides Density(1f,scale)){Box(Modifier.size(width.dp,height.dp).background(OldFashionTokens.Chrome).padding(top=top.dp,bottom=bottom.dp)){Face()}}}}
  for((w,h,t,b) in listOf(listOf(360,800,28,24),listOf(393,852,44,34),listOf(412,915,36,28)))for(font in listOf(1f,2f)){
   c.runOnIdle{width=w;height=h;scale=font;top=t;bottom=b};c.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy){it(0f,-100000f)};c.waitForIdle();c.onNodeWithContentDescription("Check2GO").performScrollTo().assertIsDisplayed();val a=c.onNodeWithTag("old-fashion-live-aperture").fetchSemanticsNode().boundsInRoot;assertEquals(a.height*119f/132f,a.width,1.1f);shot("camera-${w}x${h}-font$font-top")
   if(font>1.4f){c.onNodeWithText("Take photograph").performScrollTo().assertIsEnabled();val target=c.onNodeWithText("Take photograph").onParent().fetchSemanticsNode().boundsInRoot;assertTrue(target.height>=80f-.1f);c.onNodeWithTag("old-fashion-essential-controls").assertExists()}
   c.onNodeWithContentDescription("Settings").performScrollTo().assertIsDisplayed().performClick();val nav=c.onNodeWithContentDescription("Settings").fetchSemanticsNode().boundsInRoot;assertTrue(nav.height>=80f-.1f);shot("camera-${w}x${h}-font$font-navigation");c.onNodeWithContentDescription("Gallery").performScrollTo().performClick()
  };c.runOnIdle{assertEquals(6,openedSettings);assertEquals(6,gallery)}
 }
 @Test fun ejectionUsesTheRealPrintAspectAndAnUndevelopedPhoto(){cap=CaptureUi(CapturePhase.EJECTING,photoId=paper.id,ejection=.55f);remaining=9;show();c.onNodeWithTag("old-fashion-front-lip").assertExists();val mouth=c.onNodeWithTag("old-fashion-paper-mouth").fetchSemanticsNode().boundsInRoot;assertEquals(mouth.height*paper.geometry.aspect,mouth.width,1.1f);c.onNodeWithContentDescription("Undeveloped Classic photograph; shake to reveal").assertExists();shot("ejection-55-percent");c.onNodeWithContentDescription("Take photograph").assertIsNotEnabled()}
 @Test fun existingPickerHasAccurateOldPreviewAndHonestQueuedCancellation(){var pending by mutableStateOf<CameraAppearance?>(null);c.setContent{InstantTheme{Box(Modifier.size(393.dp,852.dp)){CameraAppearanceScreen(settings,{}, {settings=it},pending,{pending=null})}}};c.onNodeWithContentDescription("Old Fashion, Black camera preview").assertIsDisplayed();shot("camera-design-selection");c.onNodeWithContentDescription("Camera color Dark Olive").performScrollTo().performClick();c.runOnIdle{assertEquals(CameraColor.OLIVE,settings.cameraColor);pending=CameraAppearance(CameraStyle.INSTA_STYLE,CameraColor.MINT)};c.onNodeWithText("Cancel appearance change").performScrollTo().assertIsDisplayed().performClick();c.runOnIdle{assertNull(pending);assertEquals(CameraStyle.OLD_FASION,settings.cameraStyle)}}
 @Test fun actualDispatcherPreservesThreeSeparateCameraFaces()=runBlocking<Unit>{val app=ApplicationProvider.getApplicationContext<Application>();val db=Room.inMemoryDatabaseBuilder(app,InstantDatabase::class.java).build();val repo=PhotoRepository(app,db,FilmProcessor(Preferences(app)));repo.replace(FilmFormat.CLASSIC,Appearance.COLOR);val cart=repo.active();val vm=RoomViewModel(app,repo)
  try{c.setContent{InstantTheme{Box(Modifier.size(393.dp,852.dp)){CameraScreen(vm,cart,CaptureUi(),settings,true,{})}}};for(style in CameraStyle.entries){c.runOnIdle{settings=settings.copy(cameraStyle=style,cameraColor=style.defaultColor)};c.onNodeWithTag(when(style){CameraStyle.OLD_FASION->"old-fashion-camera";CameraStyle.ONE_AND_ONLY->"one-and-only-camera";else->"insta-style-camera"}).assertExists();shot("dispatch-${style.name}")}}
  finally{vm.viewModelScope.cancel();db.close()}
 }
}
