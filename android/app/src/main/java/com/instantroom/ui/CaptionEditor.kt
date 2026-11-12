package com.instantroom.ui

import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import com.instantroom.core.*
import com.instantroom.data.PhotoEntity
import com.instantroom.data.geometry
import com.instantroom.imaging.CaptionRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

typealias CaptionSave = (String,CaptionFont,(Boolean)->Unit)->Unit

@Composable fun PhotoCaptionAction(photo: PhotoEntity, defaultFont: CaptionFont=CaptionFont.SOL_KOL, save: CaptionSave) {
 var open by rememberSaveable(photo.id){mutableStateOf(false)}
 TextButton(onClick={open=true}){Text(if(photo.caption.isEmpty())"Write on photo" else "Edit caption")}
 if(open)CaptionEditor(photo,{open=false},save,defaultFont)
}

@Composable fun CaptionEditor(photo: PhotoEntity, dismiss: ()->Unit, save: CaptionSave, defaultFont: CaptionFont=CaptionFont.SOL_KOL) {
 var text by rememberSaveable(photo.id){mutableStateOf(photo.caption)}
 var style by rememberSaveable(photo.id){mutableStateOf(if(photo.caption.isEmpty())defaultFont.name else photo.captionFont)}
 var saving by remember{mutableStateOf(false)}
 var failure by remember{mutableStateOf(false)}
 val font=runCatching{CaptionFont.valueOf(style)}.getOrDefault(CaptionFont.SOL_KOL)
 val validation=runCatching{CaptionRules.normalize(text)}
 val normalized=validation.getOrNull()
 val context=LocalContext.current
 val base=photo.baseProcessed.ifEmpty{photo.processed}
 val bitmap by produceState<android.graphics.Bitmap?>(null,base,text,style) {
  value=if(normalized==null)null else withContext(Dispatchers.Default) {
   BitmapFactory.decodeFile(base,BitmapFactory.Options().apply{inSampleSize=2;inMutable=true})?.also {
    CaptionRenderer(context).draw(Canvas(it),it.width,it.height,photo.geometry,normalized,font)
   }
  }
 }
 fun submit(value: String) {saving=true;failure=false;save(value,font){ok->saving=false;if(ok)dismiss() else failure=true}}
 @Composable fun Fields(modifier: Modifier=Modifier) {
  Column(modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
   bitmap?.let{Image(it.asImageBitmap(),"Caption preview",Modifier.width(minOf(180.dp,220.dp*photo.geometry.aspect)).height(minOf(180.dp/photo.geometry.aspect,220.dp)))}
   OutlinedTextField(value=text,onValueChange={text=it},enabled=!saving,label={Text("Your handwritten note")},
    supportingText={Text(validation.exceptionOrNull()?.message ?: "Up to 80 characters · two short lines")},isError=validation.isFailure,maxLines=2,modifier=Modifier.fillMaxWidth())
   Text("Handwriting",style=MaterialTheme.typography.labelLarge)
   CaptionFont.entries.forEach {option->FilterChip(selected=font==option,onClick={style=option.name},enabled=!saving,label={Text(option.label)})}
   if(failure)Text("Could not save. Your existing print is unchanged; try again.",color=MaterialTheme.colorScheme.error)
   if(saving)Text("Saving your note…")
  }
 }
 @Composable fun Confirm(){TextButton(onClick={submit(normalized.orEmpty())},enabled=!saving && normalized!=null){Text("Save caption")}}
 @Composable fun Dismiss(){FlowRow {
  if(photo.caption.isNotEmpty())TextButton(onClick={submit("")},enabled=!saving){Text("Remove caption")}
  TextButton(onClick=dismiss,enabled=!saving){Text("Cancel")}
 }}
 if(LocalDensity.current.fontScale>1.4f) {
  // A bounded dialog keeps native IME space and actions outside the scrolling fields.
  // Avoid AlertDialog's intrinsic-width measurement at very large text sizes.
  Dialog(onDismissRequest={if(!saving)dismiss()},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
   Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp),contentAlignment=Alignment.Center) {
    Surface(modifier=Modifier.fillMaxWidth().fillMaxHeight(.92f),shape=MaterialTheme.shapes.extraLarge,color=MaterialTheme.colorScheme.surface) {
     Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
      Text("Write on your print",style=MaterialTheme.typography.headlineSmall)
      Fields(Modifier.weight(1f).fillMaxWidth())
      FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){Confirm();Dismiss()}
     }
    }
   }
  }
 }else AlertDialog(onDismissRequest={if(!saving)dismiss()},title={Text("Write on your print")},text={Fields()},confirmButton={Confirm()},dismissButton={Dismiss()})
}
