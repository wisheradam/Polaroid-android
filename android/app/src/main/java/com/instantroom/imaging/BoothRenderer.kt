package com.instantroom.imaging

import android.content.Context
import android.graphics.*
import androidx.exifinterface.media.ExifInterface
import androidx.core.graphics.createBitmap
import com.instantroom.core.*
import com.instantroom.data.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.*

/** Full sensor JPEGs are retained. Each entire upright image is fitted into its cell. */
class BoothRenderer(private val context: Context) {
 fun render(sources: List<PhotoEntity>, layout: BoothLayout, id: String): String {
  require(sources.size==4)
  val g=layout.geometry
  val low=Preferences(context).state.value.lowMemory
  val width=if(layout==BoothLayout.GRID)if(low)1600 else 2400 else if(low)800 else 1200
  val height=(width/g.aspect).roundToInt()
  val output=createBitmap(width,height)
  val canvas=Canvas(output);canvas.drawColor(Color.rgb(247,242,226))
  val scale=width/g.width
  try {
   sources.forEachIndexed {index,p->
    val bitmap=upright(p.original,1600)
    try {
     val cell=g.cells[index]
     val factor=min(cell.width*scale/bitmap.width,cell.height*scale/bitmap.height)
     val w=bitmap.width*factor;val h=bitmap.height*factor
     val rect=RectF(cell.left*scale+(cell.width*scale-w)/2,cell.top*scale+(cell.height*scale-h)/2,0f,0f).apply{right=left+w;bottom=top+h}
     val saturation=when(Appearance.valueOf(p.appearance)){Appearance.MONO->0f;Appearance.WARM->.67f;else->.83f}
     val matrix=ColorMatrix().apply{setSaturation(saturation)}
     val warm=p.appearance==Appearance.WARM.name
     val mono=p.appearance==Appearance.MONO.name
     matrix.postConcat(ColorMatrix(if(mono)floatArrayOf(.9f,0f,0f,0f,10f,0f,.9f,0f,0f,10f,0f,0f,.9f,0f,10f,0f,0f,0f,1f,0f)else floatArrayOf(.9f,0f,0f,0f,if(warm)20f else 9f,0f,.89f,0f,0f,10f,0f,0f,if(warm).76f else .89f,0f,if(warm)1f else 12f,0f,0f,0f,1f,0f)))
     val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply{colorFilter=ColorMatrixColorFilter(matrix)}
     canvas.drawBitmap(bitmap,null,rect,paint)
     val vignette=Paint().apply{shader=RadialGradient(rect.centerX(),rect.centerY(),max(w,h)*.8f,intArrayOf(Color.TRANSPARENT,0x250F140E),null,Shader.TileMode.CLAMP)}
     canvas.drawRect(rect,vignette)
    }finally{bitmap.recycle()}
   }
   val file=File(context.filesDir,"photos/$id-film.jpg").also{it.parentFile!!.mkdirs()}
   val temp=File(file.path+".tmp")
   try {
    FileOutputStream(temp).use{check(output.compress(Bitmap.CompressFormat.JPEG,95,it));it.fd.sync()}
    check(temp.renameTo(file));return file.absolutePath
   }finally{temp.delete()}
  }finally{output.recycle()}
 }
 fun upright(path: String, longest: Int): Bitmap {
  val options=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeFile(path,options)
  var sample=1;while(max(options.outWidth,options.outHeight)/sample>longest*1.4)sample*=2
  options.inJustDecodeBounds=false;options.inSampleSize=sample
  val source=BitmapFactory.decodeFile(path,options) ?: error("A booth source image cannot be decoded")
  val matrix=Matrix().apply {
   when(ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL)) {
    ExifInterface.ORIENTATION_ROTATE_90->postRotate(90f)
    ExifInterface.ORIENTATION_ROTATE_180->postRotate(180f)
    ExifInterface.ORIENTATION_ROTATE_270->postRotate(270f)
    ExifInterface.ORIENTATION_FLIP_HORIZONTAL->postScale(-1f,1f)
    ExifInterface.ORIENTATION_FLIP_VERTICAL->postScale(1f,-1f)
    ExifInterface.ORIENTATION_TRANSPOSE->{postRotate(90f);postScale(-1f,1f)}
    ExifInterface.ORIENTATION_TRANSVERSE->{postRotate(270f);postScale(-1f,1f)}
   }
  }
  return try{Bitmap.createBitmap(source,0,0,source.width,source.height,matrix,true).also{if(it!==source)source.recycle()}}catch(e:Exception){source.recycle();throw e}
 }
}
