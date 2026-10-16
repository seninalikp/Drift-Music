package com.soundify.player.music

import android.util.Log
import com.soundify.player.model.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap

/**
 * The "what plays next" brain, now wired directly to [Learner]:
 * - Tracks exact [Origin] provenance for every queued track so Thompson-sampling bandits learn which source worked.
 * - Updates [Learner.session] in RAM on every finish/skip/like so the very next track reacts in <1ms.
 * - On early skip, repels the session's SonicVector away from the skipped song and re-ranks/refills the queue.
 */
class QueueEngine(
    source: MusicSource,
    private val repo: Repo,
    private val scope: CoroutineScope,
    val learner: Learner = Learner(),
    private val ranker: Ranker = Ranker(learner = learner),
) {
    private val generator = CandidateGenerator(source)

    private val _upcoming = MutableStateFlow<List<Track>>(emptyList())
    val upcoming: StateFlow<List<Track>> = _upcoming.asStateFlow()

    private val _current = MutableStateFlow<Track?>(null)
    val current: StateFlow<Track?> = _current.asStateFlow()

    private val played = ArrayDeque<Track>()
    private val originMap = ConcurrentHashMap<String, Set<Origin>>() // dedupeKey -> Origins that suggested it
    private var refillJob: Job? = null
    private var cachedProfile: Profile? = null
    private var lastProfileBuildMs = 0L

    // ---------------- profile ----------------

    suspend fun profile(): Profile {
        val now = System.currentTimeMillis()
        val cur = cachedProfile
        if (cur != null && (now - lastProfileBuildMs) < 90_000L) return cur
        return Taste.build(repo.logs(), repo.blocked(), now).also {
            cachedProfile = it
            lastProfileBuildMs = now
        }
    }

    private fun invalidateProfile() {
        cachedProfile = null
    }

    // ---------------- control ----------------

    /** User explicitly started a song from Search, Home, or Library. */
    fun start(seed: Track, shelfId: String? = null) {
        refillJob?.cancel()
        if (shelfId != null) learner.onShelfClick(shelfId)
        learner.session.softReset(seed)
        played.clear()
        val id = TrackIdentity.of(seed)
        originMap[id.dedupeKey] = setOf(Origin.RELATED)
        _current.value = seed
        _upcoming.value = emptyList()
        refill(force = true)
    }

    fun startPlaylist(tracks: List<Track>, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        refillJob?.cancel()
        played.clear()
        val ordered = if (shuffle) tracks.shuffled() else tracks
        val first = ordered.first()
        learner.session.softReset(first)
        val id = TrackIdentity.of(first)
        originMap[id.dedupeKey] = setOf(Origin.RELATED)
        _current.value = first
        _upcoming.value = ordered.drop(1)
        if (_upcoming.value.size < LOW_WATER) refill(force = true)
    }

    fun advance(): Track? {
        val next = _upcoming.value.firstOrNull() ?: return null
        _upcoming.update { it.drop(1) }
        _current.value?.let {
            played.addLast(it)
            while (played.size > 100) played.removeFirst()
        }
        _current.value = next
        refill()
        return next
    }

    suspend fun awaitNext(): Track? {
        if (_upcoming.value.isEmpty()) refill(force = true)
        withTimeoutOrNull(6_000) { _upcoming.first { it.isNotEmpty() } }
        return advance()
    }

    fun previous(): Track? {
        val prev = played.removeLastOrNull() ?: return null
        _current.value?.let { cur -> _upcoming.update { listOf(cur) + it } }
        _current.value = prev
        return prev
    }

    fun playNext(track: Track) = _upcoming.update { listOf(track) + it }
    fun enqueue(track: Track) = _upcoming.update { it + track }
    fun removeAt(index: Int) = _upcoming.update { l -> l.filterIndexed { i, _ -> i != index } }

    // ---------------- feedback ----------------

    /**
     * Called when a track finishes or is skipped.
     * 1. Updates [Learner] in RAM immediately (session affinities, sonic vector drift, Thompson bandit rewards).
     * 2. Persists the event log to Room.
     * 3. If skipped early, prunes similar tracks and pivots the upcoming queue.
     */
    suspend fun reportFinished(track: Track, listenedMs: Long) {
        val id = TrackIdentity.of(track)
        val origins = originMap[id.dedupeKey] ?: setOf(Origin.RELATED)
        val signal = learner.onFinished(track, id.artistKey, origins, listenedMs)

        repo.log(track, Action.PLAY, listenedMs)
        if (Feedback.isSkip(signal)) {
            pruneAndPivotAfterSkip(track, hardSkip = Feedback.isHardSkip(signal))
        } else if (signal >= 0.5) {
            // Positive completion: refresh cached profile periodically
            invalidateProfile()
        }
    }

    suspend fun logAction(track: Track, action: Action) {
        val id = TrackIdentity.of(track)
        val origins = originMap[id.dedupeKey] ?: emptySet()
        when (action) {
            Action.LIKE -> learner.onExplicit(track, id.artistKey, origins, signal = 1.0, weight = 1.6)
            Action.SEARCH_PICK -> learner.onExplicit(track, id.artistKey, origins, signal = 0.85, weight = 1.3)
            Action.DISLIKE -> learner.onExplicit(track, id.artistKey, origins, signal = -1.0, weight = 2.0)
            else -> {}
        }
        repo.log(track, action)
        invalidateProfile()
    }

    suspend fun like(track: Track) = logAction(track, Action.LIKE)

    suspend fun dislike(track: Track, blockArtist: Boolean = false) {
        logAction(track, Action.DISLIKE)
        if (blockArtist) repo.blockArtist(track) else repo.blockTrack(track)
        val id = TrackIdentity.of(track)
        _upcoming.update { list ->
            list.filter {
                val i = TrackIdentity.of(it)
                i.dedupeKey != id.dedupeKey && !(blockArtist && i.artistKey == id.artistKey)
            }
        }
        refill(force = true)
    }

    // ---------------- internals ----------------

    /**
     * User bailed early:
     * - Drops same artist (and on hard skip, drops tracks with very high sonic/tag similarity to the skipped song).
     * - Re-ranks the survivors using the newly repelled SessionView.
     * - If the user skipped 2+ times in a row, forces a fresh candidate fetch anchored on their last positive track.
     */
    private suspend fun pruneAndPivotAfterSkip(skipped: Track, hardSkip: Boolean) {
        val skippedId = TrackIdentity.of(skipped)
        val sv = learner.session.view()
        val p = profile()

        // Log an impression penalty for the skipped track
        repo.log(skipped, Action.IMPRESSION)

        if (sv.consecutiveSkips >= 2) {
            // User is unhappy with this radio direction -> pivot queue using their last liked/completed track
            _upcoming.value = emptyList()
            refill(force = true)
            return
        }

        val kept = _upcoming.value.filter { candTrack ->
            val i = TrackIdentity.of(candTrack)
            val sameArtist = i.artistKey == skippedId.artistKey
            val tooSimilarOnHardSkip = hardSkip && candTrack.sonic.similarity(skipped.sonic) > 0.88
            i.dedupeKey != skippedId.dedupeKey && !sameArtist && !tooSimilarOnHardSkip
        }

        val cands = kept.map {
            val id = TrackIdentity.of(it)
            Candidate(it, id, originMap[id.dedupeKey] ?: setOf(Origin.RELATED))
        }
        val anchor = sv.lastPositive ?: _current.value
        _upcoming.value = ranker.select(cands, p, anchor, kept.size, sessionOverride = sv)
            .map { it.track }
        refill()
    }

    private fun refill(force: Boolean = false) {
        if (!force && (_upcoming.value.size >= LOW_WATER || refillJob?.isActive == true)) return
        val sv = learner.session.view()
        // If user is on a skip streak, anchor refill on the last track they actually enjoyed
        val seed = (if (sv.consecutiveSkips >= 2) sv.lastPositive else null) ?: _current.value ?: return

        refillJob?.cancel()
        refillJob = scope.launch {
            val batch = withContext(Dispatchers.Default) {
                val p = profile()
                val trust = learner.sampleTrust()
                var cands = generator.forSeed(seed, p, sv)

                ensureActive()
                val exclude = HashSet<String>().apply {
                    played.takeLast(45).forEach { add(TrackIdentity.of(it).dedupeKey) }
                    _upcoming.value.forEach { add(TrackIdentity.of(it).dedupeKey) }
                    _current.value?.let { add(TrackIdentity.of(it).dedupeKey) }
                    add(TrackIdentity.of(seed).dedupeKey)
                }

                var selected = ranker.select(cands, p, seed, BATCH, exclude, sessionOverride = sv, trustOverride = trust)

                if (selected.isEmpty() && _upcoming.value.isEmpty()) {
                    cands = generator.forTaste(p, sv)
                    ensureActive()
                    selected = ranker.select(cands, p, null, BATCH, exclude, sessionOverride = sv, trustOverride = trust)
                }
                selected
            }

            for (c in batch) {
                originMap[c.id.dedupeKey] = c.origins
            }

            Log.d(TAG, "seed='${seed.title}' cands=${batch.size} picked=${batch.size} skips=${sv.consecutiveSkips}")
            _upcoming.update { it + batch.map { c -> c.track } }
        }
    }

    private companion object {
        const val TAG = "QueueEngine"
        const val LOW_WATER = 4
        const val BATCH = 12
    }
}
