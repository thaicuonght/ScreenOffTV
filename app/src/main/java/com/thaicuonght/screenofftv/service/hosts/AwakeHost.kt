package com.thaicuonght.screenofftv.service.hosts

import android.content.Context
import android.os.PowerManager

/** Keep Android awake while SurfaceControl turns the physical panel off. */
@Suppress("DEPRECATION")
class AwakeHost(context: Context) {
    private val lock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
        .newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "ScreenOffTV:panel-off")
        .apply { setReferenceCounted(false) }
    fun create() = Unit
    fun startKeepAwake() { if (!lock.isHeld) lock.acquire() }
    fun stopKeepAwake() { if (lock.isHeld) lock.release() }
    fun destroy() = stopKeepAwake()
}
