package com.instantroom.device

import android.content.Context
import android.media.*
import android.os.*
import com.instantroom.R
import com.instantroom.core.*
import com.instantroom.data.Preferences

/** Original synthesized PCM assets. No boot-time playback. */
class Feedback(context: Context, private val prefs: Preferences) : FeedbackPort {
 private val pool=SoundPool.Builder().setMaxStreams(3).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
 private val loaded=mutableSetOf<Int>()
 private val streams=mutableListOf<Int>()
 private val vibrator = if(Build.VERSION.SDK_INT>=31) (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
 private val charge: Int
 private val shutter: Int
 private val motor: Int
 private val pin: Int
 private val tick: Int
 private var developmentPulse=false
 init {
  pool.setOnLoadCompleteListener { _,id,status -> if(status==0)loaded.add(id) }
  charge=pool.load(context,R.raw.charge,1); shutter=pool.load(context,R.raw.shutter,1); motor=pool.load(context,R.raw.eject,1); pin=pool.load(context,R.raw.pin,1)
  tick=pool.load(context,R.raw.countdown,1)
 }
 private fun play(id: Int, rate: Float=1f) { if(prefs.state.value.sound && id in loaded) streams.add(pool.play(id,.55f,.55f,1,0,rate)) }
 private fun pulse(duration: Long, amplitude: Int) { if(prefs.state.value.haptics && vibrator.hasVibrator()){developmentPulse=false;vibrator.vibrate(VibrationEffect.createOneShot(duration,amplitude))} }
 private fun exclusive(id: Int,rate: Float=1f){streams.forEach{pool.stop(it)};streams.clear();play(id,rate)}
 override fun charge(durationMs: Long) { exclusive(charge,(650f/durationMs).coerceIn(.5f,2f)); pulse(18,35) }
 override fun shutter() { exclusive(shutter); pulse(24,50) }
 override fun countdown(finalSeconds: Boolean) {exclusive(tick,if(finalSeconds)1.4f else 1f)}
 override fun exposed() {pulse(14,28)}
 override fun eject() {
  play(motor)
  if(prefs.state.value.haptics && vibrator.hasVibrator()) {
   developmentPulse=false
   vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0,22,278,20,300,22,318,20,320,20,280,20,180,24),intArrayOf(0,25,0,28,0,25,0,30,0,25,0,22,0,45),-1))
  }
 }
 override fun pin() { play(pin); pulse(16,38) }
 override fun develop() {
  if(!prefs.state.value.haptics || !vibrator.hasVibrator())return
  // Short isolated pulses also remain restrained on motors without amplitude control.
  developmentPulse=true
  vibrator.vibrate(VibrationEffect.createOneShot(if(vibrator.hasAmplitudeControl())12 else 8,24))
 }
 override fun stopDevelopment() { if(developmentPulse)vibrator.cancel();developmentPulse=false }
 override fun stop() { streams.forEach { pool.stop(it) }; streams.clear(); developmentPulse=false;vibrator.cancel() }
 fun release() { stop(); pool.release() }
}
