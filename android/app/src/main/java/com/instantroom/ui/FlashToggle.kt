package com.instantroom.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.instantroom.core.FlashMotion

/** One persistent bolt. Only its color, halo and strike-through are animated. */
@Composable fun FlashToggle(on: Boolean,available: Boolean,enabled: Boolean,iconResource: Int?=null,toggle: (Boolean)->Unit) {
 val progress by animateFloatAsState(if(on)1f else 0f,tween(FlashMotion.durationMs,easing=FastOutSlowInEasing),label="Flash illumination")
 val ink=MaterialTheme.colorScheme.onSurface
 val surface=MaterialTheme.colorScheme.surface
 val green=if(surface.luminance()<.3f)Color(0xFF92EAB3)else Color(0xFF155D38)
 val active=FlashMotion.toggleAllowed(available,!enabled)
 val shape=RoundedCornerShape(14.dp)
 Box(Modifier.size(52.dp).testTag("flash-toggle").background(surface,shape).border(1.dp,lerp(ink.copy(alpha=.3f),green,progress),shape)
  .toggleable(value=on,enabled=active,role=Role.Switch,onValueChange=toggle)
  .semantics{contentDescription="Rear camera flash";stateDescription=if(!available)"Flash unavailable" else if(on)"Flash on" else "Flash off"},contentAlignment=Alignment.Center) {
  Canvas(Modifier.fillMaxSize().testTag("flash-bolt")) {
   val p=Path().apply{moveTo(size.width*.56f,size.height*.2f);lineTo(size.width*.28f,size.height*.54f);lineTo(size.width*.46f,size.height*.54f);lineTo(size.width*.4f,size.height*.8f);lineTo(size.width*.73f,size.height*.43f);lineTo(size.width*.54f,size.height*.43f);close()}
   if(progress>0f)drawCircle(Brush.radialGradient(listOf(green.copy(alpha=.28f*progress),green.copy(alpha=.08f*progress),Color.Transparent)),size.minDimension*.62f)
   // Alpha never approaches zero, including during rapid reversals.
   if(iconResource==null)drawPath(p,lerp(ink,green,progress),alpha=if(available).8f+.2f*progress else .38f)
  }
  if(iconResource!=null)Icon(painterResource(iconResource),null,tint=lerp(ink,green,progress).copy(alpha=if(available).8f+.2f*progress else .38f),modifier=Modifier.size(24.dp))
  Canvas(Modifier.fillMaxSize()) {
   if(progress<1f)drawLine(ink,Offset(size.width*.25f,size.height*.25f),Offset(size.width*.76f,size.height*.76f),2.dp.toPx(),StrokeCap.Round,alpha=(1-progress)*(if(available)1f else .38f))
  }
 }
}
