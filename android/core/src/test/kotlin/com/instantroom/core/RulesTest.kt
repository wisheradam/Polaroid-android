package com.instantroom.core

import org.junit.Assert.*
import org.junit.Test

class RulesTest {
 private fun cart()=Cartridge("test",FilmFormat.CLASSIC,Appearance.COLOR)
 @Test fun newCartridgeHasTenExposures(){assertEquals(10,cart().remaining)}
 @Test fun exactlyTenSuccessfulCapturesExhaust(){var c=cart();repeat(10){c=c.expose()};assertEquals(0,c.remaining)}
 @Test(expected=IllegalStateException::class) fun emptyBlocksCapture(){cart().copy(remaining=0).expose()}
 @Test fun manualReplacementRestoresTen(){val old=cart().copy(remaining=0);assertEquals(10,Cartridge("new",old.format,old.appearance).remaining)}
 @Test fun exposurePreservesFilmFormat(){assertEquals(FilmFormat.SPECTRA,cart().copy(format=FilmFormat.SPECTRA).expose().format)}
 @Test fun loadingRequiresPhysicalSteps(){val l=LoadingMachine();l.open();l.remove();l.insert();l.close();assertEquals(LoadingPhase.CLOSED,l.phase)}
 @Test(expected=IllegalStateException::class) fun loadingCannotSkipRemoval(){LoadingMachine().insert()}
 @Test fun zeroInputNeverDevelops(){var p=.2f;repeat(10000){p=Development.acceptedShake(p,0f)};assertEquals(.2f,p,0f)}
 @Test fun acceptedInputDevelops(){assertTrue(Development.acceptedShake(0f,.5f)>0f)}
 @Test fun strongerShakesDevelopFaster(){assertTrue(Development.acceptedShake(.2f,1.2f)>Development.acceptedShake(.2f,.4f))}
 @Test fun progressClamped(){var p=0f;repeat(10000){p=Development.acceptedShake(p,1.4f)};assertEquals(1f,p,0f)}
 @Test fun rejectsNonFiniteInput(){assertEquals(.3f,Development.acceptedShake(.3f,Float.NaN),0f)}
 @Test fun accessibilityRequiresOptIn(){assertEquals(.2f,Development.accessiblePress(.2f,false),0f);assertTrue(Development.accessiblePress(.2f,true)>.2f)}
 @Test fun gentleMovementRejected(){val s=ShakeDetector();repeat(1000){assertEquals(0f,s.sample(1_000_000_000L+it*20_000_000L,2f,-1f,.5f),0f)}}
 @Test fun singleImpactRejected(){val s=ShakeDetector();assertEquals(0f,s.sample(1_000_000_000L,15f,0f,0f),0f);assertEquals(0f,s.sample(1_100_000_000L,0f,0f,0f),0f)}
 @Test fun alternatingPeaksAccepted(){val s=ShakeDetector();s.sample(1_000_000_000L,12f,0f,0f);assertTrue(s.sample(1_150_000_000L,-12f,0f,0f)>0f)}
 @Test fun sameDirectionPeaksRejected(){val s=ShakeDetector();s.sample(1_000_000_000L,12f,0f,0f);assertEquals(0f,s.sample(1_150_000_000L,12f,0f,0f),0f)}
 @Test fun longGapDoesNotCount(){val s=ShakeDetector();s.sample(1_000_000_000L,12f,0f,0f);assertEquals(0f,s.sample(5_000_000_000L,-12f,0f,0f),0f)}
 @Test fun sensorResetDiscardsOldPeaks(){val s=ShakeDetector();s.sample(1_000_000_000L,12f,0f,0f);s.reset();assertEquals(0f,s.sample(1_150_000_000L,-12f,0f,0f),0f)}
 @Test fun noSamplesMeansNoDevelopment(){val p=.42f;val s=ShakeDetector();s.reset();assertEquals(.42f,p,0f)}
 @Test fun shakesTooCloseAreRejected(){val s=ShakeDetector();s.sample(1_000_000_000L,14f,0f,0f);assertEquals(0f,s.sample(1_020_000_000L,-14f,0f,0f),0f)}
 @Test fun pinLocksPositionAndRotation(){val p=Placement(pinned=true);assertEquals(p,p.move(50f,10f,30f))}
 @Test fun unpinUnlocks(){val p=Placement(pinned=true).copy(pinned=false);assertEquals(150f,p.move(50f,10f,30f).x,0f)}
 @Test fun boardCoordinatesRemainBounded(){val p=Placement().move(-9999f,9999f,999f);assertEquals(0f,p.x,0f);assertEquals(1400f,p.y,0f);assertEquals(180f,p.rotation,0f)}
 @Test fun ejectionEndsAtFinalClick(){assertEquals(1800L,EjectionTimeline.durationMs);assertEquals(EjectionTimeline.durationMs,EjectionTimeline.pulseTimesMs.last());assertEquals(1f,EjectionTimeline.position(1800),0f)}
 @Test fun ejectionIsMonotonic(){val positions=(0L..1900L step 20).map(EjectionTimeline::position);assertTrue(positions.zipWithNext().all{it.first<=it.second});assertEquals(0f,EjectionTimeline.position(-20),0f)}
 @Test fun filmGeometryHasLargerBottomMargin(){FilmFormat.entries.forEach{f->assertTrue(f.height-f.top-f.imageHeight>f.top);assertTrue(f.width>f.imageWidth)}}
 @Test fun miniPhysicallySmaller(){assertTrue(FilmFormat.GO.width<FilmFormat.CLASSIC.width);assertTrue(FilmFormat.GO.height<FilmFormat.CLASSIC.height)}
 @Test fun wideHasLandscapeImage(){assertTrue(FilmFormat.SPECTRA.imageAspect>1.2f)}
 @Test fun roundUsesClassicExterior(){assertEquals(FilmFormat.CLASSIC.width,FilmFormat.ROUND.width,0f);assertEquals(FilmFormat.CLASSIC.height,FilmFormat.ROUND.height,0f)}
}
