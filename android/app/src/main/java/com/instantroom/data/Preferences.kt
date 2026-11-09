package com.instantroom.data
import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow

data class Settings(val sound: Boolean = true, val haptics: Boolean = true, val accessible: Boolean = false, val threshold: Float = 6.5f, val chargeMs: Long = 650, val lowMemory: Boolean = false, val front: Boolean = false, val imperfections: Boolean = true)
class Preferences(context: Context) {
 private val p=context.getSharedPreferences("settings",Context.MODE_PRIVATE)
 val state=MutableStateFlow(Settings(p.getBoolean("sound",true),p.getBoolean("haptics",true),p.getBoolean("accessible",false),p.getFloat("threshold",6.5f),p.getLong("charge",650),p.getBoolean("low",false),p.getBoolean("front",false),p.getBoolean("imperfections",true)))
 fun set(s: Settings) { state.value=s; p.edit { putBoolean("sound",s.sound);putBoolean("haptics",s.haptics);putBoolean("accessible",s.accessible);putFloat("threshold",s.threshold);putLong("charge",s.chargeMs);putBoolean("low",s.lowMemory);putBoolean("front",s.front);putBoolean("imperfections",s.imperfections) } }
}
