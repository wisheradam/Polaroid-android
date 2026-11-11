package com.instantroom.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.sp
import com.instantroom.R

@OptIn(ExperimentalTextApi::class)
private val InterFamily=FontFamily(
 Font(R.font.inter_variable,FontWeight.Normal,variationSettings=FontVariation.Settings(FontVariation.weight(400))),
 Font(R.font.inter_variable,FontWeight.Medium,variationSettings=FontVariation.Settings(FontVariation.weight(500))),
 Font(R.font.inter_variable,FontWeight.Bold,variationSettings=FontVariation.Settings(FontVariation.weight(700)))
)
private val DefaultTypography=Typography()
val InstantTypography=Typography(
 displayLarge=DefaultTypography.displayLarge.copy(fontFamily=InterFamily),
 displayMedium=DefaultTypography.displayMedium.copy(fontFamily=InterFamily),
 displaySmall=DefaultTypography.displaySmall.copy(fontFamily=InterFamily),
 headlineLarge=DefaultTypography.headlineLarge.copy(fontFamily=InterFamily,fontSize=24.sp,lineHeight=30.sp),
 headlineMedium=DefaultTypography.headlineMedium.copy(fontFamily=InterFamily,fontSize=24.sp,lineHeight=30.sp),
 headlineSmall=DefaultTypography.headlineSmall.copy(fontFamily=InterFamily),
 titleLarge=DefaultTypography.titleLarge.copy(fontFamily=InterFamily),
 titleMedium=DefaultTypography.titleMedium.copy(fontFamily=InterFamily),
 titleSmall=DefaultTypography.titleSmall.copy(fontFamily=InterFamily),
 bodyLarge=DefaultTypography.bodyLarge.copy(fontFamily=InterFamily),
 bodyMedium=DefaultTypography.bodyMedium.copy(fontFamily=InterFamily),
 bodySmall=DefaultTypography.bodySmall.copy(fontFamily=InterFamily),
 labelLarge=DefaultTypography.labelLarge.copy(fontFamily=InterFamily),
 labelMedium=DefaultTypography.labelMedium.copy(fontFamily=InterFamily),
 labelSmall=DefaultTypography.labelSmall.copy(fontFamily=InterFamily,fontSize=12.sp)
)
