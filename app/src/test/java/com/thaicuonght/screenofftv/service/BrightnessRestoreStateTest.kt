package com.thaicuonght.screenofftv.service

import org.junit.Assert.*
import org.junit.Test

class BrightnessRestoreStateTest {
    @Test fun standbyWithoutCaptureDoesNotWriteTvSettings() {
        val state = BrightnessRestoreState()
        state.restore({ fail("Brightness was never captured") }, { fail("Mode was never captured") })
        assertFalse(state.pending)
    }

    @Test fun successfulRestoreWritesOriginalValuesOnlyOnce() {
        val state = BrightnessRestoreState()
        val writes = mutableListOf<String>()
        state.capture(83, 1)
        repeat(2) { state.restore({ writes += "brightness=$it" }, { writes += "mode=$it" }) }
        assertEquals(listOf("brightness=83", "mode=1"), writes)
        assertFalse(state.pending)
    }

    @Test fun failedBrightnessWritePreservesSnapshotForRetry() {
        val state = BrightnessRestoreState()
        state.capture(83, 1)
        try {
            state.restore({ error("Shizuku disconnected") }, { fail("Mode must wait for brightness") })
            fail("Expected failed write")
        } catch (_: IllegalStateException) { }
        assertTrue(state.pending)
        assertEquals(83, state.brightness)
        var restored = 0
        state.restore({ restored = it }, {})
        assertEquals(83, restored)
        assertFalse(state.pending)
    }

    @Test fun failedModeWriteRetriesOriginalAutomaticMode() {
        val state = BrightnessRestoreState()
        state.capture(83, 1)
        try {
            state.restore({}, { error("Mode write failed") })
            fail("Expected failed mode write")
        } catch (_: IllegalStateException) { }
        var restoredMode = -1
        state.restore({}, { restoredMode = it })
        assertEquals(1, restoredMode)
        assertFalse(state.pending)
    }

    @Test fun newCaptureCannotOverwritePendingOriginalSettings() {
        val state = BrightnessRestoreState()
        state.capture(83, 1)
        try {
            state.capture(255, 0)
            fail("Pending original settings must not be overwritten")
        } catch (_: IllegalStateException) { }
        var restoredMode = -1
        state.restore({ assertEquals(83, it) }, { restoredMode = it })
        assertEquals(1, restoredMode)
        state.capture(100, 0)
        assertEquals(100, state.brightness)
    }

    @Test fun disabledManualCompatibilityDoesNotWriteBrightnessMode() {
        val state = BrightnessRestoreState()
        state.capture(83, null)
        state.restore({ assertEquals(83, it) }, { fail("Mode must remain untouched") })
        assertFalse(state.pending)
    }

    @Test fun originalSettingsSurviveProcessRecreation() {
        var disk: BrightnessRestoreState.Snapshot? = null
        val beforeCrash = BrightnessRestoreState(persist = { disk = it })
        beforeCrash.capture(83, 1)
        val afterRestart = BrightnessRestoreState(disk, { disk = it })
        var restoredMode = -1
        afterRestart.restore({ assertEquals(83, it) }, { restoredMode = it })
        assertEquals(1, restoredMode)
        assertNull(disk)
    }

    @Test fun failedJournalSavePreventsCapture() {
        val state = BrightnessRestoreState(persist = { error("Storage full") })
        try { state.capture(83, 1); fail("Expected journal failure") }
        catch (_: IllegalStateException) { }
        assertFalse(state.pending)
    }

    @Test fun failedJournalClearKeepsOriginalValuesForNextRestart() {
        val saved = BrightnessRestoreState.Snapshot(83, 1)
        val state = BrightnessRestoreState(saved, { error("Storage failure") })
        try { state.restore({}, {}); fail("Expected journal failure") }
        catch (_: IllegalStateException) { }
        assertTrue(state.pending)
        assertEquals(83, state.brightness)
    }
}
