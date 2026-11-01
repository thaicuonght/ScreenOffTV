package com.thaicuonght.screenofftv

import com.thaicuonght.screenofftv.i18n.UiText
import com.thaicuonght.screenofftv.R
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.thaicuonght.screenofftv.i18n.AppLanguage
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.thaicuonght.screenofftv.service.ScreenOffTVService
import com.thaicuonght.screenofftv.settings.data.settingsDataStore
import com.thaicuonght.screenofftv.settings.data.settingsRepository
import com.thaicuonght.screenofftv.tv.TvDashboard
import com.thaicuonght.screenofftv.ui.theme.ScreenOffTVTheme
import rikka.shizuku.Shizuku
import rikka.sui.Sui

class MainActivity : ComponentActivity() {
    private var appLanguage by mutableStateOf("system")
    private var ready by mutableStateOf(false)
    private var granted by mutableStateOf(false)
    private var message by mutableStateOf<UiText?>(null)
    private var pendingStart = false
    private var awaitingNotification = false
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == 100) runOnUiThread {
            refresh()
            if (pendingStart && result == PackageManager.PERMISSION_GRANTED) startWithNotification()
            else if (pendingStart) {
                pendingStart = false
                message = UiText(R.string.permission_request_denied)
            }
        }
    }
    private val binderListener = Shizuku.OnBinderReceivedListener { runOnUiThread { refresh() } }
    private val deadListener = Shizuku.OnBinderDeadListener { runOnUiThread { refresh() } }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        awaitingNotification = false
        if (pendingStart) {
            pendingStart = false
            startServiceSafely()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingStart = savedInstanceState?.getBoolean("pending_start") ?: false
        awaitingNotification = savedInstanceState?.getBoolean("awaiting_notification") ?: false
        appLanguage = AppLanguage.saved(this)
        Sui.init(packageName)
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        Shizuku.addBinderDeadListener(deadListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        val settings = settingsRepository(settingsDataStore)
        setContent {
            val configuration = LocalConfiguration.current
            val localizedContext = remember(appLanguage, configuration) { AppLanguage.wrap(this@MainActivity, appLanguage) }
            CompositionLocalProvider(LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration) {
            ScreenOffTVTheme(darkTheme = true, dynamicColor = false) {
                TvDashboard(
                    settings = settings,
                    language = appLanguage,
                    onLanguageChange = { language ->
                        AppLanguage.save(this, language)
                        appLanguage = language
                        createNotificationChannels()
                        if (ScreenOffTVService.state.value == ScreenOffTVService.State.Prepared) {
                            startServiceSafely(Intent(this, ScreenOffTVService::class.java)
                                .setAction(ScreenOffTVService.ACTION_LANGUAGE_CHANGED))
                        }
                    },
                    shizukuReady = ready,
                    shizukuGranted = granted,
                    message = message,
                    onStart = { requestStart() },
                    onStop = {
                        pendingStart = false
                        ScreenOffTVService.state.value = ScreenOffTVService.State.Stopping
                        if (!stopService(Intent(this, ScreenOffTVService::class.java))) {
                            ScreenOffTVService.state.value = ScreenOffTVService.State.Destroyed
                        }
                    },
                    onScreenOff = {
                        startServiceSafely(Intent(this, ScreenOffTVService::class.java).putExtra(
                            ScreenOffTVService.EXTRA_SCREEN, ScreenOffTVService.EXTRA_VALUE_SCREEN_OFF
                        ))
                    },
                    onOpenShizuku = { openShizuku() },
                    onRefresh = {
                        refresh()
                        when {
                            !ready -> message = UiText(R.string.shizuku_not_started)
                            !granted -> message = UiText(R.string.shizuku_not_authorized)
                            ScreenOffTVService.state.value == ScreenOffTVService.State.Destroyed ->
                                message = UiText(R.string.connected_service_off)
                            else -> {
                                message = null
                                ScreenOffTVService.connectionReport.value = UiText(R.string.checking_connection)
                                startServiceSafely(Intent(this, ScreenOffTVService::class.java)
                                    .putExtra(ScreenOffTVService.EXTRA_CHECK_CONNECTION, true))
                            }
                        }
                    },
                )
            }
            }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("pending_start", pendingStart)
        outState.putBoolean("awaiting_notification", awaitingNotification)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        refresh()
        if (pendingStart && granted && !awaitingNotification) startWithNotification()
    }
    private fun refresh() {
        ready = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        granted = ready && runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)
    }
    private fun requestStart() {
        if (pendingStart || awaitingNotification) return
        refresh()
        message = null
        if (!ready) { message = UiText(R.string.start_shizuku_first); return }
        pendingStart = true
        if (!granted) {
            runCatching { Shizuku.requestPermission(100) }.onFailure {
                pendingStart = false
                message = UiText(R.string.permission_request_failed)
            }
            return
        }
        startWithNotification()
    }
    private fun startWithNotification() {
        if (awaitingNotification) return
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            awaitingNotification = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            pendingStart = false
            startServiceSafely()
        }
    }
    private fun startServiceSafely(intent: Intent = Intent(this, ScreenOffTVService::class.java)) {
        runCatching { ContextCompat.startForegroundService(this, intent) }.onFailure {
            message = UiText(R.string.service_start_failed, it.localizedMessage.orEmpty())
        }
    }
    private fun openShizuku() {
        val intent = packageManager.getLeanbackLaunchIntentForPackage("moe.shizuku.privileged.api")
            ?: packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
        if (intent == null) { message = UiText(R.string.shizuku_not_installed); return }
        runCatching { startActivity(intent) }.onFailure { message = UiText(R.string.shizuku_open_failed) }
    }
    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderListener)
        Shizuku.removeBinderDeadListener(deadListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        super.onDestroy()
    }
}
