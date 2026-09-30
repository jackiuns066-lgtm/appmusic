package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.audio.PlaybackState
import com.example.data.audio.RepeatMode
import com.example.data.local.PlaylistEntity
import com.example.data.local.TrackEntity
import com.example.data.share.BrandPoster
import com.example.data.share.TrackSharing
import com.example.ui.components.TrackCoverImage
import com.example.ui.components.TrackCoverImageFill
import com.example.ui.i18n.displayAlbum
import com.example.ui.i18n.displayArtist
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val track = playbackState.currentTrack
    if (track == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.now_playing_empty), style = MaterialTheme.typography.titleMedium)
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
                    contentDescription = stringResource(R.string.cd_back)
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
                    text = stringResource(R.string.now_playing_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(
                    onClick = {
                        TrackSharing.shareTrack(
                            context = context,
                            track = track,
                            chooserTitle = context.getString(R.string.share_chooser_song),
                            subject = context.getString(R.string.share_track_subject, track.title, track.artist),
                            text = context.getString(R.string.share_song_text, track.title, track.artist)
                        )
                    },
                    modifier = Modifier.testTag("now_playing_share_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.cd_share)
                    )
                }

                // Brand poster: a ready to post story image with cover art + Novo / webnovo.ir
                IconButton(
                    onClick = {
                        val accentColor = MaterialTheme.colorScheme.primary.toArgb()
                        scope.launch {
                            val shared = BrandPoster.share(
                                context = context,
                                track = track,
                                accentColor = accentColor,
                                chooserTitle = context.getString(R.string.share_poster_chooser),
                                subject = context.getString(R.string.share_track_subject, track.title, track.artist),
                                text = context.getString(R.string.share_poster_text, track.title, track.artist),
                                brandLine = context.getString(R.string.poster_brand_line),
                                tagline = context.getString(R.string.poster_tagline)
                            )
                            if (!shared) {
                                Toast.makeText(context, R.string.share_poster_failed, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.testTag("now_playing_poster_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = stringResource(R.string.cd_share_poster),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onCycleVisualizerStyle,
                    modifier = Modifier.testTag("now_playing_style_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = stringResource(R.string.cd_visual_style),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onNavigateToEqualizer,
                    modifier = Modifier.testTag("now_playing_eq_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = stringResource(R.string.cd_equalizer)
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
                "glass_3d" -> Floating3DGlassVisualizer(isPlaying = playbackState.isPlaying, track = track)
                "pulse_rings" -> PulsingRingsVisualizer(isPlaying = playbackState.isPlaying, track = track)
                else -> NeonVinylVisualizer(isPlaying = playbackState.isPlaying, track = track)
            }
        }

        // Style Indicator Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.clickable { onCycleVisualizerStyle() }
        ) {
            val styleName = stringResource(
                when (visualizerStyle) {
                    "spectrum_wave" -> R.string.style_spectrum_wave
                    "glass_3d" -> R.string.style_glass_3d
                    "pulse_rings" -> R.string.style_pulse_rings
                    else -> R.string.style_neon_vinyl
                }
            )
            val styleTitle = stringResource(R.string.now_playing_effect_prefix, styleName)
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
                text = stringResource(R.string.now_playing_swipe_hint),
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
                    text = displayArtist(track.artist) + " • " + displayAlbum(track.album),
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
                    contentDescription = stringResource(R.string.cd_favorite),
                    tint = if (track.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp)
                )
            }

            IconButton(
                onClick = { showAddToPlaylistDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlaylistAdd,
                    contentDescription = stringResource(R.string.cd_add_to_playlist),
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
                    contentDescription = stringResource(R.string.cd_shuffle),
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
                    contentDescription = stringResource(R.string.cd_previous),
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
                    contentDescription = if (playbackState.isPlaying) stringResource(R.string.cd_pause) else stringResource(R.string.cd_play),
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
                    contentDescription = stringResource(R.string.cd_next),
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
                    contentDescription = stringResource(R.string.cd_repeat),
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
                        text = if (playbackState.stopAfterCurrentTrack) stringResource(R.string.now_playing_stop_after_active) else stringResource(R.string.now_playing_stop_after),
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
                    contentDescription = stringResource(R.string.cd_queue)
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
                    text = stringResource(R.string.now_playing_queue_title, playbackState.currentQueue.size),
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
                                    text = displayArtist(qTrack.artist),
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
            title = { Text(stringResource(R.string.dialog_add_to_playlist_generic)) },
            text = {
                Column {
                    Button(
                        onClick = {
                            showAddToPlaylistDialog = false
                            showCreatePlaylistDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.action_create_new_playlist))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (playlists.isEmpty()) {
                        Text(stringResource(R.string.now_playing_no_playlists))
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
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text(stringResource(R.string.dialog_new_playlist_short)) },
            text = {
                Column {
                    androidx.compose.material3.OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text(stringResource(R.string.label_playlist_name_short)) },
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
                    Text(stringResource(R.string.action_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

// 1. Neon Vinyl Visualizer
// The album art fills the whole frame, the vinyl rings / tonearm are drawn on top of it.
@Composable
fun NeonVinylVisualizer(
    isPlaying: Boolean,
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
            .size(280.dp)
            .shadow(18.dp, RoundedCornerShape(26.dp), spotColor = primaryColor)
            .clip(RoundedCornerShape(26.dp)),
        contentAlignment = Alignment.Center
    ) {
        TrackCoverImageFill(
            track = track,
            modifier = Modifier.fillMaxSize(),
            shapeRadius = 26.dp,
            fallbackTint = primaryColor
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f - 10f

            // Vignette so the vinyl grooves stay readable above the artwork
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                    center = center,
                    radius = size.width / 2f
                ),
                radius = size.width / 2f,
                center = center
            )

            rotate(degrees = currentRotation, pivot = center) {
                listOf(0.95f, 0.86f, 0.77f, 0.68f, 0.59f, 0.5f).forEach { factor ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.13f),
                        radius = outerRadius * factor,
                        center = center,
                        style = Stroke(width = 1.4f)
                    )
                }

                // Rotating light reflection on the record surface
                drawArc(
                    color = Color.White.copy(alpha = 0.16f),
                    startAngle = 0f,
                    sweepAngle = 46f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2f, outerRadius * 2f),
                    style = Stroke(width = outerRadius * 0.22f)
                )

                // Inner neon accent ring
                drawCircle(
                    color = primaryColor.copy(alpha = 0.75f),
                    radius = outerRadius * 0.42f,
                    center = center,
                    style = Stroke(width = 2.5f)
                )
            }

            // Outer neon glow ring
            drawCircle(
                color = primaryColor.copy(alpha = if (isPlaying) 0.8f else 0.35f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 3f)
            )

            // Tonearm / stylus (pivot in the top-right corner)
            val pivotX = size.width - 26f
            val pivotY = 26f
            rotate(degrees = tonearmAngle, pivot = Offset(pivotX, pivotY)) {
                drawCircle(color = Color(0xFFE5E7EB), radius = 9f, center = Offset(pivotX, pivotY))
                drawLine(
                    color = Color(0xFF9CA3AF),
                    start = Offset(pivotX, pivotY),
                    end = Offset(pivotX - 70f, pivotY + 120f),
                    strokeWidth = 4.5f
                )
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(pivotX - 80f, pivotY + 112f),
                    size = Size(20f, 13f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }
        }
    }
}

// 2. Neon Spectrum Wave Visualizer (bars are drawn above the full frame album art)
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
            .shadow(18.dp, RoundedCornerShape(26.dp), spotColor = primaryColor)
            .clip(RoundedCornerShape(26.dp)),
        contentAlignment = Alignment.Center
    ) {
        TrackCoverImageFill(
            track = track,
            modifier = Modifier.fillMaxSize(),
            shapeRadius = 26.dp,
            fallbackTint = primaryColor
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black.copy(alpha = 0.3f))

            val barCount = 18
            val barWidth = size.width / (barCount * 1.5f)
            val maxBarHeight = size.height * 0.5f
            val baseY = size.height - 24f

            for (i in 0 until barCount) {
                val phase = i.toFloat() * 0.35f + if (isPlaying) time else 0f
                val dynamicFactor = if (isPlaying) {
                    ((sin(phase) * 0.5f + 0.5f) * 0.7f + 0.3f)
                } else 0.2f

                val h = maxBarHeight * dynamicFactor
                val x = i * (barWidth * 1.5f) + 14f

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00E5FF),
                            primaryColor,
                            primaryColor.copy(alpha = 0.35f)
                        ),
                        startY = baseY - h,
                        endY = baseY
                    ),
                    topLeft = Offset(x, baseY - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }

            // Neon frame
            drawRoundRect(
                color = primaryColor.copy(alpha = 0.6f),
                topLeft = Offset(2f, 2f),
                size = Size(size.width - 4f, size.height - 4f),
                cornerRadius = CornerRadius(26f, 26f),
                style = Stroke(width = 3f)
            )
        }
    }
}

// 3. Floating 3D Glass Visualizer (glass card floats up and down, album art fills the frame)
@Composable
fun Floating3DGlassVisualizer(
    isPlaying: Boolean,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = currentOffset.dp)
                .shadow(20.dp, RoundedCornerShape(32.dp), spotColor = primaryColor)
                .clip(RoundedCornerShape(32.dp))
                .border(
                    2.dp,
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.65f),
                            primaryColor.copy(alpha = 0.75f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(32.dp)
                )
        ) {
            TrackCoverImageFill(
                track = track,
                modifier = Modifier.fillMaxSize(),
                shapeRadius = 32.dp,
                fallbackTint = primaryColor
            )

            // Glass shine above the cover
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.1f)
                            )
                        )
                    )
            )
        }
    }
}

// 4. Pulsing Radial Rings Visualizer (rings pulse above the album art)
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
            .size(280.dp)
            .shadow(18.dp, RoundedCornerShape(26.dp), spotColor = primaryColor)
            .clip(RoundedCornerShape(26.dp)),
        contentAlignment = Alignment.Center
    ) {
        TrackCoverImageFill(
            track = track,
            modifier = Modifier.fillMaxSize(),
            shapeRadius = 26.dp,
            fallbackTint = primaryColor
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                color = primaryColor.copy(alpha = 0.12f),
                radius = size.width / 2f,
                center = center
            )

            drawCircle(
                color = primaryColor.copy(alpha = (0.22f * (1.5f - currentPulse)).coerceAtLeast(0f)),
                radius = (size.width * 0.44f) * currentPulse,
                center = center,
                style = Stroke(width = 3f)
            )

            drawCircle(
                color = primaryColor.copy(alpha = (0.32f * (1.4f - currentPulse)).coerceAtLeast(0f)),
                radius = (size.width * 0.33f) * currentPulse,
                center = center,
                style = Stroke(width = 3.5f)
            )

            drawCircle(
                color = primaryColor.copy(alpha = 0.5f),
                radius = (size.width * 0.22f) * currentPulse,
                center = center,
                style = Stroke(width = 4f)
            )

            drawCircle(
                color = primaryColor.copy(alpha = 0.3f),
                radius = 26f,
                center = center
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
