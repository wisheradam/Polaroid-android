package com.instantroom.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.instantroom.core.*
import com.instantroom.data.Settings

/** Reuses the existing camera coordinator choices; sheets own no capture state. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CameraTimerSheet(settings: Settings,busy: Boolean,dismiss: ()->Unit,select: (SelfTimer)->Unit) {
 if(!busy)ModalBottomSheet(onDismissRequest=dismiss,containerColor=MaterialTheme.colorScheme.surface) {
  Column(Modifier.fillMaxWidth().padding(horizontal=24.dp).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text("Self-timer",style=MaterialTheme.typography.headlineSmall)
   SelfTimer.entries.forEach{value->Row(Modifier.fillMaxWidth().heightIn(min=56.dp).clickable{select(value);dismiss()}.semantics{role=Role.RadioButton;selected=settings.timer==value},verticalAlignment=Alignment.CenterVertically){RadioButton(selected=settings.timer==value,onClick=null);Spacer(Modifier.width(12.dp));Text(value.label)}}
  }
 }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CameraSelfieSheet(settings: Settings,busy: Boolean,dismiss: ()->Unit,select: (SelfieLightMode)->Unit) {
 if(!busy)ModalBottomSheet(onDismissRequest=dismiss) {
  Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Text("Selfie lighting",style=MaterialTheme.typography.headlineSmall)
   SelfieLightMode.entries.forEach{mode->Row(Modifier.fillMaxWidth().heightIn(min=56.dp).clickable{select(mode);dismiss()},verticalAlignment=Alignment.CenterVertically){RadioButton(selected=settings.selfieLight==mode,onClick=null);Text(mode.label,Modifier.padding(start=12.dp))}}
  }
 }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CameraBoothSheet(settings: Settings,busy: Boolean,dismiss: ()->Unit,selectMode: (Boolean)->Unit,layout: (BoothLayout)->Unit,light: (SelfieLightMode)->Unit) {
 if(!busy)ModalBottomSheet(onDismissRequest=dismiss) {
  Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
   Text("Photo Booth",style=MaterialTheme.typography.headlineSmall)
   Text("Four photographs · five seconds apart · four exposures")
   BoothLayoutControl(settings.boothLayout,true,layout)
   if(settings.front)SelfieLightControl(settings.selfieLight,true,light)
   Button(onClick={selectMode(true);dismiss()},modifier=Modifier.fillMaxWidth()){Text("Use Photo Booth")}
   OutlinedButton(onClick={selectMode(false);dismiss()},modifier=Modifier.fillMaxWidth()){Text("Single photo")}
  }
 }
}
