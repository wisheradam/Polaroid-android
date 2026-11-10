package com.instantroom.core

enum class CameraColor(val label: String) {
 BLACK("Black"), IVORY("Ivory"), OLIVE("Dark Olive"), BRASS("Brown / Brass"), WHITE("White"),
 PINK("Pink"), BLUE("Baby Blue"), MINT("Mint"), LAVENDER("Lavender"), RED("Red Accent"), SPECTRUM("Cream / Spectrum")
}
enum class CameraStyle(val label: String) {
 OLD_FASION("Old Fasion"), INSTA_STYLE("Insta Style"), ONE_AND_ONLY("One and Only");
 val colors get()=when(this) {
  OLD_FASION->listOf(CameraColor.BLACK,CameraColor.IVORY,CameraColor.OLIVE,CameraColor.BRASS)
  INSTA_STYLE->listOf(CameraColor.WHITE,CameraColor.PINK,CameraColor.BLUE,CameraColor.MINT,CameraColor.LAVENDER)
  ONE_AND_ONLY->listOf(CameraColor.WHITE,CameraColor.BLACK,CameraColor.RED,CameraColor.SPECTRUM)
 }
 val defaultColor get()=colors.first()
 fun colorOrDefault(color: CameraColor)=if(color in colors)color else defaultColor
}

object FlashMotion { const val durationMs=300; fun toggleAllowed(available: Boolean,busy: Boolean)=available && !busy }
