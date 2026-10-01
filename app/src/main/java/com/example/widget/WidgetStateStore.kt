package com.example.widget

import android.content.Context
import com.example.data.local.TrackEntity

/** Snapshot of the current playback state used to render the home screen widget. */
data class WidgetSnapshot(
    val hasTrack: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val isFavorite: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = 0,          // 0 = off, 1 = all, 2 = one (RepeatMode ordinal)
    val queueSize: Int = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val trackId: String = "",
    val sourceUri: String = "",
    val sourcePath: String? = null,
    val languageMode: String = "system",
    val accentTheme: String = "gold"
)

/**
 * Persists the data the widget needs. Widgets are rendered by the launcher process, so the state
 * has to be available without a running activity.
 */
object WidgetStateStore {

    private const val PREFS_NAME = "novo_widget_state"
    private const val KEY_HAS_TRACK = "has_track"
    private const val KEY_TITLE = "title"
    private const val KEY_ARTIST = "artist"
    private const val KEY_IS_PLAYING = "is_playing"
    private const val KEY_IS_FAVORITE = "is_favorite"
    private const val KEY_IS_SHUFFLE = "is_shuffle"
    private const val KEY_REPEAT_MODE = "repeat_mode"
    private const val KEY_QUEUE_SIZE = "queue_size"
    private const val KEY_POSITION = "position"
    private const val KEY_DURATION = "duration"
    private const val KEY_TRACK_ID = "track_id"
    private const val KEY_SOURCE_URI = "source_uri"
    private const val KEY_SOURCE_PATH = "source_path"
    private const val KEY_LANGUAGE = "language_mode"
    private const val KEY_ACCENT = "accent_theme"

    fun save(
        context: Context,
        track: TrackEntity?,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long,
        languageMode: String,
        accentTheme: String,
        isShuffleEnabled: Boolean = false,
        repeatMode: Int = 0,
        queueSize: Int = 0
    ) {
        try {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_HAS_TRACK, track != null)
                .putString(KEY_TITLE, track?.title ?: "")
                .putString(KEY_ARTIST, track?.artist ?: "")
                .putBoolean(KEY_IS_PLAYING, isPlaying)
                .putBoolean(KEY_IS_FAVORITE, track?.isFavorite ?: false)
                .putBoolean(KEY_IS_SHUFFLE, isShuffleEnabled)
                .putInt(KEY_REPEAT_MODE, repeatMode)
                .putInt(KEY_QUEUE_SIZE, queueSize)
                .putLong(KEY_POSITION, positionMs)
                .putLong(KEY_DURATION, durationMs)
                .putString(KEY_TRACK_ID, track?.id ?: "")
                .putString(KEY_SOURCE_URI, track?.uriString ?: "")
                .putString(KEY_SOURCE_PATH, track?.downloadedFilePath)
                .putString(KEY_LANGUAGE, languageMode)
                .putString(KEY_ACCENT, accentTheme)
                .apply()
        } catch (_: Exception) {
        }
    }

    fun load(context: Context): WidgetSnapshot {
        return try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            WidgetSnapshot(
                hasTrack = prefs.getBoolean(KEY_HAS_TRACK, false),
                title = prefs.getString(KEY_TITLE, "").orEmpty(),
                artist = prefs.getString(KEY_ARTIST, "").orEmpty(),
                isPlaying = prefs.getBoolean(KEY_IS_PLAYING, false),
                isFavorite = prefs.getBoolean(KEY_IS_FAVORITE, false),
                isShuffleEnabled = prefs.getBoolean(KEY_IS_SHUFFLE, false),
                repeatMode = prefs.getInt(KEY_REPEAT_MODE, 0),
                queueSize = prefs.getInt(KEY_QUEUE_SIZE, 0),
                positionMs = prefs.getLong(KEY_POSITION, 0L),
                durationMs = prefs.getLong(KEY_DURATION, 0L),
                trackId = prefs.getString(KEY_TRACK_ID, "").orEmpty(),
                sourceUri = prefs.getString(KEY_SOURCE_URI, "").orEmpty(),
                sourcePath = prefs.getString(KEY_SOURCE_PATH, null),
                languageMode = prefs.getString(KEY_LANGUAGE, "system") ?: "system",
                accentTheme = prefs.getString(KEY_ACCENT, "gold") ?: "gold"
            )
        } catch (_: Exception) {
            WidgetSnapshot()
        }
    }

    /** Rebuilds a minimal track so the artwork loader can find the embedded cover. */
    fun toTrackEntity(snapshot: WidgetSnapshot): TrackEntity? {
        if (!snapshot.hasTrack) return null
        if (snapshot.sourceUri.isBlank() && snapshot.sourcePath.isNullOrBlank()) return null
        return TrackEntity(
            id = snapshot.trackId.ifBlank { "widget_track" },
            title = snapshot.title,
            artist = snapshot.artist,
            uriString = snapshot.sourceUri.ifBlank { snapshot.sourcePath.orEmpty() },
            downloadedFilePath = snapshot.sourcePath,
            isFavorite = snapshot.isFavorite
        )
    }
}
