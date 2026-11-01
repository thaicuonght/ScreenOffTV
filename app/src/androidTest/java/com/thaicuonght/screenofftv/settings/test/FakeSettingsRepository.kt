package com.thaicuonght.screenofftv.settings.test

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.thaicuonght.screenofftv.settings.data.ISettingsRepository
import com.thaicuonght.screenofftv.settings.data.SettingsTokens

class FakeSettingsRepository : ISettingsRepository {
    override var solution by mutableStateOf(SettingsTokens.SolutionValue.ShizukuPowerOffScreen)

    override val compatibility = object : ISettingsRepository.Compatibility {
        override var brightnessManualWhenScreenOff: Boolean by mutableStateOf(false)
    }

}