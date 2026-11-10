package com.instantroom

import android.content.Context
import android.os.VibratorManager
import androidx.test.core.app.ApplicationProvider
import com.instantroom.data.*
import com.instantroom.device.Feedback
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class FeedbackOwnershipTest {
 @Test fun developmentCleanupPreservesShutterAndEjectionButCancelsItsOwnPulse() {
  val context=ApplicationProvider.getApplicationContext<Context>()
  val vibrator=(context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
  val shadow=shadowOf(vibrator);shadow.setHasVibrator(true)
  val prefs=Preferences(context);prefs.set(Settings(sound=false,haptics=true))
  val feedback=Feedback(context,prefs)
  try {
   feedback.eject();assertTrue(shadow.isVibrating)
   feedback.stopDevelopment();assertTrue("Keep the final roller/click waveform",shadow.isVibrating)
   feedback.stop()
   feedback.develop();assertTrue(shadow.isVibrating)
   feedback.stopDevelopment();assertFalse(shadow.isVibrating)
   feedback.develop();feedback.shutter();feedback.stopDevelopment()
   assertTrue("A shutter pulse supersedes development ownership",shadow.isVibrating)
   prefs.set(prefs.state.value.copy(haptics=false));feedback.stop();feedback.develop();assertFalse(shadow.isVibrating)
  }finally{feedback.release()}
 }
}
