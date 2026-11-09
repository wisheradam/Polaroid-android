package com.instantroom.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.semantics.*
import com.instantroom.core.*
import com.instantroom.data.PhotoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*
import kotlin.random.Random

val Ink=Color(0xFF202823)
val Cream=Color(0xFFF3EBD9)
val Amber=Color(0xFFE6B578)
val Rust=Color(0xFFB9583F)
@Composable fun InstantTheme(content: @Composable ()->Unit) {
 MaterialTheme(colorScheme=darkColorScheme(primary=Amber,onPrimary=Ink,background=Color(0xFF151C18),surface=Ink,onSurface=Cream,secondary=Color(0xFF9FB7A6)),typography=Typography(),content=content)
}
@Composable fun SmallLabel(text: String, modifier: Modifier=Modifier) { Text(text,modifier,style=MaterialTheme.typography.labelSmall,letterSpacing=2.sp,color=Amber) }
@Composable fun Panel(modifier: Modifier=Modifier, content: @Composable ColumnScope.()->Unit) { Column(modifier.background(Ink,RoundedCornerShape(20.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content) }
private val imageCache=object: android.util.LruCache<String,android.graphics.Bitmap>(20*1024*1024) { override fun sizeOf(key:String,value:android.graphics.Bitmap)=value.byteCount }
@Composable fun FilmPrint(photo: PhotoEntity?, modifier: Modifier=Modifier, format: FilmFormat=photo?.let{FilmFormat.valueOf(it.format)} ?: FilmFormat.CLASSIC, maxSide: Int=1000) {
 val path=photo?.processed.orEmpty()
 val bitmap by produceState<android.graphics.Bitmap?>(null,path,maxSide) {
  value=withContext(Dispatchers.IO) {
   if(path.isBlank()) null else {
    val key="$path/$maxSide"
    imageCache.get(key) ?: run {
     val o=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeFile(path,o)
     var sample=1;while(max(o.outWidth,o.outHeight)/sample>maxSide*1.3)sample*=2
     o.inJustDecodeBounds=false;o.inSampleSize=sample
     BitmapFactory.decodeFile(path,o)?.also { imageCache.put(key,it) }
    }
   }
  }
 }
 val progress=photo?.progress ?: 0f
 Canvas(modifier.aspectRatio(format.aspect).semantics { contentDescription=if(progress>=1f) "Developed ${format.label} photograph" else "Undeveloped ${format.label} photograph; shake to reveal" }) {
  drawRect(Color(0xFFF7F2E2))
  bitmap?.let { drawImage(it.asImageBitmap(),dstSize=IntSize(size.width.toInt(),size.height.toInt()),filterQuality=FilterQuality.Medium) }
  if(progress<1f || bitmap==null) {
   val s=size.width/format.width
   val left=(size.width-format.imageWidth*s)/2
   val top=format.top*s
   val rect=Rect(left,top,left+format.imageWidth*s,top+format.imageHeight*s)
   val pathClip=Path().apply {if(format==FilmFormat.ROUND)addOval(rect) else addRect(rect)}
   clipPath(pathClip) {
    val cover=if(bitmap==null)1f else (1-progress).pow(1.35f)
    drawRect(Color(0xFFBDBDA2),topLeft=rect.topLeft,size=rect.size,alpha=cover)
    val r=Random(photo?.id.hashCode())
    repeat(9) {
     val center=Offset(rect.left+r.nextFloat()*rect.width,rect.top+r.nextFloat()*rect.height)
     val radius=rect.width*(.15f+r.nextFloat()*.35f)
     drawCircle(Brush.radialGradient(listOf(Color(0xFF6B7E58).copy(alpha=(cover*(1-cover)*.65f)),Color.Transparent),center,radius),radius,center)
    }
    if(progress>0f)drawRect(Color(0xFFCF9552),topLeft=rect.topLeft,size=rect.size,alpha=cover*(1-cover)*.28f)
   }
  }
 }
}
