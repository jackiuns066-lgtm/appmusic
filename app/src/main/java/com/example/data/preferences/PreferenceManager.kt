package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.AppSettings
import com.example.ui.SortOrder

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("ava_music_settings_pref", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_PERSIAN = "is_persian"
        private const val KEY_IS_AMOLED = "is_amoled"
        private const val KEY_ACCENT_THEME = "accent_theme"
        private const val KEY_NOW_PLAYING_STYLE = "now_playing_style"
        private const val KEY_MIN_DURATION = "min_duration"
        private const val KEY_CROSSFADE = "crossfade"
        private const val KEY_PAUSE_ON_HEADSET = "pause_on_headset"
        private const val KEY_HIFI_AUDIO = "hifi_audio"
        private const val KEY_LOCK_SCREEN_CONTROLS = "lock_screen_controls"
        private const val KEY_SORT_ORDER = "sort_order"
        private const val KEY_INITIAL_SCAN_DONE = "initial_scan_done"
    }

    fun loadSettings(): AppSettings {
        return AppSettings(
            isPersian = prefs.getBoolean(KEY_IS_PERSIAN, true),
            isAmoledDark = prefs.getBoolean(KEY_IS_AMOLED, false),
            accentTheme = prefs.getString(KEY_ACCENT_THEME, "gold") ?: "gold",
            sleepTimerMinutes = 0,
            nowPlayingStyle = prefs.getString(KEY_NOW_PLAYING_STYLE, "neon_vinyl") ?: "neon_vinyl",
            minDurationSeconds = prefs.getInt(KEY_MIN_DURATION, 15),
            crossfadeSeconds = prefs.getInt(KEY_CROSSFADE, 0),
            pauseOnHeadsetDisconnect = prefs.getBoolean(KEY_PAUSE_ON_HEADSET, true),
            hifiAudioMode = prefs.getBoolean(KEY_HIFI_AUDIO, true),
            lockScreenControlsEnabled = prefs.getBoolean(KEY_LOCK_SCREEN_CONTROLS, true)
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putBoolean(KEY_IS_PERSIAN, settings.isPersian)
            .putBoolean(KEY_IS_AMOLED, settings.isAmoledDark)
            .putString(KEY_ACCENT_THEME, settings.accentTheme)
            .putString(KEY_NOW_PLAYING_STYLE, settings.nowPlayingStyle)
            .putInt(KEY_MIN_DURATION, settings.minDurationSeconds)
            .putInt(KEY_CROSSFADE, settings.crossfadeSeconds)
            .putBoolean(KEY_PAUSE_ON_HEADSET, settings.pauseOnHeadsetDisconnect)
            .putBoolean(KEY_HIFI_AUDIO, settings.hifiAudioMode)
            .putBoolean(KEY_LOCK_SCREEN_CONTROLS, settings.lockScreenControlsEnabled)
            .apply()
    }

    fun loadSortOrder(): SortOrder {
        val name = prefs.getString(KEY_SORT_ORDER, SortOrder.RECENTLY_ADDED.name) ?: SortOrder.RECENTLY_ADDED.name
        return try {
            SortOrder.valueOf(name)
        } catch (_: Exception) {
            SortOrder.RECENTLY_ADDED
        }
    }

    fun saveSortOrder(sortOrder: SortOrder) {
        prefs.edit().putString(KEY_SORT_ORDER, sortOrder.name).apply()
    }

    fun hasCompletedInitialScan(): Boolean {
        return prefs.getBoolean(KEY_INITIAL_SCAN_DONE, false)
    }

    fun setInitialScanCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_INITIAL_SCAN_DONE, completed).apply()
    }
}
