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
                val skipNext = playerEngine.onSkipNextRequested ?: playerEngine.onTrackCompletedCallback
                skipNext?.invoke()
            }
            MediaNotificationManager.ACTION_PREVIOUS -> {
                val skipPrevious = playerEngine.onSkipPreviousRequested
                if (skipPrevious != null) {
                    skipPrevious.invoke()
                } else {
                    playerEngine.seekTo(0L)
                }
            }
        }
    }
}
