package com.instantroom.device

import android.content.Context
import android.hardware.*
import com.instantroom.core.*

class MotionController(context: Context, private val detector: ShakeDetector) : MotionPort, SensorEventListener {
 private val manager=context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
 private val linear=manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
 private val sensor=linear ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
 private var callback: ((Float)->Unit)?=null
 private val gravity=FloatArray(3)
 private var lastTime=0L
 private var startTime=0L
 override val available get()=sensor!=null
 override fun start(onShake: (Float)->Unit) {
  stop(); callback=onShake; detector.reset(); gravity.fill(0f); lastTime=0; startTime=0
  sensor?.let { manager.registerListener(this,it,SensorManager.SENSOR_DELAY_GAME) }
 }
 override fun stop() { manager.unregisterListener(this); callback=null; detector.reset() }
 override fun onSensorChanged(e: SensorEvent) {
  if(startTime==0L) startTime=e.timestamp
  val values=e.values.copyOf()
  if(linear==null) {
   val dt=if(lastTime==0L) .02f else ((e.timestamp-lastTime)/1e9f).coerceIn(.001f,.2f)
   val alpha=.24f/(.24f+dt)
   for(i in 0..2) { gravity[i]=alpha*gravity[i]+(1-alpha)*values[i]; values[i]-=gravity[i] }
  }
  lastTime=e.timestamp
  if(e.timestamp-startTime < 500_000_000L) return
  val strength=detector.sample(e.timestamp,values[0],values[1],values[2])
  if(strength>0f)callback?.invoke(strength)
 }
 override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
