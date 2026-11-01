package com.thaicuonght.screenofftv.shizuku

import android.os.Build
import android.os.IBinder
import android.util.Log
import com.thaicuonght.screenofftv.hiddenapi.server.display.DisplayControlProxy
import com.thaicuonght.screenofftv.hiddenapi.view.SurfaceControlProxy
import com.thaicuonght.screenofftv.ipc.result.UnitResult
import kotlin.system.exitProcess

class DisplayControlService : IDisplayControl.Stub() {
    companion object {

        const val TAG = "DisplayControlService"

        const val POWER_MODE_OFF = SurfaceControlProxy.POWER_MODE_OFF
        const val POWER_MODE_NORMAL = SurfaceControlProxy.POWER_MODE_NORMAL

        const val BRIGHTNESS_MODE_MANUAL = 0
        const val BRIGHTNESS_MODE_AUTO = 1
    }

    private val settingsCommands = ShellCommandRunner()

    private val primaryDisplayToken: IBinder by lazy { resolvePrimaryDisplayToken() }

    private fun getPrimaryPhysicalDisplayToken(): IBinder = primaryDisplayToken

    private fun resolvePrimaryDisplayToken(): IBinder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val primaryPhysicalDisplayId = DisplayControlProxy.getPhysicalDisplayIds()[0]
            return DisplayControlProxy.getPhysicalDisplayToken(primaryPhysicalDisplayId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return SurfaceControlProxy.getInternalDisplayToken()
        }
        return SurfaceControlProxy.getBuiltInDisplay()
    }

    override fun prepare(): UnitResult = try {
        getPrimaryPhysicalDisplayToken()
        UnitResult.Ok()
    } catch (e: Exception) { UnitResult.Err(e) }

    override fun setPowerModeToSurfaceControl(mode: Int): UnitResult {
        try {
            val displayToken = getPrimaryPhysicalDisplayToken()
            SurfaceControlProxy.setDisplayPowerMode(
                displayToken, mode
            )
        } catch (e: Exception) {
            return UnitResult.Err(e)
        }
        return UnitResult.Ok()
    }

    override fun setBrightnessToSurfaceControl(brightness: Float): UnitResult {
        try {
            val displayToken = getPrimaryPhysicalDisplayToken()
            if (!SurfaceControlProxy.setDisplayBrightness(displayToken, brightness)) {
                return UnitResult.Err(UnsupportedOperationException("This TV does not support brightness control with this method."))
            }
        } catch (e: Exception) {
            return UnitResult.Err(e)
        }
        return UnitResult.Ok()
    }

    override fun setBrightnessToSetting(brightness: Int): UnitResult = try {
        settingsCommands.putSystemInt("screen_brightness", brightness)
        UnitResult.Ok()
    } catch (e: Exception) { UnitResult.Err(e) }

    override fun setBrightnessModeToSetting(mode: Int): UnitResult = try {
        require(mode == BRIGHTNESS_MODE_MANUAL || mode == BRIGHTNESS_MODE_AUTO) { "mode should be 0 or 1" }
        settingsCommands.putSystemInt("screen_brightness_mode", mode)
        UnitResult.Ok()
    } catch (e: Exception) { UnitResult.Err(e) }

    override fun destroy() {
        Log.d(TAG, "destroy: $TAG")
        exitProcess(0)
    }

}
