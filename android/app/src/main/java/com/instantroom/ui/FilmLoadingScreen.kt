package com.instantroom.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.*
import com.instantroom.core.*
import com.instantroom.data.CartridgeEntity

@Composable fun FilmLoadingScreen(vm: RoomViewModel, cartridge: CartridgeEntity?, close: ()->Unit) {
 var phaseName by rememberSaveable{mutableStateOf(LoadingPhase.CLOSED.name)}
 val phase=LoadingPhase.valueOf(phaseName)
 var formatName by rememberSaveable{mutableStateOf(cartridge?.format ?: FilmFormat.CLASSIC.name)}
 var appearanceName by rememberSaveable{mutableStateOf(cartridge?.appearance ?: Appearance.COLOR.name)}
 var warning by remember{mutableStateOf(false)}
 var committing by remember{mutableStateOf(false)}
 val door by animateFloatAsState(if(phase==LoadingPhase.CLOSED)0f else 58f,tween(600),label="Film compartment door")
 val cartOffset by animateDpAsState(if(phase==LoadingPhase.REMOVED)170.dp else 0.dp,tween(600),label="Cartridge insertion")
 fun advance() {
  if(committing)return
  when(phase) {
   LoadingPhase.CLOSED->{if((cartridge?.remaining ?: 0)>0)warning=true else phaseName=LoadingPhase.OPEN.name}
   LoadingPhase.OPEN->phaseName=LoadingPhase.REMOVED.name
   LoadingPhase.REMOVED->phaseName=LoadingPhase.INSERTED.name
   LoadingPhase.INSERTED->{committing=true;vm.replace(FilmFormat.valueOf(formatName),Appearance.valueOf(appearanceName)){close()}}
  }
 }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick=close,enabled=!committing){Text("Back")};SmallLabel("THE FILM ROOM")}
  Text("A fresh beginning.",style=MaterialTheme.typography.headlineLarge)
  Text("Every cartridge holds exactly ten photographs. The film you load sets the shape and character of every exposure.",color=Cream.copy(alpha=.7f))
  Box(Modifier.fillMaxWidth().height(210.dp).background(androidx.compose.ui.graphics.Color.Black,RoundedCornerShape(20.dp)).clipToBounds(),contentAlignment=Alignment.Center) {
   Panel(Modifier.width(220.dp).offset{IntOffset(0,cartOffset.roundToPx())}.pointerInput(phase){var drag=0f;detectDragGestures(onDragStart={drag=0f},onDragEnd={if(kotlin.math.abs(drag)>50 && phase in listOf(LoadingPhase.OPEN,LoadingPhase.REMOVED))advance()},onDrag={change,amount->change.consume();drag+=amount.y})}) {
    SmallLabel(if(phase==LoadingPhase.REMOVED)"EMPTY BAY" else "INSTANT ROOM / FILM")
    Text(FilmFormat.valueOf(formatName).label,style=MaterialTheme.typography.titleLarge)
    Text("10 EXPOSURES",color=Amber)
    Text(Appearance.valueOf(appearanceName).label)
   }
   Box(Modifier.fillMaxWidth().height(60.dp).align(Alignment.BottomCenter).graphicsLayer{rotationX=door;transformOrigin=androidx.compose.ui.graphics.TransformOrigin(.5f,1f)}.background(Rust,RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){Text("FILM COMPARTMENT",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSecondary)}
  }
  Text(when(phase){LoadingPhase.CLOSED->"Open the hinged film door.";LoadingPhase.OPEN->"Pull the old cartridge out of the bay.";LoadingPhase.REMOVED->"Choose your film, then slide the new cartridge into the bay.";LoadingPhase.INSERTED->"The new cartridge is seated. Close the door to load it."})
  if(phase==LoadingPhase.REMOVED) {
   SmallLabel("FORMAT")
   FilmFormat.entries.forEach { format->
    Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth().clickable{formatName=format.name}) {
     RadioButton(selected=formatName==format.name,onClick={formatName=format.name});Text(format.label);Spacer(Modifier.weight(1f));Text("${format.width.toInt()} × ${format.height.toInt()} mm",style=MaterialTheme.typography.bodySmall)
    }
   }
   SmallLabel("APPEARANCE")
   Appearance.entries.forEach { appearance->FilterChip(selected=appearanceName==appearance.name,onClick={appearanceName=appearance.name},label={Text(appearance.label)}) }
  }
  Button(onClick={advance()},enabled=!committing,modifier=Modifier.fillMaxWidth()) {Text(if(committing)"Loading…" else when(phase){LoadingPhase.CLOSED->"Open film door";LoadingPhase.OPEN->"Remove cartridge ↓";LoadingPhase.REMOVED->"Insert cartridge ↑";LoadingPhase.INSERTED->"Close film door"})}
 }
 if(warning)AlertDialog(onDismissRequest={warning=false},title={Text("Discard unused film?")},text={Text("${cartridge?.remaining} remaining exposures will be lost when you finish replacing this cartridge. Your photographs stay in the archive.")},confirmButton={TextButton(onClick={warning=false;phaseName=LoadingPhase.OPEN.name}){Text("Open film door")}},dismissButton={TextButton(onClick={warning=false}){Text("Keep film")}})
}
