package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.data.brand.BrandLinks
import com.example.ui.AppSettings
import com.example.ui.i18n.AppLanguage

@Composable
fun SettingsScreen(
    settings: AppSettings,
    sleepTimerRemaining: Int?,
    onSetLanguage: (String) -> Unit,
    onToggleAmoled: () -> Unit,
    onSetAccentTheme: (String) -> Unit,
    onSetNowPlayingStyle: (String) -> Unit,
    onSetMinDuration: (Int) -> Unit,
    onSetCrossfade: (Int) -> Unit,
    onTogglePauseOnHeadset: () -> Unit,
    onToggleHifi: () -> Unit,
    onToggleLockScreenControls: () -> Unit,
    onStartSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onScanAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onScanAudio()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Professional & Visual Customization Section
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.settings_section_pro),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.settings_visual_style_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.settings_visual_style_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                val visualizerOptions = listOf(
                    Triple("neon_vinyl", stringResource(R.string.style_neon_vinyl), stringResource(R.string.style_neon_vinyl_desc)),
                    Triple("spectrum_wave", stringResource(R.string.style_spectrum_wave), stringResource(R.string.style_spectrum_wave_desc)),
                    Triple("glass_3d", stringResource(R.string.style_glass_3d), stringResource(R.string.style_glass_3d_desc)),
                    Triple("pulse_rings", stringResource(R.string.style_pulse_rings), stringResource(R.string.style_pulse_rings_desc))
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    visualizerOptions.forEach { (code, title, desc) ->
                        val isSelected = settings.nowPlayingStyle == code
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSetNowPlayingStyle(code) },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Minimum Duration Filter (Exclude ringtones)
                Text(
                    text = stringResource(R.string.settings_min_duration_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.settings_min_duration_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val durationOptions = listOf(
                        15 to stringResource(R.string.duration_15s),
                        30 to stringResource(R.string.duration_30s),
                        60 to stringResource(R.string.duration_60s)
                    )
                    durationOptions.forEach { (sec, label) ->
                        FilterChip(
                            selected = settings.minDurationSeconds == sec,
                            onClick = { onSetMinDuration(sec) },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Crossfade
                Text(
                    text = stringResource(R.string.settings_crossfade_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val crossfadeOptions = listOf(
                        0 to stringResource(R.string.action_off),
                        3 to stringResource(R.string.crossfade_3s),
                        5 to stringResource(R.string.crossfade_5s),
                        8 to stringResource(R.string.crossfade_8s)
                    )
                    crossfadeOptions.forEach { (sec, label) ->
                        FilterChip(
                            selected = settings.crossfadeSeconds == sec,
                            onClick = { onSetCrossfade(sec) },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pause on Headset Disconnect
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Headphones, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_headset_title), style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.settings_headset_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.pauseOnHeadsetDisconnect,
                        onCheckedChange = { onTogglePauseOnHeadset() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Hi-Fi Audio Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_hifi_title), style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.settings_hifi_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.hifiAudioMode,
                        onCheckedChange = { onToggleHifi() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Lock Screen & Notification Controls Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_lockscreen_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.settings_lockscreen_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.lockScreenControlsEnabled,
                        onCheckedChange = { onToggleLockScreenControls() }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Appearance Section
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_appearance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Amoled Dark Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.DarkMode, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_amoled), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(R.string.settings_amoled_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.isAmoledDark,
                        onCheckedChange = { onToggleAmoled() },
                        modifier = Modifier.testTag("amoled_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App language (system / Persian / English)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_language_title), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(R.string.settings_language_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val languageOptions = listOf(
                        AppLanguage.MODE_SYSTEM to stringResource(R.string.language_system),
                        AppLanguage.MODE_PERSIAN to stringResource(R.string.language_persian),
                        AppLanguage.MODE_ENGLISH to stringResource(R.string.language_english)
                    )
                    languageOptions.forEach { (mode, label) ->
                        FilterChip(
                            selected = settings.languageMode == mode,
                            onClick = { onSetLanguage(mode) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("language_chip_" + mode)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Accent Theme Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.ColorLens, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.settings_accent_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.settings_accent_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Horizontal Row of Color Swatches
                val colorThemes = listOf(
                    Triple("gold", stringResource(R.string.color_gold), Color(0xFFFFC107)),
                    Triple("purple", stringResource(R.string.color_purple), Color(0xFF7C4DFF)),
                    Triple("emerald", stringResource(R.string.color_emerald), Color(0xFF00E676)),
                    Triple("cyan", stringResource(R.string.color_cyan), Color(0xFF00E5FF)),
                    Triple("crimson", stringResource(R.string.color_crimson), Color(0xFFFF5252))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorThemes.forEach { (code, name, color) ->
                        val isSelected = settings.accentTheme == code
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSetAccentTheme(code) }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier.border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = stringResource(R.string.cd_selected),
                                        tint = if (code == "gold" || code == "cyan" || code == "emerald") Color.Black else Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tools Section
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_tools_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sleep Timer Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSleepTimerDialog = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Timer, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_sleep_timer), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (sleepTimerRemaining != null) stringResource(R.string.settings_sleep_timer_remaining, sleepTimerRemaining) else stringResource(R.string.settings_disabled),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (sleepTimerRemaining != null) {
                        Button(onClick = onCancelSleepTimer) {
                            Text(stringResource(R.string.settings_turn_off))
                        }
                    } else {
                        Button(onClick = { showSleepTimerDialog = true }) {
                            Text(stringResource(R.string.action_set))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Re-scan Device Audio
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.READ_MEDIA_AUDIO
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                            permissionLauncher.launch(perm)
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_rescan_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.settings_rescan_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About Novo & Developer Info Section
        val context = LocalContext.current
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.settings_about_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.settings_about_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.settings_about_author),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Visit Website, Rate App, Share App
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://webnovo.ir"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_website), fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_share_subject))
                                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.app_share_text))
                            }
                            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.app_share_chooser)))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_share_app), fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Brand section: our other work + a measurable way to support the app
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.settings_brand_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.settings_brand_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                BrandLinkRow(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.brand_website_title),
                    description = stringResource(R.string.brand_website_desc),
                    testTag = "brand_website_row",
                    onClick = { BrandLinks.openWebsite(context) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                BrandLinkRow(
                    icon = Icons.Default.Star,
                    title = stringResource(R.string.brand_rate_title),
                    description = stringResource(R.string.brand_rate_desc),
                    testTag = "brand_rate_row",
                    onClick = { BrandLinks.openStoreListing(context) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                BrandLinkRow(
                    icon = Icons.Default.SystemUpdate,
                    title = stringResource(R.string.settings_update_title),
                    description = stringResource(R.string.settings_update_desc),
                    testTag = "brand_update_row",
                    onClick = { BrandLinks.openLatestRelease(context) }
                )
            }
        }
    }

    if (showSleepTimerDialog) {
        val timerOptions = listOf(15, 30, 45, 60, 90)
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text(stringResource(R.string.dialog_sleep_timer_title)) },
            text = {
                Column {
                    timerOptions.forEach { minutes ->
                        Text(
                            text = stringResource(R.string.sleep_timer_minutes_option, minutes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onStartSleepTimer(minutes)
                                    showSleepTimerDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun BrandLinkRow(
    icon: ImageVector,
    title: String,
    description: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.OpenInBrowser,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}
