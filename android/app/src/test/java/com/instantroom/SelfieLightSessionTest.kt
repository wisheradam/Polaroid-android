package com.instantroom

import androidx.camera.core.ImageCapture
import com.instantroom.device.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class SelfieLightSessionTest {
 private var brightness=-1f
 private var callback: ImageCapture.ScreenFlashListener?=null
 private var before=-1f
 private var clears=0
 private val delegate=object:ImageCapture.ScreenFlash {
  override fun apply(expirationTimeMillis:Long,listener:ImageCapture.ScreenFlashListener){before=brightness;brightness=1f;callback=listener}
  override fun clear(){brightness=before;clears++}
 }
 private fun session()=SelfieLightSession(delegate,{brightness},{brightness=it})
 @Test fun screenFlashAndClearRestoreOriginalBrightness(){val s=session();s.setFraming(true,null);var ready=false;s.apply(999999){ready=true};assertEquals(1f,brightness,0f);assertFalse(ready);callback!!.onCompleted();assertTrue(ready);s.clear();assertEquals(-1f,brightness,0f);assertFalse(s.active.value)}
 @Test fun edgeOnlyLeavesCameraPreviewUnobscured(){val s=session();s.setFraming(true,.7f);assertEquals(.7f,brightness,0f);assertFalse(s.active.value);s.close();assertEquals(-1f,brightness,0f)}
 @Test fun combinedFlashReturnsToEdgeThenOriginal(){val s=session();s.setFraming(true,.8f);s.apply(999999){};assertEquals(1f,brightness,0f);s.clear();assertEquals(.8f,brightness,0f);s.close();assertEquals(-1f,brightness,0f)}
 @Test fun pauseClearsActiveFlashAndInvalidatesLateCompletion(){val s=session();s.setFraming(true,.8f);var ready=false;s.apply(999999){ready=true};s.close();callback!!.onCompleted();s.clear();assertFalse(ready);assertEquals(-1f,brightness,0f);assertEquals(1,clears)}
 @Test fun failureCleanupIsIdempotent(){brightness=.25f;val s=session();s.setFraming(true,null);s.apply(999999){};s.clear();brightness=.4f;s.clear();s.close();assertEquals(.4f,brightness,0f)}
 @Test fun leavingCameraRestoresEvenWhenEdgeWasAdjusted(){val s=session();s.setFraming(true,.5f);s.setFraming(true,.9f);s.setFraming(false,null);assertEquals(-1f,brightness,0f)}
 @Test fun pausedSessionIgnoresLateFlashRequest(){val s=session();s.apply(999999){};assertFalse(s.active.value);assertEquals(-1f,brightness,0f)}
 @Test fun savedFrontImageIsMirroredAndRearIsNot(){assertTrue(CameraController.metadata(true).isReversedHorizontal);assertFalse(CameraController.metadata(false).isReversedHorizontal)}
}
