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
import androidx.compose.ui.platform.testTag
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

@Composable fun CameraScreen(vm: RoomViewModel, cartridge: CartridgeEntity?, cap: CaptureUi, settings: Settings, initialized: Boolean, load: ()->Unit, onGallery: ()->Unit = {}, onSettings: ()->Unit = {}) {
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
 var bindRetry by remember{mutableIntStateOf(0)}
 var active by remember{mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))}
 DisposableEffect(owner) {
  val obs=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_RESUME){active=true;permitted=ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED} else if(event==Lifecycle.Event.ON_PAUSE){active=false;ready=false;vm.cancelCapture();vm.graph.camera.unbind()}}
  owner.lifecycle.addObserver(obs);onDispose {owner.lifecycle.removeObserver(obs);vm.cancelCapture();vm.graph.camera.unbind()}
 }
 LaunchedEffect(permitted,settings.front,active,booth,bindRetry) {
  ready=false
  if(permitted && active)try{vm.graph.camera.bind(owner,preview,settings.front,booth);ready=true;hasFlash=vm.graph.camera.hasFlash;error=null
   val actual=vm.graph.camera.state.value.selectedFront
   if(actual!=settings.front){vm.updateSettings(vm.settings.value.copy(front=actual));vm.message.value="Requested camera is unavailable; using the available camera"}
  }catch(e:CancellationException){throw e}catch(e:Exception){error=e.message}
  else vm.graph.camera.unbind()
 }
 LaunchedEffect(availability.ready){if(!availability.ready)vm.cancelCapture()}
 val busy=cap.phase!=CapturePhase.IDLE
 if(settings.cameraStyle==CameraStyle.OLD_FASION) {
  val photos by vm.photos.collectAsStateWithLifecycle()
  OldFashionCamera(settings,cartridge,cap,initialized,ready && availability.ready,
   ready && if(settings.front)availability.rear else availability.front,hasFlash,booth,photos.find{it.id==cap.photoId},
   onCapture={if(booth)vm.startBooth(flash)else vm.takePicture(flash)},onLoad=load,onGallery=onGallery,onSettings=onSettings,
   onSwitch={if(vm.switchCamera(availability.front,availability.rear))ready=false},onFlash={vm.setRearFlash(it,hasFlash)},
   onTimer={vm.updateSettings(settings.copy(timer=it))},onBooth={booth=it},onLayout={vm.updateSettings(settings.copy(boothLayout=it))},
   onSelfieLight={vm.updateSettings(settings.copy(selfieLight=it))},onCancel=vm::cancelCapture,onRecover=vm::recover,
   cameraAccess=if(!permitted)({request.launch(Manifest.permission.CAMERA)}) else null,
   openAndroidSettings=if(denied)({context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,"package:${context.packageName}".toUri()))})else null) {
    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
     if(permitted)AndroidView(factory={preview},modifier=Modifier.fillMaxSize())
     else Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.padding(4.dp)) {
      Text("Camera access",color=Color.White,fontSize=12.sp,lineHeight=14.sp)
      TextButton(onClick={if(denied)context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,"package:${context.packageName}".toUri()))else request.launch(Manifest.permission.CAMERA)},modifier=Modifier.heightIn(min=48.dp).semantics{contentDescription=if(denied)"Open application settings for camera access" else "Allow camera access"}){Text(if(denied)"Settings" else "Enable",fontSize=12.sp)}
     }
     if(permitted && !ready)Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.background(Color.Black.copy(alpha=.7f)).padding(6.dp)) {
      Text(if(error==null)"Opening camera…" else "Camera unavailable",color=Color.White,style=MaterialTheme.typography.labelSmall)
      if(error!=null)TextButton(onClick={bindRetry++}){Text("Try again",color=Color.White)}
     }
    }
   }
  return
 }
 if(settings.cameraStyle==CameraStyle.ONE_AND_ONLY) {
  val photos by vm.photos.collectAsStateWithLifecycle()
  OneAndOnlyCamera(settings,cartridge,cap,initialized,ready && availability.ready,
   ready && if(settings.front)availability.rear else availability.front,hasFlash,booth,
   photos.find{it.id==cap.photoId},
   onCapture={if(booth)vm.startBooth(flash)else vm.takePicture(flash)},onLoad=load,
   onGallery=onGallery,onSettings=onSettings,
   onSwitch={if(vm.switchCamera(availability.front,availability.rear))ready=false},
   onFlash={vm.setRearFlash(it,hasFlash)},onTimer={vm.updateSettings(settings.copy(timer=it))},
   onBooth={booth=it},onLayout={vm.updateSettings(settings.copy(boothLayout=it))},
   onSelfieLight={vm.updateSettings(settings.copy(selfieLight=it))},onRecover=vm::recover) {
   Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
    if(permitted)AndroidView(factory={preview},modifier=Modifier.fillMaxSize())
    else Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.padding(8.dp)) {
     Text("Camera access",color=Color.White,style=MaterialTheme.typography.bodySmall)
     Button(onClick={if(denied)context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,"package:${context.packageName}".toUri()))else request.launch(Manifest.permission.CAMERA)},modifier=Modifier.semantics{contentDescription=if(denied)"Open application settings for camera access" else "Allow camera access"}){Text(if(denied)"Settings" else "Enable",fontSize=12.sp)}
    }
    if(permitted && !ready)Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.background(Color.Black.copy(alpha=.65f)).padding(8.dp)) {
     Text(if(error==null)"Opening camera…" else "Camera unavailable",color=Color.White,style=MaterialTheme.typography.labelLarge)
     if(error!=null)TextButton(onClick={bindRetry++}){Text("Try again",color=Color.White)}
    }
    CountdownOverlay(cap.countdown,cap.shot,cap.booth)
    if(busy)TextButton(onClick={vm.cancelCapture()},modifier=Modifier.align(Alignment.BottomCenter).background(Color.Black.copy(alpha=.75f),RoundedCornerShape(12.dp)).semantics{contentDescription=if(cap.booth)"Cancel session" else "Cancel countdown"}){Text("Cancel",color=Color.White)}
   }
  }
  return
 }
  val photos by vm.photos.collectAsStateWithLifecycle()
  InstaStyleCamera(settings,cartridge,cap,initialized,ready && availability.ready,
   ready && if(settings.front)availability.rear else availability.front,hasFlash,booth,photos.find{it.id==cap.photoId},
   onCapture={if(booth)vm.startBooth(flash)else vm.takePicture(flash)},onLoad=load,onGallery=onGallery,onSettings=onSettings,
   onSwitch={if(vm.switchCamera(availability.front,availability.rear))ready=false},onFlash={vm.setRearFlash(it,hasFlash)},
   onTimer={vm.updateSettings(settings.copy(timer=it))},onBooth={booth=it},onLayout={vm.updateSettings(settings.copy(boothLayout=it))},
   onSelfieLight={vm.updateSettings(settings.copy(selfieLight=it))},onCancel=vm::cancelCapture,onRecover=vm::recover,
   cameraAccess=if(!permitted)({request.launch(Manifest.permission.CAMERA)}) else null,
   openAndroidSettings=if(denied)({context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,"package:${context.packageName}".toUri()))})else null) {
    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
     if(permitted)AndroidView(factory={preview},modifier=Modifier.fillMaxSize())
     else Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.padding(4.dp)) {
      Text("Camera access",color=Color.White,fontSize=12.sp,lineHeight=14.sp)
      TextButton(onClick={if(denied)context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,"package:${context.packageName}".toUri()))else request.launch(Manifest.permission.CAMERA)},modifier=Modifier.heightIn(min=48.dp).semantics{contentDescription=if(denied)"Open application settings for camera access" else "Allow camera access"}){Text(if(denied)"Settings" else "Enable",fontSize=12.sp)}
     }
     if(permitted && !ready)Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.background(Color.Black.copy(alpha=.7f)).padding(6.dp)) {
      Text(if(error==null)"Opening camera…" else "Camera unavailable",color=Color.White,style=MaterialTheme.typography.labelSmall)
      if(error!=null)TextButton(onClick={bindRetry++}){Text("Try again",color=Color.White)}
     }
    }
   }
}
