package com.instantroom

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.instantroom.core.*
import com.instantroom.data.Preferences
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class VNextPreferencesTest {
 private val context get()=ApplicationProvider.getApplicationContext<Context>()
 @Before fun clean(){context.getSharedPreferences("settings",Context.MODE_PRIVATE).edit().clear().commit();context.deleteDatabase("instant-room.db")}
 @Test fun freshInstallFollowsSystem(){assertEquals(ThemeMode.SYSTEM,Preferences(context).state.value.theme)}
 @Test fun legacyPreferenceInstallationRetainsDarkAppearance(){context.getSharedPreferences("settings",0).edit().putBoolean("sound",false).putBoolean("front",true).commit();val p=Preferences(context);assertEquals(ThemeMode.DARK,p.state.value.theme);assertFalse(p.state.value.sound);assertTrue(p.state.value.front)}
 @Test fun legacyDatabaseWithoutPreferencesRetainsDarkAppearance(){context.openOrCreateDatabase("instant-room.db",0,null).close();assertEquals(ThemeMode.DARK,Preferences(context).state.value.theme)}
 @Test fun allThemesSurviveRecreation(){ThemeMode.entries.forEach{mode->val p=Preferences(context);p.set(p.state.value.copy(theme=mode));assertEquals(mode,Preferences(context).state.value.theme)}}
 @Test fun newInstallDoesNotBecomeDarkAfterItsDatabaseAppears(){Preferences(context);context.openOrCreateDatabase("instant-room.db",0,null).close();assertEquals(ThemeMode.SYSTEM,Preferences(context).state.value.theme)}
 @Test fun lightingPreferencesAndCameraChoiceSurviveRecreation(){val p=Preferences(context);p.set(p.state.value.copy(front=true,selfieLight=SelfieLightMode.BOTH,edgeWidth=24f,edgeBrightness=.7f,lightTone=LightTone.WHITE));val restored=Preferences(context).state.value;assertTrue(restored.front);assertEquals(SelfieLightMode.BOTH,restored.selfieLight);assertEquals(24f,restored.edgeWidth,0f);assertEquals(.7f,restored.edgeBrightness,0f);assertEquals(LightTone.WHITE,restored.lightTone)}
 @Test fun corruptEnumFallsBackWithoutErasingOtherPreferences(){context.getSharedPreferences("settings",0).edit().putString("theme","unknown").putLong("charge",900).commit();val p=Preferences(context);assertEquals(ThemeMode.DARK,p.state.value.theme);assertEquals(900,p.state.value.chargeMs)}
}
