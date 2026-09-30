package com.example.data.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.TrackEntity
import com.example.ui.components.AudioCoverLoader
import com.example.ui.i18n.displayAlbumName
import com.example.ui.i18n.displayArtistName

class MediaNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "ava_music_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE = "com.example.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.ACTION_PREVIOUS"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel(context)
    }

    private fun createNotificationChannel(stringContext: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                stringContext.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = stringContext.getString(R.string.notif_channel_desc)
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateNotification(
        track: TrackEntity?,
        isPlaying: Boolean,
        isEnabled: Boolean,
        stringContext: Context = context
    ) {
        if (!isEnabled || track == null) {
            dismissNotification()
            return
        }

        createNotificationChannel(stringContext)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Previous intent
        val prevIntent = Intent(context, MediaControlReceiver::class.java).apply { action = ACTION_PREVIOUS }
        val prevPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Play/Pause intent
        val playPauseIntent = Intent(context, MediaControlReceiver::class.java).apply { action = ACTION_PLAY_PAUSE }
        val playPausePendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Next intent
        val nextIntent = Intent(context, MediaControlReceiver::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
        val playPauseTitle = stringContext.getString(if (isPlaying) R.string.notif_action_pause else R.string.notif_action_play)

        val artworkBitmap = AudioCoverLoader.loadArtworkSync(context, track)
        val artistLabel = displayArtistName(stringContext, track.artist)
        val albumLabel = displayAlbumName(stringContext, track.album)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_music)
            .setContentTitle(track.title)
            .setContentText("$artistLabel • $albumLabel")
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Displays on Lock Screen
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(R.drawable.ic_skip_previous, stringContext.getString(R.string.notif_action_previous), prevPendingIntent)
            .addAction(playPauseIcon, playPauseTitle, playPausePendingIntent)
            .addAction(R.drawable.ic_skip_next, stringContext.getString(R.string.notif_action_next), nextPendingIntent)

        if (artworkBitmap != null) {
            builder.setLargeIcon(artworkBitmap)
        }

        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (_: Exception) {}
    }

    fun dismissNotification() {
        try {
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }
}
