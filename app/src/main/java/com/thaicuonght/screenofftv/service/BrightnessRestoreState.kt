package com.thaicuonght.screenofftv.service

/** Original TV settings survive failed writes until the whole restoration succeeds. */
internal class BrightnessRestoreState {
    private data class Snapshot(val brightness: Int, val mode: Int?)
    private var snapshot: Snapshot? = null

    val pending: Boolean get() = snapshot != null
    val brightness: Int? get() = snapshot?.brightness

    fun capture(brightness: Int, mode: Int?) {
        check(snapshot == null) { "Restore the previous brightness settings before capturing new ones" }
        snapshot = Snapshot(brightness, mode)
    }

    fun restore(writeBrightness: (Int) -> Unit, writeMode: (Int) -> Unit) {
        val original = snapshot ?: return
        writeBrightness(original.brightness)
        original.mode?.let(writeMode)
        snapshot = null
    }
}
