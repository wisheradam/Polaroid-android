package com.instantroom.device

import androidx.camera.core.ImageCapture
import com.instantroom.core.BrightnessLease
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Idempotent bridge around CameraX's animated ScreenFlashView implementation. Main thread only. */
class SelfieLightSession(
 private val delegate: ImageCapture.ScreenFlash,
 readBrightness: ()->Float,
 writeBrightness: (Float)->Unit
) : ImageCapture.ScreenFlash {
 private val lease=BrightnessLease(readBrightness,writeBrightness)
 private var enabled=false
 private var edgeLevel: Float?=null
 private var generation=0
 private val flashing=MutableStateFlow(false)
 val active=flashing.asStateFlow()
 fun setFraming(enabled: Boolean, edgeLevel: Float?) {
  this.enabled=enabled;this.edgeLevel=if(enabled)edgeLevel else null
  if(!enabled)clear()
  if(!flashing.value)lease.set(this.edgeLevel)
 }
 override fun apply(expirationTimeMillis: Long, listener: ImageCapture.ScreenFlashListener) {
  if(!enabled){listener.onCompleted();return} // A late CameraX callback must not relight a paused window.
  clear()
  val token=++generation
  flashing.value=true
  delegate.apply(expirationTimeMillis) { if(generation==token && flashing.value)listener.onCompleted() }
 }
 override fun clear() {
  if(!flashing.value)return
  generation++;flashing.value=false
  try{delegate.clear()}finally{lease.set(if(enabled)edgeLevel else null)}
 }
 fun close() { enabled=false;edgeLevel=null;clear();lease.restore() }
}
