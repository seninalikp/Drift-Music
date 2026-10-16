package com.soundify.player.music

import com.soundify.player.model.Track
import kotlinx.coroutines.*

/**
 * Candidate Sources (Arms for the Contextual Bandit):
 * - RELATED: Acoustic/metadata neighbors of the seed song
 * - TRANSITION: Sequential Markov neighbors (artists that historically follow the current artist)
 * - SESSION_DRIFT: Artists & tags heating up right now in the live SessionView
 * - HEAVY_ROTATION: 4-day short-term high-affinity artists
 * - SAME_ARTIST: Deep cuts from the seed artist
 * - TASTE_ARTIST: Long-term favorite artists
 * - TASTE_TAG: Long-term favorite genres/tags
 * - TRENDING / NEW: Popularity & freshness exploration pools
 */
enum class Origin {
    RELATED,
    TRANSITION,
    SESSION_DRIFT,
    HEAVY_ROTATION,
    SAME_ARTIST,
    TASTE_ARTIST,
    TASTE_TAG,
    TRENDING,
    NEW
}

data class Candidate(val track: Track, val id: Identity, val origins: Set<Origin>)

internal suspend fun <T> safeList(block: suspend () -> List<T>): List<T> = try {
    withTimeout(4_000) { block() }
} catch (e: TimeoutCancellationException) {
    emptyList()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    emptyList()
}

class CandidateGenerator(private val source: MusicSource) {

    /**
     * Multi-tower candidate retrieval for Radio / Autoplay:
     * Pulls from Seed Related, Markov Transition Graph, Live Session Drift, Heavy Rotation, and Long-Term Taste.
     */
    suspend fun forSeed(seed: Track, p: Profile, sv: SessionView): List<Candidate> = coroutineScope {
        val seedId = TrackIdentity.of(seed)
        val jobs = ArrayList<Deferred<Pair<Origin, List<Track>>>>()

        // 1. Seed neighbors & same artist
        jobs += async { Origin.RELATED to safeList { source.related(seed, 35) } }
        jobs += async { Origin.SAME_ARTIST to safeList { source.byArtist(seedId.artist, 20) } }

        // 2. Sequential Markov Transitions (Artists that historically follow seedId.artistKey)
        val nextArtists = p.transitions[seedId.artistKey]
            ?.entries?.sortedByDescending { it.value }?.take(2)?.map { it.key }.orEmpty()
        for (key in nextArtists) {
            val name = p.artistNames[key] ?: continue
            jobs += async { Origin.TRANSITION to safeList { source.byArtist(name, 15) } }
        }

        // 3. Real-time Session Drift (Hot tags & hot artists from the last 5 songs)
        sv.hotTags(2).forEach { hotTag ->
            jobs += async { Origin.SESSION_DRIFT to safeList { source.byTag(hotTag, 18) } }
        }
        sv.hotArtists(1).forEach { hotArtistKey ->
            val name = p.artistNames[hotArtistKey]
            if (name != null && hotArtistKey != seedId.artistKey) {
                jobs += async { Origin.SESSION_DRIFT to safeList { source.byArtist(name, 15) } }
            }
        }

        // 4. Short-term Heavy Rotation & Long-term Taste
        p.heavyRotationArtists.take(2).forEach { key ->
            val name = p.artistNames[key] ?: return@forEach
            jobs += async { Origin.HEAVY_ROTATION to safeList { source.byArtist(name, 15) } }
        }
        p.topArtists.filter { it !in p.heavyRotationArtists }.take(2).forEach { key ->
            val name = p.artistNames[key] ?: return@forEach
            jobs += async { Origin.TASTE_ARTIST to safeList { source.byArtist(name, 12) } }
        }
        p.topTags.take(2).forEach { tag ->
            jobs += async { Origin.TASTE_TAG to safeList { source.byTag(tag, 12) } }
        }

        // 5. Exploration / Trending
        jobs += async { Origin.TRENDING to safeList { source.trending(15) } }

        combine(jobs.awaitAll(), p)
    }

    /** For Home Screen & Cold-Seed Fallback: blends Heavy Rotation, Session Drift, and Long-Term Taste. */
    suspend fun forTaste(p: Profile, sv: SessionView? = null, includeNew: Boolean = true): List<Candidate> = coroutineScope {
        val jobs = ArrayList<Deferred<Pair<Origin, List<Track>>>>()

        p.heavyRotationArtists.take(3).forEach { key ->
            val name = p.artistNames[key] ?: return@forEach
            jobs += async { Origin.HEAVY_ROTATION to safeList { source.byArtist(name, 15) } }
        }
        p.topArtists.take(4).forEach { key ->
            val name = p.artistNames[key] ?: return@forEach
            jobs += async { Origin.TASTE_ARTIST to safeList { source.byArtist(name, 15) } }
        }
        sv?.hotTags(2)?.forEach { tag ->
            jobs += async { Origin.SESSION_DRIFT to safeList { source.byTag(tag, 15) } }
        }
        p.topTags.take(3).forEach { tag ->
            jobs += async { Origin.TASTE_TAG to safeList { source.byTag(tag, 15) } }
        }
        jobs += async { Origin.TRENDING to safeList { source.trending(25) } }
        if (includeNew) jobs += async { Origin.NEW to safeList { source.newReleases(25) } }

        combine(jobs.awaitAll(), p)
    }

    fun combine(batches: List<Pair<Origin, List<Track>>>, p: Profile): List<Candidate> {
        val out = LinkedHashMap<String, Candidate>()
        for ((origin, tracks) in batches) for (t in tracks) {
            if (t.durationMs != 0L && t.durationMs !in 60_000L..(15 * 60_000L)) continue
            val id = TrackIdentity.of(t)
            if (id.artistKey in p.blockedArtists || id.dedupeKey in p.blockedKeys) continue
            val prev = out[id.dedupeKey]
            out[id.dedupeKey] =
                if (prev == null) Candidate(t, id, setOf(origin))
                else prev.copy(origins = prev.origins + origin)
        }
        return out.values.toList()
    }
}
