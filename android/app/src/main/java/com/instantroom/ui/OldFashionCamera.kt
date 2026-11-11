package com.instantroom.ui

import android.animation.ValueAnimator
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.instantroom.R
import com.instantroom.core.*
import com.instantroom.data.*
import kotlin.math.hypot
import kotlin.math.min

/** Physical roles are scoped to this family; semantic app/One and Only palettes stay independent. */
object OldFashionTokens {
 val Chrome=Color(0xFF10130F)
 val Optical=Color(0xFF070907)
 val Ivory=Color(0xFFF2E8CF)
 val Metal=Color(0xFFD8C9A5)
 val Gold=Color(0xFFC6A56C)
 val Shutter=Color(0xFFE63725)
 val Ready=Color(0xFFA9DFAD)
 val Focus=Color(0xFF8ADDEB)
 val Spectrum=listOf(Color(0xFF159EC0),Color(0xFF55953F),Color(0xFFF1C837),Color(0xFFF39A26),Color(0xFFD93757))
}
private val LocalOldTexture=staticCompositionLocalOf<ImageBitmap?>{null}
@Composable private fun OldMaterials(content: @Composable ()->Unit) {
 val resources=LocalResources.current
 val tile=remember(resources){ImageBitmap.imageResource(resources,R.drawable.of_matte_tile)}
 CompositionLocalProvider(LocalOldTexture provides tile,content=content)
}
/** One shared 128px asset; cached repeat brush, not a flattened camera bitmap. */
@Composable private fun OldSurface(color: Color,modifier: Modifier=Modifier,radius: Dp=8.dp,elevation: Dp=5.dp,outline: Color?=null,content: @Composable BoxScope.()->Unit={}) {
 val tile=LocalOldTexture.current
 val texture=remember(tile){tile?.let{ShaderBrush(ImageShader(it,TileMode.Repeated,TileMode.Repeated))}}
 val shape=RoundedCornerShape(radius)
 Box(modifier.shadow(elevation,shape).clip(shape).background(color).drawWithCache {
  val ramp=Brush.verticalGradient(listOf(Color.White.copy(alpha=.14f),Color.Transparent,Color.Black.copy(alpha=.34f)))
  onDrawBehind {
   texture?.let{drawRect(it,alpha=if(Build.VERSION.SDK_INT>=29).42f else .045f,blendMode=if(Build.VERSION.SDK_INT>=29)BlendMode.Softlight else BlendMode.SrcOver)}
   drawRect(ramp)
  }
 }.border(if(outline==null)1.dp else 2.dp,outline ?: lerp(color,OldFashionTokens.Ivory,.24f),shape),content=content)
}
@Composable private fun Engraving(content: @Composable ()->Unit) {
 val density=LocalDensity.current
 // Physical engravings retain their size; large semantic controls are duplicated in the tray.
 CompositionLocalProvider(LocalDensity provides Density(density.density,1f),content=content)
}

@Composable fun OldFashionCamera(
 settings: Settings,cartridge: CartridgeEntity?,cap: CaptureUi,initialized: Boolean,
 captureReady: Boolean,switchReady: Boolean,hasFlash: Boolean,booth: Boolean,photo: PhotoEntity?,
 onCapture: ()->Unit,onLoad: ()->Unit,onGallery: ()->Unit,onSettings: ()->Unit,onSwitch: ()->Unit,
 onFlash: (Boolean)->Unit,onTimer: (SelfTimer)->Unit,onBooth: (Boolean)->Unit,onLayout: (BoothLayout)->Unit,
 onSelfieLight: (SelfieLightMode)->Unit,onCancel: ()->Unit={},onRecover: ()->Unit={},
 cameraAccess: (() -> Unit)?=null,openAndroidSettings: (() -> Unit)?=null,preview: @Composable ()->Unit
) {
 val busy=cap.phase!=CapturePhase.IDLE
 val canCapture=captureReady && initialized && !busy && (cartridge?.remaining ?: 0)>=(if(booth)4 else 1)
 val captureStatus=when {
  !initialized->"Restoring archive"
  busy->cap.phase.name.lowercase()
  !captureReady->"Camera unavailable"
  cartridge==null->"Load film"
  (cartridge.remaining)<(if(booth)4 else 1)->if(booth)"Four exposures required" else "Film empty"
  else->"Ready"
 }
 val finish=cameraFinish(CameraStyle.OLD_FASION,settings.cameraColor)
 var sheet by rememberSaveable{mutableStateOf<String?>(null)}
 val scroll=rememberScrollState()
 LaunchedEffect(busy){if(busy)scroll.scrollTo(0)}
 val fontScale=LocalDensity.current.fontScale
 val motion=ValueAnimator.areAnimatorsEnabled()
 BoxWithConstraints(Modifier.fillMaxSize().background(OldFashionTokens.Chrome).testTag("old-fashion-camera").semantics{isTraversalGroup=true}) {
  val g=OldFashionLayout.fit(maxWidth.value.coerceAtLeast(240f),maxHeight.value,fontScale)
  val s=g.scale
  OldMaterials {
   Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
    Box(Modifier.fillMaxWidth().height(g.height.dp).zIndex(if(cap.phase==CapturePhase.EJECTING)1f else 0f)) {
     OldSurface(finish.body,Modifier.offset(12.dp,g.bodyY.dp).width((g.width-24).dp).height((g.height-g.bodyY-6).dp),10.dp)
     OldSupports(Modifier.offset((12+12*s).dp,(g.flashTop+114*s).dp).size((345*s).dp,(70*s).dp))
     OldFlashHousing(finish,settings,cap,hasFlash,Modifier.offset(12.dp,g.flashTop.dp).size((369*s).dp,(116*s).dp))
     OldSurface(finish.body,Modifier.offset((12+8*s).dp,g.bodyY.dp).size((353*s).dp,(250*s).dp),12.dp) {
      Canvas(Modifier.fillMaxSize()){drawLine(Color(0xFF73796A).copy(alpha=.5f),Offset(8*s.dp.toPx(),6*s.dp.toPx()),Offset(size.width-8*s.dp.toPx(),6*s.dp.toPx()),2*s.dp.toPx())}
     }
     OldViewfinder(finish.accent,Modifier.offset(g.viewfinderX.dp,g.viewfinderY.dp).size((145*s).dp,(158*s).dp).semantics{traversalIndex=1f},preview)
     OldGoldMesh(Modifier.offset((12+18*s).dp,(g.bodyY+80*s).dp).size((92*s).dp))
     OldOptics(Modifier.offset((g.width-12-100*s).dp,(g.bodyY+36*s).dp).size((80*s).dp,(150*s).dp))
     val physical=if(g.essentialControls)Modifier.clearAndSetSemantics{} else Modifier
     OldCounter(cartridge?.remaining,initialized && !busy,!g.essentialControls,onLoad,
      Modifier.offset((12+16*s).dp,(g.bodyY+8*s).dp).then(physical).semantics{traversalIndex=0f})
     Row(Modifier.offset(((g.width-168-64*s)/2).dp,(g.bodyY+198*s).dp).then(physical),horizontalArrangement=Arrangement.spacedBy((32*s).dp)) {
      OldControl("Self-timer",R.drawable.of_timer,!busy,{sheet="timer"},interactive=!g.essentialControls,
       state=if(settings.timer==SelfTimer.OFF)"Off" else "${settings.timer.seconds} seconds",index=3f)
      OldControl(if(settings.front)"Switch to rear camera" else "Switch to front camera",R.drawable.of_switch,switchReady && !busy,onSwitch,interactive=!g.essentialControls,index=4f)
      if(settings.front)OldControl("Selfie lighting",R.drawable.of_flash,!busy,{sheet="selfie"},interactive=!g.essentialControls,state=settings.selfieLight.label,index=5f)
      else OldFlashControl(settings.rearFlash,hasFlash,captureReady && !busy,onFlash,Modifier.then(if(g.essentialControls)Modifier.clearAndSetSemantics{} else Modifier),!g.essentialControls)
     }
     OldCompartment(finish,Modifier.offset((12+8*s).dp,g.compartmentY.dp).size((353*s).dp,(190*s).dp))
     OldEdgeShutter(canCapture,booth,captureStatus,onCapture,Modifier.offset(2.dp,g.shutterY.dp).then(physical).semantics{traversalIndex=2f},!g.essentialControls)
     if(!g.essentialControls)OldNavigation(!busy,booth,onGallery,{sheet="booth"},onSettings,
      Modifier.offset(28.dp,g.navigationY.dp).width((g.width-56).dp))
     if(cap.phase==CapturePhase.EJECTING) {
      val aspect=photo?.geometry?.aspect ?: FilmFormat.CLASSIC.aspect
      val available=(g.viewportHeight-g.mouthY-8).coerceAtLeast(60f)
      val w=minOf(g.width-64,210f,available*aspect).dp
      val h=w/aspect
      Box(Modifier.offset((g.width.dp-w)/2,g.mouthY.dp).width(w).height(h).clipToBounds().testTag("old-fashion-paper-mouth")) {
       FilmPrint(photo,Modifier.width(w).offset(y=h*(OldFashionMotion.paperPosition(cap.ejection,motion)-1)).testTag("old-fashion-ejecting-print"))
      }
     }
     // Foreground lip always covers the top of moving paper (layer 70).
     Canvas(Modifier.offset((12+29*s).dp,g.mouthY.dp).size((311*s).dp,(3*s).dp).testTag("old-fashion-front-lip")) {drawRoundRect(Color(0xFF363D31),cornerRadius=CornerRadius(2*s.dp.toPx()))}
     if(busy) {
      val modifier=Modifier.offset(24.dp,(g.bodyY+if(cap.countdown>0)20*s else 196*s).dp).width((g.width-48).dp)
       .background(Color.Black.copy(alpha=.9f),RoundedCornerShape(12.dp)).padding(8.dp).testTag("old-fashion-capture-status")
      if(cap.countdown>0)Column(modifier,horizontalAlignment=Alignment.CenterHorizontally) {
       CountdownOverlay(cap.countdown,cap.shot,cap.booth)
       TextButton(onClick=onCancel,modifier=Modifier.heightIn(min=48.dp).semantics{contentDescription=if(cap.booth)"Cancel session" else "Cancel countdown"}){Text("Cancel",color=OldFashionTokens.Ivory)}
      }else Row(modifier,verticalAlignment=Alignment.CenterVertically) {
       Text(when(cap.phase){CapturePhase.CHARGING->"Charging the flash…";CapturePhase.EJECTING->"The rollers are turning…";CapturePhase.PROCESSING->"Preparing your photograph…";else->"Capturing…"},Modifier.weight(1f),color=OldFashionTokens.Ivory,style=MaterialTheme.typography.bodyMedium)
       TextButton(onClick=onCancel,modifier=Modifier.heightIn(min=48.dp).semantics{contentDescription=if(cap.booth)"Cancel session" else "Cancel countdown"}){Text("Cancel",color=OldFashionTokens.Ivory)}
      }
     }
    }
    if(g.essentialControls)Column(Modifier.fillMaxWidth().padding(24.dp).testTag("old-fashion-essential-controls"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
     Text("${cartridge?.remaining ?: 0} shots remaining",color=OldFashionTokens.Ivory,style=MaterialTheme.typography.titleLarge)
     OldTrayAction(if(booth)"Start Photo Booth" else "Take photograph",canCapture,onCapture,shutter=true,index=2f)
     OldTrayAction("Replace cartridge",initialized && !busy,onLoad,index=0f)
     OldTrayAction("Self-timer · ${settings.timer.label}",!busy,{sheet="timer"},index=3f)
     OldTrayAction(if(settings.front)"Switch to rear camera" else "Switch to front camera",switchReady && !busy,onSwitch,index=4f)
     if(settings.front)OldTrayAction("Selfie lighting · ${settings.selfieLight.label}",!busy,{sheet="selfie"},index=5f)
     else OldTrayAction(if(!hasFlash)"Rear camera flash unavailable" else "Rear camera flash · ${if(settings.rearFlash)"On" else "Off"}",captureReady && hasFlash && !busy,{onFlash(!settings.rearFlash)},index=5f)
     OldNavigation(!busy,booth,onGallery,{sheet="booth"},onSettings,Modifier.fillMaxWidth(),stacked=true)
    }
    if(cameraAccess!=null)Column(Modifier.fillMaxWidth().padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
     Button(onClick=cameraAccess,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Enable camera")}
     openAndroidSettings?.let{TextButton(onClick=it){Text("Open Android settings")}}
    }
    if(booth && (cartridge?.remaining ?: 0)<4)Text("Photo Booth needs four exposures. Manually replace the cartridge.",Modifier.padding(16.dp),color=OldFashionTokens.Ivory)
    if(!initialized)TextButton(onClick=onRecover){Text("Retry archive recovery",color=OldFashionTokens.Ivory)}
    Text(if(booth)"Photo Booth · four photos, five seconds apart" else cartridge?.let{"${FilmFormat.valueOf(it.format).label} · ${Appearance.valueOf(it.appearance).label}"} ?: "Tap the counter to load film.",Modifier.padding(16.dp),color=OldFashionTokens.Ivory,style=MaterialTheme.typography.bodySmall)
    Text("The optical viewfinder crops the live view. The selected film controls the saved photograph.",Modifier.padding(horizontal=16.dp).padding(bottom=16.dp),color=OldFashionTokens.Ivory.copy(alpha=.8f),style=MaterialTheme.typography.bodySmall)
   }
  }
 }
 when(sheet) {
  "timer"->CameraTimerSheet(settings,busy,{sheet=null},onTimer)
  "selfie"->CameraSelfieSheet(settings,busy,{sheet=null},onSelfieLight)
  "booth"->CameraBoothSheet(settings,busy,{sheet=null},onBooth,onLayout,onSelfieLight)
 }
}

@Composable private fun OldSupports(modifier: Modifier) {
 Canvas(modifier.testTag("old-fashion-optical-decoration").clearAndSetSemantics{}) {
  val sx=size.width/345;val sy=size.height/70
  drawRect(Color(0xFFFFF9EF),Offset(8*sx,7*sy),Size(329*sx,53*sy))
  for(x in listOf(1f,333f))drawRect(Brush.horizontalGradient(listOf(Color(0xFF5B6155),Color(0xFF171D16),Color(0xFF070A06))),Offset(x*sx,0f),Size(11*sx,size.height))
  drawRect(Brush.horizontalGradient(listOf(Color(0xFF42473D),Color(0xFF0A0E08),Color(0xFF151B10))),Offset(142*sx,0f),Size(60*sx,size.height))
  drawRoundRect(Color(0xFF171C14),Offset(12*sx,60*sy),Size(321*sx,8*sy),CornerRadius(3*sx))
 }
}
@Composable private fun OldFlashHousing(f: CameraFinish,settings: Settings,cap: CaptureUi,available: Boolean,modifier: Modifier) {
 val enabled=if(settings.front)settings.selfieLight.screenFlash else settings.rearFlash
 val working=if(settings.front)settings.selfieLight.screenFlash else available && enabled
 val state=when{working && cap.phase==CapturePhase.CAPTURING->"Firing";working && cap.phase==CapturePhase.CHARGING->"Charging";working->"Ready";!available && !settings.front->"Unavailable";else->"Off"}
 val light by animateFloatAsState(if(state=="Firing")1f else if(state=="Charging").3f else if(working).1f else 0f,tween(300),label="Old Fashion flash lens")
 OldSurface(f.body,modifier.testTag("old-fashion-flash-housing"),10.dp) {
  BoxWithConstraints(Modifier.fillMaxSize()) {
   val s=maxWidth.value/369f
   Box(Modifier.offset((10*s).dp,(13*s).dp).size((222*s).dp,(88*s).dp).background(OldFashionTokens.Optical,RoundedCornerShape(7.dp)).border(1.dp,Color(0xFF53564D),RoundedCornerShape(7.dp)))
   BrandLogo(Modifier.offset((52*s).dp,(34*s).dp).width((160*s).dp),plate=false)
   Canvas(Modifier.offset((20*s).dp,(29*s).dp).size((20*s).dp,(40*s).dp)) {OldFashionTokens.Spectrum.forEachIndexed{i,c->drawRect(c,Offset(0f,i*size.height/5),Size(size.width,size.height/5))}}
   Engraving{Text("OLD FASHION",Modifier.offset((52*s).dp,(77*s).dp),fontSize=(10*s).sp,lineHeight=(14*s).sp,letterSpacing=(2.8*s).sp,color=OldFashionTokens.Ivory)}
   Canvas(Modifier.offset((243*s).dp,(20*s).dp).size((114*s).dp,(76*s).dp).semantics{contentDescription="Flash housing";stateDescription=state}) {
    drawRoundRect(OldFashionTokens.Optical,cornerRadius=CornerRadius(7*s.dp.toPx()))
    drawRoundRect(OldFashionTokens.Metal,Offset(3*s.dp.toPx(),3*s.dp.toPx()),Size(size.width-6*s.dp.toPx(),size.height-6*s.dp.toPx()),CornerRadius(4*s.dp.toPx()),style=Stroke(2*s.dp.toPx()))
    val inset=7*s.dp.toPx();val glass=lerp(Color(0xFFACA99E),Color(0xFFFFE0A0),light)
    drawRect(Brush.verticalGradient(listOf(Color(0xFFE9E5D6),glass,Color(0xFFFFF2CE),Color(0xFF6B695F))),Offset(inset,inset),Size(size.width-2*inset,size.height-2*inset))
    for(x in 9..106 step 6)drawLine(Color.White.copy(alpha=.65f),Offset(x*s.dp.toPx(),inset),Offset(x*s.dp.toPx(),size.height-inset),1*s.dp.toPx())
    for(y in 12..66 step 7)drawLine(Color(0xFF5D6056).copy(alpha=.45f),Offset(inset,y*s.dp.toPx()),Offset(size.width-inset,y*s.dp.toPx()),1*s.dp.toPx())
    if(light>0)drawRect(OldFashionTokens.Ivory.copy(alpha=.3f*light),Offset(inset,inset),Size(size.width-2*inset,size.height-2*inset))
   }
  }
 }
}
@Composable private fun OldViewfinder(trim: Color,modifier: Modifier,preview: @Composable ()->Unit) {
 BoxWithConstraints(modifier.testTag("old-fashion-viewfinder").semantics{contentDescription="Live camera viewfinder"},contentAlignment=Alignment.Center) {
  val s=maxWidth.value/145f
  Canvas(Modifier.fillMaxSize()) {
   drawRoundRect(trim,cornerRadius=CornerRadius(20*s.dp.toPx()))
   drawRoundRect(Brush.verticalGradient(listOf(lerp(trim,Color.White,.65f),trim,lerp(trim,Color.Black,.55f),trim,lerp(trim,Color.Black,.25f))),Offset(1*s.dp.toPx(),1*s.dp.toPx()),Size(size.width-2*s.dp.toPx(),size.height-2*s.dp.toPx()),CornerRadius(19*s.dp.toPx()))
   drawRoundRect(OldFashionTokens.Optical,Offset(4*s.dp.toPx(),4*s.dp.toPx()),Size(size.width-8*s.dp.toPx(),size.height-8*s.dp.toPx()),CornerRadius(17*s.dp.toPx()))
   drawRoundRect(Color(0xFF131A15),Offset(8*s.dp.toPx(),8*s.dp.toPx()),Size(size.width-16*s.dp.toPx(),size.height-16*s.dp.toPx()),CornerRadius(14*s.dp.toPx()))
   drawRoundRect(OldFashionTokens.Metal,Offset(8*s.dp.toPx(),8*s.dp.toPx()),Size(size.width-16*s.dp.toPx(),size.height-16*s.dp.toPx()),CornerRadius(14*s.dp.toPx()),style=Stroke(1*s.dp.toPx()))
  }
  Box(Modifier.size((119*s).dp,(132*s).dp).clip(RoundedCornerShape((10*s).dp)).background(OldFashionTokens.Optical).testTag("old-fashion-live-aperture")) {
   preview()
   Canvas(Modifier.fillMaxSize().clearAndSetSemantics{}) {
    val margin=10*s.dp.toPx();val length=12*s.dp.toPx()
    for((x,dx) in listOf(margin to 1f,size.width-margin to -1f))for((y,dy) in listOf(margin to 1f,size.height-margin to -1f)){
     drawLine(Color.White,Offset(x,y),Offset(x+dx*length,y),1.5f*s.dp.toPx());drawLine(Color.White,Offset(x,y),Offset(x,y+dy*length),1.5f*s.dp.toPx())
    }
   }
  }
 }
}
@Composable private fun OldGoldMesh(modifier: Modifier) {
 Box(modifier.testTag("old-fashion-gold-mesh").clearAndSetSemantics{}.drawWithCache {
  val s=size.width/92;val center=size.center;val holes=Path()
  for(row in -11..11)for(col in -11..11){val x=center.x+col*4*s+(if(row%2==0)0f else 2*s);val y=center.y+row*3.5f*s;if(hypot(x-center.x,y-center.y)<size.width/2-9*s)holes.addOval(Rect(x-1.5f*s,y-1.5f*s,x+1.5f*s,y+1.5f*s))}
  onDrawBehind{
   drawCircle(OldFashionTokens.Optical)
   drawCircle(Brush.linearGradient(listOf(Color(0xFFFFEDB1),OldFashionTokens.Gold,Color(0xFF644C25))),size.width/2-3*s)
   drawCircle(Color(0xFF5D4822),size.width/2-5*s,style=Stroke(2*s));drawCircle(OldFashionTokens.Gold,size.width/2-7*s,style=Stroke(1*s));drawPath(holes,Color(0xFF342B18))
  }
 })
}
@Composable private fun OldOptics(modifier: Modifier) {
 Canvas(modifier.testTag("old-fashion-optical-decoration").clearAndSetSemantics{}) {
  val s=size.width/80
  drawRoundRect(Color(0xFF4C5145),cornerRadius=CornerRadius(12*s))
  drawRoundRect(OldFashionTokens.Optical,Offset(3*s,3*s),Size(size.width-6*s,size.height-6*s),CornerRadius(9*s))
  drawRoundRect(OldFashionTokens.Metal,Offset(8*s,8*s),Size(64*s,68*s),CornerRadius(8*s),style=Stroke(4*s))
  drawCircle(Brush.radialGradient(listOf(Color(0xFF111520),Color(0xFF34404F),Color(0xFF090D13),Color(0xFF5C616D))),27*s,Offset(40*s,40*s))
  drawCircle(Color(0xFF0A0E15),19*s,Offset(40*s,40*s))
  drawOval(Color(0xFFBCC5DD).copy(alpha=.45f),Offset(22*s,18*s),Size(20*s,10*s))
  drawCircle(Color(0xFF797AA8).copy(alpha=.35f),5*s,Offset(52*s,50*s))
  for(x in 10..70 step 4)drawLine(Color(0xFF363D31),Offset(x*s,87*s),Offset(x*s,size.height-9*s),1*s)
 }
}
@Composable private fun OldCounter(remaining: Int?,enabled: Boolean,interactive: Boolean,load: ()->Unit,modifier: Modifier) {
 Column(modifier.size(80.dp,50.dp).clip(RoundedCornerShape(11.dp)).background(OldFashionTokens.Optical).border(1.dp,Color(0xFF555A4F),RoundedCornerShape(11.dp))
  .then(if(interactive)Modifier.clickable(enabled=enabled,role=Role.Button,onClick=load).semantics(mergeDescendants=true){contentDescription=if(remaining==null)"Load film" else "Film: $remaining shots remaining. Replace cartridge"}else Modifier),horizontalAlignment=Alignment.CenterHorizontally) {
  Engraving{Text(remaining?.toString() ?: "—",color=OldFashionTokens.Ivory,fontSize=26.sp,lineHeight=30.sp,fontWeight=FontWeight.Bold);Text(if(remaining==0)"EMPTY" else "SHOTS",color=OldFashionTokens.Ivory,fontSize=12.sp,lineHeight=16.sp,fontWeight=FontWeight.Bold)}
 }
}
@Composable private fun OldControl(label: String,icon: Int,enabled: Boolean,click: ()->Unit,modifier: Modifier=Modifier,interactive: Boolean=true,state: String?=null,index: Float=0f) {
 val interaction=remember{MutableInteractionSource()};val pressed by interaction.collectIsPressedAsState();val focused by interaction.collectIsFocusedAsState()
 val elevation by animateDpAsState(if(pressed)1.dp else 3.dp,tween(if(pressed)70 else 110),label="Old control depth")
 OldSurface(Color(0xFF333630),modifier.size(56.dp,48.dp).then(if(interactive)Modifier.clickable(enabled=enabled,role=Role.Button,interactionSource=interaction,indication=LocalIndication.current,onClick=click).semantics{contentDescription=label;traversalIndex=index;state?.let{stateDescription=it}}else Modifier),11.dp,elevation,if(focused)OldFashionTokens.Focus else null) {
  Icon(painterResource(icon),null,tint=OldFashionTokens.Ivory.copy(alpha=if(enabled)1f else .42f),modifier=Modifier.size(24.dp).align(Alignment.Center))
  if(label=="Self-timer" && state!=null && state!="Off")Engraving{Text(state.substringBefore(' ')+"s",Modifier.align(Alignment.BottomEnd).padding(2.dp).background(OldFashionTokens.Optical),fontSize=8.sp,lineHeight=10.sp,color=OldFashionTokens.Ivory)}
 }
}
@Composable private fun OldFlashControl(on: Boolean,available: Boolean,enabled: Boolean,toggle: (Boolean)->Unit,modifier: Modifier,interactive: Boolean) {
 var focused by remember{mutableStateOf(false)}
 val colors=MaterialTheme.colorScheme.copy(surface=Color(0xFF333630),onSurface=OldFashionTokens.Ivory)
 MaterialTheme(colorScheme=colors){FlashToggle(on,available,enabled && interactive,iconResource=R.drawable.of_flash,modifier=modifier.size(56.dp,48.dp).onFocusChanged{focused=it.isFocused}.then(if(focused)Modifier.border(2.dp,OldFashionTokens.Focus,RoundedCornerShape(14.dp))else Modifier).semantics{traversalIndex=5f},activeTint=OldFashionTokens.Ready,toggle=toggle)}
}
@Composable private fun OldEdgeShutter(enabled: Boolean,booth: Boolean,status: String,click: ()->Unit,modifier: Modifier,interactive: Boolean) {
 val interaction=remember{MutableInteractionSource()};val pressed by interaction.collectIsPressedAsState();val focused by interaction.collectIsFocusedAsState()
 val motion=ValueAnimator.areAnimatorsEnabled()
 val depth by animateFloatAsState(if(pressed && motion)2f else 0f,tween(if(pressed)OldFashionMotion.pressMs else OldFashionMotion.releaseMs,easing=LinearOutSlowInEasing),label="Old Fashion shutter depression")
 Box(modifier.size(48.dp,80.dp).then(if(interactive)Modifier.testTag("old-fashion-shutter").clickable(enabled=enabled,role=Role.Button,interactionSource=interaction,indication=null,onClick=click).semantics{contentDescription=if(booth)"Start Photo Booth" else "Take photograph";stateDescription=if(pressed && enabled)"Pressed" else status}else Modifier)) {
  Canvas(Modifier.fillMaxSize().alpha(if(enabled)1f else .42f)) {
   val p=1.dp.toPx()
   drawRoundRect(OldFashionTokens.Optical,size=Size(40*p,80*p),cornerRadius=CornerRadius(9*p))
   drawRoundRect(Color(0xFF5D6257),size=Size(40*p,80*p),cornerRadius=CornerRadius(9*p),style=Stroke(p))
   drawRoundRect(OldFashionTokens.Metal,Offset(7*p,7*p),Size(27*p,66*p),CornerRadius(6*p))
   drawRoundRect(Color(0xFF121610),Offset(8*p,8*p),Size(25*p,64*p),CornerRadius(5*p))
   val shadowDepth=if(motion)depth else if(pressed)2f else 0f
   drawRoundRect(Color.Black.copy(alpha=.6f-.1f*shadowDepth),Offset((10+depth)*p,(14-shadowDepth)*p),Size(23*p,60*p),CornerRadius(3*p))
   if(focused)drawRoundRect(OldFashionTokens.Focus,size=Size(48*p,80*p),cornerRadius=CornerRadius(9*p),style=Stroke(2*p))
   translate(depth*p,0f){drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFFFF6741),OldFashionTokens.Shutter,Color(0xFFA51F14))),Offset(10*p,11*p),Size(21*p,58*p),CornerRadius(3*p));for(x in 12..27 step 3)drawLine(Color(0xFFFFAD7A).copy(alpha=.4f),Offset(x*p,13*p),Offset(x*p,67*p),p)}
  }
 }
}
@Composable private fun OldCompartment(f: CameraFinish,modifier: Modifier) {
 OldSurface(f.body,modifier.testTag("old-fashion-film-compartment")) {
  BoxWithConstraints(Modifier.fillMaxSize().clearAndSetSemantics{}) {
   val s=maxWidth.value/353
   Box(Modifier.offset((10*s).dp,(14*s).dp).size((333*s).dp,(49*s).dp).background(OldFashionTokens.Optical,RoundedCornerShape(6.dp)))
   Engraving {
    Text("InstantRoom",Modifier.offset((28*s).dp,(20*s).dp),fontSize=(24*s).sp,lineHeight=(30*s).sp,fontWeight=FontWeight.Bold,fontStyle=FontStyle.Italic,color=OldFashionTokens.Ivory)
    Text("INSTANT",Modifier.offset((223*s).dp,(24*s).dp),fontSize=(10*s).sp,lineHeight=(14*s).sp,letterSpacing=(2*s).sp,color=OldFashionTokens.Ivory)
    Text("10",Modifier.offset((295*s).dp,(19*s).dp),fontSize=(26*s).sp,lineHeight=(30*s).sp,fontWeight=FontWeight.Bold,color=OldFashionTokens.Shutter)
   }
   Canvas(Modifier.fillMaxSize()) {
    val u=size.width/353
    OldFashionTokens.Spectrum.forEachIndexed{i,c->drawRect(c,Offset((22+29*i)*u,52*u),Size(30*u,6*u))}
    val x=13*u;val y=86*u;val w=size.width-26*u
    drawRoundRect(OldFashionTokens.Optical,Offset(x,y),Size(w,64*u),CornerRadius(8*u))
    drawRoundRect(Color(0xFF53594E),Offset(x,y),Size(w,64*u),CornerRadius(8*u),style=Stroke(u))
    drawRoundRect(Color(0xFF010301),Offset(x+8*u,y+8*u),Size(w-16*u,45*u),CornerRadius(5*u))
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF171B17),Color(0xFF8C9084),Color(0xFF3A4036),Color(0xFF0B100A))),Offset(x+10*u,y+12*u),Size(w-20*u,19*u),CornerRadius(5*u))
    drawLine(Color(0xFFD8D7C4).copy(alpha=.35f),Offset(x+12*u,y+14*u),Offset(x+w-12*u,y+14*u),u)
   }
  }
 }
}
@Composable private fun OldNavigation(enabled: Boolean,booth: Boolean,gallery: ()->Unit,openBooth: ()->Unit,settings: ()->Unit,modifier: Modifier,stacked: Boolean=false) {
 if(stacked)Column(modifier,verticalArrangement=Arrangement.spacedBy(8.dp)) {
  OldDestination("Gallery",R.drawable.of_gallery,enabled,false,gallery,Modifier.fillMaxWidth())
  OldDestination("Photo Booth",R.drawable.of_booth,enabled,true,openBooth,Modifier.fillMaxWidth(),booth)
  OldDestination("Settings",R.drawable.of_settings,enabled,false,settings,Modifier.fillMaxWidth())
 }else Row(modifier,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
  OldDestination("Gallery",R.drawable.of_gallery,enabled,false,gallery,Modifier.weight(1f))
  OldDestination("Photo Booth",R.drawable.of_booth,enabled,true,openBooth,Modifier.weight(1f),booth)
  OldDestination("Settings",R.drawable.of_settings,enabled,false,settings,Modifier.weight(1f))
 }
}
@Composable private fun OldDestination(label: String,icon: Int,enabled: Boolean,light: Boolean,click: ()->Unit,modifier: Modifier,selected: Boolean=false) {
 val color=if(light)OldFashionTokens.Metal else Color(0xFF333630)
 val ink=if(light)Color(0xFF242526)else OldFashionTokens.Ivory
 val interaction=remember{MutableInteractionSource()};val pressed by interaction.collectIsPressedAsState();val focused by interaction.collectIsFocusedAsState()
 val elevation by animateDpAsState(if(pressed)1.dp else 3.dp,tween(if(pressed)70 else 110),label="Old navigation depth")
 OldSurface(color,modifier.heightIn(min=80.dp).clickable(enabled=enabled,role=Role.Button,interactionSource=interaction,indication=LocalIndication.current,onClick=click).semantics(mergeDescendants=true){contentDescription=label;this.selected=selected;traversalIndex=6f},13.dp,elevation,if(focused || selected)OldFashionTokens.Focus else null) {
  Column(Modifier.fillMaxWidth().padding(horizontal=6.dp,vertical=12.dp).alpha(if(enabled)1f else .42f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Icon(painterResource(icon),null,tint=ink,modifier=Modifier.size(24.dp))
   Text(label.uppercase(),color=ink,fontSize=12.sp,lineHeight=16.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,letterSpacing=0.sp)
  }
 }
}
@Composable private fun OldTrayAction(label: String,enabled: Boolean,click: ()->Unit,shutter: Boolean=false,index: Float=0f) {
 Button(onClick=click,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=if(shutter)80.dp else 56.dp).semantics{traversalIndex=index},colors=ButtonDefaults.buttonColors(containerColor=if(shutter)OldFashionTokens.Shutter else OldFashionTokens.Metal,contentColor=if(shutter)inkOn(OldFashionTokens.Shutter) else Color(0xFF242526))) {Text(label,textAlign=TextAlign.Center)}
}

@Composable fun OldFashionStylePreview(color: CameraColor,modifier: Modifier=Modifier) {
 val f=cameraFinish(CameraStyle.OLD_FASION,color)
 OldMaterials {
  Box(modifier.fillMaxWidth().height(250.dp).semantics{contentDescription="Old Fashion, ${color.label} camera preview"},contentAlignment=Alignment.Center) {
   Box(Modifier.size(154.dp,244.dp)) {
    OldSupports(Modifier.offset(10.dp,38.dp).size(134.dp,28.dp))
    OldSurface(f.body,Modifier.offset(4.dp,4.dp).size(146.dp,44.dp)) {BrandLogo(Modifier.align(Alignment.CenterStart).padding(start=12.dp).width(78.dp),plate=false);Box(Modifier.align(Alignment.CenterEnd).padding(end=6.dp).size(42.dp,28.dp).background(OldFashionTokens.Metal,RoundedCornerShape(3.dp)))}
    OldSurface(f.body,Modifier.offset(7.dp,70.dp).size(140.dp,164.dp))
    OldGoldMesh(Modifier.offset(12.dp,104.dp).size(36.dp))
    OldViewfinder(f.accent,Modifier.offset(49.dp,80.dp).size(56.dp,61.dp)){Box(Modifier.fillMaxSize().background(OldFashionTokens.Optical))}
    OldOptics(Modifier.offset(110.dp,84.dp).size(30.dp,58.dp))
    OldCompartment(f,Modifier.offset(7.dp,160.dp).size(140.dp,75.dp))
   }
  }
 }
}

@Composable fun OldFashionLoadingCard(color: CameraColor,format: FilmFormat,appearance: Appearance,empty: Boolean,modifier: Modifier=Modifier) {
 val finish=cameraFinish(CameraStyle.OLD_FASION,color)
 OldMaterials {
  OldSurface(finish.body,modifier,8.dp) {
   Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text(if(empty)"EMPTY BAY" else "INSTANT ROOM / FILM",color=finish.ink,style=MaterialTheme.typography.labelSmall)
    Text(format.label,color=finish.ink,style=MaterialTheme.typography.titleLarge)
    Text("10 EXPOSURES",color=finish.ink,style=MaterialTheme.typography.bodyMedium)
    Text(appearance.label,color=finish.ink,style=MaterialTheme.typography.bodySmall)
   }
  }
 }
}
