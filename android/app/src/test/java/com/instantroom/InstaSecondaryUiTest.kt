package com.instantroom

import android.app.Application
import android.graphics.Bitmap
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35],qualifiers="w393dp-h852dp-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class InstaSecondaryUiTest {
 @get:Rule val c=createComposeRule()
 private lateinit var db: InstantDatabase;private lateinit var repo: PhotoRepository;private lateinit var vm: RoomViewModel;private lateinit var photo: PhotoEntity
 private val s=Settings(cameraStyle=CameraStyle.INSTA_STYLE,cameraColor=CameraColor.PINK,theme=ThemeMode.LIGHT)
 @Before fun setup()=runBlocking<Unit>{val app=ApplicationProvider.getApplicationContext<Application>();db=Room.inMemoryDatabaseBuilder(app,InstantDatabase::class.java).build();repo=PhotoRepository(app,db,FilmProcessor(Preferences(app)));repo.replace(FilmFormat.CLASSIC,Appearance.COLOR);val file=File(app.cacheDir,"insta-caption-fixture.jpg");val b=Bitmap.createBitmap(180,220,Bitmap.Config.ARGB_8888);b.eraseColor(android.graphics.Color.rgb(245,240,225));file.outputStream().use{b.compress(Bitmap.CompressFormat.JPEG,95,it)};b.recycle();photo=PhotoEntity("secondary",file.path,file.path,1,"CLASSIC","COLOR",repo.active()!!.id,.4f,false);db.dao().insertPhoto(photo);vm=RoomViewModel(app,repo);vm.updateSettings(s);withTimeout(10000){while(!vm.initialized.value){shadowOf(Looper.getMainLooper()).idle();delay(5)}}}
 @After fun close(){vm.viewModelScope.cancel();db.close()}
 private fun shot(name:String){val dir=File("build/verification/insta-style").apply{mkdirs()};File(dir,"$name.png").outputStream().use{c.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}}
 @Test fun developmentPresentationNeverAdvancesWithoutAcceptedInput()=runBlocking<Unit>{c.setContent{InstantTheme(s.theme,s.cameraStyle,s.cameraColor){DevelopmentScreen(vm,photo,s,{},{},{})}};c.onNodeWithText("Develop your photograph").assertIsDisplayed();c.onNodeWithText("40% developed").assertExists();shot("development-paused");c.mainClock.advanceTimeBy(5000);assertEquals(.4f,repo.photo(photo.id).progress,0f);assertEquals(10,repo.active()!!.remaining)}
 @Test fun archiveAndCorkboardRetainTheirSharedPhotoAndMaterialBehavior()=runBlocking<Unit>{val developed=photo.copy(progress=1f);db.dao().progress(photo.id,1f);var board by mutableStateOf(false);c.setContent{InstantTheme(s.theme,s.cameraStyle,s.cameraColor){if(board)CorkboardScreen(vm,listOf(developed)) else ArchiveScreen(listOf(developed),{},{},{board=true})}};c.onNodeWithText("Archive").assertExists();shot("archive");c.onNodeWithText("Corkboard").performClick();c.onNodeWithText("Stay a little longer.").assertExists();shot("corkboard");assertEquals(10,repo.active()!!.remaining)}
 @Test fun captionEntryAndSaveRemainReachableAtTwoHundredPercentText(){org.robolectric.RuntimeEnvironment.setFontScale(2f);var saved="";c.setContent{InstantTheme(s.theme,s.cameraStyle,s.cameraColor){CaptionEditor(photo.copy(progress=1f),{}, {text,_,done->saved=text;done(true)})}};c.onNode(hasSetTextAction()).performTextInput("Morning light");c.onNodeWithText("Save caption").assertIsDisplayed();shot("caption-editor-200-percent");c.onNodeWithText("Save caption").performClick();c.runOnIdle{assertEquals("Morning light",saved)}}
}
