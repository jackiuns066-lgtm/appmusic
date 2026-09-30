package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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
import com.example.data.share.TrackSharing
import com.example.ui.SortOrder
import com.example.ui.components.TrackCoverImage
import com.example.ui.i18n.displayArtist
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun TracksScreen(
    allTracks: List<TrackEntity>,
    favoriteTracks: List<TrackEntity>,
    mostPlayedTracks: List<TrackEntity>,
    recentlyPlayedTracks: List<TrackEntity>,
    playbackState: PlaybackState,
    playlists: List<PlaylistEntity>,
    searchQuery: String,
    sortOrder: SortOrder,
    isScanning: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onScanDevice: () -> Unit,
    onTrackClick: (TrackEntity, List<TrackEntity>) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onImportAudio: (Uri) -> Unit,
    onAddToPlaylist: (playlistId: String, trackId: String) -> Unit,
    onCreatePlaylistAndAddTrack: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shareTrack: (TrackEntity) -> Unit = { sharedTrack ->
        TrackSharing.shareTrack(
            context = context,
            track = sharedTrack,
            chooserTitle = context.getString(R.string.share_chooser_song),
            subject = context.getString(R.string.share_track_subject, sharedTrack.title, sharedTrack.artist),
            text = context.getString(R.string.share_song_text, sharedTrack.title, sharedTrack.artist)
        )
    }

    val scope = rememberCoroutineScope()
    val tabTitles = listOf(
        stringResource(R.string.tracks_tab_all),
        stringResource(R.string.tracks_tab_favorites),
        stringResource(R.string.tracks_tab_most_played),
        stringResource(R.string.tracks_tab_recent)
    )
    // One pager page per tab: swiping the list changes the tab, tapping a tab slides the list.
    val pagerState = rememberPagerState(pageCount = { tabTitles.size })

    var showSortMenu by remember { mutableStateOf(false) }
    var trackForPlaylist by remember { mutableStateOf<TrackEntity?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onScanDevice()
        }
    }

    val importPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportAudio(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("tracks_screen")
    ) {
        // App Title Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "NOVO",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Music Player",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(R.string.tracks_brand_team),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        }

        // Search & Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("track_search_field"),
                placeholder = {
                    Text(
                        stringResource(R.string.tracks_search_hint),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.cd_search),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_clear),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Sort Menu Button
            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.testTag("sort_button")
                ) {
                    Icon(imageVector = Icons.Default.Sort, contentDescription = stringResource(R.string.cd_sort))
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_recently_added)) },
                        onClick = { onSortOrderChange(SortOrder.RECENTLY_ADDED); showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_title)) },
                        onClick = { onSortOrderChange(SortOrder.TITLE_AZ); showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_artist)) },
                        onClick = { onSortOrderChange(SortOrder.ARTIST_AZ); showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_most_played)) },
                        onClick = { onSortOrderChange(SortOrder.MOST_PLAYED); showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_duration)) },
                        onClick = { onSortOrderChange(SortOrder.DURATION); showSortMenu = false }
                    )
                }
            }

            // Scan Device Button
            IconButton(
                onClick = {
                    val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    permissionLauncher.launch(perm)
                },
                modifier = Modifier.testTag("scan_device_button")
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = stringResource(R.string.cd_scan_device))
                }
            }

            // Import File Button
            IconButton(
                onClick = { importPickerLauncher.launch(arrayOf("audio/*")) },
                modifier = Modifier.testTag("import_audio_button")
            ) {
                Icon(imageVector = Icons.Default.FolderOpen, contentDescription = stringResource(R.string.cd_import_audio))
            }
        }

        // Tabs — kept in sync with the pager, animated highlight
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabTitles.forEachIndexed { index, title ->
                val selected = pagerState.currentPage == index
                val labelColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(durationMillis = 280),
                    label = "tab_label_color"
                )
                val labelScale by animateFloatAsState(
                    targetValue = if (selected) 1f else 0.9f,
                    animationSpec = spring(
                        dampingRatio = 0.55f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "tab_label_scale"
                )
                Tab(
                    selected = selected,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    modifier = Modifier.testTag("tracks_tab_$index"),
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = labelColor,
                            modifier = Modifier.graphicsLayer {
                                scaleX = labelScale
                                scaleY = labelScale
                            }
                        )
                    }
                )
            }
        }

        // Track List — one pager page per tab, so the list follows the finger while swiping
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            val pageTracks = when (page) {
                1 -> favoriteTracks
                2 -> mostPlayedTracks
                3 -> recentlyPlayedTracks
                else -> allTracks
            }
            val pageList = remember(pageTracks, searchQuery, sortOrder) {
                filterAndSortTracks(pageTracks, searchQuery, sortOrder)
            }

            // Attractive cross-transition: the incoming page fades and scales in while sliding.
            val pageDistance = abs((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                .coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 1f - 0.45f * pageDistance
                        val scale = 1f - 0.06f * pageDistance
                        scaleX = scale
                        scaleY = scale
                        translationY = 26.dp.toPx() * pageDistance
                    }
            ) {
                if (pageList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    stringResource(R.string.tracks_empty_search)
                                } else {
                                    stringResource(R.string.tracks_empty_list)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        Manifest.permission.READ_MEDIA_AUDIO
                                    } else {
                                        Manifest.permission.READ_EXTERNAL_STORAGE
                                    }
                                    permissionLauncher.launch(perm)
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.action_scan_device_files))
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        items(pageList, key = { it.id }) { track ->
                            TrackItemRow(
                                track = track,
                                isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying,
                                isCurrent = playbackState.currentTrack?.id == track.id,
                                onClick = { onTrackClick(track, pageList) },
                                onToggleFavorite = { onToggleFavorite(track) },
                                onAddToPlaylist = { trackForPlaylist = track },
                                onShare = { shareTrack(track) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add To Playlist Dialog
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
fun TrackItemRow(
    track: TrackEntity,
    isPlaying: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("track_row_${track.id}"),
        color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isCurrent) 4.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackCoverImage(
                track = track,
                size = 46.dp,
                shapeRadius = 10.dp,
                fallbackTint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayArtist(track.artist),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = " • ${formatDuration(track.durationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("fav_btn_${track.id}")
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(R.string.cd_favorite),
                    tint = if (track.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onAddToPlaylist,
                modifier = Modifier.testTag("add_playlist_btn_${track.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = stringResource(R.string.cd_add_to_playlist),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (onShare != null) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.testTag("share_btn_${track.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.cd_share),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AddToPlaylistDialog(
    track: TrackEntity,
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onCreateNewPlaylistAndAdd: (String) -> Unit
) {
    var showCreateField by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_add_to_playlist_title, track.title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (playlists.isEmpty() && !showCreateField) {
                    Text(
                        text = stringResource(R.string.playlist_none_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (!showCreateField) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(playlists) { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onAddToPlaylist(p.id) }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = p.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                if (showCreateField) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text(stringResource(R.string.label_new_playlist_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (showCreateField) {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreateNewPlaylistAndAdd(newPlaylistName)
                        }
                    },
                    enabled = newPlaylistName.isNotBlank()
                ) {
                    Text(stringResource(R.string.action_create_and_add))
                }
            } else {
                Button(onClick = { showCreateField = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_new_playlist))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

/** Search + sort helper shared by every tab of the songs screen. */
private fun filterAndSortTracks(
    tracks: List<TrackEntity>,
    searchQuery: String,
    sortOrder: SortOrder
): List<TrackEntity> {
    val filtered = if (searchQuery.isBlank()) {
        tracks
    } else {
        tracks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true) ||
                it.album.contains(searchQuery, ignoreCase = true)
        }
    }

    return when (sortOrder) {
        SortOrder.RECENTLY_ADDED -> filtered.sortedByDescending { it.addedAt }
        SortOrder.TITLE_AZ -> filtered.sortedBy { it.title.lowercase(Locale.getDefault()) }
        SortOrder.ARTIST_AZ -> filtered.sortedBy { it.artist.lowercase(Locale.getDefault()) }
        SortOrder.MOST_PLAYED -> filtered.sortedByDescending { it.playCount }
        SortOrder.DURATION -> filtered.sortedByDescending { it.durationMs }
    }
}
