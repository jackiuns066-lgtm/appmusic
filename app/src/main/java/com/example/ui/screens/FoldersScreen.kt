package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.audio.PlaybackState
import com.example.data.local.PlaylistEntity
import com.example.data.local.TrackEntity
import com.example.ui.i18n.displayAlbum
import com.example.ui.i18n.displayArtist
import com.example.ui.i18n.displayFolder
import com.example.ui.i18n.displayGenre

enum class CategoryType {
    FOLDERS, ARTISTS, ALBUMS, GENRES
}

@Composable
private fun localizedGroupName(type: CategoryType, value: String): String = when (type) {
    CategoryType.FOLDERS -> displayFolder(value)
    CategoryType.ARTISTS -> displayArtist(value)
    CategoryType.ALBUMS -> displayAlbum(value)
    CategoryType.GENRES -> displayGenre(value)
}

@Composable
fun FoldersScreen(
    allTracks: List<TrackEntity>,
    folders: List<String>,
    artists: List<String>,
    albums: List<String>,
    genres: List<String>,
    playbackState: PlaybackState,
    playlists: List<PlaylistEntity>,
    onTrackClick: (TrackEntity, List<TrackEntity>) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onAddToPlaylist: (playlistId: String, trackId: String) -> Unit,
    onCreatePlaylistAndAddTrack: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryType by remember { mutableStateOf(CategoryType.FOLDERS) }
    var selectedGroupValue by remember { mutableStateOf<String?>(null) }
    var trackForPlaylist by remember { mutableStateOf<TrackEntity?>(null) }

    val categoryTabs = listOf(
        Triple(CategoryType.FOLDERS, stringResource(R.string.categories_folders), Icons.Default.Folder),
        Triple(CategoryType.ARTISTS, stringResource(R.string.categories_artists), Icons.Default.Person),
        Triple(CategoryType.ALBUMS, stringResource(R.string.categories_albums), Icons.Default.Album),
        Triple(CategoryType.GENRES, stringResource(R.string.categories_genres), Icons.Default.Category)
    )

    if (selectedGroupValue != null) {
        // Group Detail View
        val groupTracks = remember(selectedCategoryType, selectedGroupValue, allTracks) {
            when (selectedCategoryType) {
                CategoryType.FOLDERS -> allTracks.filter { it.folderPath == selectedGroupValue }
                CategoryType.ARTISTS -> allTracks.filter { it.artist == selectedGroupValue }
                CategoryType.ALBUMS -> allTracks.filter { it.album == selectedGroupValue }
                CategoryType.GENRES -> allTracks.filter { it.genre == selectedGroupValue }
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("category_detail_screen")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedGroupValue = null }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedGroupValue?.let { localizedGroupName(selectedCategoryType, it) } ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = pluralStringResource(R.plurals.audio_items_count, groupTracks.size, groupTracks.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (groupTracks.isNotEmpty()) {
                Button(
                    onClick = { onTrackClick(groupTracks.first(), groupTracks) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_play_all_category))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                items(groupTracks, key = { it.id }) { track ->
                    TrackItemRow(
                        track = track,
                        isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying,
                        isCurrent = playbackState.currentTrack?.id == track.id,
                        onClick = { onTrackClick(track, groupTracks) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onAddToPlaylist = { trackForPlaylist = track }
                    )
                }
            }
        }
    } else {
        // Categories Browser
        val activeItems = when (selectedCategoryType) {
            CategoryType.FOLDERS -> folders
            CategoryType.ARTISTS -> artists
            CategoryType.ALBUMS -> albums
            CategoryType.GENRES -> genres
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .testTag("categories_screen")
        ) {
            TabRow(
                selectedTabIndex = selectedCategoryType.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                categoryTabs.forEach { (type, label, icon) ->
                    Tab(
                        selected = selectedCategoryType == type,
                        onClick = { selectedCategoryType = type },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (activeItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.categories_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeItems) { itemValue ->
                        val count = when (selectedCategoryType) {
                            CategoryType.FOLDERS -> allTracks.count { it.folderPath == itemValue }
                            CategoryType.ARTISTS -> allTracks.count { it.artist == itemValue }
                            CategoryType.ALBUMS -> allTracks.count { it.album == itemValue }
                            CategoryType.GENRES -> allTracks.count { it.genre == itemValue }
                        }

                        val icon = when (selectedCategoryType) {
                            CategoryType.FOLDERS -> Icons.Default.Folder
                            CategoryType.ARTISTS -> Icons.Default.Person
                            CategoryType.ALBUMS -> Icons.Default.Album
                            CategoryType.GENRES -> Icons.Default.Category
                        }

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedGroupValue = itemValue }
                                .testTag("group_card_$itemValue"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = localizedGroupName(selectedCategoryType, itemValue),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = pluralStringResource(R.plurals.tracks_count, count, count),
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
