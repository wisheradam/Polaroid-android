package com.instantroom

import android.content.Context
import android.graphics.*
import androidx.test.core.app.ApplicationProvider
import com.instantroom.core.*
import com.instantroom.imaging.CaptionRenderer
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35]) @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CaptionRendererTest {
 private val context get()=ApplicationProvider.getApplicationContext<Context>()
 @Test fun allFormatsAndAllFontsDrawOnlyWithinLowerPaperMargin() {
  val renderer=CaptionRenderer(context)
  for(format in FilmFormat.entries)for(font in CaptionFont.entries) {
   val b=Bitmap.createBitmap(600,(600/format.aspect).toInt(),Bitmap.Config.ARGB_8888);b.eraseColor(Color.rgb(247,242,226))
   val pixels=IntArray(b.width*b.height);b.getPixels(pixels,0,b.width,0,0,b.width,b.height)
   renderer.draw(Canvas(b),b.width,b.height,format,"Summer in Netanya\n10 November 2026",font)
   val after=IntArray(pixels.size);b.getPixels(after,0,b.width,0,0,b.width,b.height)
   val area=renderer.writingArea(b.width,b.height,format)
   var changed=0
   for(i in pixels.indices)if(pixels[i]!=after[i]) {changed++;assertTrue("$format $font outside writing area",area.contains((i%b.width).toFloat(),(i/b.width).toFloat()))}
   assertTrue("$format $font must draw visible ink",changed>100)
   val dir=File("build/verification/captions").apply{mkdirs()}
   File(dir,"${format.name}-${font.name}.png").outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}
   b.recycle()
  }
 }
 @Test fun longCaptionsFitWithinTwoLinesAcrossFormatsAndFonts() {
  val r=CaptionRenderer(context)
  for(f in FilmFormat.entries)for(font in CaptionFont.entries) {
   val area=r.writingArea(1440,(1440/f.aspect).toInt(),f)
   val l=r.layout("A very happy memory by the sea with family and friends on a beautiful sunny day.",font,area)
   assertTrue(l.lineCount<=2);assertTrue(l.height<=area.height())
  }
 }
 @Test fun threeFontFilesProduceDistinctInk() {
  val r=CaptionRenderer(context)
  val ink=CaptionFont.entries.map{font->
   val b=Bitmap.createBitmap(600,730,Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE)
   r.draw(Canvas(b),600,730,FilmFormat.CLASSIC,"Our story",font)
   val p=IntArray(600*730);b.getPixels(p,0,600,0,0,600,730);b.recycle();p.contentHashCode()
  }
  assertEquals(3,ink.toSet().size)
 }
 @Test fun emptyCaptionReturnsIdenticalBaseWithoutReencoding() {
  val base=File(context.filesDir,"empty-base.jpg").apply{writeBytes(byteArrayOf(1,2,3))}
  assertEquals(base.path,CaptionRenderer(context).render(base,FilmFormat.GO,"",CaptionFont.SOL_KOL))
  assertArrayEquals(byteArrayOf(1,2,3),base.readBytes())
 }
}
