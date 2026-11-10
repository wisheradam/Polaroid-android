package com.instantroom

import androidx.compose.ui.graphics.Color
import com.instantroom.core.*
import com.instantroom.ui.*
import org.junit.Test
import org.junit.Assert.*

class CameraStylePaletteTest {
 @Test fun everyBodyAndShutterHasReadableInk(){for(style in CameraStyle.entries)for(color in style.colors){val f=cameraFinish(style,color);assertTrue("$style/$color body",contrast(f.body,f.ink)>=4.5f);assertTrue("$style/$color shutter",contrast(f.shutter,inkOn(f.shutter))>=4.5f)}}
 @Test fun accentsRemainReadableOnPhysicalAndAppSurfaces(){for(style in CameraStyle.entries)for(color in style.colors){val f=cameraFinish(style,color);for(bg in listOf(f.body,Color(0xFF202823),Color(0xFFFAF3E6))){assertTrue("$style/$color accent",contrast(readableAccent(f.accent,bg),bg)>=4.5f)}}}
 @Test fun eachStyleHasDistinctBodyVariants(){for(style in CameraStyle.entries){assertEquals(style.colors.size,style.colors.map{cameraFinish(style,it).body}.toSet().size)}}
}
