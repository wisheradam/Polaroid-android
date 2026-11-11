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
 primary=Color(0xFF7EDCEA),onPrimary=Color(0xFF17191A),background=Color(0xFF17191A),
 surface=Color(0xFF242526),onSurface=Color(0xFFFFF9EF),onBackground=Color(0xFFFFF9EF),
 secondary=Color(0xFFFFB4AB),onSecondary=Color(0xFF242526),
 primaryContainer=Color(0xFF30474A),onPrimaryContainer=Color(0xFFFFF9EF),
 secondaryContainer=Color(0xFF4B3431),onSecondaryContainer=Color(0xFFFFF9EF),
 surfaceVariant=Color(0xFF303133),onSurfaceVariant=Color(0xFFBDB8AF),outline=Color(0xFFBDB8AF),
 surfaceContainerHighest=Color(0xFF38393A),surfaceContainerHigh=Color(0xFF303133),
 surfaceContainer=Color(0xFF292A2C),surfaceContainerLow=Color(0xFF202122),surfaceContainerLowest=Color(0xFF101112),
 error=Color(0xFFFFB4AB)
)
private val LightPalette=lightColorScheme(
 primary=Color(0xFF007C91),onPrimary=Color.White,background=Color(0xFFFFF9EF),
 surface=Color(0xFFFFF9EF),onSurface=Color(0xFF242526),onBackground=Color(0xFF242526),
 secondary=Color(0xFFB3261E),onSecondary=Color.White,outline=Color(0xFF777571),
 primaryContainer=Color(0xFFD8EFF0),onPrimaryContainer=Color(0xFF242526),
 secondaryContainer=Color(0xFFF1E2D9),onSecondaryContainer=Color(0xFF242526),
 surfaceVariant=Color(0xFFEDE5D8),onSurfaceVariant=Color(0xFF5D5A55),outlineVariant=Color(0xFFBDB8AF),
 surfaceContainerHighest=Color(0xFFE3DACE),surfaceContainerHigh=Color(0xFFEDE5D8),
 surfaceContainer=Color(0xFFF3EDE3),surfaceContainerLow=Color(0xFFF8F2E8),surfaceContainerLowest=Color.White,
 error=Color(0xFFB3261E)
)
val Ink: Color @Composable get()=MaterialTheme.colorScheme.surface
val Cream: Color @Composable get()=MaterialTheme.colorScheme.onSurface
val Amber: Color @Composable get()=MaterialTheme.colorScheme.primary
val Rust: Color @Composable get()=MaterialTheme.colorScheme.secondary
val LocalInstantDark=staticCompositionLocalOf{true}
@Composable fun InstantTheme(mode: ThemeMode=ThemeMode.SYSTEM, cameraStyle: CameraStyle=CameraStyle.ONE_AND_ONLY, cameraColor: CameraColor=CameraColor.WHITE, content: @Composable ()->Unit) {
 val dark=mode.isDark(isSystemInDarkTheme())
 val view=LocalView.current
 SideEffect {
  if(!view.isInEditMode) runCatching {
   WindowCompat.getInsetsController(view.context.activity().window,view).apply {isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark}
  }
 }
 CompositionLocalProvider(LocalInstantDark provides dark) {
  val base=if(dark)DarkPalette else LightPalette
  val finish=cameraFinish(cameraStyle,cameraColor)
  val primary=if(cameraStyle==CameraStyle.ONE_AND_ONLY)base.primary else readableAccent(finish.accent,base.surface)
  MaterialTheme(colorScheme=base.copy(primary=primary,onPrimary=inkOn(primary),secondary=if(cameraStyle==CameraStyle.ONE_AND_ONLY)base.secondary else finish.shutter,onSecondary=if(cameraStyle==CameraStyle.ONE_AND_ONLY)base.onSecondary else inkOn(finish.shutter)),
   shapes=Shapes(small=RoundedCornerShape(cameraCorner(cameraStyle)/2),medium=RoundedCornerShape(cameraCorner(cameraStyle)),large=RoundedCornerShape(cameraCorner(cameraStyle))),typography=InstantTypography,content=content)
 }
}
@Composable fun SmallLabel(text: String, modifier: Modifier=Modifier) { Text(text,modifier,style=MaterialTheme.typography.labelSmall,letterSpacing=2.sp,color=Amber) }
@Composable fun Panel(modifier: Modifier=Modifier, content: @Composable ColumnScope.()->Unit) { Column(modifier.background(Ink,MaterialTheme.shapes.large).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content) }
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
