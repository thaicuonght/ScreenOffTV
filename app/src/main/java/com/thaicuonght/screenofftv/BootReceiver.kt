package com.thaicuonght.screenofftv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.thaicuonght.screenofftv.service.ScreenOffTVService
import com.thaicuonght.screenofftv.tv.TvPreferences

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!TvPreferences(context).autoStart) return
        // The service waits for Shizuku after entering foreground; each service binding attempt has a bounded timeout.
        runCatching {
            ContextCompat.startForegroundService(context, Intent(context, ScreenOffTVService::class.java))
        }.onFailure { Log.e("BootReceiver", "Cannot start TV service after boot", it) }
    }
}
