package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.audio.PlaybackState
import com.example.data.local.TrackEntity
import com.example.ui.i18n.displayArtist
import java.util.Locale

/**
 * Compact persistent player shown above the bottom navigation bar.
 *
 * Controls are icon only (previous / play-pause / next) and the timeline can be dragged or tapped
 * to jump to any position of the current song.
 */
@Composable
fun MiniPlayer(
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onSeekTo: (Long) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack ?: return

    val durationMs = playbackState.durationMs
    val safeDuration = if (durationMs > 0L) durationMs else 1L

    var isSeeking by remember { mutableStateOf(false) }
    var seekRatio by remember { mutableFloatStateOf(0f) }

    val liveRatio = (playbackState.currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val displayRatio = if (isSeeking) seekRatio else liveRatio
    val displayPositionMs = if (isSeeking) (seekRatio * safeDuration).toLong() else playbackState.currentPositionMs

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("mini_player_surface"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 4.dp
    ) {
        Column {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art
                TrackCoverImage(
                    track = track,
                    size = 46.dp,
                    shapeRadius = 10.dp,
                    fallbackTint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = displayArtist(track.artist),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Favorite Button
                MiniPlayerButton(
                    icon = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = stringResource(R.string.cd_favorite),
                    buttonSize = 32.dp,
                    iconSize = 19.dp,
                    tint = if (track.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                    testTag = "mini_favorite_button",
                    onClick = { onToggleFavorite(track) }
                )

                // Previous Button
                MiniPlayerButton(
                    icon = Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.cd_previous),
                    buttonSize = 34.dp,
                    iconSize = 24.dp,
                    tint = MaterialTheme.colorScheme.onSurface,
                    testTag = "mini_prev_button",
                    onClick = { onSkipPrevious() }
                )

                // Play / Pause Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onPlayPause() }
                        .testTag("mini_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_pause),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Next Button
                MiniPlayerButton(
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.cd_next),
                    buttonSize = 34.dp,
                    iconSize = 24.dp,
                    tint = MaterialTheme.colorScheme.onSurface,
                    testTag = "mini_next_button",
                    onClick = { onSkipNext() }
                )
            }

            // Timeline: current position - draggable seek bar - total duration
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatMiniTime(displayPositionMs),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(6.dp))

                MiniPlayerTimeline(
                    progress = displayRatio,
                    enabled = playbackState.durationMs > 0L,
                    onSeekStart = { ratio ->
                        isSeeking = true
                        seekRatio = ratio
                    },
                    onSeekMove = { ratio -> seekRatio = ratio },
                    onSeekFinished = { ratio ->
                        seekRatio = ratio
                        isSeeking = false
                        onSeekTo((ratio * safeDuration).toLong())
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mini_seek_timeline")
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = formatMiniTime(playbackState.durationMs),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String,
    tint: Color,
    buttonSize: Dp,
    iconSize: Dp
) {
    Box(
        modifier = Modifier
            .size(buttonSize)
            .clip(CircleShape)
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Thin, draggable progress timeline. Tap anywhere to jump, drag to scrub.
 */
@Composable
private fun MiniPlayerTimeline(
    progress: Float,
    enabled: Boolean,
    onSeekStart: (Float) -> Unit,
    onSeekMove: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var barWidthPx by remember { mutableIntStateOf(1) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .onSizeChanged { barWidthPx = it.width.coerceAtLeast(1) }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startRatio = (down.position.x / barWidthPx.toFloat()).coerceIn(0f, 1f)
                    onSeekStart(startRatio)
                    onSeekMove(startRatio)

                    var pointer = down
                    while (pointer.pressed) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        pointer = change
                        if (!change.pressed) break
                        val ratio = (change.position.x / barWidthPx.toFloat()).coerceIn(0f, 1f)
                        onSeekMove(ratio)
                        change.consume()
                    }

                    val finalRatio = (pointer.position.x / barWidthPx.toFloat()).coerceIn(0f, 1f)
                    onSeekFinished(if (finalRatio.isNaN()) 0f else finalRatio)
                }
            }
    ) {
        val barHeight = 4.dp.toPx()
        val centerY = size.height / 2f
        val corner = CornerRadius(barHeight / 2f, barHeight / 2f)

        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, centerY - barHeight / 2f),
            size = Size(size.width, barHeight),
            cornerRadius = corner
        )

        val progressWidth = (size.width * progress.coerceIn(0f, 1f))
        if (progressWidth > 0f) {
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(0f, centerY - barHeight / 2f),
                size = Size(progressWidth, barHeight),
                cornerRadius = corner
            )
        }

        val thumbRadius = 5.dp.toPx()
        val thumbCenterX = progressWidth.coerceIn(thumbRadius, size.width - thumbRadius)
        drawCircle(
            color = primaryColor,
            radius = thumbRadius,
            center = Offset(thumbCenterX, centerY)
        )
    }
}

private fun formatMiniTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
