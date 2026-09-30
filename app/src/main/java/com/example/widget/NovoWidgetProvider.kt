package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.audio.MediaControlReceiver
import com.example.data.audio.MediaNotificationManager
import com.example.ui.components.AudioCoverLoader
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocaleAwareContext
import com.example.ui.i18n.displayArtistName
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val PROGRESS_MAX = 1000
private const val ARTWORK_MAX_SIZE = 256
private const val EMPTY_TIME = "--:--"

/**
 * Home screen widget: cover art, song title / artist, icon buttons (previous, play-pause, next)
 * and a progress timeline of the current song.
 */
class NovoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WidgetUpdater.refreshAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_PLAY_PAUSE, ACTION_NEXT, ACTION_PREVIOUS -> {
                sendPlaybackAction(context, intent.action.orEmpty())
                WidgetUpdater.refreshAll(context)
            }
        }
    }

    private fun sendPlaybackAction(context: Context, widgetAction: String) {
        val receiverAction = when (widgetAction) {
            ACTION_NEXT -> MediaNotificationManager.ACTION_NEXT
            ACTION_PREVIOUS -> MediaNotificationManager.ACTION_PREVIOUS
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
        val views = RemoteViews(context.packageName, R.layout.widget_now_playing)

        if (!snapshot.hasTrack) {
            views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_notification_music)
            views.setTextViewText(R.id.widget_title, stringContext.getString(R.string.widget_no_track))
            views.setTextViewText(R.id.widget_artist, stringContext.getString(R.string.app_name))
            views.setTextViewText(R.id.widget_position, EMPTY_TIME)
            views.setTextViewText(R.id.widget_duration, EMPTY_TIME)
            views.setProgressBar(R.id.widget_progress, PROGRESS_MAX, 0, false)
        } else {
            val track = WidgetStateStore.toTrackEntity(snapshot)
            val artwork = if (track != null) AudioCoverLoader.loadArtworkSync(stringContext, track) else null
            if (artwork != null) {
                views.setImageViewBitmap(R.id.widget_artwork, scaleBitmap(artwork, ARTWORK_MAX_SIZE))
            } else {
                views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_notification_music)
            }

            views.setTextViewText(R.id.widget_title, snapshot.title)
            views.setTextViewText(R.id.widget_artist, displayArtistName(stringContext, snapshot.artist))
            views.setTextViewText(R.id.widget_position, formatWidgetTime(snapshot.positionMs))
            views.setTextViewText(R.id.widget_duration, formatWidgetTime(snapshot.durationMs))

            val progress = if (snapshot.durationMs > 0L) {
                ((snapshot.positionMs.toFloat() / snapshot.durationMs.toFloat()).coerceIn(0f, 1f) *
                    PROGRESS_MAX).toInt()
            } else 0
            views.setProgressBar(R.id.widget_progress, PROGRESS_MAX, progress, false)
        }

        views.setImageViewResource(
            R.id.widget_play_pause,
            if (snapshot.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
        )

        views.setOnClickPendingIntent(R.id.widget_prev, broadcastPendingIntent(context, NovoWidgetProvider.ACTION_PREVIOUS, 11))
        views.setOnClickPendingIntent(R.id.widget_play_pause, broadcastPendingIntent(context, NovoWidgetProvider.ACTION_PLAY_PAUSE, 12))
        views.setOnClickPendingIntent(R.id.widget_next, broadcastPendingIntent(context, NovoWidgetProvider.ACTION_NEXT, 13))

        val openIntent = openAppPendingIntent(context)
        views.setOnClickPendingIntent(R.id.widget_artwork, openIntent)
        views.setOnClickPendingIntent(R.id.widget_title, openIntent)
        views.setOnClickPendingIntent(R.id.widget_artist, openIntent)

        applyAccent(views, snapshot.accentTheme)

        try {
            manager.updateAppWidget(widgetId, views)
        } catch (_: Exception) {
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
            14,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun applyAccent(views: RemoteViews, accentTheme: String) {
        val color = accentColorFor(accentTheme)
        try {
            views.setInt(R.id.widget_progress, "setProgressTintList", ColorStateList.valueOf(color))
        } catch (_: Exception) {
        }
        try {
            views.setInt(R.id.widget_play_pause, "setColorFilter", color)
        } catch (_: Exception) {
        }
    }

    /** Mirrors the accent colors of the in-app theme (see ui/theme/Color.kt). */
    private fun accentColorFor(accentTheme: String): Int = when (accentTheme) {
        "purple" -> 0xFF7C4DFF.toInt()
        "emerald" -> 0xFF00E676.toInt()
        "cyan" -> 0xFF00E5FF.toInt()
        "crimson" -> 0xFFFF5252.toInt()
        else -> 0xFFFFC107.toInt()
    }

    private fun scaleBitmap(source: Bitmap, maxSize: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= 0 || height <= 0) return source
        if (width <= maxSize && height <= maxSize) return source

        val scale = maxSize.toFloat() / maxOf(width, height).toFloat()
        val newWidth = (width * scale).toInt().coerceAtLeast(1)
        val newHeight = (height * scale).toInt().coerceAtLeast(1)
        return try {
            Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
        } catch (_: Exception) {
            source
        }
    }

    private fun formatWidgetTime(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
