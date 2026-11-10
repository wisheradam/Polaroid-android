package com.instantroom.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.instantroom.core.*
import com.instantroom.data.*

@Composable fun DevelopmentScreen(vm: RoomViewModel, photo: PhotoEntity, settings: Settings, back: ()->Unit, save: ()->Unit, attach: ()->Unit) {
 val owner=LocalLifecycleOwner.current
 val currentProgress by rememberUpdatedState(photo.progress)
 DisposableEffect(owner,photo.id,photo.processing,photo.progress>=1f,settings.threshold) {
  vm.graph.detector.threshold=settings.threshold
  fun start() {if(!photo.processing && currentProgress<1f)vm.graph.motion.start {strength->vm.develop(photo.id,strength)}}
  val obs=LifecycleEventObserver{_,e->if(e==Lifecycle.Event.ON_RESUME)start() else if(e==Lifecycle.Event.ON_PAUSE)vm.graph.motion.stop()}
  owner.lifecycle.addObserver(obs)
  if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))start()
  onDispose{owner.lifecycle.removeObserver(obs);vm.graph.motion.stop()}
 }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(20.dp)) {
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick=back){Text("Archive")};SmallLabel(if(photo.progress>=1f)"A MOMENT, KEPT." else "LET IT COME TO LIFE")}
  Spacer(Modifier.height(4.dp))
  FilmPrint(photo,Modifier.widthIn(max=360.dp).fillMaxWidth(.88f).shadow(18.dp).rotate(-2f))
  Text(when{photo.processing->"This exposure is safely archived. Retry processing to continue.";photo.progress>=1f->"Yours to keep.";else->"Shake gently. Watch it appear."},style=MaterialTheme.typography.titleLarge)
  if(photo.processing)Button(onClick={vm.recover()}){Text("Retry processing")}
  else if(photo.progress<1f) {
   Text("Stronger shakes bring the colors out faster. When you rest, the photograph rests too. You can return to it anytime.",style=MaterialTheme.typography.bodyMedium,color=Cream.copy(alpha=.7f))
   if(!vm.graph.motion.available)Text("No motion sensor is available. Enable manual development in Settings → Accessibility.",color=Amber)
   if(settings.accessible)OutlinedButton(onClick={vm.accessibleDevelop(photo.id)}){Text("Develop one step · accessibility")}
   Text("Physical shaking is a playful simulation. Real instant film should not be shaken.",style=MaterialTheme.typography.labelSmall,color=Cream.copy(alpha=.45f))
  } else {
   Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Button(onClick=save){Text("Save to gallery")};ShareActions{vm.share(photo,it)}}
   TextButton(onClick=attach){Text("Place on corkboard →")}
  }
 }
}
