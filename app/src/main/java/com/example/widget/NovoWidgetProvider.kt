package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import com.example.MainActivity
import com.example.R
import com.example.data.audio.MediaControlReceiver
import com.example.data.audio.MediaNotificationManager
import com.example.ui.components.AudioCoverLoader
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocaleAwareContext
import com.example.ui.i18n.displayArtistName
import java.util.Locale
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val PROGRESS_MAX = 1000
private const val ARTWORK_MAX_SIZE = 320
private const val EMPTY_TIME = "--:--"

/** Below this the widget switches to the compact layout. */
private const val COMPACT_MAX_WIDTH_DP = 220
private const val COMPACT_MAX_HEIGHT_DP = 110

/**
 * Home screen widget: rounded cover art, song title / artist, a live timeline and icon controls
 * (shuffle, previous, play-pause, next, repeat, favourite).
 *
 * Two layouts are shipped: a compact one for small widgets and the full one for wide widgets.
 * Which one is used is decided from the widget's own size, so resizing it just works.
 */
class NovoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WidgetUpdater.refreshAll(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        // Resizing the widget may switch between the compact and the full layout.
        WidgetUpdater.refreshAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_PLAY_PAUSE, ACTION_NEXT, ACTION_PREVIOUS,
            ACTION_TOGGLE_SHUFFLE, ACTION_TOGGLE_REPEAT, ACTION_TOGGLE_FAVORITE -> {
                sendPlaybackAction(context, intent.action.orEmpty())
                WidgetUpdater.refreshAll(context)
            }
        }
    }

    private fun sendPlaybackAction(context: Context, widgetAction: String) {
        val receiverAction = when (widgetAction) {
            ACTION_NEXT -> MediaNotificationManager.ACTION_NEXT
            ACTION_PREVIOUS -> MediaNotificationManager.ACTION_PREVIOUS
            ACTION_TOGGLE_SHUFFLE -> MediaNotificationManager.ACTION_TOGGLE_SHUFFLE
            ACTION_TOGGLE_REPEAT -> MediaNotificationManager.ACTION_TOGGLE_REPEAT
            ACTION_TOGGLE_FAVORITE -> MediaNotificationManager.ACTION_TOGGLE_FAVORITE
            else -> MediaNotificationManager.ACTION_PLAY_PAUSE
        }

        try {
            context.sendBroadcast(
                Intent(context, MediaControlReceiver::class.java).apply { action = receiverAction }
            )
        } catch (_: Exception) {
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.widget.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.widget.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.widget.ACTION_PREVIOUS"
        const val ACTION_TOGGLE_SHUFFLE = "com.example.widget.ACTION_TOGGLE_SHUFFLE"
        const val ACTION_TOGGLE_REPEAT = "com.example.widget.ACTION_TOGGLE_REPEAT"
        const val ACTION_TOGGLE_FAVORITE = "com.example.widget.ACTION_TOGGLE_FAVORITE"

        /** Re-renders every placed widget. */
        fun refreshAll(context: Context) {
            WidgetUpdater.refreshAll(context.applicationContext)
        }
    }
}

/** Renders the widgets off the main thread (artwork loading touches the disk). */
private object WidgetUpdater {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun refreshAll(context: Context) {
        scope.launch {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, NovoWidgetProvider::class.java))
                ids.forEach { widgetId -> renderWidget(context, manager, widgetId) }
            } catch (_: Exception) {
            }
        }
    }

    private fun renderWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val snapshot = WidgetStateStore.load(context)
        val stringContext: Context = localizedContext(context, snapshot.languageMode)
        val accent = accentColorFor(snapshot.accentTheme)
        val isCompact = isCompactLayout(manager, widgetId)

        val layout = if (isCompact) R.layout.widget_now_playing_compact else R.layout.widget_now_playing
        val views = RemoteViews(context.packageName, layout)

        if (!snapshot.hasTrack) {
            views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_notification_music)
            views.setTextViewText(R.id.widget_title, stringContext.getString(R.string.widget_no_track))
            views.setTextViewText(R.id.widget_artist, stringContext.getString(R.string.widget_open_app))
            if (!isCompact) {
                views.setTextViewText(R.id.widget_position, EMPTY_TIME)
                views.setTextViewText(R.id.widget_duration, EMPTY_TIME)
            }
            views.setProgressBar(R.id.widget_progress, PROGRESS_MAX, 0, false)
        } else {
            val track = WidgetStateStore.toTrackEntity(snapshot)
            val artwork = if (track != null) AudioCoverLoader.loadArtworkSync(stringContext, track) else null
            val rounded = artwork?.let { roundCorners(context, scaleBitmap(it, ARTWORK_MAX_SIZE), 18f) }

            if (rounded != null) {
                views.setImageViewBitmap(R.id.widget_artwork, rounded)
            } else {
                views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_notification_music)
            }

            views.setTextViewText(R.id.widget_title, snapshot.title)
            views.setTextViewText(R.id.widget_artist, displayArtistName(stringContext, snapshot.artist))

            if (!isCompact) {
                views.setTextViewText(R.id.widget_position, formatWidgetTime(snapshot.positionMs))
                views.setTextViewText(R.id.widget_duration, formatWidgetTime(snapshot.durationMs))
            }

            val progress = if (snapshot.durationMs > 0L) {
                ((snapshot.positionMs.toFloat() / snapshot.durationMs.toFloat()).coerceIn(0f, 1f) *
                    PROGRESS_MAX).toInt()
            } else 0
            views.setProgressBar(R.id.widget_progress, PROGRESS_MAX, progress, false)
        }

        // The timeline takes the album-art colour when it is usable, otherwise the app accent.
        val timelineColor = artworkColor(snapshot, stringContext) ?: accent

        views.setImageViewResource(
            R.id.widget_play_pause,
            if (snapshot.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
        )
        views.setOnClickPendingIntent(R.id.widget_play_pause, broadcastPendingIntent(context, NovoWidgetProvider.ACTION_PLAY_PAUSE, 12))

        if (isCompact) {
            val openIntent = openAppPendingIntent(context)
            views.setOnClickPendingIntent(R.id.widget_root, openIntent)
            views.setOnClickPendingIntent(R.id.widget_artwork, openIntent)
            views.setOnClickPendingIntent(R.id.widget_title, openIntent)
            views.setOnClickPendingIntent(R.id.widget_artist, openIntent)
        } else {
            views.setOnClickPendingIntent(R.id.widget_prev, broadcastPendingIntent(context, NovoWidgetProvider.ACTION_PREVIOUS, 11))
            views.setOnClickPendingIntent(R.id.widget_next, broadcastPendingIntent(context, NovoWidgetProvider.ACTION_NEXT, 13))
            views.setOnClickPendingIntent(R.id.widget_shuffle, broadcastPendingIntent(context, NovoWidgetProvider.ACTION_TOGGLE_SHUFFLE, 14))
            views.setOnClickPendingIntent(R.id.widget_repeat, broadcastPendingIntent(context, NovoWidgetProvider.ACTION_TOGGLE_REPEAT, 15))
            views.setOnClickPendingIntent(R.id.widget_favorite, broadcastPendingIntent(context, NovoWidgetProvider.ACTION_TOGGLE_FAVORITE, 16))

            views.setImageViewResource(
                R.id.widget_favorite,
                if (snapshot.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )
            views.setImageViewResource(
                R.id.widget_repeat,
                if (snapshot.repeatMode == 2) R.drawable.ic_repeat_one else R.drawable.ic_repeat
            )

            // Active toggles are highlighted, inactive ones stay dimmed.
            tint(views, R.id.widget_shuffle, if (snapshot.isShuffleEnabled) accent else DIMMED)
            tint(views, R.id.widget_repeat, if (snapshot.repeatMode != 0) accent else DIMMED)
            tint(views, R.id.widget_favorite, if (snapshot.isFavorite) FAVORITE_COLOR else DIMMED)
            tint(views, R.id.widget_prev, if (snapshot.hasTrack) Color.WHITE else DIMMED)
            tint(views, R.id.widget_next, if (snapshot.hasTrack) Color.WHITE else DIMMED)

            val openIntent = openAppPendingIntent(context)
            views.setOnClickPendingIntent(R.id.widget_artwork, openIntent)
            views.setOnClickPendingIntent(R.id.widget_title, openIntent)
            views.setOnClickPendingIntent(R.id.widget_artist, openIntent)
            views.setOnClickPendingIntent(R.id.widget_position, openIntent)
            views.setOnClickPendingIntent(R.id.widget_duration, openIntent)
        }

        // Play/pause button: accent coloured circle with a matching icon.
        views.setColorStateList(R.id.widget_play_pause, "setBackgroundTintList", ColorStateList.valueOf(accent))
        views.setColorStateList(R.id.widget_play_pause, "setImageTintList", ColorStateList.valueOf(onAccent(accent)))
        views.setColorStateList(R.id.widget_progress, "setProgressTintList", ColorStateList.valueOf(timelineColor))

        try {
            manager.updateAppWidget(widgetId, views)
        } catch (_: Exception) {
        }
    }

    private const val DIMMED = 0x8AFFFFFF.toInt()
    private val FAVORITE_COLOR = Color.parseColor("#FF5252")

    private fun tint(views: RemoteViews, viewId: Int, color: Int) {
        try {
            views.setColorStateList(viewId, "setImageTintList", ColorStateList.valueOf(color))
        } catch (_: Exception) {
        }
    }

    /** Picks the compact layout for small widgets. */
    private fun isCompactLayout(manager: AppWidgetManager, widgetId: Int): Boolean {
        return try {
            val options = manager.getAppWidgetOptions(widgetId)
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            if (width <= 0 && height <= 0) return false
            width < COMPACT_MAX_WIDTH_DP || height < COMPACT_MAX_HEIGHT_DP
        } catch (_: Exception) {
            false
        }
    }

    private fun localizedContext(base: Context, languageMode: String): Context {
        val deviceLanguage = AppLanguage.deviceLanguage(base)
        return if (AppLanguage.isPersianLanguageCode(deviceLanguage) ==
            AppLanguage.isPersian(languageMode, deviceLanguage)
        ) {
            base
        } else {
            LocaleAwareContext(base, AppLanguage.localeFor(languageMode, deviceLanguage))
        }
    }

    private fun broadcastPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, NovoWidgetProvider::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_NOW_PLAYING, true)
        }
        return PendingIntent.getActivity(
            context,
            20,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Mirrors the accent colors of the in-app theme (see ui/theme/Color.kt). */
    private fun accentColorFor(accentTheme: String): Int = when (accentTheme) {
        "purple" -> 0xFF7C4DFF.toInt()
        "emerald" -> 0xFF00E676.toInt()
        "cyan" -> 0xFF00E5FF.toInt()
        "crimson" -> 0xFFFF5252.toInt()
        else -> 0xFFFFC107.toInt()
    }

    /** Black or white, whichever stays readable on top of the accent colour. */
    private fun onAccent(accent: Int): Int =
        if (ColorUtils.calculateLuminance(accent) > 0.55) Color.BLACK else Color.WHITE

    private fun scaleBitmap(source: Bitmap, maxSize: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= 0 || height <= 0) return source
        if (width <= maxSize && height <= maxSize) return source

        val scale = maxSize.toFloat() / max(width, height).toFloat()
        return try {
            Bitmap.createScaledBitmap(
                source,
                (width * scale).toInt().coerceAtLeast(1),
                (height * scale).toInt().coerceAtLeast(1),
                true
            )
        } catch (_: Exception) {
            source
        }
    }

    /** RemoteViews cannot clip an ImageView, so the artwork is rounded before it is handed over. */
    private fun roundCorners(context: Context, source: Bitmap, radiusDp: Float): Bitmap? {
        return try {
            val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
            val bounds = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
            val radius = radiusDp * context.resources.displayMetrics.density
            canvas.drawRoundRect(bounds, radius, radius, paint)
            paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(source, 0f, 0f, paint)
            output
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Colour picked from the current artwork (nice "album adaptive" timeline). Returns null when the
     * artwork is missing or the result would not be readable.
     */
    private fun artworkColor(snapshot: WidgetSnapshot, stringContext: Context): Int? {
        if (!snapshot.hasTrack) return null
        val track = WidgetStateStore.toTrackEntity(snapshot) ?: return null
        val bitmap = try {
            AudioCoverLoader.loadArtworkSync(stringContext, track)
        } catch (_: Throwable) {
            null
        } ?: return null

        return try {
            val scaled = Bitmap.createScaledBitmap(bitmap, 24, 24, true)
            var red = 0L
            var green = 0L
            var blue = 0L
            var weight = 0L
            val pixel = FloatArray(3)
            for (x in 0 until scaled.width) {
                for (y in 0 until scaled.height) {
                    val color = scaled.getPixel(x, y)
                    ColorUtils.colorToHSL(color, pixel)
                    val saturation = pixel[1]
                    val lightness = pixel[2]
                    if (saturation < 0.25f || lightness < 0.30f || lightness > 0.85f) continue
                    red += Color.red(color)
                    green += Color.green(color)
                    blue += Color.blue(color)
                    weight++
                }
            }
            scaled.recycle()
            if (weight == 0L) return null

            val average = Color.rgb((red / weight).toInt(), (green / weight).toInt(), (blue / weight).toInt())
            // lift very dark results so the timeline stays visible on the dark widget card
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(average, hsl)
            hsl[2] = hsl[2].coerceIn(0.45f, 0.75f)
            ColorUtils.HSLToColor(hsl)
        } catch (_: Throwable) {
            null
        }
    }

    private fun formatWidgetTime(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
