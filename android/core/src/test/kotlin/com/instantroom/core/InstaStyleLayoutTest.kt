package com.instantroom.core
import org.junit.Test
import org.junit.Assert.*
class InstaStyleLayoutTest {
 @Test fun canonicalInsetIndependentGeometry(){val g=InstaStyleLayout.fit(393f,774f);assertEquals(334f,g.lensDiameter,0f);assertEquals(282.284f,g.apertureDiameter,.001f);assertEquals(147f,g.lensY,0f);assertEquals(497f,g.shutterY,0f);assertEquals(519f,g.controlsY,0f);assertEquals(616f,g.slotY,0f);assertEquals(708f,g.navigationY,0f);assertFalse(g.essentialControls)}
 @Test fun compactControlsHaveIndependentNonOverlappingTargets(){val g=InstaStyleLayout.fit(360f,748f);assertEquals(88f,g.shutterSize,0f);assertEquals(136f,g.shutterX,0f);assertTrue(g.timerX+54<=g.flashX);assertTrue(g.flashX+54<g.shutterX);assertTrue(g.shutterX+g.shutterSize<g.switchX);assertEquals(682f,g.navigationY,0f)}
 @Test fun matrixKeepsCircularPreviewAndScrollableNavigation(){for(w in listOf(320f,360f,393f,412f))for(h in listOf(520f,774f,851f))for(s in listOf(1f,2f)){val g=InstaStyleLayout.fit(w,h,s);assertTrue(g.lensX>=24);assertEquals(262f/310,g.apertureDiameter/g.lensDiameter,.0001f);assertTrue(g.navigationY>=g.slotY+g.slotHeight);assertTrue(g.height>=g.lensY+g.lensDiameter);assertEquals(w<360 || s>1.4f,g.essentialControls)}}
 @Test fun longStripTransportIsLinearAndNotSquashed(){val h=220f/BoothLayout.STRIP.geometry.aspect;assertTrue(h>800);assertEquals(-h,InstaStyleMotion.paperOffset(0f,h,false),0f);assertEquals(-h/2,InstaStyleMotion.paperOffset(.5f,h,false),.001f);assertEquals(0f,InstaStyleMotion.paperOffset(1f,h,false),0f)}
 @Test fun reducedMotionChangesOnlyPresentation(){val h=200f;assertEquals(-100f,InstaStyleMotion.paperOffset(.1f,h,true),0f);assertEquals(-100f,InstaStyleMotion.paperOffset(.9f,h,true),0f);assertEquals(0f,InstaStyleMotion.paperOffset(1f,h,true),0f);assertEquals(70,InstaStyleMotion.pressMs);assertEquals(110,InstaStyleMotion.releaseMs);assertEquals(1800L,EjectionTimeline.durationMs)}
 @Test fun inputDimensionsAreValidated(){for(w in listOf(Float.NaN,200f,Float.POSITIVE_INFINITY)){assertThrows(IllegalArgumentException::class.java){InstaStyleLayout.fit(w,800f)}};assertThrows(IllegalArgumentException::class.java){InstaStyleLayout.fit(393f,0f)}}
}
