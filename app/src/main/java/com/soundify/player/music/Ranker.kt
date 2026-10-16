package com.soundify.player.music

import com.soundify.player.model.Track
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class RankConfig(
    val wArtistLong: Double = 0.30,
    val wArtistShort: Double = 0.28,      // 4-day Heavy Rotation affinity
    val wArtistSession: Double = 0.42,    // Live RAM session affinity (reacts immediately to skips/likes)
    val wDaypart: Double = 0.14,          // Time-of-day artist & sonic fit
    val wTagLong: Double = 0.18,
    val wTagSession: Double = 0.25,
    val wSonicFlow: Double = 0.35,        // 4D SonicVector continuity with session & seed
    val wTransition: Double = 0.32,       // Markov transition probability P(candidate | seed)
    val wSeed: Double = 0.40,             // Seed similarity
    val wBanditTrust: Double = 0.38,      // Contextual Thompson-sampled Origin trust
    val wPopularity: Double = 0.06,
    val noveltyBonus: Double = 0.14,      // Unheard track boost
    val skipPenalty: Double = 0.55,       // Per-skip penalty
    val impressionPenalty: Double = 0.08, // Per-unclicked-impression fatigue
    val repeatCooldownMs: Long = 4 * 3_600_000L,
    val diversity: Double = 0.32,         // MMR diversity weight
    val maxPerArtist: Int = 2,
    val exploration: Double = 0.14,       // Base exploration rate (scales up if user is skipping!)
)

class Ranker(
    val cfg: RankConfig = RankConfig(),
    private val learner: Learner? = null,
    private val rng: Random = Random.Default,
) {
    private class Scored(
        val c: Candidate,
        val score: Double,
        val familiarity: Double,
        val uncertaintyBonus: Double,
    )

    fun select(
        cands: List<Candidate>,
        p: Profile,
        seed: Track?,
        limit: Int,
        exclude: Set<String> = emptySet(),
        sessionOverride: SessionView? = null,
        trustOverride: Map<Origin, Double>? = null,
    ): List<Candidate> {
        if (cands.isEmpty() || limit <= 0) return emptyList()

        val now = System.currentTimeMillis()
        val sv = sessionOverride ?: learner?.session?.view()
        val trust = trustOverride ?: learner?.sampleTrust() ?: defaultTrust()

        val seedId = seed?.let { TrackIdentity.of(it) }
        val seedTags = seed?.tags?.map { it.lowercase() }?.toSet().orEmpty()

        val pool = cands.asSequence()
            .filter { it.id.dedupeKey !in exclude && it.id.dedupeKey != seedId?.dedupeKey }
            .filter { it.id.artistKey !in p.blockedArtists && it.id.dedupeKey !in p.blockedKeys }
            .map { score(it, p, sv, trust, seed, seedId, seedTags, now) }
            .sortedByDescending { it.score }
            .toMutableList()

        val picked = ArrayList<Scored>()
        val perArtist = HashMap<String, Int>()

        // Dynamic Exploration: if the user skipped 2+ tracks in a row, boost exploration to pivot the vibe
        val activeExploration = if ((sv?.consecutiveSkips ?: 0) >= 2) {
            (cfg.exploration + 0.20).coerceAtMost(0.45)
        } else {
            cfg.exploration
        }

        while (picked.size < limit && pool.isNotEmpty()) {
            val prevTrack = picked.lastOrNull()?.c?.track ?: seed
            val explore = picked.isNotEmpty() && rng.nextDouble() < activeExploration
            val chosen = (if (explore) explorePick(pool, perArtist) else null)
                ?: mmrPick(pool, picked, perArtist, prevTrack)
                ?: break
            pool.remove(chosen)
            picked += chosen
            perArtist[chosen.c.id.artistKey] = (perArtist[chosen.c.id.artistKey] ?: 0) + 1
        }
        return picked.map { it.c }
    }

    private fun score(
        c: Candidate,
        p: Profile,
        sv: SessionView?,
        trust: Map<Origin, Double>,
        seed: Track?,
        seedId: Identity?,
        seedTags: Set<String>,
        now: Long,
    ): Scored {
        val a = c.id.artistKey
        val tags = c.track.tags.map { it.lowercase() }

        val aLong = p.artist[a] ?: 0.0
        val aShort = p.artistShortTerm[a] ?: 0.0
        val aSess = sv?.artistAffinity(a) ?: 0.0
        val tLong = mean(tags.map { p.tag[it] ?: 0.0 })
        val tSess = sv?.tagAffinity(tags) ?: 0.0

        // 1. Multi-Time-Scale Taste (Long-Term + 4-Day Heavy Rotation + Instant RAM Session)
        var s = cfg.wArtistLong * aLong +
            cfg.wArtistShort * aShort +
            cfg.wArtistSession * aSess +
            cfg.wDaypart * ((p.daypartArtist[a] ?: 0.0) * 0.7 + p.daypartSonic.similarity(c.track.sonic) * 0.3) +
            cfg.wTagLong * tLong +
            cfg.wTagSession * tSess

        // 2. Continuous 4D Sonic Vibe Flow
        val sessionSonicFit = sv?.sonicAlignment(c.track) ?: 0.5
        val seedSonicFit = seed?.sonic?.similarity(c.track.sonic) ?: sessionSonicFit
        s += cfg.wSonicFlow * (0.55 * sessionSonicFit + 0.45 * seedSonicFit)

        // 3. Seed & Sequential Markov Transition Boost (P(nextArtist | currentArtist))
        if (seedId != null) {
            val tagSim = jaccard(tags.toSet(), seedTags)
            var seedSim = if (a == seedId.artistKey) 0.85 else 0.65 * tagSim
            if (Origin.RELATED in c.origins) seedSim = min(1.0, seedSim + 0.35)
            s += cfg.wSeed * seedSim

            val histTrans = p.transitions[seedId.artistKey]?.get(a) ?: 0.0
            val sessTrans = sv?.transitionAffinity(seedId.artistKey, a) ?: 0.0
            s += cfg.wTransition * max(histTrans, sessTrans)
        }

        // 4. Contextual Thompson-Sampled Bandit Trust + Multi-Source Consensus Bonus
        val bestOriginTrust = c.origins.maxOfOrNull { trust[it] ?: 0.5 } ?: 0.5
        val multiSourceConsensus = 0.06 * (c.origins.size - 1)
        s += cfg.wBanditTrust * bestOriginTrust + multiSourceConsensus

        // 5. Popularity prior
        s += cfg.wPopularity * ((c.track.popularity ?: 50) / 100.0)

        // 6. Session Artist Spacing & Fatigue (Exponential cooldown if artist just played)
        val dist = sv?.distanceSinceArtist(a)
        if (dist != null) {
            // dist=0 means just played (-0.85 penalty), dist=1 (-0.45), dist=2 (-0.22)...
            s -= 0.85 * exp(-0.65 * dist.toDouble())
        }
        val sessionRepeatCount = sv?.recentCount(a) ?: 0
        if (sessionRepeatCount >= 2) {
            s -= 0.35 * (sessionRepeatCount - 1)
        }

        // 7. Track Cooldown, Skip Penalty, and Impression Fatigue
        val last = p.lastPlayed[c.id.dedupeKey]
        s += when {
            last == null -> cfg.noveltyBonus
            now - last < cfg.repeatCooldownMs -> -1.8 * exp(-(now - last).toDouble() / cfg.repeatCooldownMs)
            else -> -0.04
        }
        s -= cfg.skipPenalty * min(p.skipCount[c.id.dedupeKey] ?: 0, 3)
        s -= cfg.impressionPenalty * min(p.impressions[c.id.dedupeKey] ?: 0, 6)

        val familiarity = max(max(aLong, aShort), max(0.0, aSess))
        val uncertaintyBonus = bestOriginTrust * (1.0 - familiarity.coerceIn(0.0, 1.0))

        return Scored(c, s, familiarity, uncertaintyBonus)
    }

    /**
     * Sequence-Aware MMR:
     * Balances relevance, diversity across the whole batch, AND smooth sonic transition from `prevTrack`.
     */
    private fun mmrPick(
        pool: List<Scored>,
        picked: List<Scored>,
        perArtist: Map<String, Int>,
        prevTrack: Track?,
    ): Scored? {
        var best: Scored? = null
        var bestValue = -Double.MAX_VALUE
        val lastArtist = picked.lastOrNull()?.c?.id?.artistKey

        for (cand in pool.asSequence().take(60)) {
            val artist = cand.c.id.artistKey
            if ((perArtist[artist] ?: 0) >= cfg.maxPerArtist) continue

            val maxSim = picked.maxOfOrNull { sim(it.c, cand.c) } ?: 0.0
            val backToBackPenalty = if (artist == lastArtist) 0.35 else 0.0

            // Smooth track-to-track transition bonus (prevents jarring sonic jumps between adjacent songs)
            val transitionSmoothness = prevTrack?.sonic?.similarity(cand.c.track.sonic) ?: 0.5

            val value = (1.0 - cfg.diversity) * cand.score -
                cfg.diversity * maxSim +
                0.12 * transitionSmoothness -
                backToBackPenalty

            if (value > bestValue) {
                bestValue = value
                best = cand
            }
        }
        return best
    }

    /**
     * Uncertainty-Guided Exploration (UCB/Thompson hybrid):
     * Picks an unfamiliar artist that has high sonic/bandit promise rather than pure random noise.
     */
    private fun explorePick(pool: List<Scored>, perArtist: Map<String, Int>): Scored? =
        pool.asSequence()
            .filter { it.familiarity < 0.15 && (perArtist[it.c.id.artistKey] ?: 0) < cfg.maxPerArtist }
            .sortedByDescending { it.score + 0.35 * it.uncertaintyBonus }
            .take(8)
            .toList()
            .randomOrNull(rng)

    private fun sim(a: Candidate, b: Candidate): Double {
        val sameArtist = if (a.id.artistKey == b.id.artistKey) 0.65 else 0.0
        val tagsA = a.track.tags.map { it.lowercase() }.toSet()
        val tagsB = b.track.tags.map { it.lowercase() }.toSet()
        val sonicSim = a.track.sonic.similarity(b.track.sonic)
        return sameArtist + 0.25 * jaccard(tagsA, tagsB) + 0.10 * sonicSim
    }

    private fun defaultTrust(): Map<Origin, Double> = mapOf(
        Origin.RELATED to 0.72,
        Origin.TRANSITION to 0.70,
        Origin.SESSION_DRIFT to 0.68,
        Origin.HEAVY_ROTATION to 0.65,
        Origin.SAME_ARTIST to 0.58,
        Origin.TASTE_ARTIST to 0.56,
        Origin.TASTE_TAG to 0.52,
        Origin.NEW to 0.48,
        Origin.TRENDING to 0.42,
    )

    private fun jaccard(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val inter = a.count { it in b }
        return inter.toDouble() / (a.size + b.size - inter)
    }

    private fun mean(xs: List<Double>) = if (xs.isEmpty()) 0.0 else xs.sum() / xs.size
}
