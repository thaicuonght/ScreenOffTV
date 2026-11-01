package com.thaicuonght.screenofftv

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import com.thaicuonght.screenofftv.i18n.AppLanguage
import com.thaicuonght.screenofftv.i18n.UiText
import com.thaicuonght.screenofftv.tv.TvPreferences
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class AppLanguageTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun allLanguagesHaveLocalizedResourcesAndPrivacy() {
        for ((code, expected) in listOf("en" to "Screen-off mode", "vi" to "Chế độ tắt màn hình", "zh-CN" to "息屏模式")) {
            val localized = AppLanguage.wrap(context, code)
            assertEquals(expected, localized.getString(R.string.screen_mode))
            assertFalse(localized.resources.openRawResource(R.raw.privacy_policy).bufferedReader().use { it.readText() }.isBlank())
            assertEquals(localized.getString(R.string.connection_timeout), UiText(R.string.connection_timeout).resolve(localized))
        }
    }

    @Test fun unsupportedSystemLanguageFallsBackToEnglish() {
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(Locale.FRENCH))
        val localized = AppLanguage.wrap(context.createConfigurationContext(config), "system")
        assertEquals("Settings", localized.getString(R.string.settings))
    }

    @Test fun autoStartIsEnabledByDefaultAndExplicitOptOutIsRespected() {
        val shared = context.getSharedPreferences("tv_control", Context.MODE_PRIVATE)
        val hadValue = shared.contains("auto_start")
        val saved = shared.getBoolean("auto_start", true)
        try {
            shared.edit().remove("auto_start").commit()
            assertTrue(TvPreferences(context).autoStart)
            TvPreferences(context).autoStart = false
            assertFalse(TvPreferences(context).autoStart)
            TvPreferences(context).autoStart = true
            assertTrue(TvPreferences(context).autoStart)
        } finally {
            val edit = shared.edit()
            if (hadValue) edit.putBoolean("auto_start", saved) else edit.remove("auto_start")
            edit.commit()
        }
    }

    @Test fun languagePersistsWithoutChangingRemotePreferences() {
        val saved = AppLanguage.saved(context)
        val preferences = TvPreferences(context)
        val key = preferences.remoteKey
        val mode = preferences.pressMode
        val autoStart = preferences.autoStart
        try {
            AppLanguage.save(context, "zh-CN")
            assertEquals("zh-CN", AppLanguage.saved(context))
            assertEquals(key, TvPreferences(context).remoteKey)
            assertEquals(mode, TvPreferences(context).pressMode)
            assertEquals(autoStart, TvPreferences(context).autoStart)
        } finally {
            AppLanguage.save(context, saved)
        }
    }
}
