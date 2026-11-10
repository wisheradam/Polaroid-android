package com.instantroom.core

import org.junit.Assert.*
import org.junit.Test

class CameraPreferencesTest {
 @Test fun rearSwitchesToFrontWhenAvailable(){assertEquals(true,CameraSwitchPolicy.target(false,false,true,true))}
 @Test fun frontSwitchesToRearWhenAvailable(){assertEquals(false,CameraSwitchPolicy.target(true,false,true,true))}
 @Test fun missingFrontCannotSwitch(){assertNull(CameraSwitchPolicy.target(false,false,false,true))}
 @Test fun missingRearCannotSwitch(){assertNull(CameraSwitchPolicy.target(true,false,true,false))}
 @Test fun busyCameraCannotSwitch(){listOf(true,false).forEach{assertNull(CameraSwitchPolicy.target(it,true,true,true))}}
 @Test fun lightModeIgnoresDarkSystem(){assertFalse(ThemeMode.LIGHT.isDark(true))}
 @Test fun darkModeIgnoresLightSystem(){assertTrue(ThemeMode.DARK.isDark(false))}
 @Test fun systemModeFollowsSystem(){assertTrue(ThemeMode.SYSTEM.isDark(true));assertFalse(ThemeMode.SYSTEM.isDark(false))}
 @Test fun edgeAndScreenModesAreIndependent(){assertTrue(SelfieLightMode.SCREEN.screenFlash);assertFalse(SelfieLightMode.SCREEN.edge);assertTrue(SelfieLightMode.EDGE.edge);assertFalse(SelfieLightMode.EDGE.screenFlash)}
 @Test fun combinedModeSupportsBoth(){assertTrue(SelfieLightMode.BOTH.edge && SelfieLightMode.BOTH.screenFlash)}
 @Test fun brightnessRestoresSystemSentinel(){var actual=-1f;val b=BrightnessLease({actual},{actual=it});b.set(.8f);b.set(1f);b.restore();assertEquals(-1f,actual,0f)}
 @Test fun brightnessRestoresExplicitOriginal(){var actual=.23f;val b=BrightnessLease({actual},{actual=it});b.set(.9f);b.set(null);assertEquals(.23f,actual,0f)}
 @Test fun repeatedRestoreDoesNotOverwriteAnExternalChange(){var actual=.3f;val b=BrightnessLease({actual},{actual=it});b.set(1f);b.restore();actual=.7f;b.restore();assertEquals(.7f,actual,0f)}
 @Test fun resumeAcquiresNewOriginal(){var actual=.3f;val b=BrightnessLease({actual},{actual=it});b.set(.9f);b.restore();actual=.45f;b.set(.9f);b.restore();assertEquals(.45f,actual,0f)}
}
