package com.instantroom.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.instantroom.R
import com.instantroom.core.*
import com.instantroom.data.*
import kotlin.math.sqrt

/** Real camera content is supplied by CameraScreen; no bitmap stands in for its live preview. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun OneAndOnlyCamera(
 settings: Settings, cartridge: CartridgeEntity?, cap: CaptureUi, initialized: Boolean,
 captureReady: Boolean, switchReady: Boolean, hasFlash: Boolean, booth: Boolean,
 photo: PhotoEntity?, onCapture: ()->Unit, onLoad: ()->Unit, onGallery: ()->Unit,
 onSettings: ()->Unit, onSwitch: ()->Unit, onFlash: (Boolean)->Unit,
 onTimer: (SelfTimer)->Unit, onBooth: (Boolean)->Unit, onLayout: (BoothLayout)->Unit,
 onSelfieLight: (SelfieLightMode)->Unit, onRecover: ()->Unit = {}, preview: @Composable ()->Unit
) {
 val busy=cap.phase!=CapturePhase.IDLE
 val motionEnabled=android.animation.ValueAnimator.areAnimatorsEnabled()
 var timerOpen by rememberSaveable { mutableStateOf(false) }
 var boothOpen by rememberSaveable { mutableStateOf(false) }
 var selfieOpen by rememberSaveable { mutableStateOf(false) }
 val scroll=rememberScrollState()
 LaunchedEffect(busy) { if(busy)scroll.scrollTo(0) }
 val f=cameraFinish(CameraStyle.ONE_AND_ONLY,settings.cameraColor)
 val fontScale=LocalDensity.current.fontScale
 val sideControlRatio=if(fontScale>1.4f).42f else .36f
 val shutterRatio=if(fontScale>1.4f).70f else .62f
 val switchRatio=if(fontScale>1.4f).73f else .65f
 val paperOrigin=(46f+30f*fontScale).dp
 BoxWithConstraints(Modifier.fillMaxSize().background(AnalogTokens.Chrome).testTag("one-and-only-camera")) {
  val geometry=AnalogLayout.fit((maxWidth-24.dp).value.coerceAtLeast(1f),(maxHeight-56.dp).value.coerceAtLeast(1f),fontScale)
  val width=geometry.width.dp
  val upper=geometry.upperHeight.dp
  Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(12.dp)) {
   Box(Modifier.width(width).height(geometry.height.dp)) {
    // The selected finish stays physical. App Light/Dark affects supporting screens only.
    CameraMaterial(CameraStyle.ONE_AND_ONLY,settings.cameraColor) {
     Box(Modifier.width(width).height(upper).shadow(4.dp,RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
      .background(Brush.verticalGradient(listOf(lerp(f.body,Color.White,.25f),f.body,lerp(f.body,Color.Black,.07f))))
      .border(1.dp,lerp(f.body,Color.White,.7f),RoundedCornerShape(24.dp))) {
      val lens=geometry.lens.dp
      val lensTop=geometry.lensTop.dp
      val lensLeft=geometry.lensLeft.dp
      Canvas(Modifier.fillMaxSize()) {
       val stripeWidth=10.dp.toPx().coerceAtMost(size.width*.027f)
       val stripeTop=(geometry.lensTop+geometry.lens).dp.toPx()
       AnalogTokens.Rainbow.forEachIndexed { i,color->drawRect(color,Offset(size.width/2-stripeWidth*2.5f+i*stripeWidth,stripeTop),Size(stripeWidth,(size.height-stripeTop).coerceAtLeast(0f))) }
       drawLine(Color.White.copy(alpha=.65f),Offset(1f,size.height*.89f),Offset(size.width-1f,size.height*.89f),1.dp.toPx())
      }
      BrandLogo(Modifier.offset(10.dp,16.dp).width(minOf(width*.45f,164.dp)),plate=f.body.luminance()<.3f)
      FlashAssembly(settings.rearFlash,cap.phase,Modifier.align(Alignment.TopEnd).padding(top=18.dp,end=12.dp).size(76.dp,68.dp))
      Box(Modifier.offset(lensLeft,lensTop).size(lens).shadow(9.dp,CircleShape),contentAlignment=Alignment.Center) {
       LensRings(Modifier.fillMaxSize())
       Box(Modifier.size(geometry.aperture.dp).clip(CircleShape).background(Color.Black).testTag("analog-live-aperture"),contentAlignment=Alignment.Center) {
        preview()
        if(captureReady && !busy)FocusGuide(if(booth)4f/3f else cartridge?.let{FilmFormat.valueOf(it.format).imageAspect} ?: FilmFormat.CLASSIC.imageAspect)
       }
      }
      ExposureCounter(cartridge?.remaining,initialized && !busy,onLoad,Modifier.offset(10.dp,upper*.22f))
      AnalogFaceButton(if(settings.timer==SelfTimer.OFF)"Self-timer" else "Self-timer: ${settings.timer.seconds} seconds",R.drawable.ir_timer,!busy,
       {timerOpen=true},Modifier.offset(12.dp,upper*sideControlRatio),settings.timer.takeIf{it!=SelfTimer.OFF}?.let{"${it.seconds}s"})
      Box(Modifier.align(Alignment.TopEnd).offset(if(settings.front)(-26).dp else (-24).dp,upper*sideControlRatio)) {
       if(settings.front)AnalogFaceButton("Selfie lighting",R.drawable.ir_flash,!busy,{selfieOpen=true})
       else FlashToggle(settings.rearFlash,hasFlash,captureReady && !busy,iconResource=R.drawable.ir_flash,toggle=onFlash)
      }
      MechanicalShutter(captureReady && initialized && !busy && (cartridge?.remaining ?: 0)>=(if(booth)4 else 1),
       cap,booth,AnalogTokens.Shutter,onCapture,Modifier.offset(8.dp,upper*shutterRatio).testTag("analog-shutter"))
      AnalogFaceButton(if(settings.front)"Switch to rear camera" else "Switch to front camera",R.drawable.ir_switch,switchReady && !busy,
       onSwitch,Modifier.align(Alignment.TopEnd).offset((-26).dp,upper*switchRatio),dark=true)
     }
    }
    LowerHousing(geometry,booth,!busy,onGallery,{boothOpen=true},onSettings,Modifier.offset(y=upper))
    if(cap.phase==CapturePhase.EJECTING) {
     // The same normalized 1800 ms clock drives the real motor/audio/haptic sequence.
     val aspect=photo?.geometry?.aspect ?: FilmFormat.CLASSIC.aspect
     val paperWidth=minOf(width*.6f,210.dp,(geometry.lowerHeight.dp-paperOrigin)*aspect)
     val paperHeight=paperWidth/aspect
     Box(Modifier.offset((width-paperWidth)/2,upper+paperOrigin).width(paperWidth).height(paperHeight).clipToBounds()) {
      FilmPrint(photo,Modifier.width(paperWidth).offset(y=paperHeight*(if(motionEnabled)cap.ejection-1f else 0f)).shadow(8.dp))
     }
    }
   }
   if(!busy)Text(when {
    !initialized->"Restoring the archive…"
    cartridge==null->"Tap the counter to load film."
    cartridge.remaining==0->"Film empty · manually replace the cartridge."
    booth && cartridge.remaining<4->"Photo Booth needs four exposures."
    booth->"Photo Booth · four photos, five seconds apart"
    else->"${FilmFormat.valueOf(cartridge.format).label} · ${Appearance.valueOf(cartridge.appearance).label}"
   },Modifier.fillMaxWidth().padding(top=8.dp),color=AnalogTokens.Paper,style=MaterialTheme.typography.bodySmall)
   if(!initialized)TextButton(onClick=onRecover){Text("Retry archive recovery",color=AnalogTokens.Paper)}
  }
 }
 if(timerOpen && !busy)ModalBottomSheet(onDismissRequest={timerOpen=false},containerColor=MaterialTheme.colorScheme.surface) {
  Column(Modifier.fillMaxWidth().padding(horizontal=24.dp).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text("Self-timer",style=MaterialTheme.typography.headlineSmall)
   SelfTimer.entries.forEach { value->
    Row(Modifier.fillMaxWidth().heightIn(min=56.dp).clickable{onTimer(value);timerOpen=false}.semantics{role=Role.RadioButton;selected=settings.timer==value},verticalAlignment=Alignment.CenterVertically) {
     RadioButton(selected=settings.timer==value,onClick=null);Spacer(Modifier.width(12.dp));Text(value.label)
    }
   }
  }
 }
 if(selfieOpen && !busy)ModalBottomSheet(onDismissRequest={selfieOpen=false}) {
  Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Text("Selfie lighting",style=MaterialTheme.typography.headlineSmall)
   SelfieLightMode.entries.forEach { mode->
    Row(Modifier.fillMaxWidth().heightIn(min=56.dp).clickable{onSelfieLight(mode);selfieOpen=false},verticalAlignment=Alignment.CenterVertically) {
     RadioButton(selected=settings.selfieLight==mode,onClick=null);Text(mode.label,Modifier.padding(start=12.dp))
    }
   }
  }
 }
 if(boothOpen && !busy)ModalBottomSheet(onDismissRequest={boothOpen=false}) {
  Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
   Text("Photo Booth",style=MaterialTheme.typography.headlineSmall)
   Text("Four photographs · five seconds apart · four exposures")
   BoothLayoutControl(settings.boothLayout,true,onLayout)
   if(settings.front)SelfieLightControl(settings.selfieLight,true,onSelfieLight)
   Button(onClick={onBooth(true);boothOpen=false},modifier=Modifier.fillMaxWidth()){Text("Use Photo Booth")}
   OutlinedButton(onClick={onBooth(false);boothOpen=false},modifier=Modifier.fillMaxWidth()){Text("Single photo")}
  }
 }
}

@Composable private fun ExposureCounter(remaining: Int?,enabled: Boolean,load: ()->Unit,modifier: Modifier) {
 val largeType=LocalDensity.current.fontScale>1.4f
 Column(modifier.width(if(largeType)78.dp else 66.dp).heightIn(min=48.dp).clip(RoundedCornerShape(9.dp)).background(AnalogTokens.Charcoal)
  .clickable(enabled=enabled,role=Role.Button,onClick=load).semantics(mergeDescendants=true){contentDescription=if(remaining==null)"Load film" else "Film: $remaining shots remaining. Replace cartridge"},horizontalAlignment=Alignment.CenterHorizontally) {
  Text(remaining?.toString() ?: "—",color=AnalogTokens.Paper,fontSize=25.sp,fontWeight=FontWeight.Bold,lineHeight=29.sp)
  Text(if(largeType){if(remaining==0)"OUT" else "LEFT"}else if(remaining==0)"EMPTY" else if(remaining==1)"LAST" else "SHOTS",color=AnalogTokens.Paper,fontSize=12.sp,lineHeight=16.sp,maxLines=1,softWrap=false)
 }
}

@Composable private fun MechanicalShutter(enabled: Boolean,cap: CaptureUi,booth: Boolean,finishRed: Color,click: ()->Unit,modifier: Modifier) {
 val interaction=remember{MutableInteractionSource()}
 val pressed by interaction.collectIsPressedAsState()
 val depth by animateFloatAsState(if(pressed)2f else 0f,tween(if(pressed)70 else 110),label="Mechanical shutter depth")
 Box(modifier.size(80.dp).semantics{contentDescription=if(booth)"Start Photo Booth" else "Take photograph"}
  .clickable(enabled=enabled,role=Role.Button,interactionSource=interaction,indication=null,onClick=click),contentAlignment=Alignment.Center) {
  Canvas(Modifier.fillMaxSize().alpha(if(enabled)1f else .4f)) {
   drawCircle(Brush.radialGradient(listOf(Color.White,AnalogTokens.Metal,Color(0xFF78736B))),size.minDimension/2-1.dp.toPx())
   drawCircle(Color(0xFF70241A),size.minDimension*.435f)
   drawCircle(Brush.verticalGradient(listOf(lerp(finishRed,Color.White,.15f),finishRed,lerp(finishRed,Color.Black,.22f))),size.minDimension*.385f,center+Offset(0f,depth.dp.toPx()))
  }
  val label=when{cap.countdown>0->cap.countdown.toString();cap.phase==CapturePhase.CAPTURING->"•••";cap.booth->"${cap.shot} / 4";booth->"Start";else->""}
  if(label.isNotEmpty())Text(label,color=Color.White,fontWeight=FontWeight.Bold,fontSize=16.sp,modifier=Modifier.clearAndSetSemantics{})
 }
}

@Composable private fun AnalogFaceButton(label: String,icon: Int,enabled: Boolean,click: ()->Unit,modifier: Modifier=Modifier,badge: String?=null,dark: Boolean=false) {
 Column(modifier.sizeIn(minWidth=48.dp,minHeight=48.dp).width(48.dp).shadow(3.dp,RoundedCornerShape(12.dp))
  .clip(RoundedCornerShape(12.dp)).background(if(dark)AnalogTokens.Charcoal else AnalogTokens.Paper)
  .border(1.dp,AnalogTokens.Metal,RoundedCornerShape(12.dp)).clickable(enabled=enabled,role=Role.Button,onClick=click)
  .semantics(mergeDescendants=true){contentDescription=label}.padding(vertical=8.dp).alpha(if(enabled)1f else .4f),horizontalAlignment=Alignment.CenterHorizontally) {
  Icon(painterResource(icon),null,tint=if(dark)AnalogTokens.Paper else AnalogTokens.Ink,modifier=Modifier.size(24.dp))
  badge?.let{Text(it,fontSize=12.sp,lineHeight=16.sp,color=if(dark)AnalogTokens.Paper else AnalogTokens.Ink)}
 }
}

@Composable private fun LensRings(modifier: Modifier) {
 Canvas(modifier) {
  drawCircle(Brush.linearGradient(listOf(Color(0xFFF7F2EA),Color(0xFF6F6D69),AnalogTokens.Metal)),size.minDimension/2)
  for(i in 0..15)drawCircle(if(i%2==0)Color(0xFF17191A)else Color(0xFF363637),size.minDimension/2-3.dp.toPx()-i*1.5.dp.toPx())
 }
}

@Composable private fun FocusGuide(aspect: Float) {
 Canvas(Modifier.fillMaxSize().clearAndSetSemantics{}) {
  val h=size.minDimension*.9f/sqrt(aspect*aspect+1f);val w=h*aspect
  val left=(size.width-w)/2;val top=(size.height-h)/2;val length=10.dp.toPx()
  for((x,dx) in listOf(left to 1f,left+w to -1f))for((y,dy) in listOf(top to 1f,top+h to -1f)) {
   drawLine(Color.White,Offset(x,y),Offset(x+dx*length,y),2.dp.toPx(),StrokeCap.Round)
   drawLine(Color.White,Offset(x,y),Offset(x,y+dy*length),2.dp.toPx(),StrokeCap.Round)
  }
  drawLine(Color.White,center-Offset(5.dp.toPx(),0f),center+Offset(5.dp.toPx(),0f),1.5.dp.toPx())
  drawLine(Color.White,center-Offset(0f,5.dp.toPx()),center+Offset(0f,5.dp.toPx()),1.5.dp.toPx())
 }
}

@Composable private fun FlashAssembly(on: Boolean,phase: CapturePhase,modifier: Modifier) {
 val lit by animateFloatAsState(if(on || phase==CapturePhase.CHARGING)1f else 0f,tween(300),label="Flash window")
 Canvas(modifier) {
  drawRoundRect(AnalogTokens.Charcoal,cornerRadius=CornerRadius(10.dp.toPx()))
  drawRoundRect(AnalogTokens.Metal,Offset(5.dp.toPx(),5.dp.toPx()),Size(size.width-10.dp.toPx(),size.height-10.dp.toPx()),CornerRadius(5.dp.toPx()))
  val glass=lerp(Color(0xFFACA99E),Color(0xFFFFF1CF),lit)
  drawRect(Brush.verticalGradient(listOf(Color.White,glass,Color(0xFF8A867B))),Offset(9.dp.toPx(),9.dp.toPx()),Size(size.width-18.dp.toPx(),size.height-18.dp.toPx()))
  repeat(7){i->val x=12.dp.toPx()+i*(size.width-24.dp.toPx())/6;drawLine(Color.White.copy(alpha=.7f),Offset(x,11.dp.toPx()),Offset(x,size.height-11.dp.toPx()),1.dp.toPx())}
  repeat(4){i->val y=14.dp.toPx()+i*(size.height-28.dp.toPx())/3;drawLine(Color(0xFF9B9487),Offset(10.dp.toPx(),y),Offset(size.width-10.dp.toPx(),y),1.dp.toPx())}
  drawCircle(if(on)AnalogTokens.Ready else Color(0xFF777571),2.dp.toPx(),Offset(size.width-8.dp.toPx(),size.height-6.dp.toPx()))
 }
}

@Composable private fun LowerHousing(g: AnalogLayout,booth: Boolean,enabled: Boolean,gallery: ()->Unit,photoBooth: ()->Unit,settings: ()->Unit,modifier: Modifier) {
 Column(modifier.width(g.width.dp).height(g.lowerHeight.dp).clip(RoundedCornerShape(bottomStart=6.dp,bottomEnd=6.dp))
  .background(Brush.verticalGradient(listOf(Color(0xFF343536),Color(0xFF202122)))).padding(horizontal=16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
  Text("InstantRoom",Modifier.padding(top=12.dp,bottom=18.dp),color=AnalogTokens.Paper,fontWeight=FontWeight.Bold,fontStyle=FontStyle.Italic,fontSize=24.sp,lineHeight=30.sp)
  Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFF0C0D0E)).padding(8.dp)) {
   Box(Modifier.fillMaxWidth().height(17.dp).clip(RoundedCornerShape(3.dp)).background(Brush.verticalGradient(listOf(AnalogTokens.Metal,Color(0xFF444645)))))
  }
  Spacer(Modifier.weight(1f))
  if(LocalDensity.current.fontScale>1.4f)Column(Modifier.fillMaxWidth().padding(bottom=18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   AnalogDestination("Gallery",R.drawable.ir_gallery,enabled,gallery,Modifier.fillMaxWidth())
   AnalogDestination("Photo Booth",R.drawable.ir_booth,enabled,photoBooth,Modifier.fillMaxWidth(),booth)
   AnalogDestination("Settings",R.drawable.ir_settings,enabled,settings,Modifier.fillMaxWidth())
  } else Row(Modifier.fillMaxWidth().padding(bottom=32.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
   AnalogDestination("Gallery",R.drawable.ir_gallery,enabled,gallery,Modifier.weight(1f))
   AnalogDestination("Photo Booth",R.drawable.ir_booth,enabled,photoBooth,Modifier.weight(1f),booth)
   AnalogDestination("Settings",R.drawable.ir_settings,enabled,settings,Modifier.weight(1f))
  }
 }
}

@Composable private fun AnalogDestination(label: String,icon: Int,enabled: Boolean,click: ()->Unit,modifier: Modifier,selected: Boolean=false) {
 val light=label=="Photo Booth"
 Column(modifier.heightIn(min=76.dp).clip(RoundedCornerShape(14.dp)).background(if(light)AnalogTokens.Paper else AnalogTokens.Charcoal)
  .border(if(selected)2.dp else 1.dp,if(selected)AnalogTokens.Focus else AnalogTokens.Metal,RoundedCornerShape(14.dp))
  .clickable(enabled=enabled,role=Role.Button,onClick=click).semantics(mergeDescendants=true){contentDescription=label;this.selected=selected}
  .padding(horizontal=4.dp,vertical=12.dp).alpha(if(enabled)1f else .4f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Icon(painterResource(icon),null,tint=if(light)AnalogTokens.Ink else AnalogTokens.Paper,modifier=Modifier.size(24.dp))
  Text(label,color=if(light)AnalogTokens.Ink else AnalogTokens.Paper,fontSize=12.sp,lineHeight=16.sp,letterSpacing=0.sp,fontWeight=FontWeight.Bold,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
 }
}

/** The color picker previews the physical body, not a processed photograph. */
@Composable fun OneAndOnlyStylePreview(color: CameraColor,modifier: Modifier=Modifier) {
 val finish=cameraFinish(CameraStyle.ONE_AND_ONLY,color)
 Box(modifier.fillMaxWidth().height(240.dp).semantics{contentDescription="One and Only, ${color.label} camera preview"},contentAlignment=Alignment.Center) {
  Canvas(Modifier.width(148.dp).fillMaxHeight()) {
   val upper=size.height*.64f
   drawRoundRect(finish.body,cornerRadius=CornerRadius(12.dp.toPx()),size=Size(size.width,upper))
   drawRoundRect(AnalogTokens.Charcoal,Offset(0f,upper),Size(size.width,size.height-upper),CornerRadius(4.dp.toPx()))
   val center=Offset(size.width/2,upper*.48f);val radius=size.width*.34f
   AnalogTokens.Rainbow.forEachIndexed{i,c->drawRect(c,Offset(size.width*.43f+i*size.width*.028f,center.y+radius),Size(size.width*.028f,upper-center.y-radius))}
   drawCircle(AnalogTokens.Metal,radius,center)
   repeat(8){i->drawCircle(if(i%2==0)Color(0xFF17191A)else Color(0xFF363637),radius-2.dp.toPx()-i*1.dp.toPx(),center)}
   drawCircle(Brush.radialGradient(listOf(Color(0xFF5E7777),Color(0xFF14201F)),center,radius*.72f),radius*.72f,center)
   drawCircle(Color.White.copy(alpha=.2f),radius*.17f,center-Offset(radius*.22f,radius*.23f))
   drawCircle(AnalogTokens.Metal,16.dp.toPx(),Offset(19.dp.toPx(),upper*.7f))
   drawCircle(AnalogTokens.Shutter,13.dp.toPx(),Offset(19.dp.toPx(),upper*.7f))
   drawRoundRect(Color(0xFF242526),Offset(size.width-35.dp.toPx(),12.dp.toPx()),Size(28.dp.toPx(),24.dp.toPx()),CornerRadius(4.dp.toPx()))
   drawRect(Color(0xFFE3D7B8),Offset(size.width-31.dp.toPx(),15.dp.toPx()),Size(20.dp.toPx(),18.dp.toPx()))
   drawRoundRect(Color.Black,Offset(10.dp.toPx(),upper+24.dp.toPx()),Size(size.width-20.dp.toPx(),18.dp.toPx()),CornerRadius(3.dp.toPx()))
   drawRect(AnalogTokens.Metal,Offset(14.dp.toPx(),upper+28.dp.toPx()),Size(size.width-28.dp.toPx(),5.dp.toPx()))
  }
 }
}
