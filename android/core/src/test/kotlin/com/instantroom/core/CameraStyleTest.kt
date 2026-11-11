package com.instantroom.core

import org.junit.Test
import org.junit.Assert.*

class CameraStyleTest {
 @Test fun exactRequestedLabelsAndMultipleVariantsAreAvailable(){assertEquals(listOf("Old Fashion","Insta Style","One and Only"),CameraStyle.entries.map{it.label});assertEquals(13,CameraStyle.entries.sumOf{it.colors.size});assertTrue(CameraStyle.entries.all{it.colors.size>=4})}
 @Test fun incompatibleColorsFallBackToTheSelectedStylesDefault(){assertEquals(CameraColor.BLACK,CameraStyle.OLD_FASION.colorOrDefault(CameraColor.PINK));assertEquals(CameraColor.PINK,CameraStyle.INSTA_STYLE.colorOrDefault(CameraColor.PINK));assertEquals(CameraColor.WHITE,CameraStyle.ONE_AND_ONLY.colorOrDefault(CameraColor.LAVENDER))}
 @Test fun flashCannotToggleWithoutHardwareOrWhileBusy(){assertTrue(FlashMotion.toggleAllowed(true,false));assertFalse(FlashMotion.toggleAllowed(false,false));assertFalse(FlashMotion.toggleAllowed(true,true));assertFalse(FlashMotion.toggleAllowed(false,true))}
 @Test fun flashMotionUsesRequestedDurationRange(){assertTrue(FlashMotion.durationMs in 250..350)}
}
