package com.instantroom

import com.instantroom.core.*
import kotlinx.coroutines.*
import org.junit.Test
import org.junit.Assert.*

class CaptureSequenceTest {
 private class Clock: CaptureClock {var time=0L;override fun nowMs()=time;override suspend fun sleep(ms:Long){time+=ms}}
 @Test fun timerOffAddsNoCountdownDelay()=runBlocking<Unit>{val c=Clock();val calls=mutableListOf<Long>();CaptureSequence(c).run(1,0,0,{_,_->},{}){calls+=c.time};assertEquals(listOf(0L),calls)}
 @Test fun allThreeTimersRequestCaptureAtExactDeadline()=runBlocking<Unit>{
  for(seconds in listOf(5,15,30)){val c=Clock();val ticks=mutableListOf<Int>();var charge=0L;var capture=0L
   CaptureSequence(c).run(1,seconds,650,{_,v->ticks+=v},{charge=c.time}){capture=c.time}
   assertEquals(seconds*1000L,capture);assertEquals(capture-650,charge);assertEquals((seconds downTo 0).toList(),ticks)
  }
 }
 @Test fun fourRequestsAreFiveSecondsApartDespiteStorageLatency()=runBlocking<Unit>{
  val c=Clock();val times=mutableListOf<Long>();val shots=mutableSetOf<Int>()
  CaptureSequence(c).run(4,5,650,{n,_->shots+=n},{}){times+=c.time;c.time+=240}
  assertEquals(listOf(5000L,10000L,15000L,20000L),times);assertEquals(setOf(0,1,2,3),shots)
 }
 @Test fun cancellingCountdownNeverInvokesCamera()=runBlocking<Unit>{
  val c=object:CaptureClock {override fun nowMs()=0L;override suspend fun sleep(ms:Long){throw CancellationException("Pause")}}
  var captures=0;assertTrue(runCatching{CaptureSequence(c).run(1,30,650,{_,_->},{}){captures++}}.exceptionOrNull() is CancellationException);assertEquals(0,captures)
 }
 @Test fun failedShotStopsRemainingRequests()=runBlocking<Unit>{val c=Clock();var calls=0;assertTrue(runCatching{CaptureSequence(c).run(4,5,650,{_,_->},{}){calls++;if(calls==2)error("Camera failure")}}.isFailure);assertEquals(2,calls)}
 @Test fun slowHardwareNeverOverlapsCapturesAndStillAllowsCharging()=runBlocking<Unit>{val c=Clock();val times=mutableListOf<Long>();CaptureSequence(c).run(4,5,650,{_,_->},{}){times+=c.time;c.time+=8000};assertTrue(times.zipWithNext().all{(a,b)->b-a>=8650})}
 @Test fun printLayoutsHaveFourOrderedNonOverlappingCellsAndCaptionSpace(){
  for(layout in BoothLayout.entries){val g=layout.geometry;assertEquals(4,g.imageRegions.size);assertTrue(g.height-g.top-g.imageHeight>5)
   for(cell in g.cells){assertTrue(cell.left>=0 && cell.top>=0);assertTrue(cell.left+cell.width<g.width);assertTrue(cell.top+cell.height<=g.top+g.imageHeight)}
  }
  assertTrue(BoothLayout.STRIP.geometry.aspect<.3f);assertTrue(BoothLayout.GRID.geometry.aspect>.8f)
 }
 @Test fun invalidSequenceSizeIsRejected()=runBlocking<Unit>{assertTrue(runCatching{CaptureSequence(Clock()).run(3,5,650,{_,_->},{}){}}.isFailure)}
}
