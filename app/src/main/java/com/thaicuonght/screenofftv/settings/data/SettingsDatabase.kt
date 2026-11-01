package com.thaicuonght.screenofftv.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("screen_off_tv_settings")

fun stringSettingElement(name: String, default: String) =
    SettingElement(stringPreferencesKey(name), default)

fun booleanSettingElement(name: String, default: Boolean) =
    SettingElement(booleanPreferencesKey(name), default)

data class SettingElement<T>(val key: Preferences.Key<T>, val default: T)
