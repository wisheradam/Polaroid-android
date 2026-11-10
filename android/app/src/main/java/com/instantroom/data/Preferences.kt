package com.instantroom.data

import android.content.Context
import androidx.core.content.edit
import com.instantroom.core.*
import kotlinx.coroutines.flow.MutableStateFlow

data class Settings(
 val sound: Boolean = true, val haptics: Boolean = true, val accessible: Boolean = false,
 val threshold: Float = 6.5f, val chargeMs: Long = 650, val lowMemory: Boolean = false,
 val front: Boolean = false, val imperfections: Boolean = true,
 val theme: ThemeMode = ThemeMode.SYSTEM, val selfieLight: SelfieLightMode = SelfieLightMode.OFF,
 val edgeWidth: Float = 16f, val edgeBrightness: Float = .85f, val lightTone: LightTone = LightTone.WARM,
 val timer: SelfTimer = SelfTimer.OFF, val boothLayout: BoothLayout = BoothLayout.GRID
)
class Preferences(context: Context) {
 private val p=context.getSharedPreferences("settings",Context.MODE_PRIVATE)
 private inline fun <reified T:Enum<T>> enum(key: String, fallback: T): T = runCatching{enumValueOf<T>(p.getString(key,null).orEmpty())}.getOrDefault(fallback)
 // v1 was always dark. Preserve that appearance on upgrade; fresh installs follow the OS.
 private val initialTheme=enum("theme",if(p.all.isNotEmpty() || context.getDatabasePath("instant-room.db").exists())ThemeMode.DARK else ThemeMode.SYSTEM)
 init { if(!p.contains("theme"))p.edit(commit=true){putString("theme",initialTheme.name)} }
 val state=MutableStateFlow(Settings(
  p.getBoolean("sound",true),p.getBoolean("haptics",true),p.getBoolean("accessible",false),
  p.getFloat("threshold",6.5f),p.getLong("charge",650),p.getBoolean("low",false),
  p.getBoolean("front",false),p.getBoolean("imperfections",true),initialTheme,
  enum("selfieLight",SelfieLightMode.OFF),p.getFloat("edgeWidth",16f).coerceIn(8f,32f),
  p.getFloat("edgeBrightness",.85f).coerceIn(.35f,1f),enum("lightTone",LightTone.WARM),enum("timer",SelfTimer.OFF),enum("boothLayout",BoothLayout.GRID)
 ))
 fun set(s: Settings) {
  val normalized=s.copy(edgeWidth=s.edgeWidth.coerceIn(8f,32f),edgeBrightness=s.edgeBrightness.coerceIn(.35f,1f))
  state.value=normalized
  p.edit {
   putBoolean("sound",s.sound);putBoolean("haptics",s.haptics);putBoolean("accessible",s.accessible)
   putFloat("threshold",s.threshold);putLong("charge",s.chargeMs);putBoolean("low",s.lowMemory)
   putBoolean("front",s.front);putBoolean("imperfections",s.imperfections);putString("theme",s.theme.name)
   putString("selfieLight",s.selfieLight.name);putFloat("edgeWidth",normalized.edgeWidth)
   putFloat("edgeBrightness",normalized.edgeBrightness);putString("lightTone",s.lightTone.name)
   putString("timer",s.timer.name);putString("boothLayout",s.boothLayout.name)
  }
 }
}
