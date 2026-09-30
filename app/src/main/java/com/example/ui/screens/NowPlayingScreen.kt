package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SwipeLeft
import androidx.compose.material.icons.rounded.SwipeRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.PlaybackState
import com.example.data.audio.RepeatMode
import com.example.data.local.PlaylistEntity
import com.example.data.local.TrackEntity
import com.example.ui.components.TrackCoverImage
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playbackState: PlaybackState,
    playlists: List<PlaylistEntity>,
    visualizerStyle: String = "neon_vinyl",
    onCycleVisualizerStyle: () -> Unit = {},
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleStopAfterTrack: () -> Unit,
    onAddToPlaylist: (String, String) -> Unit,
    onCreatePlaylistAndAddTrack: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack
    if (track == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("هیچ آهنگی در حال پخش نیست", style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekPositionRatio by remember { mutableFloatStateOf(0f) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    val currentPosition = if (isUserSeeking) {
        (seekPositionRatio * playbackState.durationMs).toLong()
    } else {
        playbackState.currentPositionMs
    }

    val sliderValue = if (playbackState.durationMs > 0) {
        (currentPosition.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .testTag("now_playing_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("now_playing_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "بازگشت"
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NOVO PLAYER",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "در حال پخش",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(
                    onClick = onCycleVisualizerStyle,
                    modifier = Modifier.testTag("now_playing_style_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "تغییر جلوه بصری",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onNavigateToEqualizer,
                    modifier = Modifier.testTag("now_playing_eq_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "اکولایزر"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dynamic Visualizer Display (Neon Vinyl / Spectrum Wave / 3D Glass / Pulse Rings)
        var dragOffsetAccumulator by remember { mutableFloatStateOf(0f) }
        Box(
            modifier = Modifier
                .size(280.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragOffsetAccumulator = 0f },
                        onDragEnd = {
                            if (dragOffsetAccumulator < -80f) {
                                // Swiped left -> Next
                                onSkipNext()
                            } else if (dragOffsetAccumulator > 80f) {
                                // Swiped right -> Previous
                                onSkipPrevious()
                            }
                            dragOffsetAccumulator = 0f
                        },
                        onDragCancel = { dragOffsetAccumulator = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetAccumulator += dragAmount
                        }
                    )
                }
                .clickable { onCycleVisualizerStyle() },
            contentAlignment = Alignment.Center
        ) {
            when (visualizerStyle) {
                "spectrum_wave" -> NeonSpectrumWaveVisualizer(isPlaying = playbackState.isPlaying, track = track)
                "glass_3d" -> Floating3DGlassVisualizer(isPlaying = playbackState.isPlaying, title = track.title, artist = track.artist, track = track)
                "pulse_rings" -> PulsingRingsVisualizer(isPlaying = playbackState.isPlaying, track = track)
                else -> NeonVinylVisualizer(isPlaying = playbackState.isPlaying, title = track.title, artist = track.artist, track = track)
            }
        }

        // Style Indicator Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.clickable { onCycleVisualizerStyle() }
        ) {
            val styleTitle = when (visualizerStyle) {
                "spectrum_wave" -> "جلوه: اکولایزر طیف نئونی"
                "glass_3d" -> "جلوه: کاور سه‌بعدی شیشه‌ای"
                "pulse_rings" -> "جلوه: امواج تپنده ریتمیک"
                else -> "جلوه: چرخش نئونی وینیل"
            }
            Text(
                text = styleTitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        // Swipe hint indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SwipeRight,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "برای رد کردن آهنگ به چپ یا راست بکشید",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Rounded.SwipeLeft,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Track Info & Favorite Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${track.artist} • ${track.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = { onToggleFavorite(track) },
                modifier = Modifier.testTag("now_playing_fav_btn")
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "علاقه‌مندی",
                    tint = if (track.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp)
                )
            }

            IconButton(
                onClick = { showAddToPlaylistDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlaylistAdd,
                    contentDescription = "افزودن به لیست",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Seek Slider & Time Labels
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = sliderValue,
                onValueChange = {
                    isUserSeeking = true
                    seekPositionRatio = it
                },
                onValueChangeFinished = {
                    val targetMs = (seekPositionRatio * playbackState.durationMs).toLong()
                    onSeekTo(targetMs)
                    isUserSeeking = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("now_playing_slider")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTime(playbackState.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Modern Sleek Playback Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (playbackState.isShuffleEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onToggleShuffle() }
                    .testTag("now_playing_shuffle_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "پخش تصادفی",
                    tint = if (playbackState.isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Previous
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                    .clickable { onSkipPrevious() }
                    .testTag("now_playing_prev_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "قبلی",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Play / Pause FAB with Glow & Gradient
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .shadow(12.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                    .clickable { onPlayPause() }
                    .testTag("now_playing_play_pause_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playbackState.isPlaying) "توقف" else "پخش",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Next
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                    .clickable { onSkipNext() }
                    .testTag("now_playing_next_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "بعدی",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Repeat Mode
            val repeatIcon = when (playbackState.repeatMode) {
                RepeatMode.ONE -> Icons.Rounded.RepeatOne
                RepeatMode.ALL -> Icons.Rounded.Repeat
                RepeatMode.OFF -> Icons.Rounded.Repeat
            }
            val isRepeatActive = playbackState.repeatMode != RepeatMode.OFF
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isRepeatActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onToggleRepeat() }
                    .testTag("now_playing_repeat_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = repeatIcon,
                    contentDescription = "تکرار",
                    tint = if (isRepeatActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Secondary Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stop after current track toggle
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (playbackState.stopAfterCurrentTrack) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clickable { onToggleStopAfterTrack() }
                    .testTag("stop_after_track_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (playbackState.stopAfterCurrentTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (playbackState.stopAfterCurrentTrack) "توقف بعد این آهنگ (فعال)" else "توقف خودکار",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Open Queue
            IconButton(
                onClick = { showQueueSheet = true },
                modifier = Modifier.testTag("now_playing_queue_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = "صف پخش"
                )
            }
        }
    }

    // Queue Bottom Sheet
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "صف پخش (${playbackState.currentQueue.size} آهنگ)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(playbackState.currentQueue) { qTrack ->
                        val isCurrent = qTrack.id == playbackState.currentTrack?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                                .clickable {
                                    onSeekTo(0)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrackCoverImage(
                                track = qTrack,
                                size = 38.dp,
                                shapeRadius = 8.dp,
                                fallbackTint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = qTrack.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = qTrack.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add to Playlist Dialog
    if (showAddToPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = false },
            title = { Text("افزودن به لیست پخش") },
            text = {
                Column {
                    Button(
                        onClick = {
                            showAddToPlaylistDialog = false
                            showCreatePlaylistDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ساخت لیست جدید")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (playlists.isEmpty()) {
                        Text("هیچ لیست پخشی وجود ندارد")
                    } else {
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(playlists) { pl ->
                                Text(
                                    text = pl.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onAddToPlaylist(pl.id, track.id)
                                            showAddToPlaylistDialog = false
                                        }
                                        .padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddToPlaylistDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("لیست پخش جدید") },
            text = {
                Column {
                    androidx.compose.material3.OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("نام لیست") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylistAndAddTrack(newPlaylistName.trim(), track.id)
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    }
                ) {
                    Text("ایجاد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

// 1. Neon Vinyl Visualizer with 360-degree rotation and glowing halo
@Composable
fun NeonVinylVisualizer(
    isPlaying: Boolean,
    title: String,
    artist: String,
    track: TrackEntity,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "spin_angle"
    )

    val tonearmAngle by animateFloatAsState(
        targetValue = if (isPlaying) 28f else 0f,
        animationSpec = tween(600),
        label = "tonearm_angle"
    )

    val currentRotation = if (isPlaying) rotationAngle else 0f
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(270.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f - 14f

            // Outer Neon Glow Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (isPlaying) 0.45f else 0.15f),
                        primaryColor.copy(alpha = if (isPlaying) 0.2f else 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.width / 2f
                ),
                radius = size.width / 2f,
                center = center
            )

            // Vinyl Rotation Scope
            rotate(degrees = currentRotation, pivot = center) {
                // Main Vinyl Body (Deep Black)
                drawCircle(
                    color = Color(0xFF151517),
                    radius = outerRadius,
                    center = center
                )

                // Vinyl Grooves (Shiny concentric rings)
                val grooveRadii = listOf(0.9f, 0.83f, 0.76f, 0.69f, 0.62f, 0.55f, 0.48f)
                grooveRadii.forEach { factor ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.07f),
                        radius = outerRadius * factor,
                        center = center,
                        style = Stroke(width = 1.2f)
                    )
                }

                // Inner Neon Accent Ring
                drawCircle(
                    color = primaryColor.copy(alpha = 0.85f),
                    radius = outerRadius * 0.40f,
                    center = center,
                    style = Stroke(width = 2.5f)
                )

                // Center Label Disc (Vibrant Gradient)
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(primaryColor, primaryColor.copy(alpha = 0.7f), Color(0xFF00E5FF))
                    ),
                    radius = outerRadius * 0.35f,
                    center = center
                )

                // Center Spindle Hole
                drawCircle(
                    color = Color(0xFF101012),
                    radius = outerRadius * 0.08f,
                    center = center
                )
            }

            // Tonearm / Stylus (Pivot in top-right)
            val pivotX = size.width - 20f
            val pivotY = 24f
            rotate(degrees = tonearmAngle, pivot = Offset(pivotX, pivotY)) {
                // Pivot Base
                drawCircle(
                    color = Color(0xFFD1D5DB),
                    radius = 8f,
                    center = Offset(pivotX, pivotY)
                )
                // Arm Line
                drawLine(
                    color = Color(0xFF9CA3AF),
                    start = Offset(pivotX, pivotY),
                    end = Offset(pivotX - 60f, pivotY + 110f),
                    strokeWidth = 4f
                )
                // Cartridge Head
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(pivotX - 70f, pivotY + 105f),
                    size = Size(18f, 12f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                )
            }
        }

        // Circular album art centered inside spinning vinyl
        TrackCoverImage(
            track = track,
            size = 82.dp,
            shapeRadius = 41.dp,
            fallbackTint = primaryColor,
            modifier = Modifier.border(2.dp, primaryColor, CircleShape)
        )
    }
}

// 2. Neon Spectrum Wave Visualizer
@Composable
fun NeonSpectrumWaveVisualizer(
    isPlaying: Boolean,
    track: TrackEntity,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spectrum_wave")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "time_val"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(280.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF10141D)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(18.dp)) {
            val barCount = 18
            val barWidth = size.width / (barCount * 1.5f)
            val maxBarHeight = size.height * 0.65f
            val centerY = size.height / 2f

            for (i in 0 until barCount) {
                val phase = i.toFloat() * 0.35f + if (isPlaying) time else 0f
                val dynamicFactor = if (isPlaying) {
                    ((sin(phase) * 0.5f + 0.5f) * 0.7f + 0.3f)
                } else 0.2f

                val h = maxBarHeight * dynamicFactor
                val x = i * (barWidth * 1.5f) + 10f

                // Draw glowing bar
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00E5FF),
                            primaryColor,
                            primaryColor.copy(alpha = 0.3f)
                        )
                    ),
                    topLeft = Offset(x, centerY - h / 2f),
                    size = Size(barWidth, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }

            // Center neon glow circle
            drawCircle(
                color = primaryColor.copy(alpha = 0.25f),
                radius = 48f,
                center = Offset(size.width / 2f, centerY)
            )
        }

        TrackCoverImage(
            track = track,
            size = 80.dp,
            shapeRadius = 20.dp,
            fallbackTint = primaryColor,
            modifier = Modifier.border(2.dp, primaryColor, RoundedCornerShape(20.dp))
        )
    }
}

// 3. Floating 3D Glass Visualizer
@Composable
fun Floating3DGlassVisualizer(
    isPlaying: Boolean,
    title: String,
    artist: String,
    track: TrackEntity,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glass_bob")
    val bobOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "bob_offset"
    )

    val currentOffset = if (isPlaying) bobOffset else 0f
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(250.dp)
                .padding(top = (8 + currentOffset).dp)
                .clip(RoundedCornerShape(32.dp))
                .border(
                    2.dp,
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.6f),
                            primaryColor.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(32.dp)
                ),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shadowElevation = 16.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                primaryColor.copy(alpha = 0.35f),
                                Color(0xFF1A1C24).copy(alpha = 0.8f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    TrackCoverImage(
                        track = track,
                        size = 96.dp,
                        shapeRadius = 24.dp,
                        fallbackTint = primaryColor,
                        modifier = Modifier.border(2.dp, primaryColor.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// 4. Pulsing Radial Rings Visualizer
@Composable
fun PulsingRingsVisualizer(
    isPlaying: Boolean,
    track: TrackEntity,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_rings")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "pulse_val"
    )

    val currentPulse = if (isPlaying) pulse else 1f
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(260.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Expanding ripple wave 1
            drawCircle(
                color = primaryColor.copy(alpha = 0.15f * (1.5f - currentPulse)),
                radius = 110f * currentPulse,
                center = center,
                style = Stroke(width = 3f)
            )

            // Expanding ripple wave 2
            drawCircle(
                color = primaryColor.copy(alpha = 0.25f * (1.4f - currentPulse)),
                radius = 85f * currentPulse,
                center = center,
                style = Stroke(width = 3.5f)
            )

            // Expanding ripple wave 3
            drawCircle(
                color = primaryColor.copy(alpha = 0.4f),
                radius = 60f * currentPulse,
                center = center,
                style = Stroke(width = 4f)
            )

            // Center glowing core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.6f)),
                    center = center
                ),
                radius = 45f,
                center = center
            )
        }

        TrackCoverImage(
            track = track,
            size = 80.dp,
            shapeRadius = 40.dp,
            fallbackTint = primaryColor,
            modifier = Modifier.border(2.dp, primaryColor, CircleShape)
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
