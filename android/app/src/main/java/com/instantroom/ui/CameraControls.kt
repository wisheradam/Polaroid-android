package com.instantroom.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import com.instantroom.R
import com.instantroom.core.SelfieLightMode

@Composable fun CameraSwitchControl(front: Boolean, enabled: Boolean, switch: ()->Unit) {
 IconButton(onClick=switch,enabled=enabled) {
  Icon(painterResource(R.drawable.ic_switch_camera),contentDescription=if(front)"Switch to rear camera" else "Switch to front camera",tint=MaterialTheme.colorScheme.primary.copy(alpha=if(enabled)1f else .38f))
 }
}
@Composable fun SelfieLightControl(mode: SelfieLightMode, enabled: Boolean, select: (SelfieLightMode)->Unit) {
 var expanded by remember{mutableStateOf(false)}
 androidx.compose.foundation.layout.Box {
  TextButton(onClick={expanded=true},enabled=enabled){Text("Selfie light: ${mode.label} ▾")}
  DropdownMenu(expanded=expanded && enabled,onDismissRequest={expanded=false}) {
   SelfieLightMode.entries.forEach {item->DropdownMenuItem(text={Text(item.label)},onClick={expanded=false;select(item)})}
  }
 }
}
