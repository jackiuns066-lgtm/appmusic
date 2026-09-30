package com.example.data.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.R
import com.example.data.local.TrackEntity
import java.io.File

/**
 * Shares a song with other apps.
 *
 * Local files (downloaded / imported audio and files scanned from the device) are exposed through
 * a [FileProvider] so the receiving app gets a temporary read permission, MediaStore entries are
 * shared with their content URI. If the audio itself cannot be shared, a text message with the
 * song title and artist is sent instead.
 */
object TrackSharing {

    fun shareTrack(
        context: Context,
        track: TrackEntity,
        chooserTitle: String,
        subject: String,
        text: String
    ) {
        val audioUri = audioUriFor(context, track)

        val intents = mutableListOf<Intent>()

        if (audioUri != null) {
            intents += Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, audioUri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            intents += Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, audioUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        // Text only fallback (also used when no app can receive the audio file)
        intents += Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }

        val primary = intents.removeAt(0)
        val chooser = Intent.createChooser(primary, chooserTitle).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (intents.isNotEmpty()) {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray())
            }
        }

        try {
            context.startActivity(chooser)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context.applicationContext, R.string.share_unavailable, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context.applicationContext, R.string.share_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    /** Returns a shareable URI for the audio of [track], or null when the file is gone. */
    private fun audioUriFor(context: Context, track: TrackEntity): Uri? {
        val filePath = track.downloadedFilePath
        if (!filePath.isNullOrBlank()) {
            val file = File(filePath)
            if (file.exists()) {
                fileProviderUri(context, file)?.let { return it }
            }
        }

        val parsed = try {
            Uri.parse(track.uriString)
        } catch (_: Exception) {
            null
        }

        return when (parsed?.scheme?.lowercase()) {
            "content" -> parsed
            "file" -> parsed.path?.let { path -> fileProviderUri(context, File(path)) }
            null -> null
            else -> {
                val file = File(track.uriString)
                if (file.exists()) fileProviderUri(context, file) else null
            }
        }
    }

    private fun fileProviderUri(context: Context, file: File): Uri? = try {
        FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", file)
    } catch (_: Exception) {
        null
    }
}
