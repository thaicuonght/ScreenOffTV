package com.thaicuonght.screenofftv.tv

import org.junit.Assert.*
import org.junit.Test

class RemoteGestureDetectorTest {
    @Test fun singlePressFiresOnceUntilRelease() {
        val detector = RemoteGestureDetector()
        assertTrue(detector.onKey(1, 100, RemoteGestureDetector.Mode.Single))
        assertFalse(detector.onKey(2, 150, RemoteGestureDetector.Mode.Single))
        assertFalse(detector.onKey(1, 160, RemoteGestureDetector.Mode.Single))
        assertFalse(detector.onKey(0, 200, RemoteGestureDetector.Mode.Single))
        assertTrue(detector.onKey(1, 500, RemoteGestureDetector.Mode.Single))
    }

    @Test fun doublePressFiresOnSecondDown() {
        val detector = RemoteGestureDetector()
        assertFalse(detector.onKey(1, 100, RemoteGestureDetector.Mode.Double))
        assertFalse(detector.onKey(0, 120, RemoteGestureDetector.Mode.Double))
        assertTrue(detector.onKey(1, 300, RemoteGestureDetector.Mode.Double))
        assertFalse(detector.onKey(2, 320, RemoteGestureDetector.Mode.Double))
    }

    @Test fun doublePressTogglesImmediatelyOnEachSecondDownWithoutWaitingForRelease() {
        val detector = RemoteGestureDetector()
        var screenOn = true
        fun key(value: Int, time: Long) {
            if (detector.onKey(value, time, RemoteGestureDetector.Mode.Double)) screenOn = !screenOn
        }
        key(1, 100); key(0, 110)
        assertTrue(screenOn)
        key(1, 120)
        assertFalse(screenOn) // No release or hold timeout has occurred.
        key(0, 130)
        key(1, 140); key(0, 150)
        assertFalse(screenOn)
        key(1, 160)
        assertTrue(screenOn) // Even a rapid second pair restores immediately; no extra 300 ms dead period.
    }

    @Test fun slowDoublePressDoesNotFire() {
        val detector = RemoteGestureDetector()
        assertFalse(detector.onKey(1, 100, RemoteGestureDetector.Mode.Double))
        detector.onKey(0, 120, RemoteGestureDetector.Mode.Double)
        assertFalse(detector.onKey(1, 700, RemoteGestureDetector.Mode.Double))
    }

    @Test fun holdRequiresSixHundredMillisecondsAndFiresOnce() {
        val detector = RemoteGestureDetector()
        assertFalse(detector.onKey(1, 100, RemoteGestureDetector.Mode.Hold))
        assertFalse(detector.onHoldTimeout(699))
        assertTrue(detector.onHoldTimeout(700))
        assertFalse(detector.onHoldTimeout(900))
    }

    @Test fun releaseCancelsPendingHold() {
        val detector = RemoteGestureDetector()
        detector.onKey(1, 100, RemoteGestureDetector.Mode.Hold)
        detector.onKey(0, 500, RemoteGestureDetector.Mode.Hold)
        assertFalse(detector.onHoldTimeout(700))
    }

    @Test fun resetDiscardsPendingGesture() {
        val detector = RemoteGestureDetector()
        detector.onKey(1, 100, RemoteGestureDetector.Mode.Double)
        detector.onKey(0, 120, RemoteGestureDetector.Mode.Double)
        detector.reset()
        assertFalse(detector.onKey(1, 300, RemoteGestureDetector.Mode.Double))
        detector.reset()
        assertFalse(detector.onHoldTimeout(1000))
    }

    @Test fun remotePresetsDoNotUseReservedNavigationOrPowerKeys() {
        assertTrue(RemoteKeys.presets.all { (code, _) -> code !in RemoteKeys.reserved })
        assertTrue(0x74 in RemoteKeys.reserved) // TV power
        assertTrue(0x1c in RemoteKeys.reserved) // OK / Enter
        assertEquals("Mute", RemoteKeys.label(0x71))
    }
}
