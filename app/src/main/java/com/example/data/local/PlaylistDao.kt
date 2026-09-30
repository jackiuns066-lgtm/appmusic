package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(ref: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_track_cross_ref ref ON t.id = ref.trackId
        WHERE ref.playlistId = :playlistId
        ORDER BY ref.addedAt DESC
    """)
    fun getTracksForPlaylist(playlistId: String): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM playlist_track_cross_ref WHERE playlistId = :playlistId")
    fun getTrackCountForPlaylist(playlistId: String): Flow<Int>
}
