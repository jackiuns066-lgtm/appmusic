package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.EqualizerPresetEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRef
import com.example.data.local.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MusicRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val tag = "MusicRepository"
    private val trackDao = database.trackDao()
    private val playlistDao = database.playlistDao()
    private val equalizerPresetDao = database.equalizerPresetDao()

    val allTracks: Flow<List<TrackEntity>> = trackDao.getAllTracks()
    val favoriteTracks: Flow<List<TrackEntity>> = trackDao.getFavoriteTracks()
    val mostPlayedTracks: Flow<List<TrackEntity>> = trackDao.getMostPlayedTracks()
    val recentlyPlayedTracks: Flow<List<TrackEntity>> = trackDao.getRecentlyPlayedTracks()
    val allPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
    val equalizerPresets: Flow<List<EqualizerPresetEntity>> = equalizerPresetDao.getAllPresets()

    val allFolders: Flow<List<String>> = trackDao.getAllFolders()
    val allArtists: Flow<List<String>> = trackDao.getAllArtists()
    val allAlbums: Flow<List<String>> = trackDao.getAllAlbums()
    val allGenres: Flow<List<String>> = trackDao.getAllGenres()

    fun getTracksForPlaylist(playlistId: String): Flow<List<TrackEntity>> =
        playlistDao.getTracksForPlaylist(playlistId)

    fun getTracksByFolder(folder: String): Flow<List<TrackEntity>> =
        trackDao.getTracksByFolder(folder)

    fun getTracksByArtist(artist: String): Flow<List<TrackEntity>> =
        trackDao.getTracksByArtist(artist)

    fun getTracksByAlbum(album: String): Flow<List<TrackEntity>> =
        trackDao.getTracksByAlbum(album)

    fun getTracksByGenre(genre: String): Flow<List<TrackEntity>> =
        trackDao.getTracksByGenre(genre)

    suspend fun getAllTracksOnce(): List<TrackEntity> = withContext(Dispatchers.IO) {
        trackDao.getAllTracksOnce()
    }

    suspend fun deleteDemoTracks() = withContext(Dispatchers.IO) {
        trackDao.deleteDemoTracks()
    }

    suspend fun deleteRingtonesAndShortTracks(minDurationMs: Long = 15000L) = withContext(Dispatchers.IO) {
        trackDao.deleteShortTracks(minDurationMs)
        trackDao.deleteRingtoneTracks()
    }

    suspend fun insertTracks(tracks: List<TrackEntity>) = withContext(Dispatchers.IO) {
        trackDao.insertTracks(tracks)
    }

    suspend fun setFavorite(trackId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        trackDao.setFavorite(trackId, isFavorite)
    }

    suspend fun recordTrackPlayed(trackId: String) = withContext(Dispatchers.IO) {
        trackDao.incrementPlayCount(trackId)
    }

    suspend fun createPlaylist(name: String, description: String = ""): String = withContext(Dispatchers.IO) {
        val playlistId = "playlist_${UUID.randomUUID().toString().take(8)}"
        playlistDao.insertPlaylist(
            PlaylistEntity(
                id = playlistId,
                name = name,
                description = description
            )
        )
        playlistId
    }

    suspend fun addTrackToPlaylist(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        playlistDao.insertCrossRef(
            PlaylistTrackCrossRef(playlistId = playlistId, trackId = trackId)
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun saveEqualizerPreset(preset: EqualizerPresetEntity) = withContext(Dispatchers.IO) {
        equalizerPresetDao.insertPreset(preset)
    }

    suspend fun initDefaultPresetsIfEmpty() = withContext(Dispatchers.IO) {
        try {
            if (equalizerPresetDao.getPresetCount() == 0) {
                val defaults = listOf(
                    EqualizerPresetEntity(
                        id = "preset_flat",
                        name = "معمولی (Flat)",
                        isSystemPreset = true,
                        bandLevels = "0,0,0,0,0",
                        bassBoostLevel = 0,
                        virtualizerLevel = 0
                    ),
                    EqualizerPresetEntity(
                        id = "preset_bass",
                        name = "بیس قوی (Bass Boost)",
                        isSystemPreset = true,
                        bandLevels = "600,400,0,-200,-300",
                        bassBoostLevel = 800,
                        virtualizerLevel = 200
                    ),
                    EqualizerPresetEntity(
                        id = "preset_vocal",
                        name = "وضوح صدا (Vocal & Acoustic)",
                        isSystemPreset = true,
                        bandLevels = "-200,100,500,600,300",
                        bassBoostLevel = 100,
                        virtualizerLevel = 400
                    ),
                    EqualizerPresetEntity(
                        id = "preset_persian_traditional",
                        name = "سنتی ایرانی (ساز و آواز)",
                        isSystemPreset = true,
                        bandLevels = "100,200,450,550,400",
                        bassBoostLevel = 250,
                        virtualizerLevel = 500
                    ),
                    EqualizerPresetEntity(
                        id = "preset_electronic",
                        name = "الکترونیک و کلاب",
                        isSystemPreset = true,
                        bandLevels = "500,300,-100,400,600",
                        bassBoostLevel = 600,
                        virtualizerLevel = 600
                    )
                )
                equalizerPresetDao.insertPresets(defaults)
            }
        } catch (t: Throwable) {
            Log.e(tag, "Failed to init default presets: ${t.message}")
        }
    }

    suspend fun scanDeviceAudio(minDurationMs: Long = 15000L): Int = withContext(Dispatchers.IO) {
        // First, completely remove any previous demo tracks, ringtones, and ultra-short audio
        try {
            trackDao.deleteDemoTracks()
            trackDao.deleteShortTracks(minDurationMs)
            trackDao.deleteRingtoneTracks()
        } catch (_: Exception) {}

        val scanned = mutableMapOf<String, TrackEntity>()

        // 1. Scan ONLY External MediaStore to prevent system ringtones and notification sounds from internal storage
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.IS_RINGTONE,
            MediaStore.Audio.Media.IS_NOTIFICATION,
            MediaStore.Audio.Media.IS_ALARM
        )

        // Strict filter: duration >= minDurationMs, not a ringtone, not a notification, not an alarm
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(minDurationMs.toString())

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val durationCol = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val dataCol = c.getColumnIndex(MediaStore.Audio.Media.DATA)
                val sizeCol = c.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val dateAddedCol = c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val ringtoneCol = c.getColumnIndex(MediaStore.Audio.Media.IS_RINGTONE)
                val notifCol = c.getColumnIndex(MediaStore.Audio.Media.IS_NOTIFICATION)
                val alarmCol = c.getColumnIndex(MediaStore.Audio.Media.IS_ALARM)

                while (c.moveToNext()) {
                    val mediaId = if (idCol >= 0) c.getLong(idCol) else continue

                    // Explicitly skip ringtones, notification chirps, and alarms
                    val isRingtone = ringtoneCol >= 0 && c.getInt(ringtoneCol) != 0
                    val isNotif = notifCol >= 0 && c.getInt(notifCol) != 0
                    val isAlarm = alarmCol >= 0 && c.getInt(alarmCol) != 0
                    if (isRingtone || isNotif || isAlarm) continue

                    val duration = if (durationCol >= 0) c.getLong(durationCol) else 0L
                    if (duration < minDurationMs) continue

                    val rawTitle = if (titleCol >= 0) c.getString(titleCol) else null
                    val rawArtist = if (artistCol >= 0) c.getString(artistCol) else null
                    val rawAlbum = if (albumCol >= 0) c.getString(albumCol) else null
                    val filePath = if (dataCol >= 0) c.getString(dataCol) else null
                    val size = if (sizeCol >= 0) c.getLong(sizeCol) else 0L
                    val dateAddedSec = if (dateAddedCol >= 0) c.getLong(dateAddedCol) else 0L
                    val addedAt = if (dateAddedSec > 0) dateAddedSec * 1000L else System.currentTimeMillis()

                    // Path blacklist for ringtones and notifications
                    if (isRingtoneOrSystemPath(filePath)) continue

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId)

                    val title = if (!rawTitle.isNullOrBlank() && !rawTitle.equals("<unknown>", true)) {
                        rawTitle
                    } else if (!filePath.isNullOrBlank()) {
                        File(filePath).nameWithoutExtension
                    } else "آهنگ بدون عنوان"

                    val artist = if (!rawArtist.isNullOrBlank() && !rawArtist.equals("<unknown>", true)) rawArtist else "هنرمند نامشخص"
                    val album = if (!rawAlbum.isNullOrBlank() && !rawAlbum.equals("<unknown>", true)) rawAlbum else "آلبوم نامشخص"

                    val folderName = if (!filePath.isNullOrBlank()) {
                        try {
                            File(filePath).parentFile?.name ?: "پوشه اصلی"
                        } catch (_: Exception) {
                            "حافظه دستگاه"
                        }
                    } else "حافظه دستگاه"

                    val isDownloaded = !filePath.isNullOrBlank() && File(filePath).exists()
                    val trackKey = filePath ?: contentUri.toString()

                    scanned[trackKey] = TrackEntity(
                        id = "device_ext_$mediaId",
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        uriString = contentUri.toString(),
                        genre = "محلی",
                        mood = "عادی",
                        bpm = 100,
                        folderPath = folderName,
                        isDownloaded = isDownloaded,
                        downloadProgress = if (isDownloaded) 100 else 0,
                        downloadedFilePath = filePath,
                        fileSizeBytes = size,
                        addedAt = addedAt
                    )
                }
            }
        } catch (t: Throwable) {
            Log.w(tag, "Querying MediaStore failed: ${t.message}")
        }

        // 2. Direct Storage Directory Recursive Scan for any audio format
        val audioExtensions = setOf("mp3", "wav", "m4a", "aac", "flac", "ogg", "opus", "wma", "amr", "3gp", "mid", "mka")
        val directoriesToScan = mutableListOf<File>()

        val candidateDirs = listOf(
            "Music", "Download", "Downloads", "Audiobooks", "Podcasts"
        )

        for (dirName in candidateDirs) {
            try {
                val f = File(android.os.Environment.getExternalStorageDirectory(), dirName)
                if (f.exists() && f.isDirectory) {
                    directoriesToScan.add(f)
                }
            } catch (_: Exception) {}
        }

        val mmr = android.media.MediaMetadataRetriever()
        for (dir in directoriesToScan.distinct()) {
            scanDirectoryRecursively(dir, audioExtensions, scanned, mmr, minDurationMs, maxDepth = 3)
        }
        try {
            mmr.release()
        } catch (_: Exception) {}

        val finalTracks = scanned.values.toList()
        if (finalTracks.isNotEmpty()) {
            val existingTracks = trackDao.getAllTracksOnce().associateBy { it.id }
            val existingByPath = trackDao.getAllTracksOnce().filter { !it.downloadedFilePath.isNullOrBlank() }
                .associateBy { it.downloadedFilePath }

            val mergedTracks = finalTracks.map { track ->
                val existing = existingTracks[track.id] ?: existingByPath[track.downloadedFilePath]
                if (existing != null) {
                    track.copy(
                        isFavorite = existing.isFavorite,
                        playCount = existing.playCount,
                        lastPlayedTimestamp = existing.lastPlayedTimestamp,
                        addedAt = if (existing.addedAt > 0) existing.addedAt else track.addedAt
                    )
                } else {
                    track
                }
            }
            trackDao.insertTracks(mergedTracks)
        }
        finalTracks.size
    }

    private fun isRingtoneOrSystemPath(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        val lower = path.lowercase()
        return lower.contains("/ringtones") ||
                lower.contains("/notifications") ||
                lower.contains("/alarms") ||
                lower.contains("/ui/") ||
                lower.contains("/system/media") ||
                lower.contains("/call_recording") ||
                lower.contains("/audio/ringtones")
    }

    private fun scanDirectoryRecursively(
        dir: File,
        audioExtensions: Set<String>,
        resultMap: MutableMap<String, TrackEntity>,
        mmr: android.media.MediaMetadataRetriever,
        minDurationMs: Long,
        maxDepth: Int
    ) {
        if (maxDepth <= 0 || !dir.exists() || !dir.isDirectory || !dir.canRead()) return

        // Skip ringtones, notifications, and hidden folders
        val dirNameLower = dir.name.lowercase()
        if (dir.name.startsWith(".") ||
            dirNameLower.contains("ringtone") ||
            dirNameLower.contains("notification") ||
            dirNameLower.contains("alarm") ||
            dirNameLower.contains("cache")) return

        val files = try { dir.listFiles() } catch (_: Exception) { null } ?: return

        for (file in files) {
            try {
                if (file.isDirectory) {
                    scanDirectoryRecursively(file, audioExtensions, resultMap, mmr, minDurationMs, maxDepth - 1)
                } else if (file.isFile && file.length() > 150 * 1024) { // Ignore tiny files under 150KB
                    val ext = file.extension.lowercase()
                    if (ext in audioExtensions && !resultMap.containsKey(file.absolutePath)) {
                        if (isRingtoneOrSystemPath(file.absolutePath)) continue

                        var title = file.nameWithoutExtension
                        var artist = "هنرمند نامشخص"
                        var album = "آلبوم نامشخص"
                        var durationMs = 0L

                        try {
                            mmr.setDataSource(file.absolutePath)
                            val metaTitle = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                            val metaArtist = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            val metaAlbum = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM)
                            val metaDur = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)

                            if (!metaTitle.isNullOrBlank()) title = metaTitle
                            if (!metaArtist.isNullOrBlank()) artist = metaArtist
                            if (!metaAlbum.isNullOrBlank()) album = metaAlbum
                            metaDur?.toLongOrNull()?.let { durationMs = it }
                        } catch (_: Exception) {}

                        // Discard if duration is detected and is less than minDurationMs
                        if (durationMs in 1 until minDurationMs) continue

                        val folderName = file.parentFile?.name ?: "پوشه اصلی"
                        val id = "file_${file.absolutePath.hashCode().toLong() and 0xFFFFFFFFL}"

                        resultMap[file.absolutePath] = TrackEntity(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = durationMs,
                            uriString = file.absolutePath,
                            genre = "محلی",
                            mood = "عادی",
                            bpm = 100,
                            folderPath = folderName,
                            isDownloaded = true,
                            downloadProgress = 100,
                            downloadedFilePath = file.absolutePath,
                            fileSizeBytes = file.length()
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
