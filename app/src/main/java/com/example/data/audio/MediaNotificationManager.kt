package com.example.data.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.MainActivity
import com.example.R
import com.example.data.local.TrackEntity
import com.example.ui.components.AudioCoverLoader
import com.example.ui.i18n.displayArtistName

/**
 * Lock screen / notification controls.
 *
 * The notification is a real **media** notification: it is backed by a [MediaSessionCompat], which is
 * what makes Android show the professional player card — icon buttons (previous, play/pause, next)
 * plus the song timeline — in the shade and on the lock screen, instead of a plain notification with
 * hidden actions.
 *
 * Content rules requested by the product owner: song title + artist only (no album, no extra text).
 */
class MediaNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "ava_music_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE = "com.example.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.ACTION_PREVIOUS"
        const val ACTION_PLAY = "com.example.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.ACTION_PAUSE"
        const val ACTION_SEEK = "com.example.ACTION_SEEK"
        const val ACTION_TOGGLE_SHUFFLE = "com.example.ACTION_TOGGLE_SHUFFLE"
        const val ACTION_TOGGLE_REPEAT = "com.example.ACTION_TOGGLE_REPEAT"
        const val ACTION_TOGGLE_FAVORITE = "com.example.ACTION_TOGGLE_FAVORITE"

        const val EXTRA_SEEK_POSITION = "com.example.extra.SEEK_POSITION"

        private const val SESSION_TAG = "novo_playback"
        private const val ARTWORK_MAX_SIZE = 512
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var mediaSession: MediaSessionCompat? = null

    /** Text of the notification that is currently posted, used to avoid useless re-posts. */
    private var postedContentKey: String = ""

    private var cachedArtworkKey: String? = null
    private var cachedArtwork: Bitmap? = null

    init {
        createNotificationChannel(context)
    }

    private fun createNotificationChannel(stringContext: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
            val channel = NotificationChannel(
                CHANNEL_ID,
                stringContext.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = stringContext.getString(R.string.notif_channel_desc)
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            if (existing == null) {
                notificationManager.createNotificationChannel(channel)
            } else {
                // Keep name/description in sync with the selected app language.
                existing.name = stringContext.getString(R.string.notif_channel_name)
                existing.description = stringContext.getString(R.string.notif_channel_desc)
                existing.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                notificationManager.createNotificationChannel(existing)
            }
        }
    }

    /**
     * Called on every playback state change (position included). The session is refreshed on every
     * call so the timeline moves smoothly, while the notification itself is only re-posted when the
     * visible content actually changes.
     */
    fun updatePlayback(
        track: TrackEntity?,
        isPlaying: Boolean,
        isEnabled: Boolean,
        positionMs: Long = 0L,
        durationMs: Long = 0L,
        stringContext: Context = context
    ) {
        if (!isEnabled || track == null) {
            dismissNotification()
            return
        }

        createNotificationChannel(stringContext)

        val title = track.title
        val artist = displayArtistName(stringContext, track.artist)
        val artwork = artworkFor(stringContext, track)

        val session = ensureSession()
        updateSession(session, title, artist, artwork, isPlaying, positionMs, durationMs)

        val contentKey = "$title|$artist|$isPlaying|${track.id}"
        if (contentKey == postedContentKey) return
        postedContentKey = contentKey

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_music)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(openAppPendingIntent())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // show on the lock screen
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(
                R.drawable.ic_skip_previous,
                stringContext.getString(R.string.notif_action_previous),
                broadcastPendingIntent(ACTION_PREVIOUS, 1)
            )
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
                stringContext.getString(if (isPlaying) R.string.notif_action_pause else R.string.notif_action_play),
                broadcastPendingIntent(ACTION_PLAY_PAUSE, 2)
            )
            .addAction(
                R.drawable.ic_skip_next,
                stringContext.getString(R.string.notif_action_next),
                broadcastPendingIntent(ACTION_NEXT, 3)
            )
            .setStyle(
                MediaStyle()
                    .setMediaSession(session.sessionToken)
                    // keeps previous / play-pause / next visible even when the card is collapsed
                    .setShowActionsInCompactView(0, 1, 2)
            )

        if (artwork != null) {
            builder.setLargeIcon(artwork)
        }

        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (_: Exception) {
        }
    }

    /** Kept for callers that only have the basic information. */
    fun updateNotification(
        track: TrackEntity?,
        isPlaying: Boolean,
        isEnabled: Boolean,
        stringContext: Context = context
    ) = updatePlayback(track, isPlaying, isEnabled, 0L, track?.durationMs ?: 0L, stringContext)

    fun dismissNotification() {
        postedContentKey = ""
        try {
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {
        }
        try {
            mediaSession?.isActive = false
        } catch (_: Exception) {
        }
    }

    // ── media session ────────────────────────────────────────────────────────

    private fun ensureSession(): MediaSessionCompat {
        mediaSession?.let { session ->
            if (!session.isActive) {
                try {
                    session.isActive = true
                } catch (_: Exception) {
                }
            }
            return session
        }

        val session = MediaSessionCompat(context, SESSION_TAG).apply {
            setCallback(object : MediaSessionCompat.Callback() {
                // Headset buttons / Bluetooth / Android Auto / system media controls
                override fun onPlay() = sendAction(ACTION_PLAY)
                override fun onPause() = sendAction(ACTION_PAUSE)
                override fun onSkipToNext() = sendAction(ACTION_NEXT)
                override fun onSkipToPrevious() = sendAction(ACTION_PREVIOUS)
                override fun onSeekTo(pos: Long) = sendSeek(pos)
            })
            setSessionActivity(openAppPendingIntent())
            isActive = true
        }
        mediaSession = session
        return session
    }

    private fun updateSession(
        session: MediaSessionCompat,
        title: String,
        artist: String,
        artwork: Bitmap?,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long
    ) {
        try {
            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs.coerceAtLeast(0L))
                .apply { if (artwork != null) putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork) }
                .build()
            session.setMetadata(metadata)

            val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO

            val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
            val playbackState = PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, positionMs.coerceAtLeast(0L), if (isPlaying) 1f else 0f, SystemClock.elapsedRealtime())
                .build()
            session.setPlaybackState(playbackState)
        } catch (_: Exception) {
        }
    }

    private fun sendAction(action: String) {
        try {
            context.sendBroadcast(Intent(context, MediaControlReceiver::class.java).apply { this.action = action })
        } catch (_: Exception) {
        }
    }

    private fun sendSeek(positionMs: Long) {
        try {
            context.sendBroadcast(
                Intent(context, MediaControlReceiver::class.java).apply {
                    action = ACTION_SEEK
                    putExtra(EXTRA_SEEK_POSITION, positionMs)
                }
            )
        } catch (_: Exception) {
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun artworkFor(stringContext: Context, track: TrackEntity): Bitmap? {
        val key = track.id + "|" + (track.downloadedFilePath ?: track.uriString)
        if (key == cachedArtworkKey) return cachedArtwork

        val bitmap = try {
            AudioCoverLoader.loadArtworkSync(stringContext, track)
        } catch (_: Throwable) {
            null
        }
        val scaled = bitmap?.let { scaleArtwork(it) }
        cachedArtworkKey = key
        cachedArtwork = scaled
        return scaled
    }

    /** Media metadata crosses a Binder boundary, so keep the artwork small. */
    private fun scaleArtwork(source: Bitmap): Bitmap? {
        val width = source.width
        val height = source.height
        if (width <= 0 || height <= 0) return null
        if (width <= ARTWORK_MAX_SIZE && height <= ARTWORK_MAX_SIZE) return source
        return try {
            val scale = ARTWORK_MAX_SIZE.toFloat() / maxOf(width, height).toFloat()
            Bitmap.createScaledBitmap(
                source,
                (width * scale).toInt().coerceAtLeast(1),
                (height * scale).toInt().coerceAtLeast(1),
                true
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun openAppPendingIntent(): PendingIntent {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun broadcastPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MediaControlReceiver::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
