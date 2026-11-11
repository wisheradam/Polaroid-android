package com.instantroom.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.instantroom.core.*
import com.instantroom.data.Settings
import kotlin.math.*

data class CameraFinish(val body: Color,val accent: Color,val shutter: Color) {
 val ink get()=inkOn(body)
}
fun inkOn(background: Color): Color {
 val dark=Color(0xFF1B2421);val light=Color(0xFFFFF8EA)
 val chosen=if(contrast(dark,background)>contrast(light,background))dark else light
 return if(contrast(chosen,background)>=4.5f)chosen else if(contrast(Color.Black,background)>contrast(Color.White,background))Color.Black else Color.White
}
fun contrast(a: Color,b: Color): Float = (max(a.luminance(),b.luminance())+.05f)/(min(a.luminance(),b.luminance())+.05f)
fun readableAccent(color: Color,background: Color): Color {
 var result=color
 repeat(20){if(contrast(result,background)<4.5f)result=lerp(result,inkOn(background),.15f)}
 return if(contrast(result,background)>=4.5f)result else inkOn(background)
}
fun cameraFinish(style: CameraStyle, color: CameraColor): CameraFinish {
 val c=style.colorOrDefault(color)
 return when(style) {
  CameraStyle.OLD_FASION->when(c) {
   CameraColor.IVORY->CameraFinish(Color(0xFFE9DFCA),Color(0xFF735628),Color(0xFF953F30))
   CameraColor.OLIVE->CameraFinish(Color(0xFF3B4638),Color(0xFFD7B97B),Color(0xFFBF5E43))
   CameraColor.BRASS->CameraFinish(Color(0xFF6A4936),Color(0xFFE3BF7D),Color(0xFFA64335))
   else->CameraFinish(Color(0xFF252925),Color(0xFFC6A56C),Color(0xFFB34D36))
  }
  CameraStyle.INSTA_STYLE->when(c) {
   CameraColor.PINK->CameraFinish(Color(0xFFF6D6DE),Color(0xFF8F3157),Color(0xFFBD3D60))
   CameraColor.BLUE->CameraFinish(Color(0xFFCFE5F4),Color(0xFF2B617F),Color(0xFFB85061))
   CameraColor.MINT->CameraFinish(Color(0xFFD5EAD9),Color(0xFF2F6A5A),Color(0xFFB14C56))
   CameraColor.LAVENDER->CameraFinish(Color(0xFFE1D9F4),Color(0xFF6A4A91),Color(0xFFB84972))
   else->CameraFinish(Color(0xFFF9F4EE),Color(0xFF5A4989),Color(0xFFBB416D))
  }
  CameraStyle.ONE_AND_ONLY->when(c) {
   CameraColor.BLACK->CameraFinish(Color(0xFF252A2C),Color(0xFFF4C477),Color(0xFFE15A3F))
   CameraColor.RED->CameraFinish(Color(0xFFF1E2D9),Color(0xFF9E392D),Color(0xFFB32623))
   CameraColor.SPECTRUM->CameraFinish(Color(0xFFEFE5CF),Color(0xFF4B6A57),Color(0xFFB83F32))
   else->CameraFinish(Color(0xFFF6F4EF),Color(0xFF3B625C),Color(0xFFCB452F))
  }
 }
}
fun cameraCorner(style: CameraStyle)=when(style){CameraStyle.OLD_FASION->10.dp;CameraStyle.INSTA_STYLE->30.dp;CameraStyle.ONE_AND_ONLY->16.dp}

@Composable fun CameraMaterial(style: CameraStyle,color: CameraColor,content: @Composable ()->Unit) {
 val finish=cameraFinish(style,color)
 val primary=readableAccent(finish.accent,finish.body)
 val raised=lerp(finish.body,finish.ink,.06f)
 val colors=MaterialTheme.colorScheme.copy(surface=finish.body,onSurface=finish.ink,background=finish.body,onBackground=finish.ink,
  primary=primary,onPrimary=inkOn(primary),secondary=finish.shutter,onSecondary=inkOn(finish.shutter),surfaceVariant=raised,onSurfaceVariant=finish.ink,
  surfaceContainer=finish.body,surfaceContainerHigh=raised,surfaceContainerHighest=raised,outline=lerp(finish.body,finish.ink,.55f))
 MaterialTheme(colorScheme=colors) {CompositionLocalProvider(LocalContentColor provides finish.ink,content=content)}
}

@Composable fun CameraShell(style: CameraStyle,color: CameraColor,modifier: Modifier=Modifier,content: @Composable ColumnScope.()->Unit) {
 val f=cameraFinish(style,color);val shape=RoundedCornerShape(cameraCorner(style))
 CameraMaterial(style,color) {
  Box(modifier.shadow(if(style==CameraStyle.INSTA_STYLE)4.dp else 7.dp,shape).clip(shape)
   .background(Brush.verticalGradient(listOf(lerp(f.body,Color.White,.04f),f.body,lerp(f.body,Color.Black,.04f))))
   .border(if(style==CameraStyle.OLD_FASION)2.dp else 1.dp,lerp(f.body,f.accent,.45f),shape)) {
   Canvas(Modifier.matchParentSize()) {
    when(style) {
     CameraStyle.OLD_FASION->{
      for(x in 0..(size.width/7).toInt())drawLine(f.ink.copy(alpha=.035f),Offset(x*7f,0f),Offset(x*7f,size.height),1f)
      listOf(Offset(10.dp.toPx(),10.dp.toPx()),Offset(size.width-10.dp.toPx(),10.dp.toPx()),Offset(10.dp.toPx(),size.height-10.dp.toPx()),Offset(size.width-10.dp.toPx(),size.height-10.dp.toPx())).forEach {p->
       drawCircle(f.accent.copy(alpha=.7f),2.4.dp.toPx(),p);drawLine(f.body,p-Offset(1.4.dp.toPx(),0f),p+Offset(1.4.dp.toPx(),0f),1.dp.toPx())
      }
     }
     CameraStyle.INSTA_STYLE->{drawCircle(Color.White.copy(alpha=.16f),size.width*.4f,Offset(size.width,size.height));drawCircle(f.accent.copy(alpha=.045f),size.width*.3f,Offset(0f,0f))}
     CameraStyle.ONE_AND_ONLY->{
      drawLine(f.ink.copy(alpha=.09f),Offset(20.dp.toPx(),size.height-9.dp.toPx()),Offset(size.width-20.dp.toPx(),size.height-9.dp.toPx()),1.dp.toPx())
      if(color==CameraColor.SPECTRUM)listOf(Color(0xFF357C76),Color(0xFFD09D45),Color(0xFFC26349),Color(0xFF855A7A)).forEachIndexed{i,c->drawRect(c,Offset(size.width*.55f+i*13.dp.toPx(),size.height-12.dp.toPx()),Size(9.dp.toPx(),5.dp.toPx()))}
     }
    }
   }
   Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
  }
 }
}

@Composable fun CameraHousingHeader(style: CameraStyle,ready: Boolean) {
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
  Text(style.label.uppercase(),modifier=Modifier.weight(1f).padding(end=8.dp),style=MaterialTheme.typography.labelSmall,fontFamily=if(style==CameraStyle.OLD_FASION)FontFamily.Monospace else FontFamily.SansSerif,letterSpacing=1.5.sp)
  Canvas(Modifier.width(if(style==CameraStyle.OLD_FASION)66.dp else 42.dp).height(17.dp)) {
   drawRoundRect(Color(0xFF242B28),cornerRadius=CornerRadius(if(style==CameraStyle.INSTA_STYLE)size.height/2 else 2.dp.toPx()))
   when(style) {
    CameraStyle.OLD_FASION->repeat(14){i->val x=4.dp.toPx()+i*(size.width-8.dp.toPx())/13;drawLine(Color(0xFFD4C49E),Offset(x,3.dp.toPx()),Offset(x,size.height-3.dp.toPx()),1.dp.toPx())}
    CameraStyle.INSTA_STYLE->repeat(3){i->drawCircle(if(i==0 && ready)Color(0xFF93E1B4)else Color(0xFF71817A),2.dp.toPx(),Offset(size.width*(i+1)/4,size.height/2))}
    CameraStyle.ONE_AND_ONLY->drawRect(if(ready)Color(0xFF90DDB4)else Color(0xFF6B7A73),Offset(size.width-12.dp.toPx(),4.dp.toPx()),Size(6.dp.toPx(),size.height-8.dp.toPx()))
   }
  }
 }
}

@Composable fun CameraShutter(style: CameraStyle,color: CameraColor,booth: Boolean,enabled: Boolean,click: ()->Unit) {
 val f=cameraFinish(style,color)
 Box(Modifier.size(88.dp),contentAlignment=Alignment.Center) {
  Canvas(Modifier.matchParentSize()) {
   val r=size.minDimension/2
   drawCircle(Brush.radialGradient(listOf(lerp(f.body,Color.White,.5f),f.body,lerp(f.body,Color.Black,.28f))),r)
   drawCircle(if(style==CameraStyle.OLD_FASION)f.accent else f.ink.copy(alpha=.4f),r-1.dp.toPx(),style=Stroke(if(style==CameraStyle.INSTA_STYLE)4.dp.toPx()else 1.5.dp.toPx()))
   if(style==CameraStyle.OLD_FASION)repeat(40){i->val a=i*2*PI/40;drawCircle(f.ink.copy(alpha=.4f),.7.dp.toPx(),center+Offset(cos(a).toFloat()*(r-4.dp.toPx()),sin(a).toFloat()*(r-4.dp.toPx())))}
  }
  Button(onClick=click,enabled=enabled,shape=CircleShape,colors=ButtonDefaults.buttonColors(containerColor=f.shutter,contentColor=inkOn(f.shutter)),contentPadding=PaddingValues(0.dp),modifier=Modifier.size(if(style==CameraStyle.INSTA_STYLE)72.dp else 70.dp).semantics{contentDescription=if(booth)"Start Photo Booth" else "Take photograph"}) {
   Text(if(booth)"Start" else "●",fontSize=if(booth)18.sp else 32.sp)
  }
 }
}

@Composable fun CameraStylePreview(style: CameraStyle,color: CameraColor,modifier: Modifier=Modifier) {
 if(style==CameraStyle.OLD_FASION){OldFashionStylePreview(color,modifier);return}
 if(style==CameraStyle.ONE_AND_ONLY){OneAndOnlyStylePreview(color,modifier);return}
 val f=cameraFinish(style,color)
 Canvas(modifier.fillMaxWidth().height(138.dp).semantics{contentDescription="${style.label}, ${color.label} camera preview"}) {
  val w=size.width*.82f;val h=size.height*.83f;val x=(size.width-w)/2;val y=(size.height-h)/2
  val corner=when(style){CameraStyle.OLD_FASION->6.dp.toPx();CameraStyle.INSTA_STYLE->27.dp.toPx();CameraStyle.ONE_AND_ONLY->10.dp.toPx()}
  drawRoundRect(f.ink.copy(alpha=.15f),Offset(x+3.dp.toPx(),y+5.dp.toPx()),Size(w,h),CornerRadius(corner))
  drawRoundRect(f.body,Offset(x,y),Size(w,h),CornerRadius(corner))
  val lens=Offset(size.width*.5f,size.height*.46f);val r=h*.29f
  if(style==CameraStyle.OLD_FASION)repeat(24){i->val xx=x+i*w/24;drawLine(f.ink.copy(alpha=.09f),Offset(xx,y+6.dp.toPx()),Offset(xx,y+h-6.dp.toPx()),1f)}
  drawCircle(f.accent,r+4.dp.toPx(),lens);drawCircle(Color(0xFF202B29),r,lens)
  drawCircle(Color(0xFF536D68),r*.69f,lens);drawCircle(Color(0xFF132120),r*.48f,lens)
  drawCircle(Color.White.copy(alpha=.45f),r*.17f,lens-Offset(r*.22f,r*.23f))
  drawRoundRect(Color(0xFF323B37),Offset(x+w*.73f,y+h*.16f),Size(w*.16f,h*.17f),CornerRadius(if(style==CameraStyle.INSTA_STYLE)6.dp.toPx()else 1.dp.toPx()))
  drawCircle(f.shutter,h*.105f,Offset(x+w*.18f,y+h*.67f))
  drawRoundRect(f.ink,Offset(x+w*.13f,y+h*.85f),Size(w*.74f,h*.045f),CornerRadius(2.dp.toPx()))
  if(style==CameraStyle.ONE_AND_ONLY && color==CameraColor.SPECTRUM)listOf(Color(0xFF357C76),Color(0xFFD09D45),Color(0xFFC26349),Color(0xFF855A7A)).forEachIndexed{i,c->drawRect(c,Offset(x+w*.7f+i*5.dp.toPx(),y+h*.57f),Size(3.dp.toPx(),h*.18f))}
 }
}

@Composable fun CameraStyleSettings(s: Settings,set: (Settings)->Unit) {
 Panel {
  SmallLabel("CAMERA STYLE")
  CameraStylePreview(s.cameraStyle,s.cameraColor)
  FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   CameraStyle.entries.forEach{style->FilterChip(selected=s.cameraStyle==style,onClick={set(s.copy(cameraStyle=style,cameraColor=style.colorOrDefault(s.cameraColor)))},label={Text(style.label)},modifier=Modifier.semantics{contentDescription="Camera style ${style.label}"})}
  }
  SmallLabel("CAMERA COLOR")
  FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   s.cameraStyle.colors.forEach{color->FilterChip(selected=s.cameraColor==color,onClick={set(s.copy(cameraColor=color))},label={Text(color.label)},leadingIcon={Box(Modifier.size(14.dp).background(cameraFinish(s.cameraStyle,color).body,CircleShape).border(1.dp,MaterialTheme.colorScheme.outline,CircleShape))},modifier=Modifier.semantics{contentDescription="Camera color ${color.label}"})}
  }
  Text("Camera materials keep their chosen colors. Light, Dark and Follow System control the surrounding app.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
 }
}
