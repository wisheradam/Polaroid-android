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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.core.view.WindowCompat
import com.instantroom.data.PhotoEntity
import com.instantroom.data.geometry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*
import kotlin.random.Random

private val DarkPalette=darkColorScheme(
 primary=Color(0xFFE6B578),onPrimary=Color(0xFF202823),background=Color(0xFF151C18),
 surface=Color(0xFF202823),onSurface=Color(0xFFF3EBD9),onBackground=Color(0xFFF3EBD9),
 secondary=Color(0xFFAF4C36),onSecondary=Color(0xFFFFF3DF),
 primaryContainer=Color(0xFF4B3B26),onPrimaryContainer=Color(0xFFF3EBD9),
 secondaryContainer=Color(0xFF3C4A41),onSecondaryContainer=Color(0xFFE4EBD8),
 surfaceVariant=Color(0xFF344038),onSurfaceVariant=Color(0xFFBFCABB),outline=Color(0xFF859181),
 surfaceContainerHighest=Color(0xFF344038),surfaceContainerHigh=Color(0xFF2C372F),
 surfaceContainer=Color(0xFF273028),surfaceContainerLow=Color(0xFF1C251F),surfaceContainerLowest=Color(0xFF101713),
 surfaceTint=Color(0xFFE6B578)
)
private val LightPalette=lightColorScheme(
 primary=Color(0xFF76501F),onPrimary=Color(0xFFFFF8ED),background=Color(0xFFECE3D2),
 surface=Color(0xFFFAF3E6),onSurface=Color(0xFF243029),onBackground=Color(0xFF243029),
 secondary=Color(0xFF9F452F),onSecondary=Color(0xFFFFF5E8),outline=Color(0xFF786B58),
 primaryContainer=Color(0xFFF0DDBB),onPrimaryContainer=Color(0xFF352915),
 secondaryContainer=Color(0xFFE1E8D8),onSecondaryContainer=Color(0xFF26362B),
 surfaceVariant=Color(0xFFE2D7C5),onSurfaceVariant=Color(0xFF524B3E),outlineVariant=Color(0xFFCDBFA7),
 surfaceContainerHighest=Color(0xFFE3D8C5),surfaceContainerHigh=Color(0xFFECE2D1),
 surfaceContainer=Color(0xFFF0E7D7),surfaceContainerLow=Color(0xFFF6EEDF),surfaceContainerLowest=Color(0xFFFFFAF1),
 surfaceTint=Color(0xFF76501F)
)
val Ink: Color @Composable get()=MaterialTheme.colorScheme.surface
val Cream: Color @Composable get()=MaterialTheme.colorScheme.onSurface
val Amber: Color @Composable get()=MaterialTheme.colorScheme.primary
val Rust: Color @Composable get()=MaterialTheme.colorScheme.secondary
val LocalInstantDark=staticCompositionLocalOf{true}
@Composable fun InstantTheme(mode: ThemeMode=ThemeMode.SYSTEM, content: @Composable ()->Unit) {
 val dark=mode.isDark(isSystemInDarkTheme())
 val view=LocalView.current
 SideEffect {
  if(!view.isInEditMode) runCatching {
   WindowCompat.getInsetsController(view.context.activity().window,view).apply {isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark}
  }
 }
 CompositionLocalProvider(LocalInstantDark provides dark) {
  MaterialTheme(colorScheme=if(dark)DarkPalette else LightPalette,typography=Typography(),content=content)
 }
}
@Composable fun SmallLabel(text: String, modifier: Modifier=Modifier) { Text(text,modifier,style=MaterialTheme.typography.labelSmall,letterSpacing=2.sp,color=Amber) }
@Composable fun Panel(modifier: Modifier=Modifier, content: @Composable ColumnScope.()->Unit) { Column(modifier.background(Ink,RoundedCornerShape(20.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content) }
private val imageCache=object: android.util.LruCache<String,android.graphics.Bitmap>(20*1024*1024) { override fun sizeOf(key:String,value:android.graphics.Bitmap)=value.byteCount }
@Composable fun FilmPrint(photo: PhotoEntity?, modifier: Modifier=Modifier, format: PrintGeometry=photo?.geometry ?: FilmFormat.CLASSIC.geometry, maxSide: Int=1000) {
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
 Canvas(modifier.aspectRatio(format.aspect).testTag(if(bitmap==null)"film-print-loading" else "film-print-ready").semantics { contentDescription=if(progress>=1f) "Developed ${format.label} photograph" else "Undeveloped ${format.label} photograph; shake to reveal" }) {
  drawRect(Color(0xFFF7F2E2))
  bitmap?.let { drawImage(it.asImageBitmap(),dstSize=IntSize(size.width.toInt(),size.height.toInt()),filterQuality=FilterQuality.Medium) }
  if(progress<1f || bitmap==null) {
   val s=size.width/format.width
   format.imageRegions.forEach {cell->
   val left=cell.left*s
   val top=cell.top*s
   val rect=Rect(left,top,left+cell.width*s,top+cell.height*s)
   val pathClip=Path().apply {if(format.round)addOval(rect) else addRect(rect)}
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
}
