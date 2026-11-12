package com.instantroom.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.instantroom.R

@Composable fun BrandLogo(modifier: Modifier=Modifier,plate: Boolean=true) {
 // A small warm paper plate keeps the supplied blue/teal artwork legible in both themes.
 Box(modifier.widthIn(max=230.dp).background(if(plate)Color(0xFFF7F2E2)else Color.Transparent,RoundedCornerShape(10.dp)).padding(horizontal=if(plate)12.dp else 0.dp,vertical=if(plate)10.dp else 0.dp)) {
  Image(painterResource(R.drawable.check2go_logo),"Check2GO",Modifier.fillMaxWidth().aspectRatio(1200f/209f).testTag("camera-brand-logo"),contentScale=ContentScale.Fit)
 }
}
