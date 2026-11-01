package com.thaicuonght.screenofftv.tv

/** Double press fires on the second DOWN, without waiting for its release. */
class RemoteGestureDetector {
    enum class Mode {
        Single, Double, Hold
    }
    private var down = false
    private var downAt = 0L
    private var firstDown: Long? = null
    private var fired = false
    private var blockedUntil = 0L

    @Synchronized
    fun onKey(value: Int, now: Long, mode: Mode): Boolean {
        if (value == 0) { down = false; fired = false; return false }
        if (value != 1 || down) return false
        down = true
        downAt = now
        fired = false
        if (now < blockedUntil) return false
        val trigger = when (mode) {
            Mode.Single -> true
            Mode.Double -> {
                val first = firstDown
                if (first != null && now - first in 1..450L) {
                    firstDown = null
                    true
                } else { firstDown = now; false }
            }
            Mode.Hold -> false
        }
        if (trigger) {
            fired = true
            // Two complete presses already delimit a double gesture. Do not suppress the next pair.
            blockedUntil = if (mode == Mode.Double) now else now + 300L
        }
        return trigger
    }

    @Synchronized
    fun onHoldTimeout(now: Long): Boolean {
        if (!down || fired || now < blockedUntil || now - downAt < 600L) return false
        fired = true
        blockedUntil = now + 300L
        return true
    }

    @Synchronized
    fun reset() { down = false; firstDown = null; fired = false; blockedUntil = 0L }
}

object RemoteKeys {
    val presets = listOf(0x71 to "Mute", 0x73 to "Volume up", 0x72 to "Volume down")
    fun label(code: Int) = presets.firstOrNull { it.first == code }?.second
        ?: "Key (0x${code.toString(16)})"
    // Do not let a shortcut interfere with essential navigation or TV power.
    val reserved = setOf(0x74, 0x8e, 0x8f, 0x66, 0x67, 0x69, 0x6a, 0x6c, 0x1c, 0x160, 0x9e, 0xae)
}
