package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.PlaybackState
import com.example.data.local.EqualizerPresetEntity

@Composable
fun EqualizerScreen(
    presets: List<EqualizerPresetEntity>,
    playbackState: PlaybackState,
    onBandLevelChange: (band: Short, levelMilliBels: Short) -> Unit,
    onBassBoostChange: (strength: Short) -> Unit,
    onVirtualizerChange: (strength: Short) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onApplyPreset: (EqualizerPresetEntity) -> Unit,
    onSaveCustomPreset: (name: String, bandLevels: List<Short>, bass: Int, virtualizer: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEnabled by remember { mutableStateOf(true) }
    var selectedPresetId by remember { mutableStateOf<String?>("preset_flat") }

    val bandFrequencies = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
    val bandLevels = remember { mutableStateListOf(0, 0, 0, 0, 0) }
    var bassBoostVal by remember { mutableIntStateOf(0) }
    var virtualizerVal by remember { mutableIntStateOf(0) }
    var speedVal by remember { mutableFloatStateOf(playbackState.playbackSpeed) }

    var showSaveDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("equalizer_screen")
    ) {
        // Master Switch Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "اکولایزر صوتی و تقویت‌کننده",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEnabled) "افکت‌ها فعال هستند" else "افکت‌ها غیرفعال هستند",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it },
                    modifier = Modifier.testTag("equalizer_master_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Presets Chips
        Text(
            text = "پریست‌های آماده",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(presets) { preset ->
                FilterChip(
                    selected = selectedPresetId == preset.id,
                    onClick = {
                        selectedPresetId = preset.id
                        onApplyPreset(preset)
                        val levels = preset.bandLevels.split(",").mapNotNull { it.trim().toIntOrNull() }
                        levels.forEachIndexed { i, lv ->
                            if (i < bandLevels.size) bandLevels[i] = lv
                        }
                        bassBoostVal = preset.bassBoostLevel
                        virtualizerVal = preset.virtualizerLevel
                    },
                    label = { Text(preset.name) },
                    modifier = Modifier.testTag("preset_chip_${preset.id}")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5-Band Equalizer Sliders
        Text(
            text = "فرکانس‌های صدا (۵ باند)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                bandFrequencies.forEachIndexed { index, freqLabel ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = freqLabel,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(60.dp)
                        )
                        Slider(
                            value = bandLevels[index].toFloat(),
                            onValueChange = { newValue ->
                                bandLevels[index] = newValue.toInt()
                                selectedPresetId = null
                                onBandLevelChange(index.toShort(), newValue.toInt().toShort())
                            },
                            valueRange = -1000f..1000f,
                            enabled = isEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("eq_band_slider_$index")
                        )
                        Text(
                            text = "${bandLevels[index] / 100}dB",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(44.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bass Boost & Virtualizer
        Text(
            text = "تقویت بیس و صدای فراگیر (۳ بعدی)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Bass Boost
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Equalizer, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تقویت بیس (Bass Boost): ${bassBoostVal / 10}٪",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                Slider(
                    value = bassBoostVal.toFloat(),
                    onValueChange = {
                        bassBoostVal = it.toInt()
                        onBassBoostChange(it.toInt().toShort())
                    },
                    valueRange = 0f..1000f,
                    enabled = isEnabled,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Virtualizer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.SurroundSound, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "صدای فراگیر (Virtualizer): ${virtualizerVal / 10}٪",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                Slider(
                    value = virtualizerVal.toFloat(),
                    onValueChange = {
                        virtualizerVal = it.toInt()
                        onVirtualizerChange(it.toInt().toShort())
                    },
                    valueRange = 0f..1000f,
                    enabled = isEnabled,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Playback Speed
        Text(
            text = "سرعت پخش صدا",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Speed, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "سرعت پخش: ${String.format("%.2f", speedVal)}x",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                Slider(
                    value = speedVal,
                    onValueChange = {
                        speedVal = it
                        onPlaybackSpeedChange(it)
                    },
                    valueRange = 0.5f..2.0f,
                    steps = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { showSaveDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("ذخیره به عنوان پریست اختصاصی")
        }
    }

    if (showSaveDialog) {
        var presetName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("ذخیره پریست شخصی") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("نام پریست") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            onSaveCustomPreset(
                                presetName,
                                bandLevels.map { it.toShort() },
                                bassBoostVal,
                                virtualizerVal
                            )
                            showSaveDialog = false
                        }
                    },
                    enabled = presetName.isNotBlank()
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}
