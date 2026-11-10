package com.instantroom

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.instantroom.core.*
import com.instantroom.data.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class CameraStylePreferencesTest {
 private val context get()=ApplicationProvider.getApplicationContext<Context>()
 @Test fun everyStyleAndColorSurvivesPreferencesRecreation(){val p=Preferences(context);for(style in CameraStyle.entries)for(color in style.colors){p.set(Settings(cameraStyle=style,cameraColor=color));val restored=Preferences(context).state.value;assertEquals(style,restored.cameraStyle);assertEquals(color,restored.cameraColor)}}
 @Test fun invalidStoredStyleAndColorRecoverToCompatibleDefaults(){context.getSharedPreferences("settings",0).edit().clear().putString("cameraStyle","REMOVED").putString("cameraColor","PINK").commit();val s=Preferences(context).state.value;assertEquals(CameraStyle.ONE_AND_ONLY,s.cameraStyle);assertEquals(CameraColor.WHITE,s.cameraColor)}
 @Test fun invalidPairsAreNormalizedOnWriteAndReload(){val p=Preferences(context);p.set(Settings(cameraStyle=CameraStyle.OLD_FASION,cameraColor=CameraColor.MINT));assertEquals(CameraColor.BLACK,p.state.value.cameraColor);assertEquals(CameraColor.BLACK,Preferences(context).state.value.cameraColor)}
 @Test fun legacyTimerBoothThemeAndSoundAreNotOverridden(){context.getSharedPreferences("settings",0).edit().clear().putString("theme","DARK").putString("timer","THIRTY").putString("boothLayout","STRIP").putBoolean("sound",false).commit();val s=Preferences(context).state.value;assertEquals(ThemeMode.DARK,s.theme);assertEquals(SelfTimer.THIRTY,s.timer);assertEquals(BoothLayout.STRIP,s.boothLayout);assertFalse(s.sound);assertEquals(CameraStyle.ONE_AND_ONLY,s.cameraStyle)}
 @Test fun RearFlashAndDefaultCaptionFontPersistIndependentlyOfCameraStyle(){val p=Preferences(context);p.set(Settings(rearFlash=true,captionFont=CaptionFont.HISTORIC,cameraStyle=CameraStyle.INSTA_STYLE,cameraColor=CameraColor.LAVENDER));val restored=Preferences(context).state.value;assertTrue(restored.rearFlash);assertEquals(CaptionFont.HISTORIC,restored.captionFont);p.set(restored.copy(cameraStyle=CameraStyle.OLD_FASION));assertTrue(p.state.value.rearFlash);assertEquals(CaptionFont.HISTORIC,p.state.value.captionFont)}
}
