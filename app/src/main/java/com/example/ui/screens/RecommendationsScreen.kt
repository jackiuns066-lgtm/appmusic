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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.audio.PlaybackState
import com.example.data.local.PlaylistEntity
import com.example.data.local.TrackEntity
import com.example.data.recommendation.TasteProfile
import com.example.data.recommendation.TrackRecommendation
import com.example.ui.components.TrackCoverImage
import com.example.ui.i18n.displayArtist
import com.example.ui.i18n.displayGenre
import com.example.ui.i18n.displayMood
import com.example.ui.i18n.recommendationReason

@Composable
fun RecommendationsScreen(
    recommendations: List<TrackRecommendation>,
    tasteProfile: TasteProfile,
    playbackState: PlaybackState,
    playlists: List<PlaylistEntity>,
    onTrackClick: (TrackEntity, List<TrackEntity>) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onAddToPlaylist: (playlistId: String, trackId: String) -> Unit,
    onCreatePlaylistAndAddTrack: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var trackForPlaylist by remember { mutableStateOf<TrackEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("recommendations_screen")
    ) {
        // AI Taste Header Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.rec_taste_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TasteMetricChip(label = stringResource(R.string.rec_label_top_genre), value = displayGenre(tasteProfile.topGenre))
                    TasteMetricChip(label = stringResource(R.string.rec_label_mood), value = displayMood(tasteProfile.topMood))
                    TasteMetricChip(label = stringResource(R.string.rec_label_liked), value = "${tasteProfile.favoriteCount}")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.rec_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (recommendations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.rec_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val recTracks = remember(recommendations) { recommendations.map { it.track } }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recommendations, key = { it.track.id }) { rec ->
                    val isCurrent = playbackState.currentTrack?.id == rec.track.id

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onTrackClick(rec.track, recTracks) }
                            .testTag("rec_track_${rec.track.id}"),
                        color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TrackCoverImage(
                                    track = rec.track,
                                    size = 44.dp,
                                    shapeRadius = 10.dp,
                                    fallbackTint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rec.track.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = displayArtist(rec.track.artist),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Match Percentage Badge
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.rec_match, rec.matchPercentage),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                IconButton(onClick = { onToggleFavorite(rec.track) }) {
                                    Icon(
                                        imageVector = if (rec.track.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = stringResource(R.string.cd_favorite),
                                        tint = if (rec.track.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(onClick = { trackForPlaylist = rec.track }) {
                                    Icon(
                                        imageVector = Icons.Default.PlaylistAdd,
                                        contentDescription = stringResource(R.string.cd_add_to_playlist),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "💡 " + recommendationReason(rec),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (trackForPlaylist != null) {
        val tr = trackForPlaylist!!
        AddToPlaylistDialog(
            track = tr,
            playlists = playlists,
            onDismiss = { trackForPlaylist = null },
            onAddToPlaylist = { pId ->
                onAddToPlaylist(pId, tr.id)
                trackForPlaylist = null
            },
            onCreateNewPlaylistAndAdd = { name ->
                onCreatePlaylistAndAddTrack(name, tr.id)
                trackForPlaylist = null
            }
        )
    }
}

@Composable
fun TasteMetricChip(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
