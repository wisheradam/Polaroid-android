package com.instantroom.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.lifecycle.compose.*
import com.instantroom.core.CameraStyle
import com.instantroom.core.CapturePhase
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.instantroom.core.SelfieLightMode

@Composable fun InstantRoom(vm: RoomViewModel) {
 val lighting=rememberSelfieLightingHost(vm.graph.camera)
 val cameraState by vm.graph.camera.state.collectAsStateWithLifecycle()
 var resumed by remember {mutableStateOf(false)}
 var screen by rememberSaveable { mutableStateOf("Camera") }
 var selected by rememberSaveable { mutableStateOf<String?>(null) }
 var handled by rememberSaveable { mutableStateOf<String?>(null) }
 val photos by vm.photos.collectAsStateWithLifecycle()
 val cartridge by vm.cartridge.collectAsStateWithLifecycle()
 val cap by vm.capture.collectAsStateWithLifecycle()
 val settings by vm.settings.collectAsStateWithLifecycle()
 val initialized by vm.initialized.collectAsStateWithLifecycle()
 val message by vm.message.collectAsStateWithLifecycle()
 val host=remember {SnackbarHostState()}
 val owner=LocalLifecycleOwner.current
 DisposableEffect(owner) {
  val observer=LifecycleEventObserver { _,event ->
   if(event==Lifecycle.Event.ON_RESUME)resumed=true
   if(event==Lifecycle.Event.ON_PAUSE){resumed=false;vm.cancelCapture()}
   if(event==Lifecycle.Event.ON_START)vm.foreground=true
   if(event==Lifecycle.Event.ON_STOP) { vm.foreground=false;vm.graph.motion.stop();vm.graph.feedback.stop() }
  }
  owner.lifecycle.addObserver(observer);resumed=owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED);vm.foreground=owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
  onDispose {owner.lifecycle.removeObserver(observer);vm.graph.motion.stop();vm.graph.feedback.stop()}
 }
 LaunchedEffect(message) { message?.let{host.showSnackbar(it);vm.clearMessage()} }
 LaunchedEffect(cap) { if(cap.phase==CapturePhase.IDLE && cap.photoId!=null && cap.photoId!=handled) { handled=cap.photoId;selected=cap.photoId;screen="Development" } }
 val context=LocalContext.current
 var pendingSave by remember {mutableStateOf<com.instantroom.data.PhotoEntity?>(null)}
 val storage=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if(ok)pendingSave?.let(vm::save) else vm.message.value="Storage permission is needed on Android 8 and 9 to save to the gallery";pendingSave=null }
 fun navigate(s: String) { if(cap.phase==CapturePhase.IDLE)screen=s }
 BackHandler(cap.phase!=CapturePhase.IDLE){vm.cancelCapture()}
 BackHandler(screen!="Camera" && cap.phase==CapturePhase.IDLE) {screen=if(screen=="Camera Design")"Settings" else "Camera"}
 val lightingActive=screen=="Camera" && resumed && settings.front && cameraState.selectedFront && cameraState.ready
 val analogCamera=screen=="Camera" && settings.cameraStyle in listOf(CameraStyle.ONE_AND_ONLY,CameraStyle.OLD_FASION)
 val cameraChrome=if(settings.cameraStyle==CameraStyle.OLD_FASION)OldFashionTokens.Chrome else AnalogTokens.Chrome
 val pendingAppearance by vm.pendingAppearance.collectAsStateWithLifecycle()
 val view=LocalView.current
 val dark=LocalInstantDark.current
 SideEffect { if(!view.isInEditMode)runCatching { WindowCompat.getInsetsController(context.activity().window,view).apply { isAppearanceLightStatusBars=if(analogCamera)false else !dark;isAppearanceLightNavigationBars=if(analogCamera)false else !dark } } }
 Box(Modifier.fillMaxSize()) {
 Scaffold(containerColor=if(analogCamera)cameraChrome else MaterialTheme.colorScheme.background,modifier=Modifier.padding(if(lightingActive && settings.selfieLight.edge)settings.edgeWidth.dp else 0.dp),snackbarHost={SnackbarHost(host)},bottomBar={
  if(!analogCamera && screen in listOf("Camera","Archive","Corkboard","Settings"))NavigationBar(containerColor=Ink) {
   listOf("Camera" to "◉","Archive" to "▧","Corkboard" to "⌗","Settings" to "⚙").forEach { (name,symbol)->NavigationBarItem(selected=screen==name,onClick={navigate(name)},enabled=cap.phase==CapturePhase.IDLE,icon={Text(symbol)},label={Text(name)}) }
  }
 }) { padding ->
  Box(Modifier.fillMaxSize().padding(padding)) {
   when(screen) {
    "Camera"->CameraScreen(vm,cartridge,cap,settings,initialized,{screen="Film Loading"},{navigate("Archive")},{navigate("Settings")})
    "Film Loading"->FilmLoadingScreen(vm,cartridge,{screen="Camera"})
    "Archive"->ArchiveScreen(photos,{selected=it;screen="Development"},{vm.recover()},{screen="Corkboard"}){p,target->vm.share(p,target)}
    "Development"-> {
     val p=photos.find{it.id==selected}
     if(p!=null)DevelopmentScreen(vm,p,settings,{screen="Archive"},{
      if(Build.VERSION.SDK_INT<=28 && ContextCompat.checkSelfPermission(context,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED) {pendingSave=p;storage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)}else vm.save(p)
     },{vm.attach(p.id){screen="Corkboard"}})
    }
    "Corkboard"->CorkboardScreen(vm,photos)
    "Settings"->SettingsScreen(settings,openCameraStyle={screen="Camera Design"},set=vm::updateSettings)
    "Camera Design"->CameraAppearanceScreen(settings,{screen="Settings"},{vm.requestCameraAppearance(it.cameraStyle,it.cameraColor)},pendingAppearance,vm::cancelPendingAppearance)
   }
  }
 }
 SelfieLightingOverlay(lighting,lightingActive,settings,LocalInstantDark.current)
 }
}
