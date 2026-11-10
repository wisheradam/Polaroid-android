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
 var denied by remember {mutableStateOf(false)}
 var permitted by remember {mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)}
 val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){permitted=it;denied=!it}
 val preview=remember{PreviewView(context).apply {implementationMode=PreviewView.ImplementationMode.COMPATIBLE;scaleType=PreviewView.ScaleType.FILL_CENTER}}
 var ready by remember{mutableStateOf(false)}
 var hasFlash by remember{mutableStateOf(false)}
 var flash by remember{mutableStateOf(false)}
 var error by remember{mutableStateOf<String?>(null)}
 var active by remember{mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))}
 DisposableEffect(owner) {
  val obs=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_RESUME){active=true;permitted=ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED} else if(event==Lifecycle.Event.ON_PAUSE){active=false;ready=false;vm.graph.camera.unbind()}}
  owner.lifecycle.addObserver(obs);onDispose {owner.lifecycle.removeObserver(obs);vm.graph.camera.unbind()}
 }
 LaunchedEffect(permitted,settings.front,active) {
  ready=false
  if(permitted && active)try{vm.graph.camera.bind(owner,preview,settings.front);ready=true;hasFlash=vm.graph.camera.hasFlash;error=null
   val actual=vm.graph.camera.state.value.selectedFront
   if(actual!=settings.front){vm.updateSettings(vm.settings.value.copy(front=actual));vm.message.value="Requested camera is unavailable; using the available camera"}
  }catch(e:CancellationException){throw e}catch(e:Exception){error=e.message}
  else vm.graph.camera.unbind()
 }
 val window=LocalWindowInfo.current.containerSize
 val previewHeight=with(LocalDensity.current){(window.height*.31f).toDp()}.coerceIn(170.dp,300.dp)
 val busy=cap.phase!=CapturePhase.IDLE
 val format=cartridge?.let {FilmFormat.valueOf(it.format)} ?: FilmFormat.CLASSIC
 Box(Modifier.fillMaxSize()) {
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp),horizontalAlignment=Alignment.CenterHorizontally) {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {SmallLabel("CHECK2GO / ANALOG SERIES");Text("Instant Room",style=MaterialTheme.typography.headlineMedium)}
    CameraSwitchControl(settings.front,ready && !busy && if(settings.front)availability.rear else availability.front) {
     if(vm.switchCamera(availability.front,availability.rear))ready=false
    }
   }
   Panel(Modifier.fillMaxWidth()) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){SmallLabel("OPTICAL VIEWFINDER");Text(if(ready)"● LIVE" else "○ STANDBY",color=if(ready)Amber else Cream,style=MaterialTheme.typography.labelSmall)}
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val h=minOf(previewHeight,maxWidth/format.imageAspect)
    Box(Modifier.align(Alignment.Center).width(h*format.imageAspect).height(h).clip(if(format==FilmFormat.ROUND)CircleShape else RoundedCornerShape(12.dp)).background(Color.Black),contentAlignment=Alignment.Center) {
     if(permitted)AndroidView(factory={preview},modifier=Modifier.fillMaxSize())
     else Column(horizontalAlignment=Alignment.CenterHorizontally) {Text("Let the light in.");Button(onClick={request.launch(Manifest.permission.CAMERA)}){Text("Enable camera")};if(denied)TextButton(onClick={context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,"package:${context.packageName}".toUri()))}){Text("Open Android settings")} }
     if(permitted && !ready)Text(error ?: "Opening the lens…",Modifier.padding(16.dp))
     Box(Modifier.fillMaxSize().padding(18.dp).border(1.dp,Cream.copy(alpha=.3f),RoundedCornerShape(4.dp)))
    }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
     Column {SmallLabel(format.label.uppercase());Text(cartridge?.let{Appearance.valueOf(it.appearance).label} ?: "No cartridge installed")}
     if(!settings.front)TextButton(onClick={flash=!flash},enabled=hasFlash && !busy){Text(if(!hasFlash)"No flash" else if(flash)"Flash on" else "Flash off")}
    }
    if(settings.front) {
     SelfieLightControl(settings.selfieLight,!busy){vm.updateSettings(settings.copy(selfieLight=it))}
     Text("Saved selfies match the mirrored preview.",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
   Row(Modifier.fillMaxWidth().padding(horizontal=10.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
    Column {SmallLabel("EXPOSURES");Text(cartridge?.remaining?.toString()?.padStart(2,'0') ?: "—",style=MaterialTheme.typography.displayMedium)}
    Button(onClick={vm.takePicture(flash)},enabled=ready && initialized && !busy && (cartridge?.remaining ?: 0)>0,shape=CircleShape,colors=ButtonDefaults.buttonColors(containerColor=Rust),contentPadding=PaddingValues(0.dp),modifier=Modifier.size(88.dp).border(5.dp,Cream.copy(alpha=.7f),CircleShape).semantics{contentDescription="Take photograph"}){Text("●",fontSize=34.sp,color=MaterialTheme.colorScheme.onSecondary)}
    Column(horizontalAlignment=Alignment.End){SmallLabel("FILM BAY");TextButton(onClick=load,enabled=!busy && initialized){Text(if(cartridge==null)"Load film" else "Replace")}}
   }
   Box(Modifier.fillMaxWidth().height(8.dp).background(Color.Black,RoundedCornerShape(4.dp)))
   Text(when{!initialized->"Restoring the archive…";busy->when(cap.phase){CapturePhase.CHARGING->"Charging the flash…";CapturePhase.CAPTURING->"Hold still. Catching the light.";CapturePhase.PROCESSING->"Preparing your instant photograph…";else->"The rollers are turning…"};cartridge==null->"Open the film bay to begin.";cartridge.remaining==0->"FILM EMPTY · manually replace cartridge";else->"Ten moments. Make each one yours."},color=Amber,style=MaterialTheme.typography.bodySmall)
   if(!initialized)TextButton(onClick={vm.recover()}){Text("Retry archive recovery")}
  }
  if(cap.phase==CapturePhase.EJECTING) {
   val photos by vm.photos.collectAsState()
   Box(Modifier.align(Alignment.Center).fillMaxWidth().height(380.dp).clipToBounds(),contentAlignment=Alignment.TopCenter){
    FilmPrint(photos.find{it.id==cap.photoId},Modifier.width(210.dp).offset(y=((1-cap.ejection)*380).dp).shadow(14.dp))
   }
  }
 }
}
