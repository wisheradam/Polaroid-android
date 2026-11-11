package com.instantroom.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.instantroom.data.Settings

@Composable fun CameraAppearanceScreen(settings: Settings, back: ()->Unit, set: (Settings)->Unit) {
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
  TextButton(onClick=back){Text("Back to Settings")}
  Text("Camera appearance",style=MaterialTheme.typography.headlineLarge)
  CameraStyleSettings(settings,set)
  Text("Finish affects the camera body only. Film format, processing and photo colors stay independent.",style=MaterialTheme.typography.bodyMedium)
 }
}
