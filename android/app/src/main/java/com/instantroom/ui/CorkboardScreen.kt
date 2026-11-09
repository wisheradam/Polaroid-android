package com.instantroom.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instantroom.data.*
import kotlin.random.Random

@Composable fun CorkboardScreen(vm: RoomViewModel, photos: List<PhotoEntity>) {
 val placements by vm.placements.collectAsStateWithLifecycle()
 var selected by rememberSaveable{mutableStateOf<String?>(null)}
 var placingPin by rememberSaveable{mutableStateOf(false)}
 var color by rememberSaveable{mutableLongStateOf(0xFFB7543DL)}
 var zoom by rememberSaveable{mutableFloatStateOf(.65f)}
 var panX by rememberSaveable{mutableFloatStateOf(-30f)}
 var panY by rememberSaveable{mutableFloatStateOf(-30f)}
 val density=LocalDensity.current.density
 val currentZoom by rememberUpdatedState(zoom)
 val selectedPlacement=placements.find{it.photoId==selected}
 Column(Modifier.fillMaxSize()) {
  Column(Modifier.padding(horizontal=20.dp,vertical=12.dp)) {
   SmallLabel("THE CORK WALL")
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Stay a little longer.",style=MaterialTheme.typography.titleLarge);TextButton(onClick={zoom=.65f;panX=-30f;panY=-30f}){Text("Recenter")}}
   Text(if(selectedPlacement==null)"Drag a print from the tray. Pin it where it belongs." else if(placingPin)"Tap the photograph where you want the pushpin." else if(selectedPlacement.pinned)"Pinned in place. Remove its pin to move it." else "Drag or use two fingers to rotate this print.",style=MaterialTheme.typography.bodySmall)
   if(selectedPlacement!=null) {
    Row(horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically) {
     TextButton(onClick={if(selectedPlacement.pinned)vm.unpin(selectedPlacement.photoId) else placingPin=!placingPin}){Text(if(selectedPlacement.pinned)"Remove pin" else if(placingPin)"Cancel pin" else "Choose pin")}
     TextButton(onClick={vm.detach(selectedPlacement.photoId);selected=null;placingPin=false}){Text("Take off board")}
     if(!selectedPlacement.pinned)TextButton(onClick={vm.move(selectedPlacement.photoId,selectedPlacement.x,selectedPlacement.y,selectedPlacement.rotation+15)}){Text("Rotate")}
    }
    if(placingPin)Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
     listOf(0xFFB7543DL,0xFFD9AA42L,0xFF4B8798L,0xFFEEE4C9L).forEach {c->Box(Modifier.size(30.dp).background(Color(c),CircleShape).border(if(color==c)3.dp else 0.dp,Cream,CircleShape).clickable{color=c}.semantics{contentDescription="Choose ${when(c){0xFFB7543DL->"red";0xFFD9AA42L->"gold";0xFF4B8798L->"blue";else->"ivory"}} pushpin"})}
    }
   }
  }
  Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().background(Color(0xFFAF7D49)).pointerInput(Unit){detectTransformGestures {_,pan,scale,_->zoom=(zoom*scale).coerceIn(.25f,2.5f);panX=(panX+pan.x).coerceIn(-1800f*density*zoom,200f);panY=(panY+pan.y).coerceIn(-1400f*density*zoom,200f)}}) {
   Box(Modifier.wrapContentSize(Alignment.TopStart,unbounded=true).requiredSize(2000.dp,1600.dp).graphicsLayer{translationX=panX;translationY=panY;scaleX=zoom;scaleY=zoom;transformOrigin=TransformOrigin(0f,0f)}) {
    Canvas(Modifier.fillMaxSize()) {
     drawRect(Color(0xFFB28655));val random=Random(54)
     repeat(20000){val x=random.nextFloat()*size.width;val y=random.nextFloat()*size.height;drawCircle(if(random.nextBoolean())Color(0x22442B12)else Color(0x27F5D5A8),random.nextFloat()*2.4f+.6f,Offset(x,y))}
    }
    placements.forEach {p->
     val photo=photos.find{it.id==p.photoId} ?: return@forEach
     key(p.photoId) {
      BoardPrint(photo,p,selected==p.photoId,placingPin && selected==p.photoId,color,{currentZoom},
       select={selected=p.photoId;placingPin=false},move={x,y,r->vm.move(p.photoId,x,y,r)},
       pin={x,y->vm.pin(p.photoId,color,x,y);placingPin=false})
     }
    }
   }
   SmallLabel("PINCH TO ZOOM · DRAG TO EXPLORE",Modifier.align(Alignment.BottomCenter).background(Ink.copy(alpha=.7f)).padding(8.dp))
  }
  val available=photos.filter{it.progress>=1f && !it.processing && placements.none{p->p.photoId==it.id}}
  if(available.isNotEmpty()) {
   SmallLabel("PRINT TRAY · DRAG UP OR TAP TO PLACE",Modifier.padding(start=20.dp,top=10.dp))
   LazyRow(contentPadding=PaddingValues(16.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
    items(available,key={it.id}){p->
     var drag by remember{mutableStateOf(Offset.Zero)}
     Box(Modifier.width(72.dp).offset {IntOffset(drag.x.toInt(),drag.y.toInt())}.pointerInput(p.id){detectDragGestures(onDragEnd={
      if(drag.y< -25f){vm.attach(p.id){vm.move(p.id,((-panX+140*density)/(zoom*density)).coerceIn(0f,1800f),((-panY+100*density)/(zoom*density)).coerceIn(0f,1400f),-4f);selected=p.id}}
      drag=Offset.Zero
     },onDragCancel={drag=Offset.Zero},onDrag={change,amount->change.consume();drag+=amount})}.clickable{vm.attach(p.id){vm.move(p.id,(-panX/(zoom*density)+90).coerceIn(0f,1800f),(-panY/(zoom*density)+80).coerceIn(0f,1400f),-4f);selected=p.id}}) { FilmPrint(p,Modifier.fillMaxWidth().shadow(3.dp),maxSide=200) }
    }
   }
  } else Text("Develop a photograph to add it here.",Modifier.padding(16.dp),style=MaterialTheme.typography.bodySmall)
 }
}
@Composable private fun BoardPrint(photo: PhotoEntity,p: PlacementEntity,selected: Boolean,pinMode: Boolean,color: Long,zoom: ()->Float,select: ()->Unit,move: (Float,Float,Float)->Unit,pin: (Float,Float)->Unit) {
 val density=LocalDensity.current.density
 var x by remember(p.photoId){mutableFloatStateOf(p.x)}
 var y by remember(p.photoId){mutableFloatStateOf(p.y)}
 var rotation by remember(p.photoId){mutableFloatStateOf(p.rotation)}
 LaunchedEffect(p.x,p.y,p.rotation){x=p.x;y=p.y;rotation=p.rotation}
 val latestPin by rememberUpdatedState(pinMode)
 val latestSelect by rememberUpdatedState(select)
 val width=180f*com.instantroom.core.FilmFormat.valueOf(photo.format).width/88.47f
 Box(Modifier.offset {IntOffset((x*density).toInt(),(y*density).toInt())}.width(width.dp).zIndex(p.z.toFloat()).graphicsLayer{rotationZ=rotation}.shadow(8.dp)
  .pointerInput(p.pinned,p.photoId,pinMode) {
   if(!p.pinned && !pinMode) {
    awaitEachGesture {
     awaitFirstDown(requireUnconsumed=false)
     var changed=false
     do {
      val event=awaitPointerEvent()
      if(event.changes.any{it.position!=it.previousPosition}) {
       val pan=event.calculatePan();val angle=event.calculateRotation()
       if(!changed){latestSelect();changed=true}
       x=(x+pan.x/(density*zoom())).coerceIn(0f,1800f);y=(y+pan.y/(density*zoom())).coerceIn(0f,1400f);rotation=(rotation+angle).coerceIn(-180f,180f)
       event.changes.forEach{it.consume()}
      }
     } while(event.changes.any{it.pressed})
     if(changed)move(x,y,rotation) else latestSelect()
    }
   } else detectTapGestures {offset->if(latestPin)pin(offset.x/size.width,offset.y/size.height)else latestSelect()}
  }.semantics {contentDescription=if(p.pinned)"Pinned photograph. Remove pin to move."else "Movable photograph"}) {
  FilmPrint(photo,Modifier.fillMaxWidth().then(if(selected)Modifier.border(2.dp,Amber) else Modifier),maxSide=600)
  if(p.pinned) {
   val height=width/com.instantroom.core.FilmFormat.valueOf(photo.format).aspect
   Box(Modifier.offset((width*p.pinX-7).dp,(height*p.pinY-7).dp).size(14.dp).shadow(5.dp,CircleShape).background(Brush.radialGradient(listOf(Color.White,Color(p.pinColor),Color.Black)),CircleShape))
  }
 }
}
