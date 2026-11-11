package com.instantroom.core

/** Logical dp, after system/cutout insets. Decoration scales; hit targets never do. */
data class AnalogLayout(val width: Float, val height: Float, val upperHeight: Float, val lens: Float) {
 val lowerHeight get() = height - upperHeight
 val aperture get() = lens * .752f
 val lensLeft get() = (width - lens) / 2f
 val lensTop get() = upperHeight * .48f - lens / 2f
 companion object {
  fun fit(width: Float, availableHeight: Float, fontScale: Float = 1f): AnalogLayout {
   require(width.isFinite() && width > 0 && availableHeight.isFinite() && availableHeight > 0)
   require(fontScale.isFinite() && fontScale > 0)
   val height = maxOf(availableHeight, if (fontScale > 1.4f) 1100f else 620f)
   val upper = (height * .625f).coerceIn(360f, 520f)
   return AnalogLayout(width, height, upper, minOf(width * .68f, upper * .56f))
  }
 }
}
