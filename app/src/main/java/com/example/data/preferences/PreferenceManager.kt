package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.AppSettings
import com.example.ui.SortOrder
import com.example.ui.i18n.AppLanguage

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("ava_music_settings_pref", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_PERSIAN = "is_persian" // legacy RTL toggle, kept for migration
        private const val KEY_LANGUAGE_MODE = "language_mode"
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
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_RATE_PROMPT_STATE = "rate_prompt_state"
        private const val KEY_RATE_PROMPT_SNOOZE_UNTIL = "rate_prompt_snooze_until"

        // Rating prompt states
        const val RATE_NEW = "new"
        const val RATE_SNOOZED = "snoozed"
        const val RATE_RATED = "rated"
        const val RATE_NEVER = "never"
    }

    fun loadSettings(): AppSettings {
        return AppSettings(
            languageMode = loadLanguageMode(),
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
            .putString(KEY_LANGUAGE_MODE, settings.languageMode)
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

    /** Returns the stored language mode, migrating the legacy Persian/RTL switch once. */
    private fun loadLanguageMode(): String {
        prefs.getString(KEY_LANGUAGE_MODE, null)?.let { return it }
        if (prefs.contains(KEY_IS_PERSIAN)) {
            return if (prefs.getBoolean(KEY_IS_PERSIAN, true)) AppLanguage.MODE_PERSIAN else AppLanguage.MODE_ENGLISH
        }
        return AppLanguage.MODE_SYSTEM
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

    // ── first-run tour ────────────────────────────────────────────────────────
    fun hasSeenOnboarding(): Boolean = prefs.getBoolean(KEY_ONBOARDING_DONE, false)

    fun setOnboardingSeen(seen: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, seen).apply()
    }

    // ── "rate us" prompt ──────────────────────────────────────────────────────
    // Asking at the right moment (after the app has proven itself) lifts the store
    // rating far more than a static row in Settings, and a store rating is the single
    // strongest organic discovery signal we control.
    fun ratePromptState(): String = prefs.getString(KEY_RATE_PROMPT_STATE, RATE_NEW) ?: RATE_NEW

    fun ratePromptSnoozeUntil(): Long = prefs.getLong(KEY_RATE_PROMPT_SNOOZE_UNTIL, 0L)

    fun setRatePromptState(state: String, snoozeUntilMs: Long = 0L) {
        prefs.edit()
            .putString(KEY_RATE_PROMPT_STATE, state)
            .putLong(KEY_RATE_PROMPT_SNOOZE_UNTIL, snoozeUntilMs)
            .apply()
    }
}
