package com.instantroom.core

import org.junit.Test
import org.junit.Assert.*

class AnalogLayoutTest {
 @Test fun portraitSizesKeepRoundApertureAndSpaceForMechanicalControls() {
  for((w,h) in listOf(336f to 752f,369f to 804f,388f to 867f,378f to 826f)) {
   val g=AnalogLayout.fit(w,h)
   assertTrue(g.lensLeft>=0)
   assertTrue(g.lensTop>=68f)
   assertTrue(g.lensLeft+g.lens<=w)
   assertTrue(g.lowerHeight>=232f)
   assertTrue(g.aperture<g.lens)
   // Side control target rectangles must stay out of the live square aperture.
   assertTrue(g.lensLeft+(g.lens-g.aperture)/2>60f)
  }
 }
 @Test fun smallHeightAndLargeTypeExpandContentInsteadOfShrinkingTargets() {
  assertEquals(620f,AnalogLayout.fit(296f,512f).height)
  assertEquals(1100f,AnalogLayout.fit(336f,600f,2f).height)
 }
 @Test fun invalidGeometryIsRejected() {
  for(w in listOf(Float.NaN,Float.POSITIVE_INFINITY,-1f,0f)) {
   assertThrows(IllegalArgumentException::class.java){AnalogLayout.fit(w,800f)}
  }
 }
}
