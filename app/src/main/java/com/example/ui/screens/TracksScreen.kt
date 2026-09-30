package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.PlaybackState
import com.example.data.local.PlaylistEntity
import com.example.data.local.TrackEntity
import com.example.ui.SortOrder
import com.example.ui.components.TrackCoverImage
import java.util.Locale

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
    isPersian: Boolean,
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
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("همه آهنگ‌ها", "علاقه‌مندی‌ها", "پربازدیدترین‌ها", "اخیراً پخش‌شده")

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

    val activeList = when (selectedTabIndex) {
        0 -> allTracks
        1 -> favoriteTracks
        2 -> mostPlayedTracks
        3 -> recentlyPlayedTracks
        else -> allTracks
    }

    val filteredList = remember(activeList, searchQuery, sortOrder) {
        var res = if (searchQuery.isBlank()) {
            activeList
        } else {
            activeList.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true) ||
                it.album.contains(searchQuery, ignoreCase = true)
            }
        }

        when (sortOrder) {
            SortOrder.RECENTLY_ADDED -> res.sortedByDescending { it.addedAt }
            SortOrder.TITLE_AZ -> res.sortedBy { it.title.lowercase(Locale.getDefault()) }
            SortOrder.ARTIST_AZ -> res.sortedBy { it.artist.lowercase(Locale.getDefault()) }
            SortOrder.MOST_PLAYED -> res.sortedByDescending { it.playCount }
            SortOrder.DURATION -> res.sortedByDescending { it.durationMs }
        }
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
                text = "نوین وب",
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
                        "جستجو در آهنگ‌ها، خواننده‌ها...",
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
                        contentDescription = "جستجو",
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
                                contentDescription = "پاک کردن",
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
                    Icon(imageVector = Icons.Default.Sort, contentDescription = "مرتب‌سازی")
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("جدیدترین‌ها") },
                        onClick = { onSortOrderChange(SortOrder.RECENTLY_ADDED); showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("عنوان (الف-ی)") },
                        onClick = { onSortOrderChange(SortOrder.TITLE_AZ); showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("نام هنرمند") },
                        onClick = { onSortOrderChange(SortOrder.ARTIST_AZ); showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("بیشترین پخش") },
                        onClick = { onSortOrderChange(SortOrder.MOST_PLAYED); showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("مدت زمان") },
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
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "اسکن آهنگ‌های دستگاه")
                }
            }

            // Import File Button
            IconButton(
                onClick = { importPickerLauncher.launch(arrayOf("audio/*")) },
                modifier = Modifier.testTag("import_audio_button")
            ) {
                Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "افزودن فایل صوتی")
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Track List
        if (filteredList.isEmpty()) {
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
                        text = if (searchQuery.isNotBlank()) "هیچ آهنگی با این مشخصات یافت نشد" else "آهنگی در این بخش موجود نیست",
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
                        Text("اسکن فایل‌های دستگاه")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                items(filteredList, key = { it.id }) { track ->
                    TrackItemRow(
                        track = track,
                        isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying,
                        isCurrent = playbackState.currentTrack?.id == track.id,
                        onClick = { onTrackClick(track, filteredList) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onAddToPlaylist = { trackForPlaylist = track }
                    )
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
                        text = track.artist,
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
                    contentDescription = "علاقه‌مندی",
                    tint = if (track.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onAddToPlaylist,
                modifier = Modifier.testTag("add_playlist_btn_${track.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = "افزودن به لیست",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        title = { Text("افزودن «${track.title}» به لیست") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (playlists.isEmpty() && !showCreateField) {
                    Text(
                        text = "هنوز هیچ لیست پخشی ایجاد نشده است.",
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
                        label = { Text("نام لیست جدید") },
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
                    Text("ایجاد و افزودن آهنگ")
                }
            } else {
                Button(onClick = { showCreateField = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("لیست جدید")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
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
