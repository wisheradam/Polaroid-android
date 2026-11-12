package com.instantroom.ui

import android.animation.ValueAnimator
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
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.instantroom.R
import com.instantroom.core.*
import com.instantroom.data.*

/** Physical camera roles stay independent of app Light/Dark/System appearance. */
object InstaStyleTokens {
 val Raised=Color(0xFFFFF5F3)
 val Ink=Color(0xFF523641)
 val Optical=Color(0xFF281B23)
 val Paper=Color(0xFFFFF9EF)
 val Shadow=Color(0xFF9E697A)
 val Focus=Color(0xFF006B7D)
 val Flash=Color(0xFFFFE6B1)
}

@Composable private fun InstaEngraving(content: @Composable ()->Unit) {
 val density=LocalDensity.current
 CompositionLocalProvider(LocalDensity provides Density(density.density,1f),content=content)
}

@Composable private fun SatinSurface(color: Color,modifier: Modifier=Modifier,radius: Dp=29.dp,elevation: Dp=3.dp,focused: Boolean=false,content: @Composable BoxScope.()->Unit={}) {
 val shape=RoundedCornerShape(radius)
 Box(modifier.shadow(elevation,shape,ambientColor=InstaStyleTokens.Shadow,spotColor=InstaStyleTokens.Shadow).clip(shape).background(color)
  .drawWithCache{val ramp=Brush.verticalGradient(listOf(Color.White.copy(alpha=.18f),Color.Transparent,InstaStyleTokens.Shadow.copy(alpha=.04f)));onDrawBehind{drawRect(ramp)}}
  .border(if(focused)2.dp else 1.dp,if(focused)InstaStyleTokens.Focus else Color.White.copy(alpha=.7f),shape),content=content)
}

@Composable fun InstaStyleCamera(
 settings: Settings,cartridge: CartridgeEntity?,cap: CaptureUi,initialized: Boolean,captureReady: Boolean,switchReady: Boolean,
 hasFlash: Boolean,booth: Boolean,photo: PhotoEntity?,onCapture: ()->Unit,onLoad: ()->Unit,onGallery: ()->Unit,onSettings: ()->Unit,
 onSwitch: ()->Unit,onFlash: (Boolean)->Unit,onTimer: (SelfTimer)->Unit,onBooth: (Boolean)->Unit,onLayout: (BoothLayout)->Unit,
 onSelfieLight: (SelfieLightMode)->Unit,onCancel: ()->Unit={},onRecover: ()->Unit={},cameraAccess: (() -> Unit)?=null,
 openAndroidSettings: (() -> Unit)?=null,preview: @Composable ()->Unit
) {
 val f=cameraFinish(CameraStyle.INSTA_STYLE,settings.cameraColor)
 val busy=cap.phase!=CapturePhase.IDLE
 val enabled=captureReady && initialized && !busy && (cartridge?.remaining ?: 0)>=if(booth)4 else 1
 var sheet by rememberSaveable{mutableStateOf<String?>(null)}
 val scroll=rememberScrollState()
 val reducedMotion=!ValueAnimator.areAnimatorsEnabled()
 LaunchedEffect(busy){if(busy)scroll.scrollTo(0)}
 val fontScale=LocalDensity.current.fontScale
 val status=when {busy->cap.phase.name.lowercase();!initialized->"Restoring archive";!captureReady->"Camera unavailable";(cartridge?.remaining ?: 0)==0->"Film empty";booth && (cartridge?.remaining ?: 0)<4->"Four exposures required";else->"Ready"}
 BoxWithConstraints(Modifier.fillMaxSize().background(f.body).testTag("insta-style-camera").semantics{isTraversalGroup=true}) {
  val g=InstaStyleLayout.fit(maxWidth.value.coerceAtLeast(240f),maxHeight.value,fontScale)
  Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
   Box(Modifier.fillMaxWidth().height(g.height.dp).clipToBounds()) {
    SatinSurface(f.body,Modifier.fillMaxSize().testTag("insta-shell"),38.dp,0.dp)
    BrandLogo(Modifier.offset(24.dp,16.dp).size(128.dp,22.dp),plate=false)
    InstaEngraving{Text("InstantRoom",Modifier.offset((g.width-if(g.width<360)147 else 173).dp,(if(g.width<360)40 else 61).dp),fontSize=(if(g.width<360)18 else 22).sp,fontWeight=FontWeight.Bold,color=InstaStyleTokens.Ink)}
    InstaCounter(cartridge?.remaining,initialized && !busy,onLoad,Modifier.offset(24.dp,77.dp).semantics{traversalIndex=0f})
    InstaFlashWindow(settings,cap,hasFlash,Modifier.offset(104.dp,64.dp).size(44.dp,76.dp))
    Canvas(Modifier.offset(170.dp,79.dp).size(12.dp,32.dp).clearAndSetSemantics{}) {drawCircle(InstaStyleTokens.Optical,6.dp.toPx(),Offset(size.width/2,6.dp.toPx()));drawCircle(InstaStyleTokens.Optical,6.dp.toPx(),Offset(size.width/2,26.dp.toPx()))}
    InstaLens(f,Modifier.offset(g.lensX.dp,g.lensY.dp).size(g.lensDiameter.dp).semantics{traversalIndex=1f},preview)
    if(!g.essentialControls) {
     InstaShutter(f,enabled,booth,status,onCapture,Modifier.offset(g.shutterX.dp,g.shutterY.dp).size(g.shutterSize.dp),reducedMotion)
     InstaControl("Self-timer",if(settings.timer==SelfTimer.OFF)"Timer" else "${settings.timer.seconds}s",R.drawable.is_timer,!busy,{sheet="timer"},Modifier.offset(g.timerX.dp,g.controlsY.dp),3f)
     if(settings.front)InstaControl("Selfie lighting","Light",R.drawable.is_flash,!busy,{sheet="selfie"},Modifier.offset(g.flashX.dp,g.controlsY.dp),4f,settings.selfieLight!=SelfieLightMode.OFF)
     else InstaFlashControl(settings.rearFlash,captureReady && !busy,hasFlash,onFlash,Modifier.offset(g.flashX.dp,g.controlsY.dp),reducedMotion)
     InstaControl(if(settings.front)"Switch to rear camera" else "Switch to front camera","Switch",R.drawable.is_switch,switchReady && !busy,onSwitch,Modifier.offset(g.switchX.dp,g.controlsY.dp),5f)
     InstaSlot(f,Modifier.offset(28.dp,g.slotY.dp).size(g.slotWidth.dp,g.slotHeight.dp))
     InstaNavigation(f.accent,booth,!busy,onGallery,{sheet="booth"},onSettings,Modifier.offset(12.dp,(g.navigationY-12).dp).width((g.width-24).dp))
     InstaTransport(photo,cap,g.width,g.mouthY,reducedMotion)
    }
    if(busy)Column(Modifier.offset(24.dp,(g.lensY+g.lensDiameter/2-75).dp).width((g.width-48).dp).background(InstaStyleTokens.Optical.copy(alpha=.92f),RoundedCornerShape(26.dp)).padding(12.dp).testTag("insta-capture-status"),horizontalAlignment=Alignment.CenterHorizontally) {
     if(cap.countdown>0)CountdownOverlay(cap.countdown,cap.shot,cap.booth)
     else Text(when(cap.phase){CapturePhase.EJECTING->"Your print is emerging…";CapturePhase.CHARGING->"Charging the flash…";else->"Preparing your photograph…"},color=InstaStyleTokens.Paper,textAlign=TextAlign.Center)
     TextButton(onClick=onCancel,modifier=Modifier.heightIn(min=48.dp).semantics{contentDescription=if(cap.booth)"Cancel session" else "Cancel countdown"}){Text("Cancel",color=InstaStyleTokens.Paper)}
    }
   }
   if(g.essentialControls && cap.phase==CapturePhase.EJECTING)Box(Modifier.fillMaxWidth().height(240.dp).clipToBounds().testTag("insta-accessible-ejection")) {
    InstaSlot(f,Modifier.offset(28.dp,0.dp).size(g.slotWidth.dp,g.slotHeight.dp))
    InstaTransport(photo,cap,g.width,30f*g.slotWidth/337f,reducedMotion)
   }
   if(g.essentialControls)Column(Modifier.fillMaxWidth().padding(16.dp).testTag("insta-essential-controls"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
    Text("${cartridge?.remaining ?: 0} shots remaining",color=InstaStyleTokens.Ink,style=MaterialTheme.typography.titleLarge)
    InstaTrayAction(if(booth)"Start Photo Booth" else "Take photograph",enabled,onCapture,2f)
    InstaTrayAction("Replace cartridge",initialized && !busy,onLoad,0f)
    InstaTrayAction("Self-timer · ${settings.timer.label}",!busy,{sheet="timer"},3f)
    if(settings.front)InstaTrayAction("Selfie lighting · ${settings.selfieLight.label}",!busy,{sheet="selfie"},4f)
    else InstaTrayAction("Rear camera flash · ${if(!hasFlash)"Unavailable" else if(settings.rearFlash)"On" else "Off"}",captureReady && hasFlash && !busy,{onFlash(!settings.rearFlash)},4f)
    InstaTrayAction(if(settings.front)"Switch to rear camera" else "Switch to front camera",switchReady && !busy,onSwitch,5f)
    InstaNavigation(f.accent,booth,!busy,onGallery,{sheet="booth"},onSettings,Modifier.fillMaxWidth(),stacked=true)
   }
   if(cameraAccess!=null)Column(Modifier.fillMaxWidth().padding(16.dp)) {Button(onClick=cameraAccess,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Enable camera")};openAndroidSettings?.let{TextButton(onClick=it){Text("Open Android settings")}}}
   if(!initialized)TextButton(onClick=onRecover){Text("Retry archive recovery")}
   Text(if(booth)"Photo Booth · four photos, five seconds apart" else cartridge?.let{"${FilmFormat.valueOf(it.format).label} · ${Appearance.valueOf(it.appearance).label}"} ?: "Tap the counter to load film.",Modifier.padding(16.dp),color=InstaStyleTokens.Ink,style=MaterialTheme.typography.bodySmall)
   if(booth && (cartridge?.remaining ?: 0)<4)Text("Photo Booth needs four exposures. Manually replace the cartridge.",Modifier.padding(16.dp),color=InstaStyleTokens.Ink)
   Text("The circular aperture crops the live view. Your film controls the saved photograph.",Modifier.padding(horizontal=16.dp).padding(bottom=16.dp),color=InstaStyleTokens.Ink,style=MaterialTheme.typography.bodySmall)
  }
 }
 when(sheet){"timer"->CameraTimerSheet(settings,busy,{sheet=null},onTimer);"selfie"->CameraSelfieSheet(settings,busy,{sheet=null},onSelfieLight);"booth"->CameraBoothSheet(settings,busy,{sheet=null},onBooth,onLayout,onSelfieLight)}
}

/** Unbounded transport preserves physical strip length; only the visible viewport clips it. */
@Composable private fun InstaTransport(photo: PhotoEntity?,cap: CaptureUi,width: Float,mouthY: Float,reducedMotion: Boolean) {
 if(cap.phase==CapturePhase.EJECTING) {
  val aspect=photo?.geometry?.aspect ?: FilmFormat.CLASSIC.aspect
  val w=minOf(width-96,220f).dp;val h=w/aspect
  Box(Modifier.offset((width.dp-w)/2,mouthY.dp).width(w).wrapContentSize(Alignment.TopCenter,unbounded=true).height(h).clipToBounds().testTag("insta-paper-mouth")) {
   FilmPrint(photo,Modifier.width(w).offset(y=InstaStyleMotion.paperOffset(cap.ejection,h.value,reducedMotion).dp).testTag("insta-ejecting-print"))
  }
 }
 Canvas(Modifier.offset(52.dp,mouthY.dp).size((width-104).dp,3.dp).testTag("insta-front-lip").clearAndSetSemantics{}) {
  drawRoundRect(InstaStyleTokens.Optical,cornerRadius=CornerRadius(2.dp.toPx()));drawLine(Color(0xFFAF8D96),Offset.Zero,Offset(size.width,0f),1.dp.toPx())
 }
}

@Composable private fun InstaCounter(remaining: Int?,enabled: Boolean,load: ()->Unit,modifier: Modifier) {
 Column(modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(InstaStyleTokens.Optical).border(1.dp,Color.White,RoundedCornerShape(12.dp))
  .clickable(enabled=enabled,role=Role.Button,onClick=load).semantics(mergeDescendants=true){contentDescription=if(remaining==null)"Load film" else "Film: $remaining shots remaining. Replace cartridge"},horizontalAlignment=Alignment.CenterHorizontally) {
  InstaEngraving{Text(remaining?.toString() ?: "—",Modifier.padding(top=5.dp),color=InstaStyleTokens.Paper,fontSize=26.sp,lineHeight=32.sp,fontWeight=FontWeight.Bold);Text(if(remaining==0)"EMPTY" else "SHOTS",Modifier.padding(top=4.dp),color=InstaStyleTokens.Paper,fontSize=10.sp,lineHeight=14.sp)}
 }
}

@Composable private fun InstaFlashWindow(settings: Settings,cap: CaptureUi,available: Boolean,modifier: Modifier) {
 val requested=if(settings.front)settings.selfieLight.screenFlash else settings.rearFlash
 val state=when {settings.front && !settings.selfieLight.screenFlash || !settings.front && !available->"Unavailable";cap.phase==CapturePhase.CAPTURING && cap.shutterFired && requested->"Firing";cap.phase==CapturePhase.CHARGING && requested->"Charging";requested->"Ready";else->"Off"}
 Canvas(modifier.testTag("insta-flash-window").semantics{stateDescription=state}.clearAndSetSemantics{}) {
  val sx=size.width/44;val sy=size.height/76
  drawRoundRect(Color(0xFFBAB5A5),cornerRadius=CornerRadius(12*sx))
  val tint=when(state){"Firing"->Color.White;"Charging"->InstaStyleTokens.Flash;"Ready"->Color(0xFFFFF1D1);else->Color(0xFFE9E5D6)}
  drawRoundRect(Brush.verticalGradient(listOf(Color.White,tint,Color(0xFFBBB6A3))),Offset(4*sx,4*sy),Size(36*sx,68*sy),CornerRadius(9*sx))
  for(x in 7..37 step 5)drawLine(Color.White.copy(alpha=.6f),Offset(x*sx,7*sy),Offset(x*sx,68*sy),sx)
  for(y in 9..63 step 6)drawLine(Color(0xFF968E7F).copy(alpha=.35f),Offset(7*sx,y*sy),Offset(37*sx,y*sy),sy)
  if(state=="Unavailable")drawRoundRect(InstaStyleTokens.Optical.copy(alpha=.3f),Offset(4*sx,4*sy),Size(36*sx,68*sy),CornerRadius(9*sx))
 }
}

@Composable private fun InstaLens(f: CameraFinish,modifier: Modifier,preview: @Composable ()->Unit) {
 Box(modifier.testTag("insta-lens").shadow(8.dp,CircleShape,ambientColor=InstaStyleTokens.Shadow,spotColor=InstaStyleTokens.Shadow).clip(CircleShape).background(f.body).border(1.dp,Color.White,CircleShape),contentAlignment=Alignment.Center) {
  Canvas(Modifier.fillMaxSize().clearAndSetSemantics{}) {
   drawCircle(Brush.verticalGradient(listOf(Color.White.copy(alpha=.45f),Color.White.copy(alpha=.02f),Color.Black.copy(alpha=.12f))),size.minDimension/2)
   drawCircle(f.body,size.minDimension*.484f)
   drawCircle(lerp(f.body,InstaStyleTokens.Shadow,.25f),size.minDimension*.442f)
  }
  Box(Modifier.fillMaxSize(262f/310f).clip(CircleShape).background(InstaStyleTokens.Optical).testTag("insta-live-aperture").semantics{contentDescription="Live camera viewfinder"}){preview()}
  Canvas(Modifier.fillMaxSize().clearAndSetSemantics{}) {
   val side=size.width*64/310;val x=(size.width-side)/2;val a=12*size.width/310;val p=Path()
   for((xx,yy,dx,dy) in listOf(listOf(x,x,1f,1f),listOf(x+side,x,-1f,1f),listOf(x,x+side,1f,-1f),listOf(x+side,x+side,-1f,-1f))){p.moveTo(xx+dx*a,yy);p.lineTo(xx,yy);p.lineTo(xx,yy+dy*a)}
   drawPath(p,InstaStyleTokens.Paper,style=Stroke(2*size.width/310,cap=StrokeCap.Round))
  }
 }
}

@Composable private fun InstaControl(description: String,label: String,icon: Int,enabled: Boolean,click: ()->Unit,modifier: Modifier,index: Float,selected: Boolean=false,role: Role=Role.Button,surface: Color=if(selected)InstaStyleTokens.Flash else InstaStyleTokens.Raised) {
 val interaction=remember{MutableInteractionSource()};val down by interaction.collectIsPressedAsState();val focused by interaction.collectIsFocusedAsState()
 val depth by animateDpAsState(if(down)1.dp else 3.dp,tween(if(down)70 else 110),label="Insta satin depth")
 SatinSurface(surface,modifier.size(54.dp,58.dp).clickable(enabled=enabled,role=role,interactionSource=interaction,indication=LocalIndication.current,onClick=click).semantics(mergeDescendants=true){contentDescription=description;traversalIndex=index},elevation=depth,focused=focused) {
  InstaEngraving{Column(Modifier.fillMaxSize().alpha(if(enabled)1f else .42f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(painterResource(icon),null,tint=InstaStyleTokens.Ink,modifier=Modifier.size(24.dp));Text(label,color=InstaStyleTokens.Ink,fontSize=12.sp,lineHeight=16.sp,fontWeight=FontWeight.Bold)}}
 }
}

/** One persistent icon, same 300 ms transition and domain-owned toggle haptic as the other skins. */
@Composable private fun InstaFlashControl(on: Boolean,enabled: Boolean,available: Boolean,toggle: (Boolean)->Unit,modifier: Modifier,reducedMotion: Boolean) {
 val p by animateFloatAsState(if(on)1f else 0f,tween(if(reducedMotion)0 else FlashMotion.durationMs),label="Insta flash illumination")
 InstaControl("Rear camera flash","Flash",R.drawable.is_flash,enabled && available,{toggle(!on)},modifier.semantics{role=Role.Switch;stateDescription=if(!available)"Flash unavailable" else if(on)"Flash on" else "Flash off";toggleableState=if(on)androidx.compose.ui.state.ToggleableState.On else androidx.compose.ui.state.ToggleableState.Off},4f,on,Role.Switch,lerp(InstaStyleTokens.Raised,InstaStyleTokens.Flash,p))
 Canvas(modifier.size(54.dp,58.dp).clearAndSetSemantics{}) {
  if(!reducedMotion && p>0)drawCircle(Color(0xFF387E56).copy(alpha=.12f*p),18.dp.toPx(),Offset(size.width/2,18.dp.toPx()))
  if(p<1)drawLine(InstaStyleTokens.Ink,Offset(17.dp.toPx(),6.dp.toPx()),Offset(38.dp.toPx(),27.dp.toPx()),1.7.dp.toPx(),alpha=1-p)
 }
}

@Composable private fun InstaShutter(f: CameraFinish,enabled: Boolean,booth: Boolean,status: String,click: ()->Unit,modifier: Modifier,reducedMotion: Boolean) {
 val interaction=remember{MutableInteractionSource()};val down by interaction.collectIsPressedAsState();val focus by interaction.collectIsFocusedAsState()
 val depth by animateDpAsState(if(down && enabled && !reducedMotion)2.dp else 0.dp,tween(if(down)InstaStyleMotion.pressMs else InstaStyleMotion.releaseMs),label="Insta shutter depression")
 Box(modifier.testTag("insta-shutter").clickable(enabled=enabled,role=Role.Button,interactionSource=interaction,indication=null,onClick=click).semantics{contentDescription=if(booth)"Start Photo Booth" else "Take photograph";stateDescription=if(down && enabled)"Pressed" else if(booth && status=="Ready")"Photo Booth ready" else status;traversalIndex=2f},contentAlignment=Alignment.Center) {
  Box(Modifier.fillMaxSize().offset{IntOffset(0,depth.roundToPx())}.shadow(if(down)1.dp else 4.dp,CircleShape,ambientColor=InstaStyleTokens.Shadow,spotColor=InstaStyleTokens.Shadow).clip(CircleShape).background(lerp(f.body,Color.White,.12f)).then(if(focus)Modifier.border(2.dp,InstaStyleTokens.Focus,CircleShape)else Modifier),contentAlignment=Alignment.Center) {
   Box(Modifier.fillMaxSize(.84f).clip(CircleShape).background(InstaStyleTokens.Raised).border(1.dp,Color.White,CircleShape),contentAlignment=Alignment.Center){Icon(painterResource(R.drawable.is_camera),null,tint=f.accent.copy(alpha=if(enabled)1f else .42f),modifier=Modifier.size(36.dp))}
  }
 }
}

@Composable private fun InstaSlot(f: CameraFinish,modifier: Modifier) {
 SatinSurface(f.body,modifier.testTag("insta-ejection-slot"),30.dp,4.dp) {
  Canvas(Modifier.fillMaxSize().clearAndSetSemantics{}) {
   val s=size.width/337
   drawRoundRect(f.body,Offset(12*s,17*s),Size(size.width-24*s,38*s),CornerRadius(19*s))
   drawRoundRect(Color.White,Offset(12*s,17*s),Size(size.width-24*s,38*s),CornerRadius(19*s),style=Stroke(s))
   drawRoundRect(InstaStyleTokens.Optical,Offset(24*s,30*s),Size(size.width-48*s,9*s),CornerRadius(5*s))
   drawLine(Color(0xFFAF8D96),Offset(28*s,30*s),Offset(size.width-28*s,30*s),s)
  }
 }
}

@Composable private fun InstaNavigation(accent: Color,booth: Boolean,enabled: Boolean,gallery: ()->Unit,photoBooth: ()->Unit,settings: ()->Unit,modifier: Modifier,stacked: Boolean=false) {
 SatinSurface(InstaStyleTokens.Raised,modifier.testTag("insta-navigation"),39.dp) {
  @Composable fun Item(label: String,icon: Int,selected: Boolean,index: Float,click: ()->Unit,modifier: Modifier) {
   val colors=ButtonDefaults.buttonColors(containerColor=if(selected)accent else InstaStyleTokens.Raised,contentColor=if(selected)InstaStyleTokens.Paper else InstaStyleTokens.Ink,disabledContainerColor=InstaStyleTokens.Raised,disabledContentColor=InstaStyleTokens.Ink.copy(alpha=.42f))
   Button(onClick=click,enabled=enabled,modifier=modifier.heightIn(min=if(stacked)126.dp else 58.dp).shadow(3.dp,RoundedCornerShape(29.dp),ambientColor=InstaStyleTokens.Shadow,spotColor=InstaStyleTokens.Shadow).border(1.dp,Color.White,RoundedCornerShape(29.dp)).semantics{contentDescription=label;traversalIndex=index;this.selected=selected},colors=colors,shape=RoundedCornerShape(29.dp),contentPadding=PaddingValues(4.dp)) {
    if(stacked)Text(label,textAlign=TextAlign.Center) else InstaEngraving{Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(painterResource(icon),null,Modifier.size(24.dp));Text(label,fontSize=if(label=="Photo Booth")11.sp else 12.sp,lineHeight=16.sp,fontWeight=FontWeight.Bold)}}
   }
  }
  if(stacked)Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Item("Gallery",R.drawable.is_gallery,false,6f,gallery,Modifier.fillMaxWidth());Item("Photo Booth",R.drawable.is_booth,booth,7f,photoBooth,Modifier.fillMaxWidth());Item("Settings",R.drawable.is_settings,false,8f,settings,Modifier.fillMaxWidth())}
  else Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){Item("Gallery",R.drawable.is_gallery,false,6f,gallery,Modifier.weight(1f));Item("Photo Booth",R.drawable.is_booth,booth,7f,photoBooth,Modifier.weight(1f));Item("Settings",R.drawable.is_settings,false,8f,settings,Modifier.weight(1f))}
 }
}

@Composable private fun InstaTrayAction(label: String,enabled: Boolean,click: ()->Unit,index: Float) {
 Button(onClick=click,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=if(LocalDensity.current.fontScale>1.4f)126.dp else 80.dp).semantics{traversalIndex=index},colors=ButtonDefaults.buttonColors(containerColor=InstaStyleTokens.Raised,contentColor=InstaStyleTokens.Ink)){Text(label,textAlign=TextAlign.Center)}
}

/** Decorative, single, non-live preview: no CameraX binding or capture callbacks. */
@Composable fun InstaStylePreview(color: CameraColor,modifier: Modifier=Modifier) {
 val f=cameraFinish(CameraStyle.INSTA_STYLE,color)
 Column(modifier.fillMaxWidth().testTag("insta-style-preview").semantics(mergeDescendants=true){contentDescription="Insta Style, ${color.label} camera preview"},horizontalAlignment=Alignment.CenterHorizontally) {
  SatinSurface(f.body,Modifier.size(146.dp,244.dp).testTag("insta-preview-body"),20.dp) {
   BrandLogo(Modifier.offset(10.dp,12.dp).size(60.dp,12.dp),plate=false)
   Box(Modifier.offset(12.dp,42.dp).size(22.dp).background(InstaStyleTokens.Optical,RoundedCornerShape(5.dp)))
   InstaLens(f,Modifier.align(Alignment.Center).offset(y=(-12).dp).size(120.dp)){Box(Modifier.fillMaxSize().background(InstaStyleTokens.Optical))}
   Box(Modifier.align(Alignment.BottomCenter).padding(bottom=37.dp).size(32.dp).background(InstaStyleTokens.Raised,CircleShape))
   InstaSlot(f,Modifier.align(Alignment.BottomCenter).padding(bottom=12.dp).size(120.dp,25.dp))
  }
  Text("Live finish preview · ${color.label}",Modifier.padding(top=12.dp),color=MaterialTheme.colorScheme.onSurface,style=MaterialTheme.typography.bodyMedium,textAlign=TextAlign.Center)
 }
}

@Composable fun InstaLoadingCard(format: FilmFormat,appearance: Appearance,empty: Boolean,modifier: Modifier=Modifier) {
 SatinSurface(InstaStyleTokens.Optical,modifier,18.dp) {
  Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("INSTANTROOM",color=InstaStyleTokens.Paper);Text(if(empty)"EMPTY BAY" else "${format.label} · 10",color=InstaStyleTokens.Paper,style=MaterialTheme.typography.titleLarge);Text(appearance.label,color=InstaStyleTokens.Paper);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){repeat(10){Box(Modifier.size(12.dp,4.dp).background(InstaStyleTokens.Flash))}}}
 }
}
