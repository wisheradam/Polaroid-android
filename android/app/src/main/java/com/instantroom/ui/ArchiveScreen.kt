package com.instantroom.ui

import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.instantroom.core.*
import com.instantroom.data.PhotoEntity
import com.instantroom.data.geometry

@Composable fun ArchiveScreen(photos: List<PhotoEntity>, open: (String)->Unit, retry: ()->Unit, share: (PhotoEntity,ShareTarget)->Unit = {_,_->}) {
 val locale=LocalConfiguration.current.locales[0]
 Column(Modifier.fillMaxSize().padding(horizontal=20.dp)) {
  Spacer(Modifier.height(20.dp));SmallLabel("YOUR PERMANENT COLLECTION")
  Text("Little pieces of time.",style=MaterialTheme.typography.headlineMedium,modifier=Modifier.padding(vertical=12.dp))
  if(photos.isEmpty())Text("Your first photograph is waiting to happen.",Modifier.padding(vertical=40.dp))
  LazyVerticalGrid(columns=GridCells.Adaptive(140.dp),horizontalArrangement=Arrangement.spacedBy(18.dp),verticalArrangement=Arrangement.spacedBy(22.dp),contentPadding=PaddingValues(vertical=12.dp)) {
   items(photos,key={it.id}) { p->
    Column(Modifier.clickable{open(p.id)}) {
     FilmPrint(p,Modifier.fillMaxWidth().shadow(5.dp),maxSide=420)
     Text(SimpleDateFormat("MMM d · HH:mm",locale).format(Date(p.capturedAt)),style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(top=8.dp))
     Text(if(p.processing)"Processing interrupted" else if(p.progress>=1f)p.geometry.label else "Shake to develop",style=MaterialTheme.typography.bodySmall,color=Amber)
     if(p.boothSessionId.isNotEmpty())Text(if(p.boothIndex>=0)"Photo Booth · source ${p.boothIndex+1}/4" else "Photo Booth · ${p.geometry.label}",style=MaterialTheme.typography.labelSmall)
     if(p.processing)TextButton(onClick=retry){Text("Retry processing")}
     else if(p.progress>=1f)ShareActions{share(p,it)}
    }
   }
  }
 }
}
