package com.instantroom.core

/** Coordinates after the host has consumed real system insets, per responsive.json. */
data class OldFashionLayout(val width: Float,val viewportHeight: Float,val scale: Float,val essentialControls: Boolean) {
 val flashTop=8f
 val bodyY=flashTop+176f*scale
 val compartmentY=bodyY+250f*scale
 val viewfinderX=(width-145f*scale)/2
 val viewfinderY=bodyY+28f*scale
 val apertureWidth=119f*scale
 val apertureHeight=132f*scale
 val shutterY=bodyY+220f*scale
 val slotY=compartmentY+86f*scale
 val mouthY=compartmentY+137f*scale
 val navigationY=maxOf(viewportHeight-104f,compartmentY+236f*scale)
 val height=if(essentialControls)compartmentY+214f*scale else maxOf(viewportHeight,navigationY+104f)
 companion object {
  fun fit(width: Float,availableHeight: Float,fontScale: Float=1f): OldFashionLayout {
   require(width.isFinite() && width>=240 && availableHeight.isFinite() && availableHeight>0)
   require(fontScale.isFinite() && fontScale>0)
   return OldFashionLayout(width,availableHeight,(width-24f)/369f,width<360f || fontScale>1.4f)
  }
 }
}

data class CameraAppearance(val style: CameraStyle,val color: CameraColor) {
 fun normalized()=copy(color=style.colorOrDefault(color))
}
object AppearanceSwitchPolicy {
 fun canApply(phase: CapturePhase,loading: LoadingPhase,locked: Boolean)=phase==CapturePhase.IDLE && loading==LoadingPhase.CLOSED && !locked
}
object OldFashionMotion {
 const val pressMs=70
 const val releaseMs=110
 fun paperPosition(progress: Float,motionEnabled: Boolean)=if(motionEnabled)progress.coerceIn(0f,1f) else if(progress>=1f)1f else .5f
}
