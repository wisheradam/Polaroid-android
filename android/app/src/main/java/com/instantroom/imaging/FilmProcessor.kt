package com.instantroom.imaging

import android.graphics.*
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import androidx.exifinterface.media.ExifInterface
import com.instantroom.core.*
import com.instantroom.data.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.*
import kotlin.random.Random

class FilmProcessor(private val prefs: Preferences) {
 fun process(photo: PhotoEntity): String {
  val f=FilmFormat.valueOf(photo.format)
  val options=BitmapFactory.Options().apply { inJustDecodeBounds=true }
  BitmapFactory.decodeFile(photo.original,options)
  val longest=if(prefs.state.value.lowMemory) 1100 else 1800
  var sample=1
  while(max(options.outWidth,options.outHeight)/sample > longest*1.4)sample*=2
  options.inJustDecodeBounds=false; options.inSampleSize=sample
  val source=BitmapFactory.decodeFile(photo.original,options) ?: error("Cannot decode original image")
  val exif=ExifInterface(photo.original)
  val matrix=Matrix().apply {
   when(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL)) {
    ExifInterface.ORIENTATION_ROTATE_90->postRotate(90f)
    ExifInterface.ORIENTATION_ROTATE_180->postRotate(180f)
    ExifInterface.ORIENTATION_ROTATE_270->postRotate(270f)
    ExifInterface.ORIENTATION_FLIP_HORIZONTAL->postScale(-1f,1f)
    ExifInterface.ORIENTATION_FLIP_VERTICAL->postScale(1f,-1f)
    ExifInterface.ORIENTATION_TRANSPOSE->{postRotate(90f);postScale(-1f,1f)}
    ExifInterface.ORIENTATION_TRANSVERSE->{postRotate(270f);postScale(-1f,1f)}
   }
  }
  val upright=Bitmap.createBitmap(source,0,0,source.width,source.height,matrix,true)
  val width=if(prefs.state.value.lowMemory) 900 else 1440
  val height=(width/f.aspect).roundToInt()
  val output=createBitmap(width,height)
  val c=Canvas(output); c.drawColor(Color.rgb(247,242,226))
  val scale=width/f.width
  val iw=f.imageWidth*scale;val ih=f.imageHeight*scale
  val rect=RectF((width-iw)/2,f.top*scale,(width+iw)/2,f.top*scale+ih)
  val ratio=f.imageAspect
  val cropW=min(upright.width.toFloat(),upright.height*ratio)
  val cropH=cropW/ratio
  val src=Rect(((upright.width-cropW)/2).toInt(),((upright.height-cropH)/2).toInt(),((upright.width+cropW)/2).toInt(),((upright.height+cropH)/2).toInt())
  val saturation=when(Appearance.valueOf(photo.appearance)){Appearance.MONO->0f;Appearance.WARM->.67f;else->.83f}
  val colors=ColorMatrix().apply { setSaturation(saturation) }
  val mono=Appearance.valueOf(photo.appearance)==Appearance.MONO
  val warm=Appearance.valueOf(photo.appearance)==Appearance.WARM
  colors.postConcat(ColorMatrix(if(mono) floatArrayOf(.9f,0f,0f,0f,10f,0f,.9f,0f,0f,10f,0f,0f,.9f,0f,10f,0f,0f,0f,1f,0f) else floatArrayOf(.90f,0f,0f,0f,if(warm)20f else 9f, 0f,.89f,0f,0f,10f, 0f,0f,if(warm).76f else .89f,0f,if(warm)1f else 12f, 0f,0f,0f,1f,0f)))
  val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { colorFilter=ColorMatrixColorFilter(colors) }
  c.withSave {
  if(f==FilmFormat.ROUND) c.clipPath(Path().apply { addOval(rect,Path.Direction.CW) }) else c.clipRect(rect)
  // Multiple subpixel samples produce restrained optical softness.
  c.drawBitmap(upright,src,rect,paint)
  paint.alpha=24;c.drawBitmap(upright,src,RectF(rect).apply { offset(.8f,.5f) },paint);paint.alpha=255
  val vignette=Paint().apply { shader=RadialGradient(rect.centerX(),rect.centerY(),max(iw,ih)*.7f,intArrayOf(Color.TRANSPARENT,if(mono)0x430D0D0D else 0x430F140E),floatArrayOf(.25f,1f),Shader.TileMode.CLAMP) }
  c.drawRect(rect,vignette)
  val random=Random(photo.id.hashCode());val grain=Paint()
  repeat(width*height/65) {
   grain.color=if(random.nextBoolean())0x12FFFFFF else if(mono)0x13101010 else 0x130F130B
   c.drawCircle(rect.left+random.nextFloat()*iw,rect.top+random.nextFloat()*ih,.55f+random.nextFloat()*.75f,grain)
  }
  if(prefs.state.value.imperfections) {
   val blotch=Paint().apply { shader=RadialGradient(rect.left,rect.bottom,iw*.45f,intArrayOf(if(mono)0x19101010 else 0x19CCA56C,Color.TRANSPARENT),null,Shader.TileMode.CLAMP) };c.drawRect(rect,blotch)
  }
  }
  val file=File(File(photo.original).parentFile,"${photo.id}-film.jpg")
  val temp=File(file.path+".tmp")
  FileOutputStream(temp).use { check(output.compress(Bitmap.CompressFormat.JPEG,94,it)); it.fd.sync() }
  check(temp.renameTo(file)) { "Cannot finalize film image" }
  if(upright!==source)upright.recycle();source.recycle();output.recycle()
  return file.absolutePath
 }
}
