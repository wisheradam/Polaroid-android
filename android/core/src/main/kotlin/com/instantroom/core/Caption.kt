package com.instantroom.core

enum class CaptionFont(val label: String) { SOL_KOL("Sol Kol"), DENISTINA("Denistina"), HISTORIC("Historic") }

object CaptionRules {
 const val maxCharacters = 80
 fun normalize(text: String): String {
  val clean=text.replace("\r\n","\n").replace('\r','\n').trim()
  require(clean.codePointCount(0,clean.length)<=maxCharacters) { "Use at most $maxCharacters characters" }
  require(clean.lines().size<=2) { "Use at most two short lines" }
  require(clean.none { Character.isISOControl(it) && it!='\n' }) { "Use plain text without control characters" }
  return clean
 }
}

/** One small pulse per accepted progress event, never a repeating vibration/timer. */
class DevelopmentHaptics {
 private var last: Long?=null
 fun reset() {last=null}
 fun shouldPulse(nowMs: Long, progressed: Boolean, active: Boolean, enabled: Boolean): Boolean {
  if(!progressed || !active || !enabled)return false
  if(last?.let{nowMs-it in 0 until 260}==true)return false
  last=nowMs;return true
 }
}
