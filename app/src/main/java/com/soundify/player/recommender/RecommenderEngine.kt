package com.soundify.player.recommender

import com.soundify.player.model.AudioFeatures
import com.soundify.player.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.abs
import kotlin.math.pow

@Serializable
data class RecommendationEvent(
    val type: String,
    val ts: Long = System.currentTimeMillis(),
    val track: Track
)

data class UserProfile(
    val artists: Map<String, Double>,
    val genres: Map<String, Double>,
    val features: Map<String, Double>,
    val lastPlayed: Map<String, Long>,
    val skips: Map<String, Int>,
    val topArtists: List<String>,
    val topGenres: List<String>,
    val isCold: Boolean
)

data class ScoredTrack(
    val track: Track,
    val score: Double
)

@Serializable
data class HomeSection(
    val id: String,
    val title: String,
    val tracks: List<Track>
)

interface RecommendationProvider {
    suspend fun byArtist(artistId: String, limit: Int = 15): List<Track>
    suspend fun byGenre(genre: String, limit: Int = 15): List<Track>
    suspend fun trending(limit: Int = 20): List<Track>
    suspend fun newReleases(limit: Int = 20): List<Track>
}

interface RecommenderStorage {
    fun loadEvents(): List<RecommendationEvent>
    fun saveEvents(events: List<RecommendationEvent>)
}

class FileRecommenderStorage(private val file: File) : RecommenderStorage {
    private val json = Json { ignoreUnknownKeys = true }

    override fun loadEvents(): List<RecommendationEvent> {
        return try {
            if (!file.exists()) return emptyList()
            val text = file.readText()
            if (text.isBlank()) emptyList() else json.decodeFromString(text)
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun saveEvents(events: List<RecommendationEvent>) {
        try {
            val text = json.encodeToString(events)
            file.writeText(text)
        } catch (_: Exception) {
        }
    }
}

class RecommenderEngine(
    private val provider: RecommendationProvider,
    private val storage: RecommenderStorage
) {
    companion object {
        val EVENT_WEIGHTS = mapOf(
            "play" to 0.3,
            "complete" to 1.0,
            "replay" to 1.5,
            "like" to 2.0,
            "addToPlaylist" to 2.0,
            "search" to 0.5,
            "skip" to -1.0
        )

        const val HALF_LIFE_DAYS = 30.0
        const val SCORE_WEIGHT_ARTIST = 0.45
        const val SCORE_WEIGHT_GENRE = 0.30
        const val SCORE_WEIGHT_FEATURE = 0.15
        const val SCORE_WEIGHT_POPULARITY = 0.10
        const val EXPLORATION = 0.15
        const val DIVERSITY = 0.35
        const val REPEAT_COOLDOWN_HOURS = 24.0
        const val DAY_MS = 86_400_000.0

        fun decay(ts: Long, now: Long): Double {
            return (0.5).pow((now - ts).toDouble() / (HALF_LIFE_DAYS * DAY_MS))
        }

        fun normalize(map: Map<String, Double>): Map<String, Double> {
            val maxVal = map.values.maxOfOrNull { abs(it) }?.coerceAtLeast(0.0001) ?: 0.0001
            return map.mapValues { it.value / maxVal }
        }

        fun topKeys(map: Map<String, Double>, n: Int): List<String> {
            return map.filter { it.value > 0.0 }
                .toList()
                .sortedByDescending { it.second }
                .take(n)
                .map { it.first }
        }

        fun avg(values: List<Double>): Double {
            return if (values.isEmpty()) 0.0 else values.sum() / values.size
        }

        fun capitalize(s: String): String {
            return s.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        fun featureSimilarity(a: AudioFeatures?, b: Map<String, Double>): Double {
            if (a == null || b.isEmpty()) return 0.5
            val aMap = mapOf(
                "energy" to a.energy.toDouble(),
                "valence" to a.valence.toDouble(),
                "danceability" to a.danceability.toDouble(),
                "tempo" to a.tempo.toDouble()
            )
            val common = b.keys.filter { it in aMap }
            if (common.isEmpty()) return 0.5
            val dist = avg(common.map { abs((aMap[it] ?: 0.5) - (b[it] ?: 0.5)) })
            return (1.0 - minOf(1.0, dist)).coerceIn(0.0, 1.0)
        }

        fun similarity(a: Track, b: Track): Double {
            val s = if (a.resolvedArtistId.isNotBlank() && a.resolvedArtistId == b.resolvedArtistId) 0.7 else 0.0
            val ga = a.genres.toSet()
            val gb = b.genres.toSet()
            val inter = ga.intersect(gb).size
            val union = ga.union(gb).size.coerceAtLeast(1)
            return minOf(1.0, s + 0.3 * (inter.toDouble() / union.toDouble()))
        }

        fun nameOfArtist(events: List<RecommendationEvent>, artistId: String): String? {
            for (i in events.indices.reversed()) {
                val ev = events[i]
                if (ev.track.resolvedArtistId == artistId) {
                    return ev.track.artist
                }
            }
            return null
        }
    }

    private val events = mutableListOf<RecommendationEvent>()

    init {
        events.addAll(storage.loadEvents())
    }

    // ===== 1. TRACK =====
    fun log(type: String, track: Track) {
        if (!EVENT_WEIGHTS.containsKey(type)) return
        synchronized(events) {
            events.add(
                RecommendationEvent(
                    type = type,
                    ts = System.currentTimeMillis(),
                    track = track
                )
            )
            if (events.size > 3000) {
                val trimmed = events.takeLast(3000)
                events.clear()
                events.addAll(trimmed)
            }
            storage.saveEvents(events)
        }
    }

    fun logPlaybackEnd(track: Track, listenedSec: Long, durationSec: Long) {
        if (listenedSec < 30) {
            log("skip", track)
        } else if (durationSec > 0 && listenedSec >= (durationSec * 0.85).toLong()) {
            log("complete", track)
        } else {
            log("play", track)
        }
    }

    // ===== 2. PROFILE =====
    fun buildProfile(): UserProfile {
        val now = System.currentTimeMillis()
        val artists = mutableMapOf<String, Double>()
        val genres = mutableMapOf<String, Double>()
        val featSum = mutableMapOf<String, Double>()
        val featW = mutableMapOf<String, Double>()
        val lastPlayed = mutableMapOf<String, Long>()
        val skips = mutableMapOf<String, Int>()

        val eventsSnapshot = synchronized(events) { events.toList() }

        for (e in eventsSnapshot) {
            val w = (EVENT_WEIGHTS[e.type] ?: 0.0) * decay(e.ts, now)
            val t = e.track
            val artId = t.resolvedArtistId
            if (artId.isNotBlank()) {
                artists[artId] = (artists[artId] ?: 0.0) + w
            }
            for (g in t.genres) {
                val share = w / maxOf(1, t.genres.size)
                genres[g] = (genres[g] ?: 0.0) + share
            }

            val feat = t.features
            if (feat != null && w > 0.0) {
                val featMap = mapOf(
                    "energy" to feat.energy.toDouble(),
                    "valence" to feat.valence.toDouble(),
                    "danceability" to feat.danceability.toDouble(),
                    "tempo" to feat.tempo.toDouble()
                )
                for ((k, v) in featMap) {
                    featSum[k] = (featSum[k] ?: 0.0) + v * w
                    featW[k] = (featW[k] ?: 0.0) + w
                }
            }

            lastPlayed[t.id] = maxOf(lastPlayed[t.id] ?: 0L, e.ts)
            if (e.type == "skip") {
                skips[t.id] = (skips[t.id] ?: 0) + 1
            }
        }

        val features = mutableMapOf<String, Double>()
        for (k in featSum.keys) {
            val wSum = featW[k] ?: 0.0
            if (wSum > 0.0) {
                features[k] = (featSum[k] ?: 0.0) / wSum
            }
        }

        return UserProfile(
            artists = normalize(artists),
            genres = normalize(genres),
            features = features,
            lastPlayed = lastPlayed,
            skips = skips,
            topArtists = topKeys(artists, 5),
            topGenres = topKeys(genres, 5),
            isCold = eventsSnapshot.size < 5
        )
    }

    // ===== 4. SCORE =====
    fun score(track: Track, profile: UserProfile): Double {
        val artistA = profile.artists[track.resolvedArtistId] ?: 0.0
        val genreA = avg(track.genres.map { profile.genres[it] ?: 0.0 })
        val featS = featureSimilarity(track.features, profile.features)
        val pop = (track.popularity.toDouble()) / 100.0

        var s = SCORE_WEIGHT_ARTIST * maxOf(0.0, artistA) +
                SCORE_WEIGHT_GENRE * maxOf(0.0, genreA) +
                SCORE_WEIGHT_FEATURE * featS +
                SCORE_WEIGHT_POPULARITY * pop

        val heard = profile.lastPlayed[track.id]
        if (heard == null) {
            s += EXPLORATION * (0.5 + 0.5 * pop) // fresh song bonus
        } else if (System.currentTimeMillis() - heard < REPEAT_COOLDOWN_HOURS * 3_600_000.0) {
            s -= 1.0 // just heard it
        }

        val skipCount = profile.skips[track.id] ?: 0
        if (skipCount > 0) {
            s -= 0.3 * skipCount
        }
        if (artistA < 0.0) {
            s += artistA * 0.5
        }
        return s
    }

    // ===== 5. DIVERSIFY (MMR: pick high score, but penalise similarity to picks) =====
    fun diversify(scored: List<ScoredTrack>, limit: Int): List<Track> {
        val pool = scored.sortedByDescending { it.score }.toMutableList()
        val picked = mutableListOf<ScoredTrack>()
        val perArtist = mutableMapOf<String, Int>()

        while (picked.size < limit && pool.isNotEmpty()) {
            var bestIdx = 0
            var bestVal = Double.NEGATIVE_INFINITY
            val checkCount = minOf(pool.size, 60)

            for (i in 0 until checkCount) {
                val c = pool[i]
                val maxSim = if (picked.isNotEmpty()) {
                    picked.maxOf { similarity(c.track, it.track) }
                } else 0.0

                var valScore = (1.0 - DIVERSITY) * c.score - DIVERSITY * maxSim
                val artistCount = perArtist[c.track.resolvedArtistId] ?: 0
                if (artistCount >= 2) {
                    valScore -= 1.0 // max ~2 per artist
                }
                if (valScore > bestVal) {
                    bestVal = valScore
                    bestIdx = i
                }
            }
            val chosen = pool.removeAt(bestIdx)
            picked.add(chosen)
            val aid = chosen.track.resolvedArtistId
            perArtist[aid] = (perArtist[aid] ?: 0) + 1
        }
        return picked.map { it.track }
    }

    fun rank(candidates: List<Track>, profile: UserProfile, limit: Int = 20): List<Track> {
        val seen = mutableSetOf<String>()
        val unique = candidates.filter { it.id.isNotBlank() && seen.add(it.id) }
        val scored = unique.map { ScoredTrack(it, score(it, profile)) }
        return diversify(scored, limit)
    }

    // ===== 3 + 6. CANDIDATES + HOME SCREEN =====
    suspend fun buildHome(): List<HomeSection> = withContext(Dispatchers.IO) {
        val profile = buildProfile()
        val sections = mutableListOf<HomeSection>()

        // Cold start (new user): no history yet -> show popular stuff
        if (profile.isCold) {
            val trending = safeCall { provider.trending(30) }
            val fresh = safeCall { provider.newReleases(30) }
            val result = mutableListOf<HomeSection>()
            if (trending.isNotEmpty()) {
                result.add(HomeSection(id = "trending", title = "Trending now", tracks = trending.take(20)))
            }
            if (fresh.isNotEmpty()) {
                result.add(HomeSection(id = "new", title = "New releases", tracks = fresh.take(20)))
            }
            return@withContext result
        }

        // Jump back in: recent unique tracks
        val recent = mutableListOf<Track>()
        val seenIds = mutableSetOf<String>()
        val eventsSnapshot = synchronized(events) { events.toList() }
        for (i in eventsSnapshot.indices.reversed()) {
            if (recent.size >= 10) break
            val e = eventsSnapshot[i]
            if (e.type == "skip" || seenIds.contains(e.track.id)) continue
            seenIds.add(e.track.id)
            recent.add(e.track)
        }
        if (recent.isNotEmpty()) {
            sections.add(HomeSection(id = "recent", title = "Jump back in", tracks = recent))
        }

        // Made for you: mix candidates from top artists + top genres + trending
        val fromArtists = profile.topArtists.map { id -> safeCall { provider.byArtist(id, 15) } }
        val fromGenres = profile.topGenres.map { g -> safeCall { provider.byGenre(g, 15) } }
        val trending = safeCall { provider.trending(20) }

        val allCandidates = fromArtists.flatten() + fromGenres.flatten() + trending
        val madeForYou = rank(allCandidates, profile, 20)
        if (madeForYou.isNotEmpty()) {
            sections.add(HomeSection(id = "foryou", title = "Made for you", tracks = madeForYou))
        }

        // Because you listened to <artist>
        val firstArtistId = profile.topArtists.firstOrNull()
        if (firstArtistId != null) {
            val topArtistName = nameOfArtist(eventsSnapshot, firstArtistId)
            val artistTracks = fromArtists.firstOrNull().orEmpty()
            if (!topArtistName.isNullOrBlank() && artistTracks.isNotEmpty()) {
                sections.add(
                    HomeSection(
                        id = "because",
                        title = "Because you listened to $topArtistName",
                        tracks = rank(artistTracks, profile, 15)
                    )
                )
            }
        }

        // One "mix" per top genre
        profile.topGenres.take(3).forEachIndexed { i, g ->
            val genreTracks = if (i < fromGenres.size) fromGenres[i] else emptyList()
            val ranked = rank(genreTracks, profile, 15)
            if (ranked.isNotEmpty()) {
                sections.add(HomeSection(id = "mix-$g", title = "${capitalize(g)} mix", tracks = ranked))
            }
        }

        // Discover: songs they haven't heard, outside their top artists
        val fresh = safeCall { provider.newReleases(30) }
        val discover = (fresh + trending).filter { t ->
            !profile.lastPlayed.containsKey(t.id) && !profile.topArtists.contains(t.resolvedArtistId)
        }
        val discoverRanked = rank(discover, profile, 15)
        if (discoverRanked.isNotEmpty()) {
            sections.add(HomeSection(id = "discover", title = "Discover something new", tracks = discoverRanked))
        }

        return@withContext sections.filter { it.tracks.isNotEmpty() }
    }

    /** "Radio" / autoplay: what to queue after the current song */
    suspend fun nextUp(currentTrack: Track, limit: Int = 10): List<Track> = withContext(Dispatchers.IO) {
        val profile = buildProfile()
        val artistCands = safeCall { provider.byArtist(currentTrack.resolvedArtistId, 20) }
        val genreCands = currentTrack.genres.take(2).flatMap { g ->
            safeCall { provider.byGenre(g, 15) }
        }
        val cands = (artistCands + genreCands).filter { it.id != currentTrack.id }
        return@withContext rank(cands, profile, limit)
    }

    private suspend fun <T> safeCall(block: suspend () -> List<T>): List<T> {
        return try {
            block()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
