package com.example.data.audio

import android.content.Context
import com.example.R
import com.example.data.local.TrackEntity
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class SynthType {
    ACOUSTIC_PLUCK, SYNTH_BASS, LOFI_KEYS, TRADITIONAL_SETAR
}

data class DemoTrackSpec(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val mood: String,
    val bpm: Int,
    val durationSeconds: Int,
    val baseNotes: List<Double>,
    val synthType: SynthType,
    val defaultDownloaded: Boolean = true,
    val initialPlayCount: Int = 0
)

object AudioSynthGenerator {

    fun generateDemoTracksIfMissing(context: Context): List<TrackEntity> {
        val audioDir = File(context.filesDir, "demo_audio").apply { if (!exists()) mkdirs() }
        val downloadDir = File(context.filesDir, "downloads").apply { if (!exists()) mkdirs() }

        val tracks = listOf(
            DemoTrackSpec(
                id = "demo_persian_shur",
                title = context.getString(R.string.demo_persian_title),
                artist = context.getString(R.string.demo_persian_artist),
                album = context.getString(R.string.demo_persian_album),
                genre = context.getString(R.string.demo_persian_genre),
                mood = "Acoustic",
                bpm = 84,
                durationSeconds = 4,
                baseNotes = listOf(220.0, 247.5, 261.6, 293.6, 329.6),
                synthType = SynthType.TRADITIONAL_SETAR,
                defaultDownloaded = true,
                initialPlayCount = 24
            ),
            DemoTrackSpec(
                id = "demo_neon_drive",
                title = context.getString(R.string.demo_neon_title),
                artist = "CyberPulse",
                album = "Retro Horizon 1984",
                genre = "Synthwave",
                mood = "Energetic",
                bpm = 124,
                durationSeconds = 4,
                baseNotes = listOf(130.8, 146.8, 164.8, 196.0, 220.0),
                synthType = SynthType.SYNTH_BASS,
                defaultDownloaded = true,
                initialPlayCount = 14
            ),
            DemoTrackSpec(
                id = "demo_lofi_sunset",
                title = context.getString(R.string.demo_turquoise_title),
                artist = "Nima Chillout",
                album = "Midnight Cafeteria",
                genre = "Lo-Fi Beats",
                mood = "Relaxed",
                bpm = 78,
                durationSeconds = 4,
                baseNotes = listOf(174.6, 220.0, 261.6, 329.6),
                synthType = SynthType.LOFI_KEYS,
                defaultDownloaded = false,
                initialPlayCount = 18
            ),
            DemoTrackSpec(
                id = "demo_desert_pulse",
                title = context.getString(R.string.demo_desert_title),
                artist = context.getString(R.string.demo_desert_artist),
                album = context.getString(R.string.demo_desert_album),
                genre = context.getString(R.string.demo_desert_genre),
                mood = "Energetic",
                bpm = 110,
                durationSeconds = 4,
                baseNotes = listOf(196.0, 220.0, 246.9, 293.6),
                synthType = SynthType.ACOUSTIC_PLUCK,
                defaultDownloaded = true,
                initialPlayCount = 31
            )
        )

        val resultEntities = mutableListOf<TrackEntity>()

        for (spec in tracks) {
            val file = File(audioDir, "${spec.id}.wav")
            try {
                if (!file.exists() || file.length() < 500) {
                    generateWavFile(file, spec)
                }
            } catch (t: Throwable) {
                android.util.Log.e("AudioSynthGenerator", "Failed to generate ${spec.id}: ${t.message}")
            }

            var isDownloaded = spec.defaultDownloaded
            var downloadedPath: String? = null
            var fileSize = if (file.exists()) file.length() else 0L

            if (isDownloaded && file.exists()) {
                try {
                    val dlFile = File(downloadDir, "${spec.id}.wav")
                    if (!dlFile.exists() || dlFile.length() < 500) {
                        file.copyTo(dlFile, overwrite = true)
                    }
                    downloadedPath = dlFile.absolutePath
                    fileSize = dlFile.length()
                } catch (t: Throwable) {
                    android.util.Log.e("AudioSynthGenerator", "Failed to copy downloaded demo: ${t.message}")
                }
            }

            resultEntities.add(
                TrackEntity(
                    id = spec.id,
                    title = spec.title,
                    artist = spec.artist,
                    album = spec.album,
                    durationMs = (spec.durationSeconds * 1000).toLong(),
                    uriString = file.absolutePath,
                    genre = spec.genre,
                    mood = spec.mood,
                    bpm = spec.bpm,
                    playCount = spec.initialPlayCount,
                    isFavorite = spec.initialPlayCount > 20,
                    lastPlayedTimestamp = if (spec.initialPlayCount > 0) System.currentTimeMillis() - 86400000L else 0L,
                    folderPath = context.getString(R.string.demo_folder),
                    isDownloaded = isDownloaded,
                    downloadProgress = if (isDownloaded) 100 else 0,
                    downloadedFilePath = downloadedPath,
                    fileSizeBytes = fileSize
                )
            )
        }
        return resultEntities
    }

    private fun generateWavFile(outputFile: File, spec: DemoTrackSpec) {
        val sampleRate = 16000
        val totalSamples = sampleRate * spec.durationSeconds
        val samples = ShortArray(totalSamples)

        val beatDurationSamples = (sampleRate * 60.0 / spec.bpm).toInt().coerceAtLeast(1000)

        for (i in 0 until totalSamples) {
            val beatIndex = i / beatDurationSamples
            val beatPos = (i % beatDurationSamples).toDouble() / beatDurationSamples
            val noteIndex = beatIndex % spec.baseNotes.size
            val baseFreq = spec.baseNotes[noteIndex]

            val t = i.toDouble() / sampleRate
            var sampleVal = 0.0

            when (spec.synthType) {
                SynthType.ACOUSTIC_PLUCK, SynthType.TRADITIONAL_SETAR -> {
                    val env = exp(-beatPos * 5.0)
                    sampleVal = sin(2 * PI * baseFreq * t) * env
                    sampleVal += 0.5 * sin(2 * PI * (baseFreq * 2) * t) * exp(-beatPos * 8.0)
                }
                SynthType.SYNTH_BASS -> {
                    val env = exp(-beatPos * 3.0)
                    sampleVal = sin(2 * PI * baseFreq * t) * env
                    sampleVal += 0.4 * sin(2 * PI * (baseFreq * 0.5) * t) * env
                }
                SynthType.LOFI_KEYS -> {
                    val env = exp(-beatPos * 2.5)
                    sampleVal = (sin(2 * PI * baseFreq * t) + 0.3 * sin(2 * PI * baseFreq * 1.5 * t)) * env
                }
            }

            val fadeIn = (i.toDouble() / (sampleRate * 0.1)).coerceIn(0.0, 1.0)
            val fadeOut = ((totalSamples - i).toDouble() / (sampleRate * 0.1)).coerceIn(0.0, 1.0)
            sampleVal *= (fadeIn * fadeOut)

            samples[i] = (sampleVal.coerceIn(-1.0, 1.0) * 32760).toInt().toShort()
        }

        writeWav(outputFile, samples, sampleRate)
    }

    private fun writeWav(file: File, samples: ShortArray, sampleRate: Int) {
        val numChannels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * (bitsPerSample / 8)
        val blockAlign = numChannels * (bitsPerSample / 8)
        val dataSize = samples.size * 2
        val chunkSize = 36 + dataSize

        FileOutputStream(file).use { fos ->
            val header = ByteBuffer.allocate(44).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                put("RIFF".toByteArray())
                putInt(chunkSize)
                put("WAVE".toByteArray())
                put("fmt ".toByteArray())
                putInt(16)
                putShort(1.toShort())
                putShort(numChannels.toShort())
                putInt(sampleRate)
                putInt(byteRate)
                putShort(blockAlign.toShort())
                putShort(bitsPerSample.toShort())
                put("data".toByteArray())
                putInt(dataSize)
            }
            fos.write(header.array())

            val buffer = ByteBuffer.allocate(samples.size * 2).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                for (s in samples) {
                    putShort(s)
                }
            }
            fos.write(buffer.array())
        }
    }
}
