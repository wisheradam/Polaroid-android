package com.instantroom.imaging

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.SpannableString
import android.text.Spanned
import android.text.style.LineHeightSpan
import androidx.core.content.res.ResourcesCompat
import com.instantroom.R
import com.instantroom.core.*
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Draws only inside the lower paper margin; the uncaptioned print is retained. */
class CaptionRenderer(private val context: Context) {
 fun typeface(font: CaptionFont): Typeface = checkNotNull(ResourcesCompat.getFont(context,when(font) {
  CaptionFont.SOL_KOL->R.font.sol_kol
  CaptionFont.DENISTINA->R.font.denistina
  CaptionFont.HISTORIC->R.font.historic
 }))
 fun writingArea(width: Int, height: Int, format: FilmFormat): RectF = writingArea(width,height,format.geometry)
 fun writingArea(width: Int, height: Int, format: PrintGeometry): RectF {
  val scale=width/format.width
  return RectF(width*.075f,(format.top+format.imageHeight+1.5f)*scale,width*.925f,height-2.0f*scale)
 }
 fun layout(text: String, font: CaptionFont, area: RectF): StaticLayout {
  val paint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.rgb(39,45,48);typeface=typeface(font)}
  // Supplied scripts have very different em-to-ink ratios. Fit actual glyph bounds,
  // rather than letting unused font ascent/descent make a script look miniature.
  var size=area.height()*3f
  while(true) {
   paint.textSize=size
   val bounds=Rect()
   val sample=text.replace('\n',' ')
   paint.getTextBounds(sample,0,sample.length,bounds)
   val margin=maxOf(1,(size*.025f).toInt())
   val spanned=SpannableString(text).apply {
    setSpan(LineHeightSpan { _,_,_,_,_,fm->
     fm.ascent=minOf(bounds.top,0)-margin;fm.top=fm.ascent
     fm.descent=maxOf(bounds.bottom,0)+margin;fm.bottom=fm.descent;fm.leading=0
    },0,length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
   }
   val layout=StaticLayout.Builder.obtain(spanned,0,spanned.length,paint,area.width().toInt())
    .setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).setLineSpacing(0f,1.03f).build()
   if(layout.lineCount<=2 && layout.height<=area.height())return layout
   check(size>5f) { "Caption does not fit. Try a shorter note." }
   size*=.92f
  }
 }
 fun draw(canvas: Canvas, width: Int, height: Int, format: FilmFormat, text: String, font: CaptionFont) = draw(canvas,width,height,format.geometry,text,font)
 fun draw(canvas: Canvas, width: Int, height: Int, format: PrintGeometry, text: String, font: CaptionFont) {
  if(text.isEmpty())return
  val area=writingArea(width,height,format)
  val layout=layout(text,font,area)
  val save=canvas.save()
  try {
   canvas.clipRect(area)
   canvas.translate(area.left,area.top+(area.height()-layout.height)/2)
   layout.draw(canvas)
  } finally {canvas.restoreToCount(save)}
 }
 fun render(base: File, format: FilmFormat, text: String, font: CaptionFont): String = render(base,format.geometry,text,font)
 fun render(base: File, format: PrintGeometry, text: String, font: CaptionFont): String {
  val normalized=CaptionRules.normalize(text)
  if(normalized.isEmpty())return base.absolutePath.also{check(base.isFile)}
  val bitmap=BitmapFactory.decodeFile(base.path,BitmapFactory.Options().apply{inMutable=true;inPreferredConfig=Bitmap.Config.ARGB_8888}) ?: error("The uncaptioned print is unavailable")
  val file=File(base.parentFile,"caption-${UUID.randomUUID()}.jpg")
  val temp=File(file.path+".tmp")
  try {
   draw(Canvas(bitmap),bitmap.width,bitmap.height,format,normalized,font)
   FileOutputStream(temp).use{check(bitmap.compress(Bitmap.CompressFormat.JPEG,94,it));it.fd.sync()}
   check(temp.renameTo(file)) { "Could not save caption" }
   return file.absolutePath
  } finally {bitmap.recycle();temp.delete()}
 }
}
