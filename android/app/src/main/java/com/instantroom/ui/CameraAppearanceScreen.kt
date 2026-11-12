package com.instantroom.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.instantroom.data.Settings
import com.instantroom.core.CameraAppearance

@Composable fun CameraAppearanceScreen(settings: Settings, back: ()->Unit, set: (Settings)->Unit) = CameraAppearanceScreen(settings,back,set,null,{})

@Composable fun CameraAppearanceScreen(settings: Settings, back: ()->Unit, set: (Settings)->Unit,pending: CameraAppearance?,cancelPending: ()->Unit) {
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
  TextButton(onClick=back){Text("Back to Settings")}
  Text("Camera appearance",style=MaterialTheme.typography.headlineLarge)
  if(pending!=null) {
   Text("Appearance queued. Finish capturing or close the film compartment to apply it.")
   TextButton(onClick=cancelPending){Text("Cancel appearance change")}
  }
  CameraStyleSettings(if(pending==null)settings else settings.copy(cameraStyle=pending.style,cameraColor=pending.color),set)
  Text("Finish affects the camera body only. Film format, processing and photo colors stay independent.",style=MaterialTheme.typography.bodyMedium)
 }
}
