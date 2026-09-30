package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.ui.AppSettings

@Composable
fun SettingsScreen(
    settings: AppSettings,
    sleepTimerRemaining: Int?,
    onTogglePersian: () -> Unit,
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
            text = "تنظیمات پلیر Novo",
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
                        text = "تنظیمات حرفه‌ای و جلوه‌های بصری",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "نوع صفحه پخش آهنگ (Visualizer Style):",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "جلوه متحرک و گرافیکی مورد علاقه خود را انتخاب کنید",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                val visualizerOptions = listOf(
                    Triple("neon_vinyl", "چرخش نئونی وینیل", "صفحه گرامافون دوّار با حلقه‌های نئونی"),
                    Triple("spectrum_wave", "اکولایزر طیف نئونی", "امواج طیف فرکانس صوتی متحرک"),
                    Triple("glass_3d", "کاور سه‌بعدی شیشه‌ای", "کاور آلبوم شناور با افکت شیشه‌ای"),
                    Triple("pulse_rings", "امواج تپنده ریتمیک", "حلقه‌های متصاعد شونده با ضرب‌آهنگ")
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
                    text = "فیلتر حذف زنگ و صداهای کوتاه:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "فایل‌های صوتی کمتر از این مدت به عنوان زنگ و پیام فیلتر می‌شوند",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val durationOptions = listOf(15 to "۱۵ ثانیه", 30 to "۳۰ ثانیه", 60 to "۱ دقیقه")
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
                    text = "تداخل نرم بین آهنگ‌ها (Crossfade):",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val crossfadeOptions = listOf(0 to "خاموش", 3 to "۳ ثانیه", 5 to "۵ ثانیه", 8 to "۸ ثانیه")
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
                        Text("توقف با قطع هندزفری", style = MaterialTheme.typography.bodyMedium)
                        Text("توقف پخش موسیقی هنگام جدا شدن هدفون یا بلوتوث", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("خروجی صدای با کیفیت بالا (Hi-Fi)", style = MaterialTheme.typography.bodyMedium)
                        Text("بهینه‌سازی بیت‌ریت و کاهش نویز برای صدای شفاف", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("کنترل‌های صفحه قفل و پنل اعلان", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("نمایش دکمه‌های کنترل (پخش، بعدی، قبلی) و کاور روی صفحه قفل گوشی", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    text = "ظاهر و رنگ‌بندی",
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
                        Text("تم مشکی عمیق (AMOLED)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "صرفه‌جویی در مصرف باتری برای صفحات اولد",
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

                // Persian Direction Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("چیدمان راست‌به‌چپ (فارسی)", style = MaterialTheme.typography.bodyMedium)
                        Text("سازگاری کامل با جهت متن فارسی", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.isPersian,
                        onCheckedChange = { onTogglePersian() }
                    )
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
                        Text("رنگ اصلی پوسته", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("انتخاب تم رنگی دلخواه برای بخش‌های مختلف برنامه", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Horizontal Row of Color Swatches
                val colorThemes = listOf(
                    Triple("gold", "طلایی", Color(0xFFFFC107)),
                    Triple("purple", "بنفش", Color(0xFF7C4DFF)),
                    Triple("emerald", "زمردی", Color(0xFF00E676)),
                    Triple("cyan", "فیروزه‌ای", Color(0xFF00E5FF)),
                    Triple("crimson", "یاقوتی", Color(0xFFFF5252))
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
                                        contentDescription = "انتخاب شده",
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
                    text = "ابزارهای کتابخانه و زمان‌بندی",
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
                        Text("تایمر خواب خودکار", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (sleepTimerRemaining != null) "$sleepTimerRemaining دقیقه تا توقف باقیست" else "غیرفعال است",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (sleepTimerRemaining != null) {
                        Button(onClick = onCancelSleepTimer) {
                            Text("لغو")
                        }
                    } else {
                        Button(onClick = { showSleepTimerDialog = true }) {
                            Text("تنظیم")
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
                        Text("بروزرسانی کتابخانه محلی", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("اسکن مجدد و حذف خودکار زنگ‌ها و صداهای کوتاه", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        text = "درباره اپلیکیشن Novo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "موزیک پلیر مدرن، آفلاین و حرفه‌ای Novo",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "طراحی شده توسط تیم نوین وب\nwebnovo.ir",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "نسخه ۱.۰.۰ | ارائه دهنده پیشرفته‌ترین قابلیت‌های صوتی، تفکیک پوشه‌ای، اکولایزر ۵ کاناله گرافیکی و کنترل اختصاصی صفحه قفل بدون تبلیغات و کاملاً رایگان.",
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
                        Text("وب‌سایت", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "دانلود موزیک پلیر Novo")
                                putExtra(Intent.EXTRA_TEXT, "موزیک پلیر مدرن و حرفه‌ای Novo - طراحی شده توسط نوین وب: https://webnovo.ir")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری Novo"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اشتراک", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showSleepTimerDialog) {
        val timerOptions = listOf(15, 30, 45, 60, 90)
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("تنظیم تایمر خواب") },
            text = {
                Column {
                    timerOptions.forEach { minutes ->
                        Text(
                            text = "$minutes دقیقه بعد",
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
                    Text("انصراف")
                }
            }
        )
    }
}
