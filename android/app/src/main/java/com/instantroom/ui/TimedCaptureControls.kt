package com.instantroom.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.instantroom.R
import com.instantroom.core.*
import com.instantroom.data.PhotoEntity
import com.instantroom.imaging.BoothRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun TimerControl(timer: SelfTimer, enabled: Boolean, modifier: Modifier=Modifier, select: (SelfTimer)->Unit) {
 var expanded by remember{mutableStateOf(false)}
 Box(modifier) {
  OutlinedButton(onClick={expanded=true},enabled=enabled,contentPadding=PaddingValues(horizontal=10.dp)) {
   Icon(painterResource(R.drawable.ic_timer),contentDescription="Self-timer",modifier=Modifier.size(20.dp))
   Text(" ${if(timer==SelfTimer.OFF)"Off" else "${timer.seconds}s"}")
  }
  DropdownMenu(expanded && enabled,onDismissRequest={expanded=false}) {
   SelfTimer.entries.forEach{option->DropdownMenuItem(text={Text(option.label)},onClick={select(option);expanded=false})}
  }
 }
}
@Composable fun CountdownOverlay(seconds: Int, shot: Int, booth: Boolean) {
 if(seconds<=0)return
 Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.background(Color.Black.copy(alpha=.65f),CircleShape).padding(18.dp).semantics{liveRegion=LiveRegionMode.Polite}) {
  if(booth)Text("PHOTO $shot OF 4",style=MaterialTheme.typography.labelLarge,color=Color.White)
  Text(seconds.toString(),style=if(seconds<=3)MaterialTheme.typography.displayLarge else MaterialTheme.typography.displayMedium,color=if(seconds<=3)Color(0xFFFFC77A)else Color.White)
 }
}
@Composable fun BoothLayoutControl(layout: BoothLayout, enabled: Boolean, select: (BoothLayout)->Unit) {
 FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
  BoothLayout.entries.forEach{option->FilterChip(selected=layout==option,enabled=enabled,onClick={select(option)},label={Text(option.label)})}
 }
}
@Composable fun BoothThumbnails(photos: List<PhotoEntity>) {
 val context=LocalContext.current
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
  photos.forEach{photo->
   val bitmap by produceState<Bitmap?>(null,photo.original) {
    value=withContext(Dispatchers.IO){runCatching{BoothRenderer(context).upright(photo.original,240)}.getOrNull()}
   }
   bitmap?.let{Image(it.asImageBitmap(),"Captured photo ${photo.boothIndex+1} of 4",Modifier.size(48.dp))}
  }
 }
}
