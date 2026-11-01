package com.thaicuonght.screenofftv.service

import com.thaicuonght.screenofftv.i18n.UiText
import com.thaicuonght.screenofftv.R
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.thaicuonght.screenofftv.shizuku.DisplayControlService
import com.thaicuonght.screenofftv.shizuku.EventsProviderService
import com.thaicuonght.screenofftv.shizuku.IDisplayControl
import com.thaicuonght.screenofftv.shizuku.IEventsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import com.thaicuonght.screenofftv.BuildConfig
import com.thaicuonght.screenofftv.ExceptionScenes
import com.thaicuonght.screenofftv.ISolutionDependencyManager
import com.thaicuonght.screenofftv.SolutionDependencyManager
import com.thaicuonght.screenofftv.notifyException
import com.thaicuonght.screenofftv.service.hosts.AwakeHost
import com.thaicuonght.screenofftv.service.hosts.NotificationHost
import com.thaicuonght.screenofftv.service.hosts.VolumeKeyEventShizukuHost
import com.thaicuonght.screenofftv.settings.data.ISettingsRepository
import com.thaicuonght.screenofftv.settings.data.SettingsTokens
import com.thaicuonght.screenofftv.settings.data.SettingsTokens.SolutionValue.ShizukuPowerOffScreen
import com.thaicuonght.screenofftv.settings.data.SettingsTokens.SolutionValue.ShizukuScreenBrightnessNeg1
import com.thaicuonght.screenofftv.settings.data.SyncSettingsRepository
import com.thaicuonght.screenofftv.settings.data.settingsDataStore
import rikka.shizuku.Shizuku

private const val TAG = "ScreenOffTVService"

class ScreenOffTVService : LifecycleService() {

    enum class State {
        Destroyed, Created, Prepared, Error
    }

    companion object {
        const val ACTION_LANGUAGE_CHANGED = "com.thaicuonght.screenofftv.LANGUAGE_CHANGED"
        @JvmStatic
        val state = MutableStateFlow(State.Destroyed)
        val lastError = MutableStateFlow<UiText?>(null)

        @JvmStatic
        val screenState = MutableStateFlow(ScreenState.On)

        const val EXTRA_CHECK_CONNECTION = "check_connection"
        val connectionReport = MutableStateFlow<UiText?>(null)
        const val EXTRA_SCREEN = "screen"
        const val EXTRA_VALUE_SCREEN_ON = 0
        const val EXTRA_VALUE_SCREEN_OFF = 1
        const val EXTRA_VALUE_SCREEN_SWITCH = 2

        const val EXTRA_STOP = "stop"
        const val EXTRA_VALUE_STOP = 1

        const val EXTRA_VALUE_INVALID = -1
    }

    lateinit var solutionDependencyManager: ISolutionDependencyManager

    var awakeHost: AwakeHost? = null
    var volumeKeyEventShizukuHost: VolumeKeyEventShizukuHost? = null
    val notificationHost by lazy { NotificationHost(this) }

    private val updateHostStateMutex = Mutex()

    /**
     * should be invoked in main thread.
     * */
    suspend fun updateHostState(
        requestScreenOn: Boolean
    ) {
        updateHostStateMutex.withLock {
            if (requestScreenOn) awakeHost?.stopKeepAwake()
            else awakeHost?.startKeepAwake()

            volumeKeyEventShizukuHost?.wake()

        }
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate: ")
        super.onCreate()
        solutionDependencyManager = SolutionDependencyManager(this).also {
            it.updateImmediately()
        }
        val notification = notificationHost.notify(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NotificationHost.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NotificationHost.NOTIFICATION_ID, notification)
        }
    }

    /**
     * Control ScreenOffTV Service with Intent
     *
     * ## PendingIntent Internal
     *
     * ## Shell Command External
     *
     * ```
     * adb shell am startservice -n com.thaicuonght.screenofftv/.service.ScreenOffTVService --ei [key] [int_value]
     * ```
     * */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ")
        if (intent?.action == ACTION_LANGUAGE_CHANGED) {
            notificationHost.notify(isScreenOn)
            return super.onStartCommand(intent, flags, startId)
        }

        intent?.getIntExtra(EXTRA_STOP, EXTRA_VALUE_INVALID)?.let {
            if (it == EXTRA_VALUE_STOP) {
                stopSelf()
                return super.onStartCommand(intent, flags, startId)
            }
        }

        when (state.value) {
            State.Destroyed -> initialize()
            State.Error -> initialize()
            else -> Unit
        }

        if (intent?.getBooleanExtra(EXTRA_CHECK_CONNECTION, false) == true) {
            lifecycleScope.launch {
                val displayAlive = displayControlService?.asBinder()?.isBinderAlive == true
                val inputAlive = withContext(Dispatchers.IO) {
                    runCatching { eventsProviderService?.isRunning == true }.getOrDefault(false)
                }
                connectionReport.value = when {
                    displayAlive && inputAlive -> UiText(R.string.connection_good)
                    state.value == State.Created -> UiText(R.string.connection_pending)
                    else -> UiText(R.string.connection_not_ready)
                }
            }
        }
        processArgs(intent)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun processArgs(intent: Intent?) {
        val action = intent?.getIntExtra(EXTRA_SCREEN, EXTRA_VALUE_INVALID) ?: return
        if (action !in EXTRA_VALUE_SCREEN_ON..EXTRA_VALUE_SCREEN_SWITCH) return
        lifecycleScope.launch(Dispatchers.Main.immediate) {
            if (state.value != State.Prepared) return@launch
            when (action) {
                EXTRA_VALUE_SCREEN_ON -> if (!isScreenOn) turnScreenOn()
                EXTRA_VALUE_SCREEN_OFF -> if (isScreenOn) turnScreenOff()
                EXTRA_VALUE_SCREEN_SWITCH -> if (isScreenOn) turnScreenOff() else turnScreenOn()
            }
        }
    }

    private var stopping = false
    var initializeJob: Job? = null

    private fun initialize() {
        if (initializeJob?.isActive == true) return
        initializeJob = lifecycleScope.launch(Dispatchers.Main) {
            ConnectionSupervisor(
                connect = { prepareConnection() },
                isHealthy = {
                    withContext(Dispatchers.IO) {
                        Shizuku.pingBinder() && checkShizukuPermission() &&
                            displayControlService?.asBinder()?.isBinderAlive == true &&
                            eventsProviderService?.isRunning == true
                    }
                },
                disconnect = { releaseConnection() },
                onFailure = { failure ->
                    Log.e(TAG, "Connection interrupted; retrying", failure)
                    if (!stopping) {
                        state.value = State.Created
                        lastError.value = UiText(R.string.connection_retrying)
                    }
                },
            ).run()
        }
    }

    private suspend fun prepareConnection() {
        state.value = State.Created
        lastError.value = UiText(R.string.waiting_shizuku)
        // Boot and Shizuku may finish in either order. Keep waiting until explicitly stopped.
        while (!Shizuku.pingBinder() || !runCatching { checkShizukuPermission() }.getOrDefault(false)) {
            delay(500)
        }
        lastError.value = null
        solutionDependencyManager.updateImmediately()
        val repository = SyncSettingsRepository(settingsDataStore)
        repository.update()
        loadFeatureFromSettings(repository)
        if (awakeHost == null) awakeHost = AwakeHost(this)

        bindEventsProviderService()
        bindDisplayControlService()
        withTimeout(30_000) {
            while (displayControlService == null || eventsProviderService == null) delay(10)
        }
        val display = checkNotNull(displayControlService)
        val input = checkNotNull(eventsProviderService)
        withContext(Dispatchers.IO) {
            display.prepare().withResult { name, detail -> error("$name: $detail") }
        }
        volumeKeyEventShizukuHost = VolumeKeyEventShizukuHost(this, input) {
            // The gesture fires on the second DOWN; do not wait for release or a timer.
            lifecycleScope.launch(Dispatchers.Main.immediate) {
                if (state.value != State.Prepared) return@launch
                if (isScreenOn) {
                    turnScreenOff()
                } else {
                    turnScreenOn()
                }
            }
        }.also { it.register() }
        withContext(Dispatchers.IO) { input.launch(null) }
        withTimeout(2_000) {
            while (!withContext(Dispatchers.IO) { input.isRunning }) delay(25)
        }
        // Recover a panel left off if Shizuku restarted while listening.
        if (!isScreenOn) {
            turnScreenOn()
            check(isScreenOn) { "Could not restore display after reconnecting" }
            updateHostState(true)
        }
        awakeHost?.create()
        if (systemLockReceiver == null) registerSystemLockReceiver()
        notificationHost.notify(isScreenOn)
        state.value = State.Prepared
    }

    private suspend fun releaseConnection() {
        // The supervisor runs this in NonCancellable, including when the service stops.
        screenControlMutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    if (!isScreenOn && displayControlService?.asBinder()?.isBinderAlive == true) {
                        val display = checkNotNull(displayControlService)
                        runCatching {
                            when (feature.solution) {
                                ShizukuPowerOffScreen -> display.setPowerModeToSurfaceControl(DisplayControlService.POWER_MODE_NORMAL)
                                    .withResult { name, detail -> error("$name: $detail") }
                                ShizukuScreenBrightnessNeg1 -> {
                                    display.setBrightnessToSurfaceControl(savedBrightness().coerceIn(0, 255) / 255f)
                                        .withResult { name, detail -> error("$name: $detail") }
                                }
                            }
                            screenState.value = ScreenState.On
                            awakeHost?.stopKeepAwake()
                        }.onFailure {
                            lastError.value = UiText(R.string.screen_on_failed, it.localizedMessage.orEmpty())
                        }
                    }
                    if (isScreenOn && brightnessRestore.pending) {
                        runCatching { displayControlService?.let { restoreBrightnessSettings(it) } }
                            .onFailure { lastError.value = UiText(R.string.brightness_restore_failed) }
                    }
                    runCatching { volumeKeyEventShizukuHost?.unregister() }
                    volumeKeyEventShizukuHost = null
                    runCatching { eventsProviderService?.stop() }
                    if (eventsBinding) unbindEventsProviderService()
                    if (displayBinding) unbindDisplayControlService()
                    eventsProviderService = null
                    displayControlService = null
                    eventsBinding = false
                    displayBinding = false
                } finally {
                    if (stopping) awakeHost?.destroy()
                }
            }
        }
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): ScreenOffTVService = this@ScreenOffTVService
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind: ")
        super.onBind(intent)
        if (BuildConfig.DEBUG) return binder
        return null
    }

    override fun onRebind(intent: Intent?) {
        Log.d(TAG, "onRebind: ")
        super.onRebind(intent)
    }

    override fun onDestroy() {
        stopping = true
        state.value = State.Destroyed
        initializeJob?.cancel()
        VolumeKeyEventShizukuHost.endCapture()
        unregisterSystemLockReceiver()
        solutionDependencyManager.destroy()
        if (initializeJob == null) awakeHost?.destroy()
        // ConnectionSupervisor restores the display and releases remote services without blocking Main.
        super.onDestroy()
    }

    //== Feature

    var feature = Feature(
        solution = SettingsTokens.SolutionValue.valueOf(SettingsTokens.Solution.default),
        brightnessManualWhenScreenOff = SettingsTokens.Compatibility.BrightnessManualWhenScreenOff.default,
    )

    data class Feature(
        val solution: SettingsTokens.SolutionValue,
        val brightnessManualWhenScreenOff: Boolean,
    )

    private fun loadFeatureFromSettings(repository: ISettingsRepository) {
        feature = Feature(repository.solution, repository.compatibility.brightnessManualWhenScreenOff)
    }

    //== Bind with shizuku

    fun checkShizukuPermission() =
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    var displayControlService: IDisplayControl? = null

    private val displayControlServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(
            BuildConfig.APPLICATION_ID,
            DisplayControlService::class.java.name
        )
    )
        .processNameSuffix(DisplayControlService.TAG)
        .tag(DisplayControlService.TAG)
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)
        .daemon(false)

    private val displayControlServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service != null && Shizuku.pingBinder()) {
                displayControlService = IDisplayControl.Stub.asInterface(service)
            } else {
                notifyException(
                    ExceptionScenes.ExceptionWhenAccessShizukuRemote,
                    "get a null DisplayControlService connect or binder dead"
                )
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            displayControlService = null
            if (!stopping) {
                lastError.value = UiText(R.string.connection_retrying)
                state.value = State.Created
            }
        }

    }

    private var displayBinding = false
    fun bindDisplayControlService() {
        if (displayBinding) return
        displayBinding = true
        try {
            Shizuku.bindUserService(
                displayControlServiceArgs,
                displayControlServiceConnection
            )
        } catch (e: Exception) {
            displayBinding = false
            throw e
        }
    }

    fun unbindDisplayControlService() {
        try {
            Shizuku.unbindUserService(
                displayControlServiceArgs,
                displayControlServiceConnection,
                true
            )
        } catch (e: Exception) {
            notifyException(ExceptionScenes.ExceptionWhenAccessShizukuRemote, e)
        }
    }

    var eventsProviderService: IEventsProvider? = null

    private val eventsProviderServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(
            BuildConfig.APPLICATION_ID,
            EventsProviderService::class.java.name
        )
    )
        .processNameSuffix(EventsProviderService.TAG)
        .tag(EventsProviderService.TAG)
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)
        .daemon(false)

    private val eventsProviderServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service != null && Shizuku.pingBinder()) {
                eventsProviderService = IEventsProvider.Stub.asInterface(service)
            } else {
                notifyException(
                    ExceptionScenes.ExceptionWhenAccessShizukuRemote,
                    "get a null DisplayControlService connect or binder dead"
                )
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            eventsProviderService = null
            if (!stopping) {
                lastError.value = UiText(R.string.connection_retrying)
                state.value = State.Created
            }
        }

    }

    private var eventsBinding = false
    fun bindEventsProviderService() {
        if (eventsBinding) return
        eventsBinding = true
        try {
            Shizuku.bindUserService(
                eventsProviderServiceArgs,
                eventsProviderServiceConnection
            )
        } catch (e: Exception) {
            eventsBinding = false
            throw e
        }
    }

    fun unbindEventsProviderService() {
        try {
            Shizuku.unbindUserService(
                eventsProviderServiceArgs,
                eventsProviderServiceConnection,
                true
            )
        } catch (e: Exception) {
            notifyException(ExceptionScenes.ExceptionWhenAccessShizukuRemote, e)
        }
    }

    //== Screen state
    private val screenControlMutex = Mutex()
    val isScreenOn get() = screenState.value == ScreenState.On
    private val brightnessRestore = BrightnessRestoreState()

    enum class ScreenState {
        On, Off
    }

    suspend fun turnScreenOn() {
        screenControlMutex.withLock {
            val display = checkNotNull(displayControlService)
            try {
                when (feature.solution) {
                    ShizukuPowerOffScreen -> display.setPowerModeToSurfaceControl(DisplayControlService.POWER_MODE_NORMAL)
                    ShizukuScreenBrightnessNeg1 -> display.setBrightnessToSurfaceControl(savedBrightness().coerceIn(0, 255) / 255f)
                }.withResult { name, detail -> error("$name: $detail") }
                screenState.value = ScreenState.On
                updateHostState(true)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                screenState.value = ScreenState.Off
                updateHostState(false)
                lastError.value = UiText(R.string.screen_on_failed, e.localizedMessage.orEmpty())
                notifyException(ExceptionScenes.ExceptionWhenAccessShizukuRemote, e)
                return@withLock
            }
            // The picture is already visible; settings writes and notifications come afterwards.
            if (feature.solution == ShizukuScreenBrightnessNeg1) {
                try {
                    withContext(Dispatchers.IO) { restoreBrightnessSettings(display) }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    lastError.value = UiText(R.string.brightness_restore_failed)
                    notifyException(ExceptionScenes.ExceptionWhenAccessShizukuRemote, e)
                }
            }
            notificationHost.notify(true)
        }
    }

    private fun savedBrightness(): Int =
        brightnessRestore.brightness
            ?: Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)

    private fun restoreBrightnessSettings(display: IDisplayControl) {
        brightnessRestore.restore(
            writeBrightness = { brightness ->
                display.setBrightnessToSetting(brightness)
                    .withResult { name, detail -> error("$name: $detail") }
            },
            writeMode = { mode ->
                display.setBrightnessModeToSetting(mode)
                    .withResult { name, detail -> error("$name: $detail") }
            },
        )
    }

    suspend fun turnScreenOff() {
        screenControlMutex.withLock {
            val display = checkNotNull(displayControlService)
            var brightnessCaptured = false
            try {
                if (feature.solution == ShizukuScreenBrightnessNeg1) {
                    // A failed restore must finish before reading potentially modified settings.
                    withContext(Dispatchers.IO) { restoreBrightnessSettings(display) }
                    val brightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                    val mode = if (feature.brightnessManualWhenScreenOff) {
                        Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
                    } else null
                    brightnessRestore.capture(brightness, mode)
                    brightnessCaptured = true
                    if (feature.brightnessManualWhenScreenOff) {
                        // Some firmware reapplies brightness on mode changes; finish this before panel-off.
                        withContext(Dispatchers.IO) {
                            display.setBrightnessModeToSetting(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                                .withResult { name, detail -> error("$name: $detail") }
                        }
                    }
                }
                updateHostState(false)
                when (feature.solution) {
                    ShizukuPowerOffScreen -> display.setPowerModeToSurfaceControl(DisplayControlService.POWER_MODE_OFF)
                    ShizukuScreenBrightnessNeg1 -> display.setBrightnessToSurfaceControl(-1f)
                }.withResult { name, detail -> error("$name: $detail") }
                screenState.value = ScreenState.Off
                notificationHost.notify(false)
            } catch (e: Exception) {
                if (brightnessCaptured) {
                    withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) {
                        runCatching { restoreBrightnessSettings(display) }.onFailure { restoreError ->
                            Log.e(TAG, "Could not roll back brightness settings", restoreError)
                        }
                    }
                }
                if (e is kotlinx.coroutines.CancellationException) throw e
                screenState.value = ScreenState.On
                updateHostState(true)
                lastError.value = UiText(R.string.screen_off_failed, e.localizedMessage.orEmpty())
                notifyException(ExceptionScenes.ExceptionWhenAccessShizukuRemote, e)
            }
        }
    }

    /**
     * Listen system lock. Update host when locking.
     * */
    private var systemLockReceiver: BroadcastReceiver? = null

    private fun registerSystemLockReceiver() {
        systemLockReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                lifecycleScope.launch(Dispatchers.Main) {
                    screenControlMutex.withLock {
                        if (brightnessRestore.pending) {
                            try {
                                withContext(Dispatchers.IO) {
                                    displayControlService?.let { restoreBrightnessSettings(it) }
                                }
                            } catch (e: Exception) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                lastError.value = UiText(R.string.brightness_restore_failed)
                            }
                        }
                    }
                    updateHostState(true)
                }
            }
        }
        systemLockReceiver?.let {
            registerReceiver(
                it,
                IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                }
            )
        }
    }

    private fun unregisterSystemLockReceiver() {
        systemLockReceiver?.let {
            unregisterReceiver(it)
            systemLockReceiver = null
        }
    }

}
