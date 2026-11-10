package com.instantroom.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.RoundedCorner
import android.view.Window
import androidx.camera.view.ScreenFlashView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instantroom.core.LightTone
import com.instantroom.data.Settings
import com.instantroom.device.CameraController
import com.instantroom.device.SelfieLightSession

internal tailrec fun Context.activity(): Activity = when(this){is Activity->this;is ContextWrapper->baseContext.activity();else->error("Activity context required")}

class SelfieLightingHost(context: Context, window: Window) {
 val view=ScreenFlashView(context).apply {setScreenFlashWindow(window)}
 val session=SelfieLightSession(checkNotNull(view.screenFlash),{window.attributes.screenBrightness},{level->window.attributes=window.attributes.apply{screenBrightness=level}})
}

@Composable fun rememberSelfieLightingHost(camera: CameraController): SelfieLightingHost {
 val context=androidx.compose.ui.platform.LocalContext.current
 val window=context.activity().window
 val host=remember(window){SelfieLightingHost(context,window)}
 val owner=LocalLifecycleOwner.current
 DisposableEffect(host,camera,owner) {
  camera.installScreenFlash(host.session)
  val observer=LifecycleEventObserver {_,event->if(event==Lifecycle.Event.ON_PAUSE)host.session.close()}
  owner.lifecycle.addObserver(observer)
  onDispose {owner.lifecycle.removeObserver(observer);camera.installScreenFlash(null);host.session.close();host.view.setScreenFlashWindow(null)}
 }
 return host
}

@Composable fun SelfieLightingOverlay(host: SelfieLightingHost, active: Boolean, settings: Settings, dark: Boolean) {
 val flashing by host.session.active.collectAsStateWithLifecycle()
 val view=LocalView.current
 val window=view.context.activity().window
 val edge=active && settings.selfieLight.edge
 val lightColor=if(settings.lightTone==LightTone.WARM)Color(0xFFFFF4E5)else Color.White
 SideEffect {
  host.view.setBackgroundColor(if(settings.lightTone==LightTone.WARM)0xFFFFF4E5.toInt()else android.graphics.Color.WHITE)
  host.session.setFraming(active,if(edge)settings.edgeBrightness else null)
  WindowCompat.getInsetsController(window,view).apply {
   isAppearanceLightStatusBars=flashing || !dark
   isAppearanceLightNavigationBars=flashing || !dark
  }
 }
 if(edge)EdgeLight(settings.edgeWidth,lightColor)
 // Outside Scaffold/insets so the actual display is illuminated, not only the preview rectangle.
 AndroidView(factory={host.view},modifier=Modifier.fillMaxSize().zIndex(if(flashing)2f else -1f).testTag("screen-flash-overlay"))
}

@Composable fun EdgeLight(widthDp: Float, color: Color) {
 val view=LocalView.current
 val hardwareRadius=if(Build.VERSION.SDK_INT>=31)view.rootWindowInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius?.toFloat() ?: 0f else 0f
 Canvas(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).testTag("selfie-edge-light")) {
  val stroke=widthDp.dp.toPx().coerceAtMost(size.minDimension/5)
  drawRoundRect(color,topLeft=Offset(stroke/2,stroke/2),size=Size((size.width-stroke).coerceAtLeast(0f),(size.height-stroke).coerceAtLeast(0f)),cornerRadius=CornerRadius(maxOf(24.dp.toPx(),hardwareRadius)),style=Stroke(stroke))
 }
}
