package com.example.ui.components

import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.TrackEntity
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AudioCoverLoader {
    private val memoryCache = object : LruCache<String, Bitmap>(50 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    suspend fun loadArtwork(context: android.content.Context, track: TrackEntity): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = track.downloadedFilePath ?: track.uriString
        memoryCache.get(cacheKey)?.let { return@withContext it }

        // 1. Try MediaMetadataRetriever embedded picture (ID3 APIC frame)
        var bitmap: Bitmap? = null
        try {
            val mmr = MediaMetadataRetriever()
            if (!track.downloadedFilePath.isNullOrBlank() && File(track.downloadedFilePath).exists()) {
                mmr.setDataSource(track.downloadedFilePath)
            } else {
                mmr.setDataSource(context, Uri.parse(track.uriString))
            }
            val artBytes = mmr.embeddedPicture
            mmr.release()

            if (artBytes != null && artBytes.isNotEmpty()) {
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, opts)
            }
        } catch (_: Throwable) {}

        // 2. Try MediaStore album art URI if available
        if (bitmap == null && track.uriString.contains("media/external/audio/media/")) {
            try {
                val idPart = track.uriString.substringAfterLast("/")
                val mediaId = idPart.toLongOrNull()
                if (mediaId != null) {
                    val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                    val albumArtUri = ContentUris.withAppendedId(sArtworkUri, mediaId)
                    context.contentResolver.openInputStream(albumArtUri)?.use { input ->
                        bitmap = BitmapFactory.decodeStream(input)
                    }
                }
            } catch (_: Throwable) {}
        }

        // 3. Try looking for album art image file in track's parent directory (cover.jpg, folder.jpg)
        if (bitmap == null && !track.downloadedFilePath.isNullOrBlank()) {
            try {
                val parentDir = File(track.downloadedFilePath).parentFile
                if (parentDir != null && parentDir.isDirectory) {
                    val coverFile = parentDir.listFiles()?.firstOrNull { file ->
                        val name = file.name.lowercase()
                        (name.startsWith("cover") || name.startsWith("folder") || name.startsWith("album") || name.startsWith("art")) &&
                                (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp"))
                    }
                    if (coverFile != null && coverFile.exists()) {
                        bitmap = BitmapFactory.decodeFile(coverFile.absolutePath)
                    }
                }
            } catch (_: Throwable) {}
        }

        if (bitmap != null) {
            memoryCache.put(cacheKey, bitmap)
        }
        return@withContext bitmap
    }

    fun loadArtworkSync(context: android.content.Context, track: TrackEntity): Bitmap? {
        val cacheKey = track.downloadedFilePath ?: track.uriString
        memoryCache.get(cacheKey)?.let { return it }

        var bitmap: Bitmap? = null
        try {
            val mmr = MediaMetadataRetriever()
            if (!track.downloadedFilePath.isNullOrBlank() && File(track.downloadedFilePath).exists()) {
                mmr.setDataSource(track.downloadedFilePath)
            } else {
                mmr.setDataSource(context, Uri.parse(track.uriString))
            }
            val artBytes = mmr.embeddedPicture
            mmr.release()

            if (artBytes != null && artBytes.isNotEmpty()) {
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, opts)
            }
        } catch (_: Throwable) {}

        if (bitmap == null && track.uriString.contains("media/external/audio/media/")) {
            try {
                val idPart = track.uriString.substringAfterLast("/")
                val mediaId = idPart.toLongOrNull()
                if (mediaId != null) {
                    val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                    val albumArtUri = ContentUris.withAppendedId(sArtworkUri, mediaId)
                    context.contentResolver.openInputStream(albumArtUri)?.use { input ->
                        bitmap = BitmapFactory.decodeStream(input)
                    }
                }
            } catch (_: Throwable) {}
        }

        if (bitmap != null) {
            memoryCache.put(cacheKey, bitmap)
        }
        return bitmap
    }
}

@Composable
fun TrackCoverImage(
    track: TrackEntity,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shapeRadius: Dp = 10.dp,
    fallbackTint: Color = MaterialTheme.colorScheme.primary
) {
    val context = LocalContext.current
    var bitmap by remember(track.id, track.uriString, track.downloadedFilePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(track.id, track.uriString, track.downloadedFilePath) {
        bitmap = AudioCoverLoader.loadArtwork(context, track)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(shapeRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = stringResource(R.string.cd_track_cover, track.title),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = fallbackTint,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

/**
 * Album art variant that fills the size given by [modifier] (used by the now playing screen where
 * the artwork covers the whole visualizer frame).
 */
@Composable
fun TrackCoverImageFill(
    track: TrackEntity,
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 24.dp,
    fallbackTint: Color = MaterialTheme.colorScheme.primary
) {
    val context = LocalContext.current
    var bitmap by remember(track.id, track.uriString, track.downloadedFilePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(track.id, track.uriString, track.downloadedFilePath) {
        bitmap = AudioCoverLoader.loadArtwork(context, track)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(shapeRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = stringResource(R.string.cd_track_cover, track.title),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = fallbackTint,
                modifier = Modifier.fillMaxSize(0.3f)
            )
        }
    }
}
