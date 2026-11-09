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
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface CapturePort { val hasFlash: Boolean; suspend fun capture(file: File, flash: Boolean, onShutter: ()->Unit) }
class CameraController(private val context: Context) : CapturePort {
 private var provider: ProcessCameraProvider? = null
 private var imageCapture: ImageCapture? = null
 private var camera: Camera? = null
 private var previewView: PreviewView? = null
 override val hasFlash get() = camera?.cameraInfo?.hasFlashUnit() == true
 suspend fun bind(owner: LifecycleOwner, view: PreviewView, front: Boolean) {
  val p = suspendCancellableCoroutine<ProcessCameraProvider> { c ->
   val future=ProcessCameraProvider.getInstance(context)
   future.addListener({ try { if(c.isActive)c.resume(future.get()) } catch(e: Exception){if(c.isActive)c.resumeWithException(e)} },ContextCompat.getMainExecutor(context))
  }
  provider=p; previewView=view
  p.unbindAll()
  val preview=Preview.Builder().build().apply { setSurfaceProvider(view.surfaceProvider) }
  val capture=ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(ResolutionStrategy(android.util.Size(1600,1600),ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)).build()).build()
  val selector=if(front)CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
  camera=p.bindToLifecycle(owner, selector, preview, capture)
  imageCapture=capture
 }
 override suspend fun capture(file: File, flash: Boolean, onShutter: ()->Unit) = suspendCancellableCoroutine<Unit> { c ->
  val capture=imageCapture ?: return@suspendCancellableCoroutine c.resumeWithException(IllegalStateException("Camera is not ready"))
  capture.targetRotation = previewView?.display?.rotation ?: Surface.ROTATION_0
  capture.flashMode=if(flash && hasFlash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
  capture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(),ContextCompat.getMainExecutor(context),object: ImageCapture.OnImageSavedCallback {
   override fun onCaptureStarted() { onShutter() }
   override fun onImageSaved(output: ImageCapture.OutputFileResults) { if(c.isActive)c.resume(Unit) }
   override fun onError(exception: ImageCaptureException) { if(c.isActive)c.resumeWithException(exception) }
  })
 }
 fun unbind() { provider?.unbindAll(); imageCapture=null; camera=null }
}
