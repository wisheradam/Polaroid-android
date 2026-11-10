package com.instantroom.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.core.net.toUri
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.instantroom.core.*
import com.instantroom.data.*

@Composable fun CameraScreen(vm: RoomViewModel, cartridge: CartridgeEntity?, cap: CaptureUi, settings: Settings, initialized: Boolean, load: ()->Unit) {
 val availability by vm.graph.camera.state.collectAsStateWithLifecycle()
 val context=LocalContext.current
 val owner=LocalLifecycleOwner.current
 var booth by rememberSaveable{mutableStateOf(false)}
 var denied by remember {mutableStateOf(false)}
 var permitted by remember {mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)}
 val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){permitted=it;denied=!it}
 val preview=remember{PreviewView(context).apply {implementationMode=PreviewView.ImplementationMode.COMPATIBLE;scaleType=PreviewView.ScaleType.FILL_CENTER}}
 var ready by remember{mutableStateOf(false)}
 var hasFlash by remember{mutableStateOf(false)}
 val flash=settings.rearFlash
 var error by remember{mutableStateOf<String?>(null)}
 var active by remember{mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))}
 DisposableEffect(owner) {
  val obs=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_RESUME){active=true;permitted=ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED} else if(event==Lifecycle.Event.ON_PAUSE){active=false;ready=false;vm.cancelCapture();vm.graph.camera.unbind()}}
  owner.lifecycle.addObserver(obs);onDispose {owner.lifecycle.removeObserver(obs);vm.cancelCapture();vm.graph.camera.unbind()}
 }
 LaunchedEffect(permitted,settings.front,active,booth) {
  ready=false
  if(permitted && active)try{vm.graph.camera.bind(owner,preview,settings.front,booth);ready=true;hasFlash=vm.graph.camera.hasFlash;error=null
   val actual=vm.graph.camera.state.value.selectedFront
   if(actual!=settings.front){vm.updateSettings(vm.settings.value.copy(front=actual));vm.message.value="Requested camera is unavailable; using the available camera"}
  }catch(e:CancellationException){throw e}catch(e:Exception){error=e.message}
  else vm.graph.camera.unbind()
 }
 LaunchedEffect(availability.ready){if(!availability.ready)vm.cancelCapture()}
 val window=LocalWindowInfo.current.containerSize
 val previewHeight=with(LocalDensity.current){(window.height*.31f).toDp()}.coerceIn(170.dp,300.dp)
 val busy=cap.phase!=CapturePhase.IDLE
 val cameraScroll=rememberScrollState()
 LaunchedEffect(busy){if(busy)cameraScroll.animateScrollTo(0)}
 val previewAspect=if(booth)4f/3f else (cartridge?.let{FilmFormat.valueOf(it.format).imageAspect} ?: FilmFormat.CLASSIC.imageAspect)
 val format=cartridge?.let {FilmFormat.valueOf(it.format)} ?: FilmFormat.CLASSIC
 Box(Modifier.fillMaxSize()) {
  Column(Modifier.fillMaxSize().verticalScroll(cameraScroll).padding(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp),horizontalAlignment=Alignment.CenterHorizontally) {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
    BrandLogo(Modifier.weight(1f).padding(end=12.dp))
    CameraSwitchControl(settings.front,ready && !busy && if(settings.front)availability.rear else availability.front) {
     if(vm.switchCamera(availability.front,availability.rear))ready=false
    }
   }
   if(!busy)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
    FilterChip(selected=!booth,enabled=!busy,onClick={booth=false},modifier=Modifier.weight(.8f),label={Text("Single")})
    FilterChip(selected=booth,enabled=!busy,onClick={booth=true},modifier=Modifier.weight(1.2f),label={Text("Photo Booth")})
    if(!booth)TimerControl(settings.timer,!busy,Modifier.weight(1f)){vm.updateSettings(settings.copy(timer=it))}
   }
   if(booth && !busy) {
    BoothLayoutControl(settings.boothLayout,!busy){vm.updateSettings(settings.copy(boothLayout=it))}
    Text("Four photographs · 5 seconds apart · 4 exposures",style=MaterialTheme.typography.bodySmall)
    if((cartridge?.remaining ?: 0)<4)Text("Photo Booth needs four exposures. Manually load a fresh cartridge.",color=Amber)
   }
   CameraShell(settings.cameraStyle,settings.cameraColor,Modifier.fillMaxWidth()) {
    CameraHousingHeader(settings.cameraStyle,ready && availability.ready)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){SmallLabel("OPTICAL VIEWFINDER");Text(if(ready)"● LIVE" else "○ STANDBY",color=if(ready)Amber else Cream,style=MaterialTheme.typography.labelSmall)}
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val h=minOf(previewHeight,maxWidth/previewAspect)
    Box(Modifier.align(Alignment.Center).width(h*previewAspect).height(h).border(if(settings.cameraStyle==CameraStyle.INSTA_STYLE)5.dp else 2.dp,MaterialTheme.colorScheme.primary.copy(alpha=.6f),if(!booth && format==FilmFormat.ROUND)CircleShape else RoundedCornerShape(if(settings.cameraStyle==CameraStyle.OLD_FASION)4.dp else if(settings.cameraStyle==CameraStyle.INSTA_STYLE)25.dp else 8.dp)).clip(if(!booth && format==FilmFormat.ROUND)CircleShape else RoundedCornerShape(if(settings.cameraStyle==CameraStyle.OLD_FASION)4.dp else if(settings.cameraStyle==CameraStyle.INSTA_STYLE)25.dp else 8.dp)).background(Color.Black),contentAlignment=Alignment.Center) {
     if(permitted)AndroidView(factory={preview},modifier=Modifier.fillMaxSize())
     else Column(horizontalAlignment=Alignment.CenterHorizontally) {Text("Let the light in.",color=Color.White);Button(onClick={request.launch(Manifest.permission.CAMERA)}){Text("Enable camera")};if(denied)TextButton(onClick={context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,"package:${context.packageName}".toUri()))}){Text("Open Android settings")} }
     if(permitted && !ready)Text(error ?: "Opening the lens…",Modifier.padding(16.dp),color=Color.White)
     CountdownOverlay(cap.countdown,cap.shot,cap.booth)
     Box(Modifier.fillMaxSize().padding(18.dp).border(1.dp,Cream.copy(alpha=.3f),RoundedCornerShape(4.dp)))
     if(busy)TextButton(onClick={vm.cancelCapture()},modifier=Modifier.align(Alignment.BottomCenter).padding(bottom=12.dp).background(Color.Black.copy(alpha=.7f),RoundedCornerShape(12.dp)).semantics{contentDescription=if(cap.booth)"Cancel session" else "Cancel countdown"}){Text("Cancel",color=Color.White)}
    }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
     Column {SmallLabel(format.label.uppercase());Text(cartridge?.let{Appearance.valueOf(it.appearance).label} ?: "No cartridge installed")}
     if(!settings.front)FlashToggle(flash,hasFlash,ready && availability.ready && !busy){vm.setRearFlash(it,hasFlash)}
    }
    if(settings.front) {
     SelfieLightControl(settings.selfieLight,!busy){vm.updateSettings(settings.copy(selfieLight=it))}
     Text("Saved selfies match the mirrored preview.",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
   if(cap.booth && cap.sourceIds.isNotEmpty()) {val photos by vm.photos.collectAsState();BoothThumbnails(cap.sourceIds.mapNotNull{id->photos.find{it.id==id}})}
   CameraMaterial(settings.cameraStyle,settings.cameraColor) {
   Row(Modifier.fillMaxWidth().background(cameraFinish(settings.cameraStyle,settings.cameraColor).body,RoundedCornerShape(cameraCorner(settings.cameraStyle))).padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
    Column {SmallLabel("EXPOSURES");Text(cartridge?.remaining?.toString()?.padStart(2,'0') ?: "—",style=MaterialTheme.typography.displayMedium)}
    CameraShutter(settings.cameraStyle,settings.cameraColor,booth,ready && availability.ready && initialized && !busy && (cartridge?.remaining ?: 0)>=(if(booth)4 else 1)){if(booth)vm.startBooth(flash)else vm.takePicture(flash)}
    Column(horizontalAlignment=Alignment.End){SmallLabel("FILM BAY");TextButton(onClick=load,enabled=!busy && initialized){Text(if(cartridge==null)"Load film" else "Replace")}}
   }
   }
   Box(Modifier.fillMaxWidth().height(8.dp).background(Color.Black,RoundedCornerShape(4.dp)))
   Text(when{!initialized->"Restoring the archive…";busy->when(cap.phase){CapturePhase.COUNTDOWN->if(cap.booth)"PHOTO ${cap.shot} OF 4" else "Get ready…";CapturePhase.CHARGING->"Charging the flash…";CapturePhase.CAPTURING->"Hold still. Catching the light.";CapturePhase.PROCESSING->"Preparing your instant photograph…";else->"The rollers are turning…"};cartridge==null->"Open the film bay to begin.";cartridge.remaining==0->"FILM EMPTY · manually replace cartridge";else->"Ten moments. Make each one yours."},color=Amber,style=MaterialTheme.typography.bodySmall)
   if(!initialized)TextButton(onClick={vm.recover()}){Text("Retry archive recovery")}
  }
  if(cap.phase==CapturePhase.EJECTING) {
   val photos by vm.photos.collectAsState()
   Box(Modifier.align(Alignment.Center).fillMaxWidth().height(380.dp).clipToBounds(),contentAlignment=Alignment.TopCenter){
    val photo=photos.find{it.id==cap.photoId}
    FilmPrint(photo,Modifier.width(minOf(210.dp,360.dp*(photo?.geometry?.aspect ?: .8f))).offset(y=((1-cap.ejection)*380).dp).shadow(14.dp))
   }
  }
 }
}
