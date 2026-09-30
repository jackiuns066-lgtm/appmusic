package com.example.data.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.AvaMusicApp

class MediaControlReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? AvaMusicApp ?: return
        val playerEngine = app.playerEngine

        when (intent.action) {
            MediaNotificationManager.ACTION_PLAY_PAUSE -> {
                playerEngine.togglePlayPause()
            }
            MediaNotificationManager.ACTION_NEXT -> {
                playerEngine.onTrackCompletedCallback?.invoke()
            }
            MediaNotificationManager.ACTION_PREVIOUS -> {
                playerEngine.seekTo(0L)
            }
        }
    }
}
