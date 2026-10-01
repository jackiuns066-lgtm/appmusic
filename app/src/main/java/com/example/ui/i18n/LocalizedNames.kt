package com.example.ui.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.local.EqualizerPresetEntity
import com.example.data.recommendation.RecommendationReason
import com.example.data.recommendation.TrackRecommendation

/**
 * The library keeps placeholder metadata for files without tags (and some defaults are written to
 * the database while scanning). Those values are translated here, at display time, so switching
 * the app language also switches the labels of already scanned songs.
 */
private val UNKNOWN_ARTIST_VALUES = setOf("هنرمند نامشخص", "هنرمند ناشناس", "<unknown>", "Unknown artist", "unknown")
private val UNKNOWN_ALBUM_VALUES = setOf("آلبوم نامشخص", "Unknown album", "<unknown>", "unknown")
private val UNKNOWN_GENRE_VALUES = setOf("نامشخص", "Unknown")
private val UNKNOWN_TITLE_VALUES = setOf("آهنگ بدون عنوان", "Untitled track")
private val LOCAL_GENRE_VALUES = setOf("محلی", "Local")
private val GENERAL_GENRE_VALUES = setOf("عمومی", "General")
private val NORMAL_MOOD_VALUES = setOf("عادی", "Normal")
private val RELAXED_MOOD_VALUES = setOf("آرامش‌بخش", "Relaxed")
private val MAIN_FOLDER_VALUES = setOf("پوشه اصلی", "Main folder")
private val DEVICE_STORAGE_VALUES = setOf("حافظه دستگاه", "Device storage")

@Composable
fun displayArtist(raw: String?): String {
    val value = raw?.trim().orEmpty()
    return if (value.isEmpty() || UNKNOWN_ARTIST_VALUES.contains(value)) {
        stringResource(R.string.unknown_artist)
    } else {
        value
    }
}

/** Non Compose variant of [displayArtist], used by the home screen widget. */
fun displayArtistName(context: Context, raw: String?): String {
    val value = raw?.trim().orEmpty()
    return if (value.isEmpty() || UNKNOWN_ARTIST_VALUES.contains(value)) {
        context.getString(R.string.unknown_artist)
    } else {
        value
    }
}

/** Non Compose variant of [displayAlbum]. */
fun displayAlbumName(context: Context, raw: String?): String {
    val value = raw?.trim().orEmpty()
    return if (value.isEmpty() || UNKNOWN_ALBUM_VALUES.contains(value)) {
        context.getString(R.string.unknown_album)
    } else {
        value
    }
}

@Composable
fun displayAlbum(raw: String?): String {
    val value = raw?.trim().orEmpty()
    return if (value.isEmpty() || UNKNOWN_ALBUM_VALUES.contains(value)) {
        stringResource(R.string.unknown_album)
    } else {
        value
    }
}

@Composable
fun displayTitle(raw: String?): String {
    val value = raw?.trim().orEmpty()
    return if (value.isEmpty() || UNKNOWN_TITLE_VALUES.contains(value)) {
        stringResource(R.string.unknown_title)
    } else {
        value
    }
}

@Composable
fun displayGenre(raw: String?): String {
    val value = raw?.trim().orEmpty()
    return when {
        value.isEmpty() || UNKNOWN_GENRE_VALUES.contains(value) -> stringResource(R.string.unknown_genre)
        LOCAL_GENRE_VALUES.contains(value) -> stringResource(R.string.default_genre_local)
        GENERAL_GENRE_VALUES.contains(value) -> stringResource(R.string.genre_general)
        else -> value
    }
}

@Composable
fun displayMood(raw: String?): String {
    val value = raw?.trim().orEmpty()
    return when {
        value.isEmpty() || NORMAL_MOOD_VALUES.contains(value) -> stringResource(R.string.default_mood_normal)
        RELAXED_MOOD_VALUES.contains(value) -> stringResource(R.string.mood_relaxed)
        else -> value
    }
}

@Composable
fun displayFolder(raw: String?): String {
    val value = raw?.trim().orEmpty()
    return when {
        value.isEmpty() || MAIN_FOLDER_VALUES.contains(value) -> stringResource(R.string.default_folder)
        DEVICE_STORAGE_VALUES.contains(value) -> stringResource(R.string.device_storage)
        else -> value
    }
}

@Composable
fun recommendationReason(recommendation: TrackRecommendation): String = when (recommendation.reasonType) {
    RecommendationReason.GENRE_AND_MOOD -> stringResource(
        R.string.rec_reason_genre_mood,
        displayGenre(recommendation.reasonGenre),
        displayMood(recommendation.reasonMood)
    )
    RecommendationReason.GENRE -> stringResource(R.string.rec_reason_genre, displayGenre(recommendation.reasonGenre))
    RecommendationReason.MOOD -> stringResource(R.string.rec_reason_mood)
    RecommendationReason.VARIETY -> stringResource(R.string.rec_reason_variety)
}

/** Resource id of the localized name of a built-in equalizer preset, 0 for custom presets. */
fun systemPresetStringRes(presetId: String): Int = when (presetId) {
    "preset_flat" -> R.string.preset_flat
    "preset_bass" -> R.string.preset_bass
    "preset_vocal" -> R.string.preset_vocal
    "preset_persian_traditional" -> R.string.preset_persian_traditional
    "preset_electronic" -> R.string.preset_electronic
    else -> 0
}

/** Localized name of an equalizer preset (non Compose context, e.g. for toasts). */
fun equalizerPresetTitle(context: Context, preset: EqualizerPresetEntity): String {
    val resId = if (preset.isSystemPreset) systemPresetStringRes(preset.id) else 0
    return if (resId != 0) context.getString(resId) else preset.name
}

@Composable
fun presetDisplayName(preset: EqualizerPresetEntity): String {
    val resId = if (preset.isSystemPreset) systemPresetStringRes(preset.id) else 0
    return if (resId != 0) stringResource(resId) else preset.name
}
