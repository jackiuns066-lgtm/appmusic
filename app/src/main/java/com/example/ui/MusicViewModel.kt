package com.example.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AvaMusicApp
import com.example.data.audio.AudioPlayerEngine
import com.example.data.audio.MediaNotificationManager
import com.example.data.audio.PlaybackState
import com.example.data.audio.RepeatMode
import com.example.data.local.AppDatabase
import com.example.data.local.EqualizerPresetEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.TrackEntity
import com.example.data.preferences.PreferenceManager
import com.example.data.recommendation.RecommendationEngine
import com.example.data.recommendation.TasteProfile
import com.example.data.recommendation.TrackRecommendation
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class ScreenDestination {
    TRACKS, CATEGORIES, PLAYLISTS, RECOMMENDATIONS, EQUALIZER, SETTINGS, NOW_PLAYING
}

enum class SortOrder {
    RECENTLY_ADDED, TITLE_AZ, ARTIST_AZ, MOST_PLAYED, DURATION
}

data class AppSettings(
    val isPersian: Boolean = true,
    val isAmoledDark: Boolean = false,
    val accentTheme: String = "gold",
    val sleepTimerMinutes: Int = 0,
    val nowPlayingStyle: String = "neon_vinyl", // neon_vinyl, spectrum_wave, glass_3d, pulse_rings
    val minDurationSeconds: Int = 15,
    val crossfadeSeconds: Int = 0,
    val pauseOnHeadsetDisconnect: Boolean = true,
    val hifiAudioMode: Boolean = true,
    val lockScreenControlsEnabled: Boolean = true
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = (application as? AvaMusicApp)?.database ?: AppDatabase.getInstance(context)
    private val repository = (application as? AvaMusicApp)?.repository ?: MusicRepository(context, database)
    private val playerEngine = (application as? AvaMusicApp)?.playerEngine ?: AudioPlayerEngine(context)
    private val mediaNotificationManager = MediaNotificationManager(context)
    private val preferenceManager = PreferenceManager(context)

    val playbackState: StateFlow<PlaybackState> = playerEngine.playbackState

    val allTracks: StateFlow<List<TrackEntity>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<TrackEntity>> = repository.favoriteTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayedTracks: StateFlow<List<TrackEntity>> = repository.mostPlayedTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayedTracks: StateFlow<List<TrackEntity>> = repository.recentlyPlayedTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<PlaylistEntity>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val equalizerPresets: StateFlow<List<EqualizerPresetEntity>> = repository.equalizerPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFolders: StateFlow<List<String>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allArtists: StateFlow<List<String>> = repository.allArtists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAlbums: StateFlow<List<String>> = repository.allAlbums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGenres: StateFlow<List<String>> = repository.allGenres
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentScreen = MutableStateFlow(ScreenDestination.TRACKS)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(preferenceManager.loadSortOrder())
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val selectedPlaylist: StateFlow<PlaylistEntity?> = _selectedPlaylist.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val playlistTracks: StateFlow<List<TrackEntity>> = _selectedPlaylist.flatMapLatest { playlist ->
        if (playlist == null) flowOf(emptyList())
        else repository.getTracksForPlaylist(playlist.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasteProfile: StateFlow<TasteProfile> = allTracks.combine(favoriteTracks) { all, _ ->
        RecommendationEngine.analyzeTasteProfile(all)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TasteProfile())

    val recommendations: StateFlow<List<TrackRecommendation>> = allTracks.combine(tasteProfile) { all, profile ->
        RecommendationEngine.getRecommendations(all, profile, limit = 15)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _settings = MutableStateFlow(preferenceManager.loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        playerEngine.onTrackCompletedCallback = {
            playNextTrack()
        }
        playerEngine.onStopAfterTrackTriggeredCallback = {
            showToast("پخش آهنگ به پایان رسید (توقف خودکار فعال بود)")
        }

        viewModelScope.launch {
            combine(playbackState, _settings) { state, setts ->
                mediaNotificationManager.updateNotification(
                    track = state.currentTrack,
                    isPlaying = state.isPlaying,
                    isEnabled = setts.lockScreenControlsEnabled
                )
            }.collect()
        }

        // Persist settings whenever changed
        viewModelScope.launch {
            _settings.collect { setts ->
                preferenceManager.saveSettings(setts)
            }
        }
    }

    fun navigateTo(screen: ScreenDestination) {
        _currentScreen.value = screen
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        preferenceManager.saveSortOrder(order)
    }

    fun selectPlaylist(playlist: PlaylistEntity?) {
        _selectedPlaylist.value = playlist
    }

    fun playTrack(track: TrackEntity, queue: List<TrackEntity>? = null) {
        viewModelScope.launch {
            repository.recordTrackPlayed(track.id)
            val fullQueue = queue ?: allTracks.value
            playerEngine.setQueue(fullQueue)
            playerEngine.playTrack(track)
        }
    }

    fun togglePlayPause() {
        val state = playbackState.value
        if (state.isPlaying) {
            playerEngine.pause()
        } else {
            if (state.currentTrack != null) {
                playerEngine.resume()
            } else {
                val firstTrack = allTracks.value.firstOrNull()
                if (firstTrack != null) {
                    playTrack(firstTrack)
                }
            }
        }
    }

    fun playNextTrack() {
        val state = playbackState.value
        val queue = state.currentQueue.ifEmpty { allTracks.value }
        if (queue.isEmpty()) return

        val currentTrack = state.currentTrack
        val nextTrack = if (state.isShuffleEnabled) {
            queue.random()
        } else {
            val currentIndex = queue.indexOfFirst { it.id == currentTrack?.id }
            if (currentIndex in 0 until queue.lastIndex) {
                queue[currentIndex + 1]
            } else if (state.repeatMode == RepeatMode.ALL) {
                queue.first()
            } else null
        }

        if (nextTrack != null) {
            playTrack(nextTrack, queue)
        } else {
            playerEngine.pause()
        }
    }

    fun playPreviousTrack() {
        val state = playbackState.value
        val queue = state.currentQueue.ifEmpty { allTracks.value }
        if (queue.isEmpty()) return

        if (state.currentPositionMs > 3000L) {
            playerEngine.seekTo(0L)
            return
        }

        val currentIndex = queue.indexOfFirst { it.id == state.currentTrack?.id }
        val prevTrack = if (currentIndex > 0) {
            queue[currentIndex - 1]
        } else {
            queue.lastOrNull()
        }

        if (prevTrack != null) {
            playTrack(prevTrack, queue)
        }
    }

    fun seekTo(positionMs: Long) {
        playerEngine.seekTo(positionMs)
    }

    fun toggleShuffle() {
        playerEngine.toggleShuffle()
    }

    fun toggleRepeat(): RepeatMode {
        return playerEngine.toggleRepeatMode()
    }

    fun toggleStopAfterCurrentTrack(): Boolean {
        return playerEngine.toggleStopAfterCurrentTrack()
    }

    fun setPlaybackSpeed(speed: Float) {
        playerEngine.setPlaybackSpeed(speed)
    }

    fun toggleFavorite(track: TrackEntity) {
        viewModelScope.launch {
            val newFav = !track.isFavorite
            repository.setFavorite(track.id, newFav)

            if (playbackState.value.currentTrack?.id == track.id) {
                playerEngine.updateCurrentTrackFavorite(newFav)
            }

            val msg = if (newFav) "به علاقه‌مندی‌ها اضافه شد ❤️" else "از علاقه‌مندی‌ها حذف شد"
            showToast(msg)
        }
    }

    fun createPlaylist(name: String, description: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name.trim(), description.trim())
            showToast("لیست «$name» ایجاد شد")
        }
    }

    fun createPlaylistAndAddTrack(name: String, trackId: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val pId = repository.createPlaylist(name.trim(), "")
            repository.addTrackToPlaylist(pId, trackId)
            showToast("لیست «$name» ایجاد و آهنگ به آن اضافه شد")
        }
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
            showToast("به لیست پخش اضافه شد")
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
            showToast("از لیست پخش حذف شد")
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist.id)
            if (_selectedPlaylist.value?.id == playlist.id) {
                _selectedPlaylist.value = null
            }
            showToast("لیست «${playlist.name}» حذف شد")
        }
    }

    fun setEqualizerBandLevel(band: Short, levelMilliBels: Short) {
        playerEngine.setEqualizerBandLevel(band, levelMilliBels)
    }

    fun setBassBoost(strength: Short) {
        playerEngine.setBassBoost(strength)
    }

    fun setVirtualizer(strength: Short) {
        playerEngine.setVirtualizer(strength)
    }

    fun applyPreset(preset: EqualizerPresetEntity) {
        try {
            val levels = preset.bandLevels.split(",").mapNotNull { it.trim().toShortOrNull() }
            levels.forEachIndexed { index, level ->
                playerEngine.setEqualizerBandLevel(index.toShort(), level)
            }
            playerEngine.setBassBoost(preset.bassBoostLevel.toShort())
            playerEngine.setVirtualizer(preset.virtualizerLevel.toShort())
            showToast("پریست «${preset.name}» فعال شد")
        } catch (e: Exception) {
            android.util.Log.e("MusicViewModel", "Failed to apply preset: ${e.message}")
        }
    }

    fun saveCustomPreset(name: String, bandLevels: List<Short>, bass: Int, virtualizer: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = "preset_custom_${UUID.randomUUID().toString().take(6)}"
            val levelsStr = bandLevels.joinToString(",")
            val entity = EqualizerPresetEntity(
                id = id,
                name = name.trim(),
                isSystemPreset = false,
                bandLevels = levelsStr,
                bassBoostLevel = bass,
                virtualizerLevel = virtualizer
            )
            repository.saveEqualizerPreset(entity)
            showToast("پریست اکولایزر «$name» ذخیره شد")
        }
    }

    fun startSleepTimer(minutes: Int) {
        playerEngine.startSleepTimer(minutes)
        _settings.update { it.copy(sleepTimerMinutes = minutes) }
        val msg = if (minutes > 0) "تایمر خواب روی $minutes دقیقه تنظیم شد" else "تایمر خواب غیرفعال شد"
        showToast(msg)
    }

    fun cancelSleepTimer() {
        playerEngine.cancelSleepTimer()
        _settings.update { it.copy(sleepTimerMinutes = 0) }
        showToast("تایمر خواب لغو شد")
    }

    fun togglePersian() {
        _settings.update { it.copy(isPersian = !it.isPersian) }
    }

    fun toggleAmoled() {
        _settings.update { it.copy(isAmoledDark = !it.isAmoledDark) }
    }

    fun setAccentTheme(themeName: String) {
        _settings.update { it.copy(accentTheme = themeName) }
    }

    fun setNowPlayingStyle(style: String) {
        _settings.update { it.copy(nowPlayingStyle = style) }
    }

    fun cycleNowPlayingStyle() {
        val styles = listOf("neon_vinyl", "spectrum_wave", "glass_3d", "pulse_rings")
        val currentIndex = styles.indexOf(_settings.value.nowPlayingStyle)
        val nextStyle = styles[(currentIndex + 1) % styles.size]
        _settings.update { it.copy(nowPlayingStyle = nextStyle) }
        val nameFa = when (nextStyle) {
            "neon_vinyl" -> "چرخش نئونی وینیل"
            "spectrum_wave" -> "اکولایزر طیف نئونی"
            "glass_3d" -> "کاور سه‌بعدی شیشه‌ای"
            else -> "امواج تپنده ریتمیک"
        }
        showToast("جلوه بصری: $nameFa")
    }

    fun setMinDurationSeconds(seconds: Int) {
        _settings.update { it.copy(minDurationSeconds = seconds) }
        viewModelScope.launch {
            repository.deleteRingtonesAndShortTracks(seconds * 1000L)
            showToast("فیلتر حداقل زمان روی $seconds ثانیه تنظیم شد")
        }
    }

    fun setCrossfadeSeconds(seconds: Int) {
        _settings.update { it.copy(crossfadeSeconds = seconds) }
    }

    fun togglePauseOnHeadsetDisconnect() {
        _settings.update { it.copy(pauseOnHeadsetDisconnect = !it.pauseOnHeadsetDisconnect) }
    }

    fun toggleHifiAudioMode() {
        _settings.update { it.copy(hifiAudioMode = !it.hifiAudioMode) }
    }

    fun toggleLockScreenControls() {
        _settings.update {
            val updated = !it.lockScreenControlsEnabled
            if (!updated) {
                mediaNotificationManager.dismissNotification()
            }
            it.copy(lockScreenControlsEnabled = updated)
        }
    }

    fun performStartupScan() {
        viewModelScope.launch {
            val isFirstTime = !preferenceManager.hasCompletedInitialScan()
            if (isFirstTime) {
                _isScanning.value = true
                val count = repository.scanDeviceAudio(_settings.value.minDurationSeconds * 1000L)
                preferenceManager.setInitialScanCompleted(true)
                _isScanning.value = false
                if (count > 0) {
                    showToast("$count آهنگ در حافظه دستگاه یافت شد")
                }
            } else {
                // Quick silent background incremental check: detects newly downloaded/added audio files
                try {
                    repository.scanDeviceAudio(_settings.value.minDurationSeconds * 1000L)
                } catch (_: Exception) {}
            }
        }
    }

    fun scanDeviceAudio() {
        viewModelScope.launch {
            _isScanning.value = true
            val count = repository.scanDeviceAudio(_settings.value.minDurationSeconds * 1000L)
            preferenceManager.setInitialScanCompleted(true)
            _isScanning.value = false
            showToast("$count آهنگ در حافظه دستگاه بررسی و همگام شد")
        }
    }

    fun importAudioUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val fileName = getFileNameFromUri(uri) ?: "imported_${System.currentTimeMillis()}.mp3"
                val audioDir = File(context.filesDir, "imported_audio").apply { if (!exists()) mkdirs() }
                val destFile = File(audioDir, fileName)

                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                val title = fileName.substringBeforeLast(".")
                val track = TrackEntity(
                    id = "import_${UUID.randomUUID().toString().take(8)}",
                    title = title,
                    artist = "فایل وارد شده",
                    album = "دانلودها / وارد شده",
                    durationMs = 0L,
                    uriString = destFile.absolutePath,
                    folderPath = "وارد شده دستی",
                    isDownloaded = true,
                    downloadProgress = 100,
                    downloadedFilePath = destFile.absolutePath,
                    fileSizeBytes = destFile.length()
                )
                repository.insertTracks(listOf(track))
                showToast("آهنگ «$title» به کتابخانه اضافه شد")
            } catch (e: Exception) {
                showToast("خطا در وارد کردن فایل: ${e.message}")
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        name = it.getString(nameIndex)
                    }
                }
            }
        } catch (_: Exception) {}
        return name ?: uri.lastPathSegment
    }

    private fun showToast(message: String) {
        viewModelScope.launch {
            _toastEvent.emit(message)
            try {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaNotificationManager.dismissNotification()
        playerEngine.release()
    }
}
