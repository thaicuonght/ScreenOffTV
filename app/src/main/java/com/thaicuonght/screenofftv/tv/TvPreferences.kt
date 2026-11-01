package com.thaicuonght.screenofftv.tv

import android.content.Context

class TvPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("tv_control", Context.MODE_PRIVATE)
    var autoStart: Boolean
        get() = preferences.getBoolean("auto_start", true)
        set(value) { preferences.edit().putBoolean("auto_start", value).apply() }
    var remoteKey: Int
        get() = preferences.getInt("remote_key", 0x71)
        set(value) { preferences.edit().putInt("remote_key", value).apply() }
    var pressMode: RemoteGestureDetector.Mode
        get() = runCatching { RemoteGestureDetector.Mode.valueOf(preferences.getString("press_mode", "Double")!!) }
            .getOrDefault(RemoteGestureDetector.Mode.Double)
        set(value) { preferences.edit().putString("press_mode", value.name).apply() }
}
