package com.instantroom.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.instantroom.core.ShareTarget

@Composable fun ShareActions(share: (ShareTarget)->Unit) {
 var open by remember{mutableStateOf(false)}
 Box {
  TextButton(onClick={open=true}){Text("Share")}
  DropdownMenu(expanded=open,onDismissRequest={open=false}) {
   listOf(ShareTarget.INSTAGRAM,ShareTarget.FACEBOOK,ShareTarget.SYSTEM).forEach {target->
    DropdownMenuItem(text={Text(target.label)},onClick={open=false;share(target)})
   }
  }
 }
}
