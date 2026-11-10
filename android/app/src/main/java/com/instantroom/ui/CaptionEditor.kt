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
 AlertDialog(onDismissRequest={if(!saving)dismiss()},title={Text("Write on your print")},text={
  Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
   bitmap?.let{Image(it.asImageBitmap(),"Caption preview",Modifier.widthIn(max=180.dp).heightIn(max=220.dp).aspectRatio(photo.geometry.aspect,matchHeightConstraintsFirst=true))}
   OutlinedTextField(value=text,onValueChange={text=it},enabled=!saving,label={Text("Your handwritten note")},
    supportingText={Text(validation.exceptionOrNull()?.message ?: "Up to 80 characters · two short lines")},isError=validation.isFailure,maxLines=2,modifier=Modifier.fillMaxWidth())
   Text("Handwriting",style=MaterialTheme.typography.labelLarge)
   // Vertical options remain reachable on small screens and at larger font scales.
   CaptionFont.entries.forEach {option->
    FilterChip(selected=font==option,onClick={style=option.name},enabled=!saving,label={Text(option.label)})
   }
   if(failure)Text("Could not save. Your existing print is unchanged; try again.",color=MaterialTheme.colorScheme.error)
   if(saving)Text("Saving your note…")
  }
 },confirmButton={TextButton(onClick={submit(normalized.orEmpty())},enabled=!saving && normalized!=null){Text("Save caption")}},
 dismissButton={Row {
  if(photo.caption.isNotEmpty())TextButton(onClick={submit("")},enabled=!saving){Text("Remove caption")}
  TextButton(onClick=dismiss,enabled=!saving){Text("Cancel")}
 }})
}
