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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.PlaybackState
import com.example.data.local.PlaylistEntity
import com.example.data.local.TrackEntity

@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistEntity>,
    selectedPlaylist: PlaylistEntity?,
    playlistTracks: List<TrackEntity>,
    playbackState: PlaybackState,
    onSelectPlaylist: (PlaylistEntity?) -> Unit,
    onCreatePlaylist: (name: String, description: String) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onRemoveTrackFromPlaylist: (playlistId: String, trackId: String) -> Unit,
    onTrackClick: (TrackEntity, List<TrackEntity>) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onAddToPlaylist: (playlistId: String, trackId: String) -> Unit,
    onCreatePlaylistAndAddTrack: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<PlaylistEntity?>(null) }
    var trackForPlaylist by remember { mutableStateOf<TrackEntity?>(null) }

    if (selectedPlaylist != null) {
        // Detail View for Selected Playlist
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("playlist_detail_screen")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onSelectPlaylist(null) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازگشت"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedPlaylist.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selectedPlaylist.description.isNotBlank()) {
                        Text(
                            text = selectedPlaylist.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Delete Playlist Button
                IconButton(onClick = { playlistToDelete = selectedPlaylist }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف لیست پخش",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (playlistTracks.isNotEmpty()) {
                Button(
                    onClick = { onTrackClick(playlistTracks.first(), playlistTracks) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("پخش همه آهنگ‌ها (${playlistTracks.size} آهنگ)")
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (playlistTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "این لیست هنوز آهنگی ندارد.\nمی‌توانید از تب «آهنگ‌ها» با زدن دکمه ＋ آهنگ‌ها را به این لیست اضافه کنید.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    items(playlistTracks, key = { it.id }) { track ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                TrackItemRow(
                                    track = track,
                                    isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying,
                                    isCurrent = playbackState.currentTrack?.id == track.id,
                                    onClick = { onTrackClick(track, playlistTracks) },
                                    onToggleFavorite = { onToggleFavorite(track) },
                                    onAddToPlaylist = { trackForPlaylist = track }
                                )
                            }
                            IconButton(
                                onClick = { onRemoveTrackFromPlaylist(selectedPlaylist.id, track.id) },
                                modifier = Modifier.testTag("remove_from_playlist_${track.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف از لیست",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // List of All Playlists
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("create_playlist_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "ایجاد لیست جدید")
                }
            }
        ) { padding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .testTag("playlists_screen")
            ) {
                Text(
                    text = "لیست‌های پخش شما",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "آهنگ‌های دلخواه خود را در پلی‌لیست‌های اختصاصی دسته‌بندی کنید",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (playlists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "هنوز هیچ لیست پخشی ایجاد نشده است",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { showCreateDialog = true }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ایجاد اولین لیست پخش")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectPlaylist(playlist) }
                                    .testTag("playlist_card_${playlist.id}"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlaylistPlay,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = playlist.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (playlist.description.isNotBlank()) {
                                            Text(
                                                text = playlist.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    IconButton(onClick = { playlistToDelete = playlist }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف لیست",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Playlist Dialog
    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc ->
                onCreatePlaylist(name, desc)
                showCreateDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (playlistToDelete != null) {
        val p = playlistToDelete!!
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = { Text("حذف لیست پخش") },
            text = { Text("آیا از حذف لیست پخش «${p.name}» مطمئن هستید؟ آهنگ‌های دستگاه حذف نخواهند شد.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePlaylist(p)
                        playlistToDelete = null
                    }
                ) {
                    Text("بله، حذف کن")
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
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
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ایجاد لیست پخش جدید") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام لیست پخش") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_playlist_name_field")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("توضیحات (اختیاری)") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name, desc) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_playlist_btn")
            ) {
                Text("ایجاد لیست")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
