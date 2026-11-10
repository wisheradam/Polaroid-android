package com.instantroom.device

import android.content.Context
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface CapturePort {
 val hasFlash: Boolean
 suspend fun capture(file: File, flash: Boolean, onShutter: ()->Unit)
 suspend fun captureWithLighting(file: File, flash: Boolean, screenFlash: Boolean, onShutter: ()->Unit) = capture(file,flash,onShutter)
}
data class CameraAvailability(val front: Boolean=false,val rear: Boolean=false,val selectedFront: Boolean=false,val ready: Boolean=false)
class CameraController(private val context: Context) : CapturePort {
 private var provider: ProcessCameraProvider?=null
 private var imageCapture: ImageCapture?=null
 private var camera: Camera?=null
 private var previewView: PreviewView?=null
 private var screenFlash: ImageCapture.ScreenFlash?=null
 private val mutableState=MutableStateFlow(CameraAvailability())
 val state=mutableState.asStateFlow()
 override val hasFlash get()=camera?.cameraInfo?.hasFlashUnit()==true
 fun installScreenFlash(value: ImageCapture.ScreenFlash?) {
  screenFlash?.clear()
  imageCapture?.let { it.flashMode=ImageCapture.FLASH_MODE_OFF;it.screenFlash=value }
  screenFlash=value
 }
 suspend fun bind(owner: LifecycleOwner, view: PreviewView, front: Boolean) {
  mutableState.value=mutableState.value.copy(ready=false)
  val p=suspendCancellableCoroutine<ProcessCameraProvider>{c->
   val future=ProcessCameraProvider.getInstance(context)
   future.addListener({try{if(c.isActive)c.resume(future.get())}catch(e:Exception){if(c.isActive)c.resumeWithException(e)}},ContextCompat.getMainExecutor(context))
  }
  currentCoroutineContext().ensureActive()
  val hasFront=p.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
  val hasRear=p.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
  val actualFront=if(front)hasFront else !hasRear && hasFront
  mutableState.value=CameraAvailability(hasFront,hasRear,actualFront)
  check(hasFront || hasRear){"No camera is available on this device"}
  provider=p;previewView=view
  screenFlash?.clear();p.unbindAll()
  val preview=Preview.Builder().build().apply{setSurfaceProvider(view.surfaceProvider)}
  val capture=ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
   .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(ResolutionStrategy(android.util.Size(1600,1600),ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)).build()).build()
  capture.screenFlash=screenFlash
  val selector=if(actualFront)CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
  camera=p.bindToLifecycle(owner,selector,preview,capture)
  imageCapture=capture
  mutableState.value=mutableState.value.copy(ready=true)
 }
 override suspend fun capture(file: File,flash: Boolean,onShutter: ()->Unit) = captureWithLighting(file,flash,false,onShutter)
 override suspend fun captureWithLighting(file: File,flash: Boolean,screenFlash: Boolean,onShutter: ()->Unit) {
  val light=this.screenFlash
  try {
   suspendCancellableCoroutine<Unit>{c->
    val capture=imageCapture ?: return@suspendCancellableCoroutine c.resumeWithException(IllegalStateException("Camera is not ready"))
    capture.targetRotation=previewView?.display?.rotation ?: Surface.ROTATION_0
    val front=mutableState.value.selectedFront
    if(screenFlash && front && light==null)return@suspendCancellableCoroutine c.resumeWithException(IllegalStateException("Screen flash is not ready"))
    capture.flashMode=when{screenFlash && front->ImageCapture.FLASH_MODE_SCREEN;flash && hasFlash && !front->ImageCapture.FLASH_MODE_ON;else->ImageCapture.FLASH_MODE_OFF}
    val options=ImageCapture.OutputFileOptions.Builder(file).setMetadata(metadata(front)).build()
    capture.takePicture(options,ContextCompat.getMainExecutor(context),object:ImageCapture.OnImageSavedCallback{
     override fun onCaptureStarted(){onShutter()}
     override fun onImageSaved(output: ImageCapture.OutputFileResults){if(c.isActive)c.resume(Unit)}
     override fun onError(exception: ImageCaptureException){if(c.isActive)c.resumeWithException(exception)}
    })
   }
  } finally {light?.clear()}
 }
 fun unbind(){
  screenFlash?.clear()
  imageCapture?.let{it.flashMode=ImageCapture.FLASH_MODE_OFF;it.screenFlash=null}
  provider?.unbindAll();imageCapture=null;camera=null
  mutableState.value=mutableState.value.copy(ready=false)
 }
 companion object {
  /** CameraX preview mirrors front cameras. Bake matching EXIF into the original. */
  fun metadata(front: Boolean)=ImageCapture.Metadata().apply{isReversedHorizontal=front}
 }
}
