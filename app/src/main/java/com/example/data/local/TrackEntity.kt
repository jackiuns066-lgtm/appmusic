package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String = "هنرمند ناشناس",
    val album: String = "آلبوم نامشخص",
    val durationMs: Long = 0L,
    val uriString: String,
    val genre: String = "نامشخص",
    val mood: String = "آرامش‌بخش",
    val bpm: Int = 100,
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    val lastPlayedTimestamp: Long = 0L,
    val folderPath: String = "پوشه اصلی",
    val addedAt: Long = System.currentTimeMillis(),
    val isDownloaded: Boolean = true,
    val downloadProgress: Int = 100,
    val downloadedFilePath: String? = null,
    val fileSizeBytes: Long = 0L
)
