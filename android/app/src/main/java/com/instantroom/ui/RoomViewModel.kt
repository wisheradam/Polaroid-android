package com.instantroom.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.instantroom.InstantRoomApp
import com.instantroom.core.*
import com.instantroom.data.*
import com.instantroom.device.CapturePort
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

data class CaptureUi(val phase: CapturePhase = CapturePhase.IDLE, val photoId: String? = null, val ejection: Float = 0f)
class RoomViewModel @JvmOverloads constructor(application: Application,
 val repo: PhotoRepository=(application as InstantRoomApp).repository,
 private val cameraPort: CapturePort=(application as InstantRoomApp).camera,
 private val feedbackPort: FeedbackPort=(application as InstantRoomApp).feedback
) : AndroidViewModel(application) {
 val graph=application as InstantRoomApp
 val photos=repo.photos.stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
 val cartridge=repo.cartridge.stateIn(viewModelScope,SharingStarted.Eagerly,null)
 val placements=repo.placements.stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
 val settings=graph.preferences.state
 val capture=MutableStateFlow(CaptureUi())
 val message=MutableStateFlow<String?>(null)
 val initialized=MutableStateFlow(false)
 var foreground=false
 private var recoveryJob: Job?=null
 init { recover() }
 fun recover() {
  if(recoveryJob?.isActive==true)return
  recoveryJob=viewModelScope.launch {
   try { repo.recover(); initialized.value=true } catch(e: Exception) { message.value="Recovery needs retry: ${e.message}" }
  }
 }
 fun clearMessage() { message.value=null }
 fun takePicture(flash: Boolean) {
  if(!initialized.value || !repo.captureLock.tryLock())return
  viewModelScope.launch {
   var saved: PhotoEntity?=null
   try {
    val cart=repo.active() ?: error("Load a film cartridge first")
    check(cart.remaining>0) { "FILM EMPTY · replace the cartridge" }
    capture.value=CaptureUi(CapturePhase.CHARGING)
    val duration=settings.value.chargeMs
    feedbackPort.charge(duration);delay(duration)
    capture.value=CaptureUi(CapturePhase.CAPTURING)
    // Once a camera request starts, finishing its durable archive takes precedence over UI cancellation.
    withContext(NonCancellable) {
     val id=UUID.randomUUID().toString()
     repo.prepareCapture(id,cart)
     try { cameraPort.captureWithLighting(repo.originalFile(id),flash,settings.value.front && settings.value.selfieLight.screenFlash) {if(foreground)feedbackPort.shutter()} }
     catch(e:Exception) { repo.abandonCapture(id);throw e }
     saved=repo.commitCapture(id,cart)
     capture.value=CaptureUi(CapturePhase.PROCESSING,id)
     repo.process(saved!!)
    }
    if(foreground) {
     capture.value=CaptureUi(CapturePhase.EJECTING,saved!!.id)
     feedbackPort.eject()
     val start=SystemClock.elapsedRealtime()
     while(foreground && SystemClock.elapsedRealtime()-start<EjectionTimeline.durationMs) {
      capture.value=capture.value.copy(ejection=EjectionTimeline.position(SystemClock.elapsedRealtime()-start));delay(16)
     }
    }
    capture.value=CaptureUi(photoId=saved!!.id,ejection=1f)
   } catch(e: Exception) {
    message.value=if(saved!=null) "Photograph archived. Processing can be retried: ${e.message}" else "Capture failed: ${e.message}"
    capture.value=CaptureUi()
   } finally { repo.captureLock.unlock() }
  }
 }
 fun switchCamera(frontAvailable: Boolean, rearAvailable: Boolean): Boolean {
  val target=CameraSwitchPolicy.target(settings.value.front,capture.value.phase!=CapturePhase.IDLE || repo.captureLock.isLocked,frontAvailable,rearAvailable)
  if(target==null){if(capture.value.phase==CapturePhase.IDLE)message.value="The other camera is unavailable";return false}
  updateSettings(settings.value.copy(front=target));return true
 }
 fun updateSettings(s: Settings) { graph.preferences.set(s);graph.detector.threshold=s.threshold;if(!s.sound || !s.haptics)feedbackPort.stop() }
 fun develop(id: String, strength: Float) { viewModelScope.launch { try { repo.develop(id,strength) }catch(e: Exception){message.value=e.message} } }
 fun accessibleDevelop(id: String) { if(settings.value.accessible)viewModelScope.launch {repo.develop(id,0f,true)} }
 fun replace(format: FilmFormat, appearance: Appearance, done: ()->Unit) = viewModelScope.launch {
  try { repo.replace(format,appearance);feedbackPort.pin();done() } catch(e: Exception){message.value=e.message}
 }
 fun attach(id: String, done: ()->Unit) = viewModelScope.launch { try{repo.attach(id);done()}catch(e:Exception){message.value=e.message} }
 fun move(id: String,x: Float,y: Float,rotation: Float) { viewModelScope.launch {repo.move(id,x,y,rotation)} }
 fun pin(id: String,color: Long,x: Float,y: Float) {viewModelScope.launch{repo.pin(id,color,x,y);feedbackPort.pin()} }
 fun unpin(id: String) {viewModelScope.launch{repo.unpin(id)}}
 fun detach(id: String) {viewModelScope.launch{repo.detach(id)}}
 fun save(p: PhotoEntity) {viewModelScope.launch{try{graph.export.save(p);message.value="Saved to Pictures / Instant Room"}catch(e:Exception){message.value="Save failed: ${e.message}"}}}
 fun share(p: PhotoEntity, target: ShareTarget = ShareTarget.SYSTEM) {try{graph.export.share(p,target)}catch(e:Exception){message.value=e.message}}
 override fun onCleared() { graph.motion.stop();feedbackPort.stop() }
}
