package com.thaicuonght.screenofftv.settings.data

interface ISettingsRepository {
    var solution: SettingsTokens.SolutionValue
    val compatibility: Compatibility

    interface Compatibility {
        var brightnessManualWhenScreenOff: Boolean
    }
}
