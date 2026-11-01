package com.thaicuonght.screenofftv.settings.data

object SettingsTokens {
    enum class SolutionValue {
        ShizukuPowerOffScreen,
        ShizukuScreenBrightnessNeg1,
    }

    val Solution = stringSettingElement("screen_method", SolutionValue.ShizukuPowerOffScreen.name)

    object Compatibility {
        val BrightnessManualWhenScreenOff = booleanSettingElement("manual_brightness_when_off", true)
    }
}
