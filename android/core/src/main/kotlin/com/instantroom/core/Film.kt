package com.instantroom.core

import kotlin.math.*

/** Dimensions are physical millimetres; width precedes height. */
enum class FilmFormat(val label: String, val width: Float, val height: Float, val imageWidth: Float, val imageHeight: Float, val top: Float) {
 CLASSIC("Classic", 88.47f, 107.52f, 76.801f, 78.94f, 5.8f),
 GO("Go · Mini", 53.9f, 66.6f, 46f, 47f, 3.8f),
 SPECTRA("Spectra · Wide", 103.124f, 101.6f, 91.44f, 73.66f, 5.5f),
 ROUND("Round Frame", 88.47f, 107.52f, 76.81f, 76.81f, 6f);
 val aspect get() = width / height
 val imageAspect get() = imageWidth / imageHeight
}
enum class Appearance(val label: String) { COLOR("Classic Color"), MONO("Black & White"), WARM("Vintage Warm") }
data class Cartridge(val id: String, val format: FilmFormat, val appearance: Appearance, val remaining: Int = 10) {
 init { require(remaining in 0..10) }
 fun expose() = copy(remaining = (remaining - 1).also { check(remaining > 0) { "FILM EMPTY" } })
}
enum class CapturePhase { IDLE, COUNTDOWN, CHARGING, CAPTURING, PROCESSING, EJECTING }
enum class LoadingPhase { CLOSED, OPEN, REMOVED, INSERTED }
class LoadingMachine(initial: LoadingPhase = LoadingPhase.CLOSED) {
 var phase = initial; private set
 fun open() { check(phase == LoadingPhase.CLOSED); phase = LoadingPhase.OPEN }
 fun remove() { check(phase == LoadingPhase.OPEN); phase = LoadingPhase.REMOVED }
 fun insert() { check(phase == LoadingPhase.REMOVED); phase = LoadingPhase.INSERTED }
 fun close() { check(phase == LoadingPhase.INSERTED); phase = LoadingPhase.CLOSED }
}
/** Same normalized clock drives picture translation, sound and haptic schedule. */
object EjectionTimeline {
 const val durationMs = 1800L
 val pulseTimesMs = listOf(0L, 300L, 620L, 960L, 1300L, 1600L, 1800L)
 fun position(elapsed: Long): Float = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
}
data class Placement(val x: Float = 100f, val y: Float = 100f, val rotation: Float = 0f, val pinned: Boolean = false) {
 fun move(dx: Float, dy: Float, angle: Float) = if (pinned) this else copy(x = (x + dx).coerceIn(0f, 1800f), y = (y + dy).coerceIn(0f, 1400f), rotation = (rotation + angle).coerceIn(-180f, 180f))
}
/** A candidate requires opposing linear acceleration peaks, not a single jolt.
 * Sensor timestamps are monotonic nanoseconds; no timer or wall-clock progress. */
class ShakeDetector(var threshold: Float = 6.5f) {
 private var previousTime = 0L
 private var peakTime = 0L
 private var peak = floatArrayOf(0f, 0f, 0f)
 private var lastAccepted = 0L
 fun reset() { previousTime = 0; peakTime = 0; lastAccepted = 0; peak.fill(0f) }
 fun sample(time: Long, x: Float, y: Float, z: Float): Float {
  if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return 0f
  if (previousTime != 0L && (time <= previousTime || time - previousTime > 600_000_000L)) reset()
  previousTime = time
  val magnitude = sqrt(x*x+y*y+z*z)
  if (magnitude < threshold) return 0f
  val sincePeak = time - peakTime
  val dot = peak[0]*x+peak[1]*y+peak[2]*z
  val accepted = peakTime != 0L && sincePeak in 65_000_000L..450_000_000L && dot < -threshold*threshold*.45f && (lastAccepted == 0L || time-lastAccepted >= 110_000_000L)
  if (peakTime == 0L || sincePeak > 450_000_000L || dot < 0) { peakTime=time; peak=floatArrayOf(x,y,z) }
  if (!accepted) return 0f
  lastAccepted=time
  return ((magnitude-threshold)/18f+.35f).coerceIn(.35f, 1.4f)
 }
}
object Development {
 fun acceptedShake(progress: Float, strength: Float): Float = if (!strength.isFinite() || strength <= 0f) progress else (progress + strength.coerceAtMost(1.4f)*.018f).coerceIn(0f, 1f)
 fun accessiblePress(progress: Float, enabled: Boolean) = if (enabled) acceptedShake(progress, .6f) else progress
}
interface FeedbackPort { fun charge(durationMs: Long); fun shutter(); fun eject(); fun pin(); fun stop(); fun develop() = Unit; fun stopDevelopment() = Unit; fun countdown(finalSeconds: Boolean) = Unit; fun exposed() = Unit; fun controlClick() = Unit }
interface MotionPort { val available: Boolean; fun start(onShake: (Float) -> Unit); fun stop() }
