package com.example.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import com.example.data.local.TrackEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class RepeatMode {
    OFF, ONE, ALL
}

data class PlaybackState(
    val currentTrack: TrackEntity? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val stopAfterCurrentTrack: Boolean = false,
    val sleepTimerMinutesRemaining: Int? = null,
    val currentQueue: List<TrackEntity> = emptyList(),
    val playbackSpeed: Float = 1.0f
)

class AudioPlayerEngine(private val context: Context) {

    private val tag = "AudioPlayerEngine"
    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequestObj: Any? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(tag, "AudioFocus loss permanent: pausing music")
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(tag, "AudioFocus loss transient: pausing music")
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(tag, "AudioFocus loss can duck: lowering volume")
                try {
                    mediaPlayer?.setVolume(0.2f, 0.2f)
                } catch (_: Exception) {}
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(tag, "AudioFocus gained: restoring volume")
                try {
                    mediaPlayer?.setVolume(1.0f, 1.0f)
                } catch (_: Exception) {}
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return true
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val req = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                audioFocusRequestObj = req
                am.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (_: Exception) {
            true
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = audioFocusRequestObj as? android.media.AudioFocusRequest
                if (req != null) {
                    am.abandonAudioFocusRequest(req)
                }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (_: Exception) {}
    }

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val engineScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressTrackingJob: Job? = null
    private var sleepCountDownTimer: CountDownTimer? = null

    var onTrackCompletedCallback: (() -> Unit)? = null
    var onStopAfterTrackTriggeredCallback: (() -> Unit)? = null

    init {
        initMediaPlayer()
    }

    private fun initMediaPlayer() {
        try {
            mediaPlayer?.release()
        } catch (_: Throwable) {}

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnCompletionListener {
                    handleTrackCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(tag, "MediaPlayer error: what=$what extra=$extra")
                    true
                }
            }
        } catch (t: Throwable) {
            Log.e(tag, "Failed to instantiate MediaPlayer: ${t.message}")
            mediaPlayer = null
        }
    }

    fun updateCurrentTrackFavorite(isFav: Boolean) {
        _playbackState.update {
            val curr = it.currentTrack
            if (curr != null) {
                it.copy(currentTrack = curr.copy(isFavorite = isFav))
            } else {
                it
            }
        }
    }

    fun playTrack(track: TrackEntity, startFromPositionMs: Long = 0L) {
        if (mediaPlayer == null) {
            initMediaPlayer()
        }
        val mp = mediaPlayer ?: return

        try {
            mp.reset()

            // Resolve file or content URI safely
            val uri = if (track.uriString.startsWith("content://") || track.uriString.startsWith("file://")) {
                Uri.parse(track.uriString)
            } else {
                val f = File(track.uriString)
                if (f.exists()) Uri.fromFile(f) else Uri.parse(track.uriString)
            }

            mp.setDataSource(context, uri)
            mp.prepare()

            // Safe audio effect session initialization after player prepared
            setupAudioEffectsSafe(mp.audioSessionId)

            if (startFromPositionMs > 0L) {
                mp.seekTo(startFromPositionMs.toInt())
            }

            requestAudioFocus()
            mp.start()

            _playbackState.update {
                it.copy(
                    currentTrack = track,
                    isPlaying = true,
                    currentPositionMs = startFromPositionMs,
                    durationMs = mp.duration.toLong().coerceAtLeast(track.durationMs)
                )
            }

            startProgressTracking()
        } catch (e: Exception) {
            Log.e(tag, "Failed to play track: ${track.title}, error: ${e.message}", e)
            _playbackState.update { it.copy(isPlaying = false) }
        }
    }

    fun resume() {
        val mp = mediaPlayer ?: return
        try {
            if (!mp.isPlaying) {
                requestAudioFocus()
                mp.start()
                _playbackState.update { it.copy(isPlaying = true) }
                startProgressTracking()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error resuming playback: ${e.message}")
        }
    }

    fun pause() {
        val mp = mediaPlayer ?: return
        try {
            if (mp.isPlaying) {
                mp.pause()
                abandonAudioFocus()
                _playbackState.update { it.copy(isPlaying = false) }
                stopProgressTracking()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error pausing playback: ${e.message}")
        }
    }

    fun togglePlayPause() {
        if (_playbackState.value.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun seekTo(positionMs: Long) {
        val mp = mediaPlayer ?: return
        try {
            mp.seekTo(positionMs.toInt())
            _playbackState.update { it.copy(currentPositionMs = positionMs) }
        } catch (e: Exception) {
            Log.e(tag, "Error seeking: ${e.message}")
        }
    }

    fun setQueue(queue: List<TrackEntity>) {
        _playbackState.update { it.copy(currentQueue = queue) }
    }

    fun toggleShuffle() {
        _playbackState.update { it.copy(isShuffleEnabled = !it.isShuffleEnabled) }
    }

    fun toggleRepeatMode(): RepeatMode {
        val nextMode = when (_playbackState.value.repeatMode) {
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
            RepeatMode.OFF -> RepeatMode.ALL
        }
        _playbackState.update { it.copy(repeatMode = nextMode) }
        return nextMode
    }

    fun toggleStopAfterCurrentTrack(): Boolean {
        val nextVal = !_playbackState.value.stopAfterCurrentTrack
        _playbackState.update { it.copy(stopAfterCurrentTrack = nextVal) }
        return nextVal
    }

    fun setPlaybackSpeed(speed: Float) {
        try {
            val mp = mediaPlayer ?: return
            val params = mp.playbackParams
            params.speed = speed
            mp.playbackParams = params
            _playbackState.update { it.copy(playbackSpeed = speed) }
        } catch (e: Exception) {
            Log.e(tag, "PlaybackParams speed change failed: ${e.message}")
        }
    }

    fun startSleepTimer(minutes: Int) {
        sleepCountDownTimer?.cancel()
        if (minutes <= 0) {
            _playbackState.update { it.copy(sleepTimerMinutesRemaining = null) }
            return
        }

        _playbackState.update { it.copy(sleepTimerMinutesRemaining = minutes) }

        sleepCountDownTimer = object : CountDownTimer(minutes * 60 * 1000L, 60 * 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val minsLeft = (millisUntilFinished / (60 * 1000L)).toInt() + 1
                _playbackState.update { it.copy(sleepTimerMinutesRemaining = minsLeft) }
            }

            override fun onFinish() {
                _playbackState.update { it.copy(sleepTimerMinutesRemaining = null) }
                pause()
            }
        }.start()
    }

    fun cancelSleepTimer() {
        sleepCountDownTimer?.cancel()
        _playbackState.update { it.copy(sleepTimerMinutesRemaining = null) }
    }

    private fun handleTrackCompletion() {
        if (_playbackState.value.stopAfterCurrentTrack) {
            _playbackState.update { it.copy(isPlaying = false, stopAfterCurrentTrack = false) }
            stopProgressTracking()
            onStopAfterTrackTriggeredCallback?.invoke()
            return
        }

        when (_playbackState.value.repeatMode) {
            RepeatMode.ONE -> {
                _playbackState.value.currentTrack?.let { playTrack(it, 0L) }
            }
            RepeatMode.ALL, RepeatMode.OFF -> {
                onTrackCompletedCallback?.invoke()
            }
        }
    }

    private fun startProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = engineScope.launch {
            while (isActive) {
                try {
                    val mp = mediaPlayer
                    if (mp != null && mp.isPlaying) {
                        val current = mp.currentPosition.toLong()
                        _playbackState.update { it.copy(currentPositionMs = current) }
                    }
                } catch (_: Exception) {}
                delay(400)
            }
        }
    }

    private fun stopProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = null
    }

    private fun setupAudioEffectsSafe(sessionId: Int) {
        try {
            equalizer?.release()
            equalizer = Equalizer(0, sessionId).apply { enabled = true }
        } catch (t: Throwable) {
            Log.w(tag, "Equalizer init bypassed: ${t.message}")
        }

        try {
            bassBoost?.release()
            bassBoost = BassBoost(0, sessionId).apply { enabled = true }
        } catch (t: Throwable) {
            Log.w(tag, "BassBoost init bypassed: ${t.message}")
        }

        try {
            virtualizer?.release()
            virtualizer = Virtualizer(0, sessionId).apply { enabled = true }
        } catch (t: Throwable) {
            Log.w(tag, "Virtualizer init bypassed: ${t.message}")
        }
    }

    fun setEqualizerBandLevel(band: Short, levelMilliBels: Short) {
        try {
            equalizer?.setBandLevel(band, levelMilliBels)
        } catch (e: Exception) {
            Log.w(tag, "Error setting EQ band: ${e.message}")
        }
    }

    fun setBassBoost(strength: Short) {
        try {
            bassBoost?.setStrength(strength)
        } catch (e: Exception) {
            Log.w(tag, "Error setting bass boost: ${e.message}")
        }
    }

    fun setVirtualizer(strength: Short) {
        try {
            virtualizer?.setStrength(strength)
        } catch (e: Exception) {
            Log.w(tag, "Error setting virtualizer: ${e.message}")
        }
    }

    fun release() {
        abandonAudioFocus()
        stopProgressTracking()
        sleepCountDownTimer?.cancel()
        try {
            mediaPlayer?.release()
        } catch (_: Throwable) {}
        mediaPlayer = null

        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (_: Throwable) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
    }
}
