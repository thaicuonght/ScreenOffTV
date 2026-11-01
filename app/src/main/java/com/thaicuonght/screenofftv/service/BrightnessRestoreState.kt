package com.thaicuonght.screenofftv.service

/** Save the original values durably before the first system-setting write. */
internal class BrightnessRestoreState(
    initial: Snapshot? = null,
    private val persist: (Snapshot?) -> Unit = {},
) {
    data class Snapshot(val brightness: Int, val mode: Int?)
    private var snapshot = initial

    val pending: Boolean get() = snapshot != null
    val brightness: Int? get() = snapshot?.brightness

    fun capture(brightness: Int, mode: Int?) {
        check(snapshot == null) { "Restore the previous brightness settings before capturing new ones" }
        val original = Snapshot(brightness, mode)
        persist(original)
        snapshot = original
    }

    fun restore(writeBrightness: (Int) -> Unit, writeMode: (Int) -> Unit) {
        val original = snapshot ?: return
        writeBrightness(original.brightness)
        original.mode?.let(writeMode)
        persist(null)
        snapshot = null
    }
}
