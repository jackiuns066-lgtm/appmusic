package com.example.data.recommendation

import com.example.data.local.TrackEntity

data class TasteProfile(
    val topGenre: String = "آرامش‌بخش",
    val topMood: String = "Relaxed",
    val preferredBpmRange: IntRange = 70..120,
    val favoriteCount: Int = 0,
    val totalPlays: Int = 0
)

/** Why a track has been recommended. The text itself is resolved by the UI in the active language. */
enum class RecommendationReason {
    GENRE_AND_MOOD, GENRE, MOOD, VARIETY
}

data class TrackRecommendation(
    val track: TrackEntity,
    val matchPercentage: Int,
    val reasonType: RecommendationReason,
    val reasonGenre: String = "",
    val reasonMood: String = ""
)

object RecommendationEngine {

    fun analyzeTasteProfile(tracks: List<TrackEntity>): TasteProfile {
        if (tracks.isEmpty()) return TasteProfile()

        val favorites = tracks.filter { it.isFavorite }
        val played = tracks.filter { it.playCount > 0 }

        val weightedTracks = mutableListOf<TrackEntity>()
        for (t in tracks) {
            val weight = (if (t.isFavorite) 3 else 1) + (t.playCount.coerceAtMost(10))
            repeat(weight) { weightedTracks.add(t) }
        }

        val topGenre = weightedTracks.groupingBy { it.genre }
            .eachCount()
            .maxByOrNull { it.value }?.key ?: "عمومی"

        val topMood = weightedTracks.groupingBy { it.mood }
            .eachCount()
            .maxByOrNull { it.value }?.key ?: "آرامش‌بخش"

        val avgBpm = if (weightedTracks.isNotEmpty()) {
            weightedTracks.map { it.bpm }.average().toInt()
        } else 90

        return TasteProfile(
            topGenre = topGenre,
            topMood = topMood,
            preferredBpmRange = (avgBpm - 20).coerceAtLeast(50)..(avgBpm + 25),
            favoriteCount = favorites.size,
            totalPlays = played.sumOf { it.playCount }
        )
    }

    fun getRecommendations(
        allTracks: List<TrackEntity>,
        profile: TasteProfile,
        limit: Int = 10
    ): List<TrackRecommendation> {
        if (allTracks.isEmpty()) return emptyList()

        return allTracks
            .map { track ->
                var score = 40

                if (track.genre == profile.topGenre) score += 30
                if (track.mood == profile.topMood) score += 20
                if (track.bpm in profile.preferredBpmRange) score += 10
                if (track.playCount == 0) score += 10 // recommend unheard songs
                if (track.isFavorite) score -= 15 // user already knows favorites well

                val boundedScore = score.coerceIn(45, 98)

                val reasonType = when {
                    track.genre == profile.topGenre && track.mood == profile.topMood ->
                        RecommendationReason.GENRE_AND_MOOD
                    track.genre == profile.topGenre -> RecommendationReason.GENRE
                    track.mood == profile.topMood -> RecommendationReason.MOOD
                    else -> RecommendationReason.VARIETY
                }

                TrackRecommendation(
                    track = track,
                    matchPercentage = boundedScore,
                    reasonType = reasonType,
                    reasonGenre = track.genre,
                    reasonMood = track.mood
                )
            }
            .sortedByDescending { it.matchPercentage }
            .take(limit)
    }
}
