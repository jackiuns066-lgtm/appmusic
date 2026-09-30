package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.RecommendationsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TracksScreen
import kotlinx.coroutines.flow.collectLatest

private data class NavItemData(
    val destination: ScreenDestination,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MusicViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val mostPlayedTracks by viewModel.mostPlayedTracks.collectAsStateWithLifecycle()
    val recentlyPlayedTracks by viewModel.recentlyPlayedTracks.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val equalizerPresets by viewModel.equalizerPresets.collectAsStateWithLifecycle()
    val allFolders by viewModel.allFolders.collectAsStateWithLifecycle()
    val allArtists by viewModel.allArtists.collectAsStateWithLifecycle()
    val allAlbums by viewModel.allAlbums.collectAsStateWithLifecycle()
    val allGenres by viewModel.allGenres.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()
    val playlistTracks by viewModel.playlistTracks.collectAsStateWithLifecycle()
    val tasteProfile by viewModel.tasteProfile.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Back handling for sub-screens
    BackHandler(enabled = currentScreen != ScreenDestination.TRACKS) {
        if (selectedPlaylist != null) {
            viewModel.selectPlaylist(null)
        } else {
            viewModel.navigateTo(ScreenDestination.TRACKS)
        }
    }

    val layoutDirection = if (settings.isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (currentScreen != ScreenDestination.NOW_PLAYING) {
                    Column {
                        // Persistent Mini Player above bottom bar
                        if (playbackState.currentTrack != null) {
                            MiniPlayer(
                                playbackState = playbackState,
                                onPlayPause = { viewModel.togglePlayPause() },
                                onSkipNext = { viewModel.playNextTrack() },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onClick = { viewModel.navigateTo(ScreenDestination.NOW_PLAYING) }
                            )
                        }

                        // Custom Floating Bottom Navigation with bounded indicators to prevent edge bleeding
                        CustomFloatingBottomNavBar(
                            currentScreen = currentScreen,
                            onNavigate = { viewModel.navigateTo(it) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    ScreenDestination.TRACKS -> {
                        TracksScreen(
                            allTracks = allTracks,
                            favoriteTracks = favoriteTracks,
                            mostPlayedTracks = mostPlayedTracks,
                            recentlyPlayedTracks = recentlyPlayedTracks,
                            playbackState = playbackState,
                            playlists = allPlaylists,
                            searchQuery = searchQuery,
                            sortOrder = sortOrder,
                            isScanning = isScanning,
                            isPersian = settings.isPersian,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onSortOrderChange = { viewModel.setSortOrder(it) },
                            onScanDevice = { viewModel.scanDeviceAudio() },
                            onTrackClick = { track, queue -> viewModel.playTrack(track, queue) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onImportAudio = { viewModel.importAudioUri(it) },
                            onAddToPlaylist = { pId, tId -> viewModel.addTrackToPlaylist(pId, tId) },
                            onCreatePlaylistAndAddTrack = { name, tId -> viewModel.createPlaylistAndAddTrack(name, tId) }
                        )
                    }
                    ScreenDestination.CATEGORIES -> {
                        FoldersScreen(
                            allTracks = allTracks,
                            folders = allFolders,
                            artists = allArtists,
                            albums = allAlbums,
                            genres = allGenres,
                            playbackState = playbackState,
                            playlists = allPlaylists,
                            onTrackClick = { track, queue -> viewModel.playTrack(track, queue) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onAddToPlaylist = { pId, tId -> viewModel.addTrackToPlaylist(pId, tId) },
                            onCreatePlaylistAndAddTrack = { name, tId -> viewModel.createPlaylistAndAddTrack(name, tId) }
                        )
                    }
                    ScreenDestination.PLAYLISTS -> {
                        PlaylistsScreen(
                            playlists = allPlaylists,
                            selectedPlaylist = selectedPlaylist,
                            playlistTracks = playlistTracks,
                            playbackState = playbackState,
                            onSelectPlaylist = { viewModel.selectPlaylist(it) },
                            onCreatePlaylist = { name, desc -> viewModel.createPlaylist(name, desc) },
                            onDeletePlaylist = { viewModel.deletePlaylist(it) },
                            onRemoveTrackFromPlaylist = { pId, tId -> viewModel.removeTrackFromPlaylist(pId, tId) },
                            onTrackClick = { track, queue -> viewModel.playTrack(track, queue) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onAddToPlaylist = { pId, tId -> viewModel.addTrackToPlaylist(pId, tId) },
                            onCreatePlaylistAndAddTrack = { name, tId -> viewModel.createPlaylistAndAddTrack(name, tId) }
                        )
                    }
                    ScreenDestination.RECOMMENDATIONS -> {
                        RecommendationsScreen(
                            recommendations = recommendations,
                            tasteProfile = tasteProfile,
                            playbackState = playbackState,
                            playlists = allPlaylists,
                            onTrackClick = { track, queue -> viewModel.playTrack(track, queue) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onAddToPlaylist = { pId, tId -> viewModel.addTrackToPlaylist(pId, tId) },
                            onCreatePlaylistAndAddTrack = { name, tId -> viewModel.createPlaylistAndAddTrack(name, tId) }
                        )
                    }
                    ScreenDestination.EQUALIZER -> {
                        EqualizerScreen(
                            presets = equalizerPresets,
                            playbackState = playbackState,
                            onBandLevelChange = { band, level -> viewModel.setEqualizerBandLevel(band, level) },
                            onBassBoostChange = { strength -> viewModel.setBassBoost(strength) },
                            onVirtualizerChange = { strength -> viewModel.setVirtualizer(strength) },
                            onPlaybackSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) },
                            onApplyPreset = { preset -> viewModel.applyPreset(preset) },
                            onSaveCustomPreset = { name, bands, bass, virt ->
                                viewModel.saveCustomPreset(name, bands, bass, virt)
                            }
                        )
                    }
                    ScreenDestination.SETTINGS -> {
                        SettingsScreen(
                            settings = settings,
                            sleepTimerRemaining = playbackState.sleepTimerMinutesRemaining,
                            onTogglePersian = { viewModel.togglePersian() },
                            onToggleAmoled = { viewModel.toggleAmoled() },
                            onSetAccentTheme = { viewModel.setAccentTheme(it) },
                            onSetNowPlayingStyle = { viewModel.setNowPlayingStyle(it) },
                            onSetMinDuration = { viewModel.setMinDurationSeconds(it) },
                            onSetCrossfade = { viewModel.setCrossfadeSeconds(it) },
                            onTogglePauseOnHeadset = { viewModel.togglePauseOnHeadsetDisconnect() },
                            onToggleHifi = { viewModel.toggleHifiAudioMode() },
                            onToggleLockScreenControls = { viewModel.toggleLockScreenControls() },
                            onStartSleepTimer = { viewModel.startSleepTimer(it) },
                            onCancelSleepTimer = { viewModel.cancelSleepTimer() },
                            onScanAudio = { viewModel.scanDeviceAudio() }
                        )
                    }
                    ScreenDestination.NOW_PLAYING -> {
                        NowPlayingScreen(
                            playbackState = playbackState,
                            playlists = allPlaylists,
                            visualizerStyle = settings.nowPlayingStyle,
                            onCycleVisualizerStyle = { viewModel.cycleNowPlayingStyle() },
                            onPlayPause = { viewModel.togglePlayPause() },
                            onSkipNext = { viewModel.playNextTrack() },
                            onSkipPrevious = { viewModel.playPreviousTrack() },
                            onSeekTo = { viewModel.seekTo(it) },
                            onToggleShuffle = { viewModel.toggleShuffle() },
                            onToggleRepeat = { viewModel.toggleRepeat() },
                            onToggleStopAfterTrack = { viewModel.toggleStopAfterCurrentTrack() },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onNavigateToEqualizer = { viewModel.navigateTo(ScreenDestination.EQUALIZER) },
                            onNavigateBack = { viewModel.navigateTo(ScreenDestination.TRACKS) },
                            onAddToPlaylist = { pId, tId -> viewModel.addTrackToPlaylist(pId, tId) },
                            onCreatePlaylistAndAddTrack = { name, tId -> viewModel.createPlaylistAndAddTrack(name, tId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomFloatingBottomNavBar(
    currentScreen: ScreenDestination,
    onNavigate: (ScreenDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItemData(ScreenDestination.TRACKS, "آهنگ‌ها", Icons.Default.MusicNote, "nav_tab_tracks"),
        NavItemData(ScreenDestination.CATEGORIES, "دسته‌بندی", Icons.Default.Folder, "nav_tab_categories"),
        NavItemData(ScreenDestination.PLAYLISTS, "لیست‌ها", Icons.Default.QueueMusic, "nav_tab_playlists"),
        NavItemData(ScreenDestination.RECOMMENDATIONS, "پیشنهادی", Icons.Default.AutoAwesome, "nav_tab_recommendations"),
        NavItemData(ScreenDestination.EQUALIZER, "اکولایزر", Icons.Default.Equalizer, "nav_tab_equalizer"),
        NavItemData(ScreenDestination.SETTINGS, "تنظیمات", Icons.Default.Settings, "nav_tab_settings")
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(26.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .testTag("main_bottom_nav"),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentScreen == item.destination
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigate(item.destination) }
                        .padding(vertical = 4.dp)
                        .testTag(item.testTag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(20.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
