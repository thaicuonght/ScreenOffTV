package com.thaicuonght.screenofftv.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlin.reflect.KProperty

private const val TAG = "SyncSettingsRepository"

class SyncSettingsRepository(
    private val settingsDatabase: DataStore<Preferences>
) : ISettingsRepository {
    private var _solution by Delegate(SettingsTokens.Solution)
    override var solution
        get() = SettingsTokens.SolutionValue.valueOf(_solution)
        set(value) {
            _solution = value.name
        }

    override val compatibility = object : ISettingsRepository.Compatibility {
        override var brightnessManualWhenScreenOff: Boolean by Delegate(SettingsTokens.Compatibility.BrightnessManualWhenScreenOff)
    }

    private var data: Preferences? = null

    suspend fun update() {
        data = settingsDatabase.data.first()
    }

    suspend fun keptUpdate() {
        settingsDatabase.data.collectLatest {
            data = it
        }
    }

    inner class Delegate<T>(private val element: SettingElement<T>) {

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
            data?.get(element.key) ?: element.default

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            require(false) {
                "SyncSettingsRepository should not be set."
            }
        }

    }
}