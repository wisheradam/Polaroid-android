package com.instantroom

import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35],qualifiers="w480dp-h1000dp-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class InstaNavigationUiTest {
 @get:Rule val c=createComposeRule()
 private lateinit var db: InstantDatabase;private lateinit var repo: PhotoRepository;private lateinit var vm: RoomViewModel;private lateinit var activity: ComponentActivity
 @Before fun setup()=runBlocking<Unit>{val app=ApplicationProvider.getApplicationContext<Application>();db=Room.inMemoryDatabaseBuilder(app,InstantDatabase::class.java).build();repo=PhotoRepository(app,db,FilmProcessor(Preferences(app)));repo.replace(FilmFormat.GO,Appearance.MONO);vm=RoomViewModel(app,repo);withTimeout(10000){while(!vm.initialized.value){shadowOf(Looper.getMainLooper()).idle();delay(5)}};vm.updateSettings(Settings(cameraStyle=CameraStyle.INSTA_STYLE,cameraColor=CameraColor.PINK,timer=SelfTimer.FIFTEEN,captionFont=CaptionFont.HISTORIC))}
 @After fun close(){vm.viewModelScope.cancel();db.close()}
 @Composable private fun App(){val s by vm.settings.collectAsState();activity=LocalContext.current.activity() as ComponentActivity;InstantTheme(s.theme,s.cameraStyle,s.cameraColor){Box(Modifier.size(393.dp,852.dp)){InstantRoom(vm)}}}
 private fun shot(name:String){val dir=File("build/verification/insta-style").apply{mkdirs()};File(dir,"$name.png").outputStream().use{c.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}
 private fun assertFinish(color: CameraColor){c.runOnIdle{assertEquals(CameraStyle.INSTA_STYLE,vm.settings.value.cameraStyle);assertEquals(color,vm.settings.value.cameraColor);assertEquals(SelfTimer.FIFTEEN,vm.settings.value.timer);assertEquals(CaptionFont.HISTORIC,vm.settings.value.captionFont)}}
 private fun routes(color: CameraColor)=runBlocking<Unit>{vm.requestCameraAppearance(CameraStyle.INSTA_STYLE,color);val before=repo.active();c.setContent{App()}
  c.onNodeWithContentDescription("Settings").performScrollTo().performClick();c.onNodeWithText("Camera design · Insta Style").performScrollTo().assertExists();assertFinish(color) // camera → settings
  c.onNodeWithText("Camera design · Insta Style").performClick();c.onNodeWithText("Camera appearance").assertIsDisplayed();assertFinish(color);c.onAllNodesWithTag("insta-style-preview").assertCountEquals(1) // settings → design
  c.onNodeWithText("Back to Settings").performScrollTo().performClick();c.onNodeWithText("Camera design · Insta Style").performScrollTo().assertExists();assertFinish(color) // design → settings
  c.onNodeWithText("Camera").performClick();c.onNodeWithTag("insta-style-camera").assertExists();assertFinish(color);assertEquals(before,repo.active()) // settings → camera
 }
 @Test fun whiteKeepsFinishAcrossFourNativeRoutes()=routes(CameraColor.WHITE)
 @Test fun pinkKeepsFinishAcrossFourNativeRoutes()=routes(CameraColor.PINK)
 @Test fun blueKeepsFinishAcrossFourNativeRoutes()=routes(CameraColor.BLUE)
 @Test fun mintKeepsFinishAcrossFourNativeRoutes()=routes(CameraColor.MINT)
 @Test fun lavenderKeepsFinishAcrossFourNativeRoutes()=routes(CameraColor.LAVENDER)
 @Test fun savedNavigationRecreationKeepsOnePreviewAndCommittedFinish(){val restoration=StateRestorationTester(c);restoration.setContent{App()};c.onNodeWithContentDescription("Settings").performClick();shot("settings-pink");c.onNodeWithText("Camera design · Insta Style").performScrollTo().performClick();c.onNodeWithContentDescription("Camera color Baby Blue").performScrollTo().performClick();restoration.emulateSavedInstanceStateRestore();c.onNodeWithText("Camera appearance").performScrollTo().assertIsDisplayed();c.onAllNodesWithTag("insta-style-preview").assertCountEquals(1);assertFinish(CameraColor.BLUE);c.runOnIdle{activity.onBackPressedDispatcher.onBackPressed()};c.onNodeWithText("Camera design · Insta Style").performScrollTo().assertExists();c.runOnIdle{activity.onBackPressedDispatcher.onBackPressed()};c.onNodeWithTag("insta-style-camera").assertExists();assertFinish(CameraColor.BLUE)}
 @Test fun freshPreferencesAndViewModelsRestoreEveryCommittedFinish()=runBlocking<Unit>{val app=ApplicationProvider.getApplicationContext<Application>();for(color in CameraStyle.INSTA_STYLE.colors){vm.requestCameraAppearance(CameraStyle.INSTA_STYLE,color);val fresh=Preferences(app).state.value;assertEquals(color,fresh.cameraColor);assertEquals(CameraStyle.INSTA_STYLE,fresh.cameraStyle);vm.viewModelScope.cancel();vm=RoomViewModel(app,repo);assertFinish(color)};assertEquals(10,repo.active()!!.remaining)}
}
