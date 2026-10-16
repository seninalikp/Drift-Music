package com.soundify.player.queue

import android.util.Log
import com.soundify.player.api.PipedRepository
import com.soundify.player.model.Track
import com.soundify.player.recommender.RecommendationProvider
import com.soundify.player.ui.PlayEvent
import com.soundify.player.ui.Recommender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Implement this with your API calls.
 * related(): songs similar to the given one (same artist, similar artists, same genre...).
 *            If your API has no "related" endpoint, search by the artist name instead.
 * trending(): popular songs, used to add some variety.
 */
interface MusicSource {
    suspend fun related(track: Track, limit: Int): List<Track>
    suspend fun trending(limit: Int): List<Track>
}

class PipedMusicSource(
    private val provider: RecommendationProvider? = null
) : MusicSource {
    override suspend fun related(track: Track, limit: Int): List<Track> {
        if (provider != null) {
            val fromProvider = provider.byArtist(track.resolvedArtistId, limit)
            if (fromProvider.isNotEmpty()) return fromProvider
        }
        val query = track.artist.trim()
        val results = PipedRepository.searchTracks(query)
        return results.take(limit)
    }

    override suspend fun trending(limit: Int): List<Track> {
        if (provider != null) {
            val fromProvider = provider.trending(limit)
            if (fromProvider.isNotEmpty()) return fromProvider
        }
        val results = PipedRepository.searchTracks("Top Global Hits 2024")
        return results.take(limit)
    }
}

/**
 * Radio-style queue:
 *  - starts from the song the user tapped (NOT the raw search list)
 *  - removes remixes / live / sped-up copies of songs already played or queued
 *  - always keeps a few songs ready, refilling from whatever is playing NOW,
 *    so the vibe drifts naturally with the listener
 *  - learns from skips immediately
 */
class PlayQueue(
    private val source: MusicSource,
    private val history: () -> List<PlayEvent>,
    private val scope: CoroutineScope,
    private val refillBelow: Int = 4,
    private val batchSize: Int = 12,
) {
    private val _upcoming = MutableStateFlow<List<Track>>(emptyList())
    val upcoming: StateFlow<List<Track>> = _upcoming.asStateFlow()

    private val recent = ArrayDeque<Track>()
    private var current: Track? = null
    private var refilling = false
    private var generation = 0 // bumps on every start(); stale refills are ignored

    /** User tapped a song. Play it, then call next() when it ends. */
    fun start(seed: Track) {
        generation++
        refilling = false
        current = seed
        recent.clear()
        remember(seed)
        _upcoming.value = emptyList()
        refill(force = true)
    }

    /** Returns the next track to play (null if the queue is still loading). */
    fun next(): Track? {
        val nextTrack = _upcoming.value.firstOrNull() ?: return null
        _upcoming.update { it.drop(1) }
        current = nextTrack
        remember(nextTrack)
        refill()
        return nextTrack
    }

    /**
     * Use this instead of next() for the Next button. If the queue is still loading
     * (e.g. first song, rapid skipping) it waits up to 5s instead of returning null,
     * so you never fall back to the raw search list.
     */
    suspend fun awaitNext(): Track? {
        if (_upcoming.value.isEmpty()) refill(force = true)
        withTimeoutOrNull(5000) { _upcoming.first { it.isNotEmpty() } }
        return next()
    }

    /**
     * Call when the user skips early. Log the PlayEvent (completed = false) FIRST,
     * then call this, then call next().
     */
    fun onSkipped(skipped: Track) {
        val key = titleKey(skipped.title)
        _upcoming.update { list ->
            val kept = list.filter { titleKey(it.title) != key && it.artist != skipped.artist }
            Recommender.rank(kept, history(), kept.size, seed = current)
        }
        refill()
    }

    /** Optional: let the user add a song to play next. */
    fun playNext(track: Track) = _upcoming.update { listOf(track) + it }

    private fun remember(track: Track) {
        recent.addLast(track)
        while (recent.size > 30) recent.removeFirst()
    }

    private fun refill(force: Boolean = false) {
        if (refilling || (!force && _upcoming.value.size >= refillBelow)) return
        val seed = current ?: return
        refilling = true
        val gen = generation
        scope.launch {
            try {
                val fresh = build(seed)
                if (gen != generation) return@launch // user already started another song
                // queue may have changed while loading, so filter again
                val have = _upcoming.value.map { titleKey(it.title) }.toSet() +
                    recent.map { titleKey(it.title) }
                _upcoming.update { it + fresh.filter { t -> titleKey(t.title) !in have } }
            } finally {
                if (gen == generation) refilling = false
            }
        }
    }

    private suspend fun build(seed: Track): List<Track> {
        val candidates =
            runCatching { source.related(seed, 40) }.getOrDefault(emptyList()) +
            runCatching { source.trending(30) }.getOrDefault(emptyList())

        // block anything already played or queued, including other versions of the same title
        val blockedIds = recent.map { it.id }.toSet() + _upcoming.value.map { it.id }
        val blockedTitles = (recent.map { titleKey(it.title) } + _upcoming.value.map { titleKey(it.title) }).toSet()

        val seenTitles = HashSet<String>()
        val fresh = candidates.filter { t ->
            val key = titleKey(t.title)
            t.id !in blockedIds && key !in blockedTitles && seenTitles.add(key)
        }
        Log.d("PlayQueue", "seed='${seed.title}' candidates=${candidates.size} fresh=${fresh.size}")
        return Recommender.rank(fresh, history(), batchSize, seed = seed)
    }

    companion object {
        // words that usually mean "same song, different version"
        private val versionWords = Regex(
            """\b(remix|remastered|remaster|live|acoustic|sped up|slowed|reverb|instrumental|""" +
                """karaoke|version|edit|mix|radio edit|extended|lofi|lo-fi|cover|\d{4})\b"""
        )

        /** "Blinding Lights (Sped Up) - feat. X" and "Blinding Lights - Remastered" -> "blinding lights" */
        fun titleKey(title: String): String = title
            .lowercase()
            .replace(Regex("""[(\[].*?[)\]]"""), " ")
            .replace(Regex("""\s[-–—]\s.*$"""), " ")
            .replace(Regex("""\b(feat|ft)\.?\s.*$"""), " ")
            .replace(versionWords, " ")
            .replace(Regex("""[^a-z0-9\u0080-\uFFFF ]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
