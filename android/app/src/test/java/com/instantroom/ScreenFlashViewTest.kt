package com.instantroom

import android.app.Activity
import androidx.camera.view.ScreenFlashView
import com.instantroom.device.SelfieLightSession
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class ScreenFlashViewTest {
 @Test fun cameraXViewRaisesAndRestoresActualWindowBrightness(){
  val lifecycle=Robolectric.buildActivity(Activity::class.java).setup().visible();val activity=lifecycle.get();val window=activity.window
  window.attributes=window.attributes.apply{screenBrightness=.31f}
  val view=ScreenFlashView(activity).apply{setScreenFlashWindow(window)};activity.setContentView(view)
  val session=SelfieLightSession(view.screenFlash!!,{window.attributes.screenBrightness},{window.attributes=window.attributes.apply{screenBrightness=it}})
  try {
   session.setFraming(true,.75f);var completed=false
   session.apply(System.currentTimeMillis()+3000){completed=true}
   shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
   assertTrue(completed);assertEquals(1f,window.attributes.screenBrightness,0f)
   session.clear();assertEquals(.75f,window.attributes.screenBrightness,0f)
   session.close();assertEquals(.31f,window.attributes.screenBrightness,0f)
  } finally{session.close();lifecycle.pause().stop().destroy()}
 }
 @Test fun closingDuringTheRampCannotRelightTheWindow(){
  val lifecycle=Robolectric.buildActivity(Activity::class.java).setup().visible();val activity=lifecycle.get();val window=activity.window
  window.attributes=window.attributes.apply{screenBrightness=-1f}
  val view=ScreenFlashView(activity).apply{setScreenFlashWindow(window)};activity.setContentView(view)
  val session=SelfieLightSession(view.screenFlash!!,{window.attributes.screenBrightness},{window.attributes=window.attributes.apply{screenBrightness=it}})
  try {
   session.setFraming(true,null);session.apply(System.currentTimeMillis()+3000){};session.close()
   shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
   assertEquals(-1f,window.attributes.screenBrightness,0f);assertEquals(0f,view.alpha,0f)
  }finally{session.close();lifecycle.pause().stop().destroy()}
 }
}
