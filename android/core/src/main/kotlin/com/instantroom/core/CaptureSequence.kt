package com.instantroom.core

enum class SelfTimer(val seconds: Int, val label: String) { OFF(0,"Off"), FIVE(5,"5 seconds"), FIFTEEN(15,"15 seconds"), THIRTY(30,"30 seconds") }
data class PrintCell(val left: Float,val top: Float,val width: Float,val height: Float)
data class PrintGeometry(val label: String,val width: Float,val height: Float,val imageWidth: Float,val imageHeight: Float,val top: Float,val round: Boolean=false,val cells: List<PrintCell> = emptyList()) {
 val aspect get()=width/height
 val imageRegions get()=cells.ifEmpty{listOf(PrintCell((width-imageWidth)/2,top,imageWidth,imageHeight))}
}
val FilmFormat.geometry get()=PrintGeometry(label,width,height,imageWidth,imageHeight,top,this==FilmFormat.ROUND)
enum class BoothLayout(val label: String) {
 GRID("Collage 2×2"), STRIP("Vertical Strip 1×4");
 val geometry get()=when(this) {
  GRID->PrintGeometry(label,124f,142f,110f,110f,6f,cells=(0..3).map{PrintCell(7f+(it%2)*57f,6f+(it/2)*57f,53f,53f)})
  STRIP->PrintGeometry(label,64f,262f,54f,228f,5f,cells=(0..3).map{PrintCell(5f,5f+it*58f,54f,54f)})
 }
}
interface CaptureClock {fun nowMs(): Long;suspend fun sleep(ms: Long)}

/** Requests are scheduled from monotonic deadlines, not five seconds after processing.
 * Camera hardware/AE may delay shutter callbacks; captures are never overlapped. */
class CaptureSequence(private val clock: CaptureClock) {
 suspend fun run(shots: Int, delaySeconds: Int, chargeMs: Long, countdown: (Int,Int)->Unit, charge: ()->Unit, capture: suspend (Int)->Unit) {
  require(shots==1 || shots==4);require(delaySeconds in listOf(0,5,15,30));require(chargeMs in 0..5000)
  var deadline=clock.nowMs()+if(delaySeconds==0)chargeMs else delaySeconds*1000L
  repeat(shots) {index->
   val chargeAt=deadline-chargeMs
   var charged=false;var previous=-1
   while(true) {
    val now=clock.nowMs()
    val left=(deadline-now).coerceAtLeast(0)
    val seconds=((left+999)/1000).toInt()
    if(seconds!=previous){countdown(index,if(delaySeconds==0 && shots==1)0 else seconds);previous=seconds}
    if(!charged && now>=chargeAt){charged=true;charge()}
    if(left==0L)break
    val boundary=deadline-(seconds-1)*1000L
    val next=if(!charged)minOf(boundary,chargeAt) else boundary
    clock.sleep((next-now).coerceAtLeast(1))
   }
   val requestedAt=clock.nowMs()
   capture(index)
   deadline=maxOf(requestedAt+5000L,clock.nowMs()+chargeMs)
  }
 }
}
