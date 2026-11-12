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
class InstaStyleUiTest {
 @get:Rule val c=createComposeRule()
 private var settings by mutableStateOf(Settings(cameraStyle=CameraStyle.INSTA_STYLE,cameraColor=CameraColor.PINK))
 private var cap by mutableStateOf(CaptureUi())
 private var remaining by mutableStateOf(10)
 private var booth by mutableStateOf(false)
 private var available by mutableStateOf(true)
 private var captures=0;private var switches=0;private var loads=0;private var gallery=0;private var openedSettings=0;private var cancelled=0
 private val paper=PhotoEntity("paper","source","",1,"CLASSIC","COLOR","c",0f,false)
 @Composable private fun Face(){InstaStyleCamera(settings,CartridgeEntity("c","CLASSIC","COLOR",remaining,true),cap,true,true,true,available,booth,paper,
  {captures++},{loads++},{gallery++},{openedSettings++},{switches++;settings=settings.copy(front=!settings.front)},
  {settings=settings.copy(rearFlash=it)},{settings=settings.copy(timer=it)},{booth=it},{settings=settings.copy(boothLayout=it)},
  {settings=settings.copy(selfieLight=it)},{cancelled++;cap=CaptureUi()}) {
   Box(Modifier.fillMaxSize().background(Color(0xFF547C65)).semantics{contentDescription="Preview test double"},contentAlignment=Alignment.Center){Text("TEST PREVIEW",color=Color.White,fontSize=8.sp)}
  }}
 private fun show(){c.setContent{InstantTheme(settings.theme,settings.cameraStyle,settings.cameraColor){Box(Modifier.size(393.dp,852.dp).background(cameraFinish(CameraStyle.INSTA_STYLE,settings.cameraColor).body).padding(top=44.dp,bottom=34.dp)){Face()}}}}
 private fun shot(name:String){val dir=File("build/verification/insta-style").apply{mkdirs()};File(dir,"$name.png").outputStream().use{c.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}
 @Test fun canonicalGeometryHasOneCircularInjectedPreviewAndNativeTargets(){show();c.onNodeWithTag("insta-style-camera").assertExists();c.onAllNodesWithContentDescription("Preview test double").assertCountEquals(1);val lens=c.onNodeWithTag("insta-lens").fetchSemanticsNode().boundsInRoot;assertEquals(334f,lens.width,.1f);assertEquals(lens.width,lens.height,.1f);assertEquals(191f,lens.top,.1f);val a=c.onNodeWithTag("insta-live-aperture").fetchSemanticsNode().boundsInRoot;assertEquals(334*262f/310,a.width,1f);assertEquals(a.width,a.height,1f);val shutter=c.onNodeWithContentDescription("Take photograph").fetchSemanticsNode().boundsInRoot;assertEquals(100f,shutter.width,.1f);assertEquals(100f,shutter.height,.1f);assertEquals(541f,shutter.top,.1f);for(d in listOf("Self-timer","Rear camera flash","Switch to front camera")){val b=c.onNodeWithContentDescription(d).fetchSemanticsNode().boundsInRoot;assertTrue(b.width>=54);assertTrue(b.height>=58)};c.onNodeWithContentDescription("Photo Booth").assertIsNotSelected();shot("pink-393x852")}
 @Test fun fiveFinishesUseIdenticalGeometryAndPhysicalPaintAcrossAppModes(){show();for(color in CameraStyle.INSTA_STYLE.colors){c.runOnIdle{settings=settings.copy(cameraColor=color,theme=ThemeMode.LIGHT)};val a=c.onNodeWithTag("insta-shell").captureToImage().asAndroidBitmap();shot("finish-${color.name}");c.runOnIdle{settings=settings.copy(theme=ThemeMode.DARK)};val b=c.onNodeWithTag("insta-shell").captureToImage().asAndroidBitmap();assertTrue(a.sameAs(b));assertTrue(contrast(cameraFinish(CameraStyle.INSTA_STYLE,color).body,InstaStyleTokens.Ink)>=4.5f);assertTrue(contrast(cameraFinish(CameraStyle.INSTA_STYLE,color).accent,InstaStyleTokens.Paper)>=4.5f)}}
 @Test fun acceptedCallbacksDisabledStatesAndContextualLighting(){show();c.onNodeWithContentDescription("Take photograph").performTouchInput{click()};c.runOnIdle{assertEquals(1,captures)};c.onNodeWithContentDescription("Film: 10 shots remaining. Replace cartridge").performClick();c.runOnIdle{assertEquals(1,loads)};c.onNodeWithContentDescription("Self-timer").performClick();c.onNodeWithText("30 seconds").performClick();c.runOnIdle{assertEquals(SelfTimer.THIRTY,settings.timer)};c.onNodeWithContentDescription("Rear camera flash").performClick();c.runOnIdle{assertTrue(settings.rearFlash)};shot("flash-active");c.onNodeWithContentDescription("Switch to front camera").performClick();c.onNodeWithContentDescription("Selfie lighting").performClick();c.onNodeWithText(SelfieLightMode.BOTH.label).performClick();c.runOnIdle{assertEquals(SelfieLightMode.BOTH,settings.selfieLight)};c.onNodeWithContentDescription("Switch to rear camera").performClick();c.runOnIdle{assertEquals(2,switches);available=false};c.onNodeWithContentDescription("Rear camera flash").assertIsNotEnabled();c.runOnIdle{remaining=0};c.onNodeWithContentDescription("Take photograph").assertIsNotEnabled();c.runOnIdle{cap=CaptureUi(CapturePhase.COUNTDOWN,countdown=3,shot=2,booth=true)};c.onNodeWithText("3").assertIsDisplayed();c.onNodeWithContentDescription("Self-timer").assertIsNotEnabled();shot("countdown");c.onNodeWithContentDescription("Cancel session").performClick();c.runOnIdle{assertEquals(1,cancelled)}}
 @Test fun pressReleaseAndCancellationDoNotInventAnExposure(){c.mainClock.autoAdvance=false;show();val n=c.onNodeWithContentDescription("Take photograph");n.performTouchInput{down(center)};c.mainClock.advanceTimeBy(250);c.waitForIdle();n.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Pressed"));shot("shutter-pressed");n.performTouchInput{cancel()};c.mainClock.advanceTimeBy(150);c.waitForIdle();n.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Ready"));c.runOnIdle{assertEquals(0,captures)}}
 @Test fun boothSelectionReflectsActualModeAndUsesExistingLayoutChoices(){show();c.onNodeWithContentDescription("Photo Booth").performClick();shot("photo-booth-layout-selection");c.onNodeWithText(BoothLayout.STRIP.label).performClick();c.onNodeWithText("Use Photo Booth").performClick();c.onNodeWithContentDescription("Photo Booth").assertIsSelected();c.onNodeWithContentDescription("Start Photo Booth").assertIsEnabled();c.runOnIdle{assertTrue(booth);assertEquals(BoothLayout.STRIP,settings.boothLayout);remaining=3};c.onNodeWithContentDescription("Start Photo Booth").assertIsNotEnabled();c.onNodeWithContentDescription("Photo Booth").performClick();c.onNodeWithText("Single photo").performClick();c.onNodeWithContentDescription("Photo Booth").assertIsNotSelected();c.onNodeWithContentDescription("Take photograph").assertIsEnabled()}
 @Test fun responsiveMatrixHasNoOverlappingControlsAndReachableLargeTextTray(){var w by mutableStateOf(360);var h by mutableStateOf(800);var scale by mutableStateOf(1f);var top by mutableStateOf(28);var bottom by mutableStateOf(24)
  c.setContent{InstantTheme{CompositionLocalProvider(LocalDensity provides Density(1f,scale)){Box(Modifier.size(w.dp,h.dp).background(cameraFinish(CameraStyle.INSTA_STYLE,settings.cameraColor).body).padding(top=top.dp,bottom=bottom.dp)){Face()}}}}
  for((ww,hh,t,b) in listOf(listOf(360,800,28,24),listOf(393,852,44,34),listOf(412,915,36,28)))for(s in listOf(1f,2f)){
   c.runOnIdle{w=ww;h=hh;top=t;bottom=b;scale=s};c.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy){it(0f,-100000f)};c.waitForIdle();val a=c.onNodeWithTag("insta-live-aperture").fetchSemanticsNode().boundsInRoot;assertEquals(a.width,a.height,1f);shot("camera-${ww}x${hh}-font$s-top")
   if(s>1.4f){c.onNodeWithText("Take photograph").performScrollTo().assertIsEnabled();c.onNodeWithTag("insta-essential-controls").assertExists()}else {val keys=listOf("Self-timer","Rear camera flash","Take photograph","Switch to front camera");val bounds=keys.map{c.onNodeWithContentDescription(it).fetchSemanticsNode().boundsInRoot};for(i in bounds.indices)for(j in i+1 until bounds.size)assertFalse("Overlapping controls at $ww",bounds[i].overlaps(bounds[j]))}
   c.onNodeWithContentDescription("Settings").performScrollTo().assertIsDisplayed().performClick();val nav=c.onNodeWithContentDescription("Settings").fetchSemanticsNode().boundsInRoot;assertTrue(nav.height>=58);shot("camera-${ww}x${hh}-font$s-navigation");c.onNodeWithContentDescription("Gallery").performScrollTo().performClick()
  };c.runOnIdle{assertEquals(6,gallery);assertEquals(6,openedSettings)}
 }
 @Test fun ejectionKeepsEveryPhysicalFormatIncludingFullHeightStrip(){var print by mutableStateOf(paper);cap=CaptureUi(CapturePhase.EJECTING,photoId=paper.id,ejection=.55f);c.setContent{InstantTheme{Box(Modifier.size(393.dp,852.dp).padding(top=44.dp,bottom=34.dp)){InstaStyleCamera(settings,CartridgeEntity("c","CLASSIC","COLOR",9,true),cap,true,true,true,true,false,print,{},{},{},{},{},{},{},{},{},{},{}){Box(Modifier.fillMaxSize().background(Color(0xFF547C65)))}}}}
  for(format in FilmFormat.entries){c.runOnIdle{print=paper.copy(format=format.name)};val p=c.onNodeWithTag("insta-paper-mouth").fetchSemanticsNode().size;assertEquals(p.width/format.aspect,p.height.toFloat(),1.1f);c.onNodeWithTag("insta-front-lip").assertExists();if(format==FilmFormat.CLASSIC)shot("ejection-classic-55-percent")}
  for(layout in BoothLayout.entries){c.runOnIdle{print=paper.copy(boothSessionId="s",boothLayout=layout.name)};val n=c.onNodeWithTag("insta-paper-mouth").fetchSemanticsNode();assertEquals(220f/layout.geometry.aspect,n.size.height.toFloat(),1.1f);if(layout==BoothLayout.STRIP){assertTrue(n.size.height>800);shot("ejection-strip-55-percent")}}
 }
 @Test fun singleDecorativePreviewAndFinishLabelsWrapAtLargeText(){var scale by mutableStateOf(1f);c.setContent{InstantTheme(settings.theme,settings.cameraStyle,settings.cameraColor){CompositionLocalProvider(LocalDensity provides Density(1f,scale)){Box(Modifier.size(360.dp,800.dp)){CameraAppearanceScreen(settings,{}, {settings=it})}}}};for(s in listOf(1f,2f))for(color in CameraStyle.INSTA_STYLE.colors){c.runOnIdle{scale=s;settings=settings.copy(cameraColor=color)};c.onAllNodesWithTag("insta-style-preview").assertCountEquals(1);c.onNodeWithContentDescription("Camera color ${color.label}").performScrollTo().assertIsDisplayed();val nodes=c.onAllNodes(hasContentDescription("Camera color",substring=true)).fetchSemanticsNodes();for(i in nodes.indices)for(j in i+1 until nodes.size)assertFalse(nodes[i].boundsInRoot.overlaps(nodes[j].boundsInRoot));if(s==1f && color==CameraColor.PINK){c.onNodeWithContentDescription("Insta Style, Pink camera preview").performScrollTo();shot("camera-design-selector")}}}
}
