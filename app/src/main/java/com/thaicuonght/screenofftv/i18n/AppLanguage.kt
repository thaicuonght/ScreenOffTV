package com.thaicuonght.screenofftv.i18n

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.StringRes
import com.thaicuonght.screenofftv.R
import com.thaicuonght.screenofftv.tv.RemoteGestureDetector
import java.util.Locale

/** App-specific resources; changing language never changes the device locale or restarts the service. */
object AppLanguage {
    val options = listOf("system", "vi", "en", "zh-CN")
    fun saved(context: Context): String = context.getSharedPreferences("tv_control", Context.MODE_PRIVATE)
        .getString("language", "system").takeIf { it in options } ?: "system"

    fun save(context: Context, language: String) {
        require(language in options)
        context.getSharedPreferences("tv_control", Context.MODE_PRIVATE).edit().putString("language", language).apply()
    }

    fun wrap(context: Context, language: String = saved(context)): Context {
        val config = Configuration(context.resources.configuration)
        val locale = if (language == "system") {
            (0 until config.locales.size()).map { config.locales[it] }
                .firstOrNull { it.language in listOf("vi", "en", "zh") } ?: Locale.ENGLISH
        } else Locale.forLanguageTag(language)
        config.setLocales(LocaleList(*listOf(locale, Locale.ENGLISH).distinct().toTypedArray()))
        return context.createConfigurationContext(config)
    }
}

/** Resolve at display time so existing errors and connection reports follow the selected language. */
data class UiText(@StringRes val resource: Int, val argument: String? = null) {
    fun resolve(context: Context): String = if (argument == null) context.getString(resource)
        else context.getString(resource, argument)
}

fun Context.localizedString(@StringRes resource: Int, vararg args: Any): String =
    AppLanguage.wrap(this).getString(resource, *args)

fun languageName(context: Context, language: String): String = when (language) {
    "vi" -> "Tiếng Việt"
    "en" -> "English"
    "zh-CN" -> "简体中文"
    else -> context.getString(R.string.language_system)
}

fun localizedKeyName(context: Context, code: Int): String = when (code) {
    0x71 -> context.getString(R.string.key_mute)
    0x73 -> context.getString(R.string.key_volume_up)
    0x72 -> context.getString(R.string.key_volume_down)
    else -> context.getString(R.string.key_unknown, code.toString(16))
}

fun localizedModeName(context: Context, mode: RemoteGestureDetector.Mode): String = context.getString(when (mode) {
    RemoteGestureDetector.Mode.Single -> R.string.press_single
    RemoteGestureDetector.Mode.Double -> R.string.press_double
    RemoteGestureDetector.Mode.Hold -> R.string.press_hold
})
