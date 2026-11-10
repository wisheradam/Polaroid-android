package com.instantroom.core

enum class ThemeMode(val label: String) { LIGHT("Light"), DARK("Dark"), SYSTEM("Follow System");
 fun isDark(systemDark: Boolean) = this == DARK || this == SYSTEM && systemDark
}
enum class SelfieLightMode(val label: String, val screenFlash: Boolean, val edge: Boolean) {
 OFF("Off",false,false), SCREEN("Screen flash",true,false), EDGE("Edge light",false,true), BOTH("Screen + edge",true,true)
}
enum class LightTone(val label: String) { WHITE("White"), WARM("Warm white") }
object CameraSwitchPolicy {
 fun target(front: Boolean, busy: Boolean, frontAvailable: Boolean, rearAvailable: Boolean): Boolean? =
  if(busy || if(front) !rearAvailable else !frontAvailable) null else !front
}
/** Owns only a Window brightness override; never modifies global system settings. */
class BrightnessLease(private val read: ()->Float, private val write: (Float)->Unit) {
 private var original: Float?=null
 fun set(level: Float?) {
  if(level==null) { restore();return }
  if(original==null)original=read()
  write(level.coerceIn(0f,1f))
 }
 fun restore() { original?.let(write);original=null }
}

enum class ShareTarget(val label: String, val packages: List<String>) {
 SYSTEM("Other apps",emptyList()), INSTAGRAM("Instagram",listOf("com.instagram.android","com.instagram.lite")), FACEBOOK("Facebook",listOf("com.facebook.katana","com.facebook.lite"))
}
