package com.instantroom.core

import org.junit.Test
import org.junit.Assert.*

class CaptionRulesTest {
 @Test fun emptyCaptionRemovesText(){assertEquals("",CaptionRules.normalize(" \n "))}
 @Test fun twoLinesNormalizeAndPreserveUnicode(){assertEquals("Café by the sea\nНетания",CaptionRules.normalize(" Café by the sea\r\nНетания "))}
 @Test fun overlongCaptionsAreRejected(){assertTrue(runCatching{CaptionRules.normalize("a".repeat(81))}.isFailure)}
 @Test fun moreThanTwoExplicitLinesAreRejected(){assertTrue(runCatching{CaptionRules.normalize("one\ntwo\nthree")}.isFailure)}
 @Test fun controlCharactersAreRejected(){assertTrue(runCatching{CaptionRules.normalize("a\u0000b")}.isFailure)}
 @Test fun surrogatePairsCountAsSingleCharacters(){assertEquals(80,CaptionRules.normalize("😀".repeat(80)).codePointCount(0,160))}
 @Test fun hapticsRequireCommittedProgressAndActiveEnabledSession(){val h=DevelopmentHaptics();assertFalse(h.shouldPulse(0,false,true,true));assertFalse(h.shouldPulse(0,true,false,true));assertFalse(h.shouldPulse(0,true,true,false));assertTrue(h.shouldPulse(0,true,true,true))}
 @Test fun hapticsAreThrottledRatherThanContinuous(){val h=DevelopmentHaptics();assertTrue(h.shouldPulse(100,true,true,true));assertFalse(h.shouldPulse(210,true,true,true));assertTrue(h.shouldPulse(360,true,true,true))}
 @Test fun restingAndCompletedPhotosCannotProducePulses(){val h=DevelopmentHaptics();h.shouldPulse(0,true,true,true);assertFalse(h.shouldPulse(10000,false,true,true))}
 @Test fun newSessionResetsThrottle(){val h=DevelopmentHaptics();h.shouldPulse(100,true,true,true);h.reset();assertTrue(h.shouldPulse(110,true,true,true))}
}
