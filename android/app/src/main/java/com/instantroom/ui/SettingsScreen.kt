package com.instantroom.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import com.instantroom.data.Settings
import com.instantroom.BuildConfig
import com.instantroom.core.*
import androidx.compose.ui.semantics.*

@Composable fun SettingsScreen(s: Settings, openCameraStyle: (() -> Unit)?=null, set: (Settings)->Unit) {
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
  SmallLabel("INSTANTROOM");Text("Settings",style=MaterialTheme.typography.headlineLarge)
  Panel {
   SmallLabel("APPEARANCE")
   ThemeMode.entries.forEach {mode->
    Row(Modifier.fillMaxWidth().clickable{set(s.copy(theme=mode))},verticalAlignment=Alignment.CenterVertically) {
     RadioButton(selected=s.theme==mode,onClick={set(s.copy(theme=mode))});Text(mode.label)
    }
   }
  }
  if(openCameraStyle==null)CameraStyleSettings(s,set) else Panel { TextButton(onClick=openCameraStyle,modifier=Modifier.fillMaxWidth()){Text("Camera design · ${s.cameraStyle.label}")};Text("Choose the camera style and physical finish.",style=MaterialTheme.typography.bodySmall) }
  Panel {
   SmallLabel("DEFAULT HANDWRITING")
   FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {CaptionFont.entries.forEach {font->FilterChip(selected=s.captionFont==font,onClick={set(s.copy(captionFont=font))},label={Text(font.label)})}}
   Text("Used for new notes. Existing captions keep their selected font.",style=MaterialTheme.typography.bodySmall)
  }
  Panel {
   SmallLabel("SELFIE LIGHTING")
   SelfieLightControl(s.selfieLight,true){set(s.copy(selfieLight=it))}
   Text("Edge width · ${s.edgeWidth.toInt()} dp")
   Slider(value=s.edgeWidth,onValueChange={set(s.copy(edgeWidth=it))},valueRange=8f..32f,modifier=Modifier.semantics{contentDescription="Edge light thickness"})
   Text("Edge brightness")
   Slider(value=s.edgeBrightness,onValueChange={set(s.copy(edgeBrightness=it))},valueRange=.35f..1f,modifier=Modifier.semantics{contentDescription="Edge light brightness"})
   Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
    LightTone.entries.forEach{tone->FilterChip(selected=s.lightTone==tone,onClick={set(s.copy(lightTone=tone))},label={Text(tone.label)})}
   }
   Text("Screen flash lights the full display only during capture. Edge light stays on while framing with the front camera. Output depends on the display and ambient light.",style=MaterialTheme.typography.bodySmall)
  }
  Panel {
   SmallLabel("SOUND & TOUCH")
   SettingToggle("Camera sounds",s.sound,{set(s.copy(sound=it))})
   SettingToggle("Gentle haptics",s.haptics,{set(s.copy(haptics=it))})
   Text("Flash charging · ${s.chargeMs} ms")
   Slider(value=s.chargeMs.toFloat(),onValueChange={set(s.copy(chargeMs=it.toLong()))},valueRange=400f..1200f,steps=7)
  }
  Panel {
   SmallLabel("DEVELOPMENT")
   Text("Shake sensitivity · ${if(s.threshold<6f)"gentle" else if(s.threshold>9f)"firm" else "balanced"}")
   Slider(value=s.threshold,onValueChange={set(s.copy(threshold=it))},valueRange=4.5f..12f)
   Text("Higher thresholds reject more ordinary movement.",style=MaterialTheme.typography.bodySmall)
   SettingToggle("Enable manual development",s.accessible,{set(s.copy(accessible=it))})
   Text("Accessibility: adds an explicit press-to-develop button. Each press advances one small step. Physical shaking remains available.",style=MaterialTheme.typography.bodySmall)
  }
  Panel {
   SmallLabel("CAMERA & PERFORMANCE")
   SettingToggle("Front camera",s.front,{set(s.copy(front=it))})
   SettingToggle("Smaller processed images",s.lowMemory,{set(s.copy(lowMemory=it))})
   SettingToggle("Chemical imperfections",s.imperfections,{set(s.copy(imperfections=it))})
   Text("These preferences apply to newly processed photographs.",style=MaterialTheme.typography.bodySmall)
  }
  Text("Instant Room · ${BuildConfig.VERSION_NAME}\nOriginal camera design. Private by default. All photographs stay on this device until you save or share them.",style=MaterialTheme.typography.bodySmall)
 }
}
@Composable private fun SettingToggle(label: String, checked: Boolean, change: (Boolean)->Unit) {Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text(label,Modifier.weight(1f));Switch(checked=checked,onCheckedChange=change)}}
