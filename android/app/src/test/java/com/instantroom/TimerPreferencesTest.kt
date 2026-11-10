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
class TimerPreferencesTest {
 @Test fun timerAndBoothLayoutPersistAcrossPreferencesRecreation(){val c=ApplicationProvider.getApplicationContext<Context>();val p=Preferences(c);for(timer in SelfTimer.entries){p.set(p.state.value.copy(timer=timer,boothLayout=BoothLayout.STRIP));val restored=Preferences(c).state.value;assertEquals(timer,restored.timer);assertEquals(BoothLayout.STRIP,restored.boothLayout)}}
 @Test fun upgradeDefaultsToTimerOffAndPreservesEarlierSettings(){val c=ApplicationProvider.getApplicationContext<Context>();c.getSharedPreferences("settings",Context.MODE_PRIVATE).edit().clear().putBoolean("sound",false).putString("theme","LIGHT").commit();val s=Preferences(c).state.value;assertEquals(SelfTimer.OFF,s.timer);assertEquals(BoothLayout.GRID,s.boothLayout);assertFalse(s.sound);assertEquals(ThemeMode.LIGHT,s.theme)}
}
