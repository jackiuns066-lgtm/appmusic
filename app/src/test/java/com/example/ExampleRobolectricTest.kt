package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.audio.AudioPlayerEngine
import com.example.data.audio.RepeatMode
import com.example.data.local.TrackEntity
import com.example.data.recommendation.RecommendationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Ava Music", appName)
    }

    @Test
    fun `test stop after current track toggle`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = AudioPlayerEngine(context)

        assertFalse(engine.playbackState.value.stopAfterCurrentTrack)
        val enabled = engine.toggleStopAfterCurrentTrack()
        assertTrue(enabled)
        assertTrue(engine.playbackState.value.stopAfterCurrentTrack)

        val disabled = engine.toggleStopAfterCurrentTrack()
        assertFalse(disabled)
        assertFalse(engine.playbackState.value.stopAfterCurrentTrack)

        engine.release()
    }

    @Test
    fun `test repeat mode toggle sequence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = AudioPlayerEngine(context)

        assertEquals(RepeatMode.ALL, engine.playbackState.value.repeatMode)
        engine.toggleRepeatMode()
        assertEquals(RepeatMode.ONE, engine.playbackState.value.repeatMode)
        engine.toggleRepeatMode()
        assertEquals(RepeatMode.OFF, engine.playbackState.value.repeatMode)
        engine.toggleRepeatMode()
        assertEquals(RepeatMode.ALL, engine.playbackState.value.repeatMode)

        engine.release()
    }

    @Test
    fun `test recommendation engine taste analysis and scoring`() {
        val tracks = listOf(
            TrackEntity(
                id = "t1",
                title = "سنتی ۱",
                artist = "هنرمند ۱",
                album = "آلبوم ۱",
                durationMs = 120000,
                uriString = "uri1",
                genre = "سنتی ایرانی",
                mood = "Relaxed",
                bpm = 80,
                playCount = 20,
                isFavorite = true
            ),
            TrackEntity(
                id = "t2",
                title = "سینث ۲",
                artist = "هنرمند ۲",
                album = "آلبوم ۲",
                durationMs = 140000,
                uriString = "uri2",
                genre = "Synthwave",
                mood = "Energetic",
                bpm = 125,
                playCount = 2,
                isFavorite = false
            ),
            TrackEntity(
                id = "t3",
                title = "پیشنهادی جدید سنتی",
                artist = "هنرمند ۳",
                album = "آلبوم ۳",
                durationMs = 130000,
                uriString = "uri3",
                genre = "سنتی ایرانی",
                mood = "Relaxed",
                bpm = 82,
                playCount = 0,
                isFavorite = false
            )
        )

        val profile = RecommendationEngine.analyzeTasteProfile(tracks)
        assertEquals("سنتی ایرانی", profile.topGenre)
        assertEquals("Relaxed", profile.topMood)

        val recs = RecommendationEngine.getRecommendations(tracks, profile)
        assertNotNull(recs)
        assertTrue(recs.isNotEmpty())

        val topRec = recs.first()
        assertEquals("سنتی ایرانی", topRec.track.genre)
        assertTrue(topRec.matchPercentage >= 70)
        assertTrue(topRec.reasonFa.isNotBlank())
    }

    @Test
    fun `test player engine updateCurrentTrackFavorite keeps state in sync`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = AudioPlayerEngine(context)

        engine.updateCurrentTrackFavorite(false)
        assertFalse(engine.playbackState.value.currentTrack?.isFavorite ?: false)

        engine.release()
    }

    @Test
    fun `test launch MainActivity`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertNotNull(controller.get())
    }
}
