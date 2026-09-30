package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Query("SELECT * FROM tracks ORDER BY addedAt DESC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY addedAt DESC")
    suspend fun getAllTracksOnce(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY addedAt DESC")
    fun getFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY playCount DESC LIMIT 30")
    fun getMostPlayedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT 30")
    fun getRecentlyPlayedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :trackId LIMIT 1")
    suspend fun getTrackById(trackId: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE uriString = :uriString LIMIT 1")
    suspend fun getTrackByUri(uriString: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE folderPath = :folderPath ORDER BY title ASC")
    fun getTracksByFolder(folderPath: String): Flow<List<TrackEntity>>

    @Query("SELECT DISTINCT folderPath FROM tracks ORDER BY folderPath ASC")
    fun getAllFolders(): Flow<List<String>>

    @Query("SELECT DISTINCT artist FROM tracks ORDER BY artist ASC")
    fun getAllArtists(): Flow<List<String>>

    @Query("SELECT DISTINCT album FROM tracks ORDER BY album ASC")
    fun getAllAlbums(): Flow<List<String>>

    @Query("SELECT DISTINCT genre FROM tracks ORDER BY genre ASC")
    fun getAllGenres(): Flow<List<String>>

    @Query("SELECT * FROM tracks WHERE artist = :artist ORDER BY title ASC")
    fun getTracksByArtist(artist: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE album = :album ORDER BY title ASC")
    fun getTracksByAlbum(album: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE genre = :genre ORDER BY title ASC")
    fun getTracksByGenre(genre: String): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracksIgnore(tracks: List<TrackEntity>)

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :trackId")
    suspend fun setFavorite(trackId: String, isFavorite: Boolean)

    @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayedTimestamp = :timestamp WHERE id = :trackId")
    suspend fun incrementPlayCount(trackId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM tracks WHERE id LIKE 'demo_%'")
    suspend fun deleteDemoTracks()

    @Query("DELETE FROM tracks WHERE durationMs < :minDurationMs AND durationMs > 0")
    suspend fun deleteShortTracks(minDurationMs: Long = 15000L)

    @Query("DELETE FROM tracks WHERE folderPath LIKE '%ringtone%' OR folderPath LIKE '%notification%' OR folderPath LIKE '%alarm%' OR folderPath LIKE '%audio/ringtones%'")
    suspend fun deleteRingtoneTracks()

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrack(trackId: String)
}
