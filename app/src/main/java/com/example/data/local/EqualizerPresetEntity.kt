package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equalizer_presets")
data class EqualizerPresetEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val isSystemPreset: Boolean = false,
    val bandLevels: String = "0,0,0,0,0",
    val bassBoostLevel: Int = 0,
    val virtualizerLevel: Int = 0
)
