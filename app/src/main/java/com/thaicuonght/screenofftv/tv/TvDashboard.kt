@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.thaicuonght.screenofftv.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.thaicuonght.screenofftv.i18n.*
import com.thaicuonght.screenofftv.BuildConfig
import com.thaicuonght.screenofftv.R
import com.thaicuonght.screenofftv.service.ScreenOffTVService
import com.thaicuonght.screenofftv.service.hosts.VolumeKeyEventShizukuHost
import com.thaicuonght.screenofftv.settings.data.ISettingsRepository
import com.thaicuonght.screenofftv.settings.data.SettingsTokens

private val Background = Color(0xFF0C1320)
private val Panel = Color(0xFF162238)
private val Accent = Color(0xFF9EDACB)

@Composable
fun TvDashboard(
    settings: ISettingsRepository,
    shizukuReady: Boolean,
    shizukuGranted: Boolean,
    message: com.thaicuonght.screenofftv.i18n.UiText?,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onScreenOff: () -> Unit,
    onOpenShizuku: () -> Unit,
    onRefresh: () -> Unit,
    language: String = "system",
    onLanguageChange: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val preferences = remember { TvPreferences(context) }
    var autoStart by remember { mutableStateOf(preferences.autoStart) }
    var selectedKey by remember { mutableIntStateOf(preferences.remoteKey) }
    var mode by remember { mutableStateOf(preferences.pressMode) }
    var showSettings by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }
    val presses by VolumeKeyEventShizukuHost.mutePressCount.collectAsStateWithLifecycle()
    var baseline by remember { mutableIntStateOf(0) }
    val state by ScreenOffTVService.state.collectAsStateWithLifecycle()
    val error by ScreenOffTVService.lastError.collectAsStateWithLifecycle()
    val connectionReport by ScreenOffTVService.connectionReport.collectAsStateWithLifecycle()
    val running = state == ScreenOffTVService.State.Prepared
    val starting = state == ScreenOffTVService.State.Created
    val keyName = localizedKeyName(context, selectedKey)
    val instruction = when (mode) {
        RemoteGestureDetector.Mode.Single -> stringResource(R.string.instruction_single, keyName)
        RemoteGestureDetector.Mode.Double -> stringResource(R.string.instruction_double, keyName)
        RemoteGestureDetector.Mode.Hold -> stringResource(R.string.instruction_hold, keyName)
    }
    val first = remember { FocusRequester() }
    val settingsFocus = remember { FocusRequester() }
    LaunchedEffect(showSettings) {
        if (showSettings) settingsFocus.requestFocus() else first.requestFocus()
    }
    DisposableEffect(Unit) { onDispose { VolumeKeyEventShizukuHost.endCapture() } }
    BackHandler(showSettings && dialog == null) { showSettings = false }
    val status = when {
        !shizukuReady -> stringResource(R.string.need_shizuku)
        !shizukuGranted -> stringResource(R.string.need_permission)
        starting -> stringResource(R.string.connecting)
        running -> stringResource(R.string.ready)
        else -> stringResource(R.string.service_off)
    }
    Column(Modifier.fillMaxSize().background(Background).padding(horizontal = 40.dp, vertical = 24.dp)) {
        Text(if (showSettings) stringResource(R.string.tv_settings) else "Screen Off TV", fontSize = 30.sp, color = Color.White)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1.1f).fillMaxHeight().background(Panel, RoundedCornerShape(24.dp))
                .padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.status_heading), color = Accent, fontSize = 13.sp, letterSpacing = 2.sp)
                Text(status, fontSize = 24.sp, color = Color.White)
                Text(stringResource(R.string.shizuku_status,
                    stringResource(if (shizukuReady) R.string.shizuku_running else R.string.shizuku_stopped),
                    stringResource(if (shizukuGranted) R.string.permission_granted else R.string.permission_missing)), color = Color(0xFFB9C5D8))
                if (message != null || error != null) Text((message ?: error)?.resolve(context).orEmpty(), color = Color(0xFFFFC9A6))
                TvButton(
                    text = when { starting -> stringResource(R.string.connecting_cancel); running -> stringResource(R.string.service_stop); else -> stringResource(R.string.service_start) },
                    modifier = Modifier.focusRequester(first),
                    onClick = if (starting || running) onStop else onStart,
                    primary = true,
                )
                TvButton(stringResource(R.string.screen_test), enabled = running) { dialog = "screenTest" }
                Text(stringResource(R.string.wake_instruction, instruction), color = Color(0xFFB9C5D8), fontSize = 15.sp)
            }
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (showSettings) {
                    TvButton(stringResource(R.string.remote_key), Modifier.focusRequester(settingsFocus), subtitle = keyName) { dialog = "key" }
                    TvButton(stringResource(R.string.press_action), subtitle = localizedModeName(context, mode)) { dialog = "mode" }
                    TvButton(stringResource(R.string.start_on_boot), toggle = autoStart) {
                        autoStart = !autoStart
                        preferences.autoStart = autoStart
                    }
                    TvButton(stringResource(R.string.screen_mode), enabled = !running && !starting) { dialog = "method" }
                    Text(if (settings.solution == SettingsTokens.SolutionValue.ShizukuPowerOffScreen)
                        stringResource(R.string.current_panel)
                        else stringResource(R.string.current_backlight), color = Color(0xFFB9C5D8), fontSize = 14.sp)
                    Text(stringResource(R.string.settings_hint), color = Color(0xFFB9C5D8), fontSize = 14.sp)
                    TvButton(stringResource(R.string.language), subtitle = languageName(context, language)) { dialog = "language" }
                    TvButton(stringResource(R.string.about)) { dialog = "about" }
                    TvButton(stringResource(R.string.back)) { showSettings = false }
                } else {
                    Text(stringResource(R.string.remote_heading), color = Accent, fontSize = 13.sp, letterSpacing = 2.sp)
                    Text(keyName, fontSize = 34.sp, color = Color.White)
                    Text(stringResource(R.string.remote_instruction, instruction), fontSize = 16.sp, color = Color(0xFFB9C5D8))
                    TvButton(stringResource(R.string.settings)) { showSettings = true }
                    TvButton(stringResource(R.string.open_shizuku), onClick = onOpenShizuku)
                    if (running) {
                        TvButton(stringResource(R.string.test_remote)) { baseline = presses; dialog = "test" }
                    } else {
                        TvButton(stringResource(R.string.check_shizuku), onClick = onRefresh)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.navigation_hint), color = Color(0xFF92A3BD), fontSize = 13.sp)
    }
    fun dismiss() {
        dialog = null
        VolumeKeyEventShizukuHost.endCapture()
    }
    if (dialog == "learn") {
        LearnRemoteDialog(
            onConfirm = { code ->
                selectedKey = code
                preferences.remoteKey = code
                dismiss()
            },
            onCancel = {
                VolumeKeyEventShizukuHost.endCapture()
                dialog = "key"
            },
        )
    } else if (dialog != null) {
        val dialogFocus = remember(dialog) { FocusRequester() }
        val dialogScroll = key(dialog) { rememberScrollState() }
        val dialogScope = rememberCoroutineScope()
        val scrollStep = with(LocalDensity.current) { 64.dp.toPx() }
        val aboutNavigation = Modifier.onPreviewKeyEvent { event ->
                if (dialog == "about" && (event.key == Key.DirectionUp || event.key == Key.DirectionDown)) {
                    if (event.type == KeyEventType.KeyDown) {
                        val direction = if (event.key == Key.DirectionDown) 1 else -1
                        dialogScope.launch { dialogScroll.scrollBy(direction * scrollStep) }
                    }
                    true
                } else false
            }
        LocalizedAlertDialog(
            onDismissRequest = { dismiss() },
            title = {
                // TV dialogs use keyboard focus even after a pointer click.
                val inputMode = LocalInputModeManager.current
                val windowFocused = LocalWindowInfo.current.isWindowFocused
                LaunchedEffect(dialog, windowFocused) {
                    if (!windowFocused) return@LaunchedEffect
                    inputMode.requestInputMode(InputMode.Keyboard)
                    withFrameNanos { }
                    dialogFocus.requestFocus()
                }
                Text(when (dialog) { "language" -> stringResource(R.string.language_title); "about" -> "Screen Off TV"; "key" -> stringResource(R.string.choose_key); "mode" -> stringResource(R.string.choose_action); "method" -> stringResource(R.string.choose_screen_mode); "screenTest" -> stringResource(R.string.screen_test); else -> stringResource(R.string.test_remote) }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (dialog == "about") {
                        Text(stringResource(R.string.about_scroll_hint), color = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.heightIn(max = 340.dp).verticalScroll(dialogScroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        when (dialog) {
                            "language" -> AppLanguage.options.forEach { code ->
                                TvButton(languageName(context, code),
                                    modifier = if (code == language) Modifier.focusRequester(dialogFocus) else Modifier,
                                    selected = code == language) {
                                    dismiss()
                                    onLanguageChange(code)
                                }
                            }
                            "about" -> {
                                Text(stringResource(R.string.version, BuildConfig.VERSION_NAME))
                                Text(stringResource(R.string.repository))
                                Text(stringResource(R.string.about_description))
                                Text(stringResource(R.string.privacy), style = MaterialTheme.typography.titleMedium)
                                Text(remember(context) { context.resources.openRawResource(R.raw.privacy_policy).bufferedReader().use { it.readText() } })
                            }
                            "key" -> {
                                Text(stringResource(R.string.key_hint))
                                Text(stringResource(R.string.selected_key, keyName))
                                TvButton(stringResource(R.string.learn_key), modifier = if (running) Modifier.focusRequester(dialogFocus) else Modifier, enabled = running) {
                                    VolumeKeyEventShizukuHost.beginCapture()
                                    dialog = "learn"
                                }
                                if (!running) Text(stringResource(R.string.learn_requires_service))
                                val choices = if (RemoteKeys.presets.any { it.first == selectedKey }) RemoteKeys.presets
                                    else RemoteKeys.presets + (selectedKey to localizedKeyName(context, selectedKey))
                                choices.forEach { (code, name) ->
                                    TvButton(localizedKeyName(context, code), modifier = if (!running && selectedKey == code) Modifier.focusRequester(dialogFocus) else Modifier,
                                        selected = selectedKey == code) {
                                        selectedKey = code; preferences.remoteKey = code; dismiss()
                                    }
                                }
                            }
                            "mode" -> RemoteGestureDetector.Mode.entries.forEach { option ->
                                TvButton(localizedModeName(context, option), modifier = if (mode == option) Modifier.focusRequester(dialogFocus) else Modifier, selected = mode == option) {
                                    mode = option; preferences.pressMode = option; dismiss()
                                }
                            }
                            "method" -> {
                                Text(stringResource(R.string.mode_description))
                                TvButton(stringResource(R.string.panel_mode), modifier = if (settings.solution == SettingsTokens.SolutionValue.ShizukuPowerOffScreen) Modifier.focusRequester(dialogFocus) else Modifier, selected = settings.solution == SettingsTokens.SolutionValue.ShizukuPowerOffScreen) {
                                    settings.solution = SettingsTokens.SolutionValue.ShizukuPowerOffScreen; dismiss()
                                }
                                Text(stringResource(R.string.panel_description))
                                TvButton(stringResource(R.string.backlight_mode), modifier = if (settings.solution == SettingsTokens.SolutionValue.ShizukuScreenBrightnessNeg1) Modifier.focusRequester(dialogFocus) else Modifier, selected = settings.solution == SettingsTokens.SolutionValue.ShizukuScreenBrightnessNeg1) {
                                    settings.solution = SettingsTokens.SolutionValue.ShizukuScreenBrightnessNeg1; dismiss()
                                }
                                Text(stringResource(R.string.backlight_description))
                            }
                            "screenTest" -> Text(stringResource(R.string.screen_test_warning, instruction))
                            else -> Text(if (presses > baseline) stringResource(R.string.remote_received, keyName)
                                else stringResource(R.string.remote_test_hint, keyName))
                        }
                    }
                }
            },
            confirmButton = {
                if (dialog == "screenTest") {
                    TvButton(stringResource(R.string.screen_off), Modifier.focusRequester(dialogFocus), enabled = running) {
                        dismiss()
                        onScreenOff()
                    }
                } else {
                    TvButton(stringResource(R.string.close),
                        (if (dialog == "about") aboutNavigation else Modifier)
                            .then(if (dialog == "test" || dialog == "about") Modifier.focusRequester(dialogFocus) else Modifier)) { dismiss() }
                }
            },
            dismissButton = {
                if (dialog == "screenTest") TvButton(stringResource(R.string.cancel)) { dismiss() }
            },
        )
    }
    if (connectionReport != null) {
        val reportFocus = remember { FocusRequester() }
        LocalizedAlertDialog(onDismissRequest = { ScreenOffTVService.connectionReport.value = null },
            title = {
                val inputMode = LocalInputModeManager.current
                val windowFocused = LocalWindowInfo.current.isWindowFocused
                LaunchedEffect(windowFocused) {
                    if (!windowFocused) return@LaunchedEffect
                    inputMode.requestInputMode(InputMode.Keyboard)
                    withFrameNanos { }
                    reportFocus.requestFocus()
                }
                Text(stringResource(R.string.shizuku_connection))
            }, text = { Text(connectionReport?.resolve(context).orEmpty()) },
            confirmButton = { TvButton(stringResource(R.string.close), Modifier.focusRequester(reportFocus)) { ScreenOffTVService.connectionReport.value = null } })
    }
}

/** Learning is a temporary preview. The stored shortcut changes only on explicit confirmation. */
@Composable
private fun LearnRemoteDialog(onConfirm: (Int) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val candidate by VolumeKeyEventShizukuHost.capturedKey.collectAsStateWithLifecycle()
    val captureError by VolumeKeyEventShizukuHost.captureError.collectAsStateWithLifecycle()
    val confirmFocus = remember { FocusRequester() }
    val cancelFocus = remember { FocusRequester() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                VolumeKeyEventShizukuHost.endCapture()
                onCancel()
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            VolumeKeyEventShizukuHost.endCapture()
        }
    }
    LocalizedAlertDialog(
        onDismissRequest = onCancel,
        title = {
            val inputMode = LocalInputModeManager.current
            val windowFocused = LocalWindowInfo.current.isWindowFocused
            LaunchedEffect(candidate, windowFocused) {
                if (!windowFocused) return@LaunchedEffect
                inputMode.requestInputMode(InputMode.Keyboard)
                withFrameNanos { }
                if (candidate != null) confirmFocus.requestFocus() else cancelFocus.requestFocus()
            }
            Text(stringResource(R.string.learn_title))
        },
        text = {
            Column(Modifier.heightIn(max = 330.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (candidate == null) {
                    Text(stringResource(R.string.learn_waiting), color = Accent, fontSize = 22.sp)
                    Text(stringResource(R.string.learn_steps))
                    Text(stringResource(R.string.learn_reserved_hint))
                } else {
                    Text(stringResource(R.string.learn_received), color = Accent, fontSize = 22.sp)
                    Text(localizedKeyName(context, candidate!!), fontSize = 24.sp)
                    Text(stringResource(R.string.key_code, candidate!!.toString(16).padStart(4, '0'), candidate!!))
                    Text(stringResource(R.string.learn_unsaved))
                }
                captureError?.let { Text(it.resolve(context), color = Color(0xFFFFC9A6)) }
                if (candidate != null) {
                    TvButton(stringResource(R.string.learn_another)) {
                        VolumeKeyEventShizukuHost.beginCapture()
                    }
                }
            }
        },
        confirmButton = {
            if (candidate != null) TvButton(stringResource(R.string.confirm_key), Modifier.focusRequester(confirmFocus), primary = true) { onConfirm(candidate!!) }
        },
        dismissButton = { TvButton(stringResource(R.string.cancel), Modifier.focusRequester(cancelFocus), onClick = onCancel) },
    )
}

@Composable
private fun TvButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    toggle: Boolean? = null,
    selected: Boolean? = null,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val toggleDescription = stringResource(if (toggle == true) R.string.toggle_on else R.string.toggle_off)
    val shape = RoundedCornerShape(14.dp)
    Button(onClick = onClick, enabled = enabled, shape = shape,
        modifier = modifier.fillMaxWidth()
            .then(if (subtitle != null || toggle != null) Modifier.height(72.dp) else Modifier.heightIn(min = 56.dp))
            .onFocusChanged { focused = it.isFocused }
            .then(if (toggle != null) Modifier.semantics { stateDescription = toggleDescription } else Modifier)
            .then(if (selected != null) Modifier.semantics { this.selected = selected } else Modifier)
            .border(BorderStroke(if (focused) 3.dp else 1.dp, if (focused) Color.White else Color(0xFF34445F)), shape),
        colors = ButtonDefaults.buttonColors(containerColor = if (primary) Accent else Panel, contentColor = if (primary) Background else Color.White),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = if (subtitle != null) 6.dp else 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(text, fontSize = 16.sp, lineHeight = 20.sp)
                if (subtitle != null) Text(subtitle, fontSize = 11.sp, lineHeight = 14.sp, color = Color(0xFFB9C5D8))
            }
            if (toggle != null) Switch(checked = toggle, onCheckedChange = null, enabled = enabled)
            if (selected != null) RadioButton(selected = selected, onClick = null, enabled = enabled)
        }
    }
}

/** Dialog windows install their own Android context; explicitly carry the selected app locale into every slot. */
@Composable
private fun LocalizedAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = { LocalizedDialogContent(context, configuration, title) },
        text = { LocalizedDialogContent(context, configuration, text) },
        confirmButton = { LocalizedDialogContent(context, configuration, confirmButton) },
        dismissButton = dismissButton?.let { content ->
            { LocalizedDialogContent(context, configuration, content) }
        },
    )
}

@Composable
private fun LocalizedDialogContent(
    context: Context,
    configuration: Configuration,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides configuration, content = content)
}
