package com.instantroom.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import com.instantroom.data.Settings

@Composable fun SettingsScreen(s: Settings, set: (Settings)->Unit) {
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
  SmallLabel("MAKE IT YOURS");Text("The quiet details.",style=MaterialTheme.typography.headlineLarge)
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
  Text("Instant Room · 0.1.0\nOriginal camera design. Private by default. All photographs stay on this device until you save or share them.",style=MaterialTheme.typography.bodySmall)
 }
}
@Composable private fun SettingToggle(label: String, checked: Boolean, change: (Boolean)->Unit) {Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text(label,Modifier.weight(1f));Switch(checked=checked,onCheckedChange=change)}}
