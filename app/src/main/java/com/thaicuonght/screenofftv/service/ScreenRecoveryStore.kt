package com.thaicuonght.screenofftv.service

import android.content.Context
import com.thaicuonght.screenofftv.settings.data.SettingsTokens.SolutionValue

/** Device-local recovery journal; deliberately excluded from Android backups. */
internal class ScreenRecoveryStore(context: Context) {
    private val preferences = context.getSharedPreferences("screen_recovery", Context.MODE_PRIVATE)

    fun brightness(): BrightnessRestoreState.Snapshot? {
        if (!preferences.contains("brightness")) return null
        return BrightnessRestoreState.Snapshot(
            preferences.getInt("brightness", 0),
            if (preferences.contains("mode")) preferences.getInt("mode", 0) else null,
        )
    }

    fun saveBrightness(snapshot: BrightnessRestoreState.Snapshot?) {
        val edit = preferences.edit().remove("brightness").remove("mode")
        snapshot?.let {
            edit.putInt("brightness", it.brightness)
            it.mode?.let { mode -> edit.putInt("mode", mode) }
        }
        check(edit.commit()) { "Could not save brightness recovery settings" }
    }

    val offSolution: SolutionValue?
        get() = preferences.getString("off_solution", null)?.let { SolutionValue.valueOf(it) }

    fun markOff(solution: SolutionValue) {
        check(preferences.edit().putString("off_solution", solution.name).commit()) {
            "Could not save picture recovery state"
        }
    }

    fun clearOff() {
        check(preferences.edit().remove("off_solution").commit()) {
            "Could not clear picture recovery state"
        }
    }
}
