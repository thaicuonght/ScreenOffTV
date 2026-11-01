package com.thaicuonght.screenofftv.service.hosts

import com.thaicuonght.screenofftv.i18n.UiText
import com.thaicuonght.screenofftv.R
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.thaicuonght.screenofftv.ipc.result.EventResult
import com.thaicuonght.screenofftv.shizuku.IEventsListener
import com.thaicuonght.screenofftv.shizuku.IEventsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import com.thaicuonght.screenofftv.tv.RemoteGestureDetector
import com.thaicuonght.screenofftv.tv.RemoteKeys
import com.thaicuonght.screenofftv.tv.TvPreferences

class VolumeKeyEventShizukuHost(owner: Context, val service: IEventsProvider, var onKeyEvent: () -> Unit = {}) {
    companion object {
        val mutePressCount = MutableStateFlow(0) // Kept for existing observers; counts the selected shortcut.
        val captureActive = MutableStateFlow(false)
        val capturedKey = MutableStateFlow<Int?>(null)
        val captureError = MutableStateFlow<UiText?>(null)
        @Volatile private var captureArmedAt = 0L
        @Volatile var inputReady = false
        fun beginCapture() {
            capturedKey.value = null
            captureError.value = null
            captureArmedAt = SystemClock.elapsedRealtime() + 300L
            captureActive.value = true
        }
        fun pauseCapture() { captureArmedAt = Long.MAX_VALUE }
        fun endCapture() { captureActive.value = false }
    }
    private val preferences = TvPreferences(owner)
    private val detector = RemoteGestureDetector()
    private val handler = Handler(Looper.getMainLooper())
    private var configuration = ""
    private var inputDevice: String? = null
    var isRegister = false
        private set
    @Volatile var isAwake = false
        private set

    private val listener = object : IEventsListener.Stub() {
        override fun onEvent(event: EventResult) {
            if (event.v0 != "0001") return
            val code = event.v1.toIntOrNull(16) ?: return
            val value = event.v2.toIntOrNull(16) ?: return
            val now = SystemClock.elapsedRealtime()
            if (captureActive.value) {
                detector.reset()
                if (value == 1 && now >= captureArmedAt && capturedKey.value == null) {
                    if (code in RemoteKeys.reserved) {
                        captureError.value = UiText(R.string.reserved_key_error)
                    } else {
                        captureError.value = null
                        capturedKey.value = code
                    }
                }
                return
            }
            val key = preferences.remoteKey
            val mode = preferences.pressMode
            val config = "$key:$mode"
            if (configuration != config) {
                configuration = config
                detector.reset()
                inputDevice = null
                handler.removeCallbacksAndMessages(null)
            }
            if (code != key) return
            if (value == 1) mutePressCount.value += 1
            if (!isAwake) { detector.reset(); return }
            if (inputDevice != null && inputDevice != event.event) detector.reset()
            inputDevice = event.event
            if (detector.onKey(value, now, mode)) onKeyEvent()
            if (mode == RemoteGestureDetector.Mode.Hold && value == 1) {
                handler.postDelayed({
                    if (isAwake && !captureActive.value &&
                        preferences.remoteKey == key && preferences.pressMode == mode &&
                        detector.onHoldTimeout(SystemClock.elapsedRealtime())) onKeyEvent()
                }, 600L)
            }
        }
    }
    fun register() {
        if (isRegister) return
        service.registerListener(listener)
        isRegister = true
        isAwake = true
        inputReady = true
    }
    fun unregister() {
        isAwake = false
        inputReady = false
        endCapture()
        handler.removeCallbacksAndMessages(null)
        detector.reset()
        if (!isRegister) return
        service.unregisterListener(listener)
        isRegister = false
    }
    fun sleep() { isAwake = false; detector.reset() }
    fun wake() { if (!isAwake) detector.reset(); isAwake = true }
}
