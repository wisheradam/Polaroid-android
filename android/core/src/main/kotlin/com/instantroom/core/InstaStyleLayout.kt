package com.instantroom.core

/** Handoff geometry in the content area after actual system insets, never illustrated bars. */
data class InstaStyleLayout(val width: Float,val viewportHeight: Float,val largeText: Boolean) {
 val compact=width<=360f
 val essentialControls=largeText || width<360f
 val lensDiameter=minOf(width-48f,334f)
 val apertureDiameter=lensDiameter*262f/310f
 val lensX=(width-lensDiameter)/2
 val lensY=147f
 val shutterSize=if(compact)88f else 100f
 val shutterX=(width-shutterSize)/2
 val shutterY=lensY+lensDiameter+16f
 val controlsY=shutterY+22f
 val timerX=if(compact)16f else 24f
 val flashX=if(compact)76f else 87f
 val switchX=width-78f
 val slotY=shutterY+119f
 val slotWidth=width-56f
 val slotHeight=70f*slotWidth/337f
 val mouthY=slotY+30f*slotWidth/337f
 val navigationY=maxOf(viewportHeight-66f,slotY+slotHeight+20f)
 val height=if(essentialControls)lensY+lensDiameter+24f else maxOf(viewportHeight,navigationY+66f)
 companion object {
  fun fit(width: Float,height: Float,fontScale: Float=1f): InstaStyleLayout {
   require(width.isFinite() && width>=240 && height.isFinite() && height>0 && fontScale.isFinite() && fontScale>0)
   return InstaStyleLayout(width,height,fontScale>1.4f)
  }
 }
}

object InstaStyleMotion {
 const val pressMs=70
 const val releaseMs=110
 /** Use the sole domain ejection clock. Tall strips retain their true width/height, offscreen too. */
 fun paperOffset(progress: Float,height: Float,reducedMotion: Boolean)=height*((if(reducedMotion && progress<1f).5f else progress.coerceIn(0f,1f))-1f)
}
