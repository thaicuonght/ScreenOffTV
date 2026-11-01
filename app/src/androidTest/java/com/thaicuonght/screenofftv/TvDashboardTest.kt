package com.thaicuonght.screenofftv

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.thaicuonght.screenofftv.i18n.AppLanguage
import com.thaicuonght.screenofftv.settings.test.FakeSettingsRepository
import com.thaicuonght.screenofftv.tv.TvDashboard
import com.thaicuonght.screenofftv.tv.TvPreferences
import com.thaicuonght.screenofftv.service.ScreenOffTVService
import com.thaicuonght.screenofftv.service.hosts.VolumeKeyEventShizukuHost
import com.thaicuonght.screenofftv.ui.theme.ScreenOffTVTheme
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TvDashboardTest {
    @get:Rule val compose = createComposeRule()

    private fun showDashboard(initialLanguage: String = "en", owner: LifecycleOwner? = null) {
        val host = ApplicationProvider.getApplicationContext<Context>()
        val settings = FakeSettingsRepository()
        compose.setContent {
            var language by remember { mutableStateOf(initialLanguage) }
            val context = remember(language) { AppLanguage.wrap(host, language) }
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides context.resources.configuration,
                LocalLifecycleOwner provides (owner ?: LocalLifecycleOwner.current)) {
                ScreenOffTVTheme(darkTheme = true, dynamicColor = false) {
                    TvDashboard(
                        settings = settings, shizukuReady = false, shizukuGranted = false, message = null,
                        onStart = {}, onStop = {}, onScreenOff = {}, onOpenShizuku = {}, onRefresh = {},
                        language = language, onLanguageChange = { language = it },
                    )
                }
            }
        }
    }

    private fun customKeyCanBeConfirmed(language: String) {
        val host = ApplicationProvider.getApplicationContext<Context>()
        val context = AppLanguage.wrap(host, language)
        val preferences = TvPreferences(host)
        val originalKey = preferences.remoteKey
        val originalState = ScreenOffTVService.state.value
        try {
            ScreenOffTVService.state.value = ScreenOffTVService.State.Prepared
            showDashboard(language)
            compose.onNodeWithText(context.getString(R.string.settings), useUnmergedTree = true).performClick()
            compose.onNodeWithText(context.getString(R.string.remote_key), useUnmergedTree = true).performClick()
            // The custom-key action must be visible without scrolling past the presets.
            compose.onNodeWithText(context.getString(R.string.learn_key)).assertIsDisplayed().performClick()
            compose.onNodeWithText(context.getString(R.string.learn_title)).assertIsDisplayed()
            compose.runOnIdle { VolumeKeyEventShizukuHost.capturedKey.value = 0x18e }
            compose.onNodeWithText(context.getString(R.string.confirm_key)).performClick()
            compose.onNodeWithText(context.getString(R.string.learn_title)).assertDoesNotExist()
            compose.onNodeWithText(context.getString(R.string.choose_key)).assertDoesNotExist()
            compose.onNodeWithText(context.getString(R.string.tv_settings)).assertIsDisplayed()
            compose.runOnIdle {
                org.junit.Assert.assertEquals(0x18e, preferences.remoteKey)
                org.junit.Assert.assertFalse(VolumeKeyEventShizukuHost.captureActive.value)
            }
        } finally {
            preferences.remoteKey = originalKey
            ScreenOffTVService.state.value = originalState
            VolumeKeyEventShizukuHost.endCapture()
            VolumeKeyEventShizukuHost.capturedKey.value = null
        }
    }

    @Test fun customKeyWorksInEnglish() = customKeyCanBeConfirmed("en")
    @Test fun customKeyWorksInChinese() = customKeyCanBeConfirmed("zh-CN")
    @Test fun customKeyWorksInVietnamese() = customKeyCanBeConfirmed("vi")

    @Test fun leavingAppCancelsKeyLearning() {
        val originalState = ScreenOffTVService.state.value
        val owner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry(this)
        }
        try {
            compose.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
            ScreenOffTVService.state.value = ScreenOffTVService.State.Prepared
            showDashboard(owner = owner)
            compose.onNodeWithText("Settings", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Remote key", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Set custom remote key").performClick()
            compose.runOnIdle {
                org.junit.Assert.assertTrue(VolumeKeyEventShizukuHost.captureActive.value)
                owner.lifecycle.currentState = Lifecycle.State.CREATED
                org.junit.Assert.assertFalse(VolumeKeyEventShizukuHost.captureActive.value)
                owner.lifecycle.currentState = Lifecycle.State.RESUMED
            }
            compose.onNodeWithText("Learn a remote key").assertDoesNotExist()
        } finally {
            ScreenOffTVService.state.value = originalState
            VolumeKeyEventShizukuHost.endCapture()
        }
    }

    @Test fun aboutScrollsWithRemoteAndClosesWithOk() {
        showDashboard()
        compose.onNodeWithText("SCREEN OFF TV").assertDoesNotExist()
        compose.onNodeWithText("Settings", useUnmergedTree = true).performClick()
        compose.onNodeWithText("About", useUnmergedTree = true).performScrollTo().performClick()
        val version = compose.onNodeWithText("Version ${BuildConfig.VERSION_NAME}")
        version.assertIsDisplayed()
        val close = compose.onNodeWithText("Close")
        close.assertIsFocused()
        repeat(30) { close.performKeyInput { pressKey(Key.DirectionDown) } }
        version.assertIsNotDisplayed()
        compose.onNodeWithText("↑ ↓  Scroll     OK / Back  Close").assertIsDisplayed()
        repeat(30) { close.performKeyInput { pressKey(Key.DirectionUp) } }
        version.assertIsDisplayed()
        close.performKeyInput { pressKey(Key.DirectionCenter) }
        version.assertDoesNotExist()
    }

    @Test fun languageSwitchesLiveAndPreservesScreenMode() {
        showDashboard()
        compose.onNodeWithText("Settings", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Screen-off mode", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Backlight off / Brightness = -1", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Language", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithText("Tiếng Việt", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Cài đặt TV").assertIsDisplayed()
        compose.onNodeWithText("Chế độ tắt màn hình", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Đang dùng: Tắt đèn nền (Backlight / Brightness = -1).").assertIsDisplayed()
        compose.onNodeWithText("Ngôn ngữ / Language", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithText("简体中文", useUnmergedTree = true).performClick()
        compose.onNodeWithText("电视设置").assertIsDisplayed()
        compose.onNodeWithText("当前：关闭背光（亮度 = -1）。").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("语言 / Language", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithText("English", useUnmergedTree = true).performClick()
        compose.onNodeWithText("TV settings").assertIsDisplayed()
        compose.onNodeWithText("Current: Backlight off / Brightness = -1.").performScrollTo().assertIsDisplayed()
    }

    @Test fun stoppingDisablesStartAndScreenModeChanges() {
        val original = ScreenOffTVService.state.value
        try {
            ScreenOffTVService.state.value = ScreenOffTVService.State.Stopping
            showDashboard()
            compose.onNode(hasText("Stopping service…") and hasClickAction()).assertIsNotEnabled()
            compose.onNodeWithText("Settings", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Screen-off mode", useUnmergedTree = true).assertIsNotEnabled()
        } finally {
            ScreenOffTVService.state.value = original
        }
    }

    @Test fun permanentFailureShowsReasonAndAllowsStop() {
        val originalState = ScreenOffTVService.state.value
        val originalError = ScreenOffTVService.lastError.value
        try {
            ScreenOffTVService.state.value = ScreenOffTVService.State.Error
            ScreenOffTVService.lastError.value = com.thaicuonght.screenofftv.i18n.UiText(
                R.string.connection_permanent, "Unsupported display API")
            showDashboard()
            compose.onNodeWithText("Service could not start").assertIsDisplayed()
            compose.onNodeWithText("Unsupported display API", substring = true).assertIsDisplayed()
            compose.onNodeWithText("Stop service").assertIsEnabled()
        } finally {
            ScreenOffTVService.state.value = originalState
            ScreenOffTVService.lastError.value = originalError
        }
    }
}
