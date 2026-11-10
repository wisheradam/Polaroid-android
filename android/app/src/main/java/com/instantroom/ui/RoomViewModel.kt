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

data class CaptureUi(val phase: CapturePhase = CapturePhase.IDLE, val photoId: String? = null, val ejection: Float = 0f, val countdown: Int=0, val shot: Int=1, val booth: Boolean=false, val sourceIds: List<String> = emptyList())
object AndroidCaptureClock: CaptureClock {override fun nowMs()=SystemClock.elapsedRealtime();override suspend fun sleep(ms: Long){delay(ms)}}
class RoomViewModel @JvmOverloads constructor(application: Application,
 val repo: PhotoRepository=(application as InstantRoomApp).repository,
 private val cameraPort: CapturePort=(application as InstantRoomApp).camera,
 private val feedbackPort: FeedbackPort=(application as InstantRoomApp).feedback,
 private val captureClock: CaptureClock=AndroidCaptureClock
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
 private var developmentPhoto: String?=null
 private var developmentGeneration=0
 private val developmentHaptics=DevelopmentHaptics()
 private var recoveryJob: Job?=null
 private var captureJob: Job?=null
 init { recover() }
 fun recover() {
  if(recoveryJob?.isActive==true || repo.captureLock.isLocked)return
  recoveryJob=viewModelScope.launch {
   try { repo.recover(); initialized.value=true } catch(e: Exception) { message.value="Recovery needs retry: ${e.message}" }
  }
 }
 fun clearMessage() { message.value=null }
 fun takePicture(flash: Boolean) = beginCapture(flash,false)
 fun startBooth(flash: Boolean) = beginCapture(flash,true)
 fun cancelCapture() {captureJob?.cancel()}
 private fun beginCapture(flash: Boolean, booth: Boolean) {
  if(!initialized.value || !repo.captureLock.tryLock())return
  val config=settings.value
  capture.value=CaptureUi(CapturePhase.COUNTDOWN,booth=booth)
  captureJob=viewModelScope.launch {
   var saved: PhotoEntity?=null
   var session: BoothSessionEntity?=null
   var cancelled=false
   try {
    val cart=repo.active() ?: error("Load a film cartridge first")
    check(cart.remaining >= if(booth)4 else 1) {if(booth)"Photo Booth needs four exposures. Manually load a fresh cartridge." else "FILM EMPTY · replace the cartridge"}
    if(booth)session=repo.startBooth(config.boothLayout,cart)
    CaptureSequence(captureClock).run(if(booth)4 else 1,if(booth)5 else config.timer.seconds,config.chargeMs,
     countdown={index,left->
      val charging=capture.value.shot==index+1 && capture.value.phase==CapturePhase.CHARGING
      capture.value=capture.value.copy(phase=if(charging)CapturePhase.CHARGING else CapturePhase.COUNTDOWN,countdown=left,shot=index+1)
      if(left>0 && foreground && !charging)feedbackPort.countdown(left<=3)
     },charge={capture.value=capture.value.copy(phase=CapturePhase.CHARGING);if(foreground)feedbackPort.charge(config.chargeMs)}
    ) { index->
     currentCoroutineContext().ensureActive()
     capture.value=capture.value.copy(phase=CapturePhase.CAPTURING,countdown=0)
     // Never discard a successful camera result because Cancel/Pause arrived mid-capture.
     withContext(NonCancellable) {
      val id=session?.let{"${it.id}-$index"} ?: UUID.randomUUID().toString()
      repo.prepareCapture(id,cart,session?.id.orEmpty(),if(booth)index else -1)
      try {cameraPort.captureWithLighting(repo.originalFile(id),flash,config.front && config.selfieLight.screenFlash){if(foreground)feedbackPort.shutter()}}
      catch(e:Exception){repo.abandonCapture(id);throw e}
      saved=repo.commitCapture(id,cart)
      if(booth) {
       if(foreground)feedbackPort.exposed()
       capture.value=capture.value.copy(sourceIds=capture.value.sourceIds+id)
      } else {
       capture.value=capture.value.copy(phase=CapturePhase.PROCESSING)
       repo.process(checkNotNull(saved));saved=repo.photo(checkNotNull(saved).id)
      }
     }
     currentCoroutineContext().ensureActive()
    }
    if(booth) {
     capture.value=capture.value.copy(phase=CapturePhase.PROCESSING)
     withContext(NonCancellable) {
      saved=repo.finishBooth(checkNotNull(session).id)
      // Individual sources remain accessible too; process after the timed sequence.
      repo.boothSources(checkNotNull(session).id).filter{it.processing}.forEach{repo.process(it)}
     }
    }
    if(foreground) {
     capture.value=capture.value.copy(phase=CapturePhase.EJECTING,photoId=checkNotNull(saved).id)
     feedbackPort.eject()
     val start=captureClock.nowMs()
     while(foreground && captureClock.nowMs()-start<EjectionTimeline.durationMs) {
      capture.value=capture.value.copy(ejection=EjectionTimeline.position(captureClock.nowMs()-start));captureClock.sleep(16)
     }
    }
   } catch(e:CancellationException) {
    cancelled=true
   } catch(e:Exception) {
    message.value="${if(booth)"Photo Booth stopped" else "Capture stopped"}: ${e.message}. Successful photographs remain in Archive."
   } finally {
    withContext(NonCancellable) {
     session?.let {current->
      try {
       val sources=repo.boothSources(current.id)
       if(sources.size==4)saved=repo.finishBooth(current.id) else {repo.interruptBooth(current.id);saved=null}
       sources.filter{it.processing}.forEach{repo.process(it)}
       if(cancelled)message.value="Photo Booth cancelled. ${sources.size} successful photographs kept in Archive; their exposures remain used."
      }catch(e:Exception){message.value="Photographs retained. Use Archive → Retry processing: ${e.message}";saved=null}
     }
    }
    if(cancelled || saved==null)feedbackPort.stop()
    if(cancelled && session==null)message.value=if(saved!=null)"Photograph kept in Archive." else "Timer cancelled."
    capture.value=CaptureUi(photoId=if(cancelled)null else saved?.takeIf{!it.processing}?.id,ejection=1f)
    repo.captureLock.unlock()
   }
  }
 }
 fun recompose(photo: PhotoEntity, layout: BoothLayout) {
  if(!repo.captureLock.tryLock())return
  viewModelScope.launch {
   try {val p=repo.recompose(photo.boothSessionId,layout);capture.value=CaptureUi(photoId=p.id)}
   catch(e:Exception){message.value=e.message}finally{repo.captureLock.unlock()}
  }
 }
 fun switchCamera(frontAvailable: Boolean, rearAvailable: Boolean): Boolean {
  val target=CameraSwitchPolicy.target(settings.value.front,capture.value.phase!=CapturePhase.IDLE || repo.captureLock.isLocked,frontAvailable,rearAvailable)
  if(target==null){if(capture.value.phase==CapturePhase.IDLE)message.value="The other camera is unavailable";return false}
  updateSettings(settings.value.copy(front=target));return true
 }
 fun updateSettings(s: Settings) { graph.preferences.set(s);graph.detector.threshold=s.threshold;if(!s.sound || !s.haptics)feedbackPort.stop() }
 fun setRearFlash(on: Boolean,available: Boolean): Boolean {
  if(!FlashMotion.toggleAllowed(available,capture.value.phase!=CapturePhase.IDLE || repo.captureLock.isLocked) || settings.value.rearFlash==on)return false
  updateSettings(settings.value.copy(rearFlash=on))
  if(settings.value.haptics)feedbackPort.controlClick()
  return true
 }
 fun startDevelopment(id: String) { stopDevelopment();developmentPhoto=id }
 fun stopDevelopment() {developmentPhoto=null;developmentGeneration++;developmentHaptics.reset();feedbackPort.stopDevelopment()}
 fun develop(id: String, strength: Float) {
  val generation=developmentGeneration
  fun active()=foreground && developmentPhoto==id && generation==developmentGeneration
  if(!active())return
  viewModelScope.launch {
   try {
    val progressed=repo.develop(id,strength,allowed={active()})
    if(developmentHaptics.shouldPulse(SystemClock.elapsedRealtime(),progressed,active(),settings.value.haptics))feedbackPort.develop()
   }catch(e: Exception){message.value=e.message}
  }
 }
 fun accessibleDevelop(id: String) { if(settings.value.accessible)viewModelScope.launch {repo.develop(id,0f,true)} }
 fun replace(format: FilmFormat, appearance: Appearance, done: ()->Unit) = viewModelScope.launch {
  try { repo.replace(format,appearance);feedbackPort.pin();done() } catch(e: Exception){message.value=e.message}
 }
 fun attach(id: String, done: ()->Unit) = viewModelScope.launch { try{repo.attach(id);done()}catch(e:Exception){message.value=e.message} }
 fun move(id: String,x: Float,y: Float,rotation: Float) { viewModelScope.launch {repo.move(id,x,y,rotation)} }
 fun pin(id: String,color: Long,x: Float,y: Float) {viewModelScope.launch{repo.pin(id,color,x,y);feedbackPort.pin()} }
 fun unpin(id: String) {viewModelScope.launch{repo.unpin(id)}}
 fun detach(id: String) {viewModelScope.launch{repo.detach(id)}}
 fun caption(id: String,text: String,font: CaptionFont,done: (Boolean)->Unit) {viewModelScope.launch {
  try{repo.setCaption(id,text,font);done(true)}catch(e:Exception){message.value="Caption not saved: ${e.message}";done(false)}
 }}
 fun save(p: PhotoEntity) {viewModelScope.launch{try{graph.export.save(repo.photo(p.id));message.value="Saved to Pictures / Instant Room"}catch(e:Exception){message.value="Save failed: ${e.message}"}}}
 fun share(p: PhotoEntity, target: ShareTarget = ShareTarget.SYSTEM) {viewModelScope.launch{try{graph.export.share(repo.photo(p.id),target)}catch(e:Exception){message.value=e.message}}}
 override fun onCleared() { graph.motion.stop();stopDevelopment();feedbackPort.stop() }
}
