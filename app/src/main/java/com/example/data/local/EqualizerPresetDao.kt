package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EqualizerPresetDao {

    @Query("SELECT * FROM equalizer_presets ORDER BY isSystemPreset DESC, name ASC")
    fun getAllPresets(): Flow<List<EqualizerPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: EqualizerPresetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<EqualizerPresetEntity>)

    @Query("DELETE FROM equalizer_presets WHERE id = :presetId AND isSystemPreset = 0")
    suspend fun deleteCustomPreset(presetId: String)

    @Query("SELECT COUNT(*) FROM equalizer_presets")
    suspend fun getPresetCount(): Int
}
