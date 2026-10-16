package com.soundify.player.music

import android.content.SharedPreferences
import com.soundify.player.model.Track
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Next-Gen On-Device Learning Layer:
 * 1. Graded Implicit + Scrub/Replay Feedback (Spotify 30s stream threshold + completion curve + replay boost).
 * 2. Real-Time Sonic Vibe Drift & Negative Skip Repulsion (tracks 4D SonicVector + velocity in RAM).
 * 3. Contextual Thompson-Sampling Bandits (BaRT: Bandits conditioned on Daypart context + Impression decay).
 */

interface LearnStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
}

class MemoryLearnStore : LearnStore {
    private val map = HashMap<String, String>()
    @Synchronized override fun get(key: String): String? = map[key]
    @Synchronized override fun put(key: String, value: String) { map[key] = value }
}

class PrefsLearnStore(private val prefs: SharedPreferences) : LearnStore {
    override fun get(key: String): String? = prefs.getString(key, null)
    override fun put(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}

// ============================ 1. Graded Implicit Feedback ============================

object Feedback {
    const val HARD_SKIP_MS = 5_000L
    const val STREAM_MS = 30_000L
    private const val FALLBACK_DURATION_MS = 210_000L

    /**
     * Returns -1.0 (instant bail) .. 0.0 (neutral) .. +1.0 (full completion / repeat listen).
     * Uses actual wall-clock listenedMs, not seek bar position.
     */
    fun signal(listenedMs: Long, durationMs: Long): Double {
        val dur = if (durationMs > 10_000L) durationMs else FALLBACK_DURATION_MS
        val ratio = (listenedMs.toDouble() / dur).coerceIn(0.0, 1.5)
        return when {
            listenedMs < HARD_SKIP_MS -> -1.0
            listenedMs < STREAM_MS && ratio < 0.45 ->
                -0.75 + 0.50 * ((listenedMs - HARD_SKIP_MS).toDouble() / (STREAM_MS - HARD_SKIP_MS))
            ratio >= 1.15 -> 1.0 // User looped or re-listened to parts of the song
            else -> (-0.10 + 1.10 * ratio.coerceIn(0.0, 1.0).pow(1.35)).coerceIn(-1.0, 1.0)
        }
    }

    fun isSkip(signal: Double) = signal <= -0.25
    fun isHardSkip(signal: Double) = signal <= -0.65
}

// ============================ 2. Real-Time Session & Sonic Drift ============================

class SessionView(
    private val artist: Map<String, Double>,
    private val tag: Map<String, Double>,
    private val transitions: Map<String, Map<String, Double>>,
    private val recentArtists: List<String>,
    val sonicTarget: SonicVector,
    val consecutiveSkips: Int,
    val lastPositive: Track?,
    val plays: Int,
) {
    fun artistAffinity(key: String): Double = tanh(artist[key] ?: 0.0)

    fun tagAffinity(tags: List<String>): Double =
        if (tags.isEmpty()) 0.0 else tanh(tags.sumOf { tag[it.lowercase()] ?: 0.0 } / sqrt(tags.size.toDouble()))

    /** Sequential transition boost: did this artist follow `seedArtistKey` in this session? */
    fun transitionAffinity(fromArtistKey: String, toArtistKey: String): Double =
        tanh(transitions[fromArtistKey]?.get(toArtistKey) ?: 0.0)

    /** How closely a candidate's sonic profile matches the current session flow. */
    fun sonicAlignment(track: Track): Double = sonicTarget.similarity(track.sonic)

    /** Exponential artist spacing: 0 plays recently = 0, 1 play = 1, etc. */
    fun recentCount(key: String): Int = recentArtists.count { it == key }

    /** How many songs ago was this artist played? (0 = just played, null = not in recent 10). */
    fun distanceSinceArtist(key: String): Int? {
        val idx = recentArtists.lastIndexOf(key)
        return if (idx < 0) null else (recentArtists.size - 1 - idx)
    }

    fun hotTags(n: Int): List<String> =
        tag.entries.filter { it.value > 0.20 }.sortedByDescending { it.value }.take(n).map { it.key }

    fun hotArtists(n: Int): List<String> =
        artist.entries.filter { it.value > 0.25 }.sortedByDescending { it.value }.take(n).map { it.key }
}

/**
 * Signed, exponentially-decaying RAM state of THIS listening session.
 * Includes a 4D SonicVector Kalman-style tracker that drifts toward completed songs
 * and actively repels away from hard-skipped songs.
 */
class SessionModel(private val decayPerTrack: Double = 0.85) {
    private val artist = HashMap<String, Double>()
    private val tag = HashMap<String, Double>()
    private val transitions = HashMap<String, HashMap<String, Double>>()
    private val recent = ArrayDeque<String>()
    private var sonicTarget = SonicVector()
    private var consecutiveSkips = 0
    private var lastPositive: Track? = null
    private var lastPositiveArtistKey: String? = null
    private var plays = 0

    @Synchronized
    fun record(track: Track, artistKey: String, signal: Double, weight: Double = 1.0) {
        fade(decayPerTrack)
        val s = (signal * weight).coerceIn(-2.0, 2.0)

        artist[artistKey] = ((artist[artistKey] ?: 0.0) + s).coerceIn(-2.0, 2.0)

        val cleanTags = track.tags.map { it.lowercase() }.distinct().take(8)
        if (cleanTags.isNotEmpty()) {
            val share = s / sqrt(cleanTags.size.toDouble())
            for (t in cleanTags) {
                tag[t] = ((tag[t] ?: 0.0) + share).coerceIn(-2.0, 2.0)
            }
        }

        // Update 4D Sonic Vibe Vector: attract on positive listen, repel on hard skip
        if (signal >= 0.25) {
            val step = (0.30 * signal).coerceIn(0.10, 0.45)
            sonicTarget = sonicTarget.nudge(track.sonic, step)
            consecutiveSkips = 0

            val prevKey = lastPositiveArtistKey
            if (prevKey != null && prevKey != artistKey) {
                val row = transitions.getOrPut(prevKey) { HashMap() }
                row[artistKey] = ((row[artistKey] ?: 0.0) + signal).coerceIn(0.0, 3.0)
            }
            lastPositive = track
            lastPositiveArtistKey = artistKey
            recent.addLast(artistKey)
            while (recent.size > 10) recent.removeFirst()
        } else if (Feedback.isSkip(signal)) {
            consecutiveSkips++
            // Repel sonic target away from the skipped song so next pick pivots the mood
            val repelRate = if (Feedback.isHardSkip(signal)) -0.22 else -0.10
            sonicTarget = sonicTarget.nudge(track.sonic, repelRate)
        }
        plays++
    }

    @Synchronized
    fun softReset(seed: Track, keep: Double = 0.45) {
        fade(keep)
        sonicTarget = sonicTarget.nudge(seed.sonic, 0.65)
        consecutiveSkips = 0
        lastPositive = seed
        lastPositiveArtistKey = TrackIdentity.of(seed).artistKey
    }

    @Synchronized
    fun view(): SessionView = SessionView(
        artist = HashMap(artist),
        tag = HashMap(tag),
        transitions = transitions.mapValues { HashMap(it.value) },
        recentArtists = recent.toList(),
        sonicTarget = sonicTarget,
        consecutiveSkips = consecutiveSkips,
        lastPositive = lastPositive,
        plays = plays
    )

    private fun fade(f: Double) {
        for (m in listOf(artist, tag)) {
            val it = m.entries.iterator()
            while (it.hasNext()) {
                val e = it.next()
                e.setValue(e.value * f)
                if (abs(e.value) < 0.03) it.remove()
            }
        }
    }
}

// ============================ 3. Contextual Thompson-Sampling Bandits (BaRT) ============================

internal fun Random.gaussian(): Double {
    var u: Double
    do { u = nextDouble() } while (u <= 1e-12)
    return sqrt(-2.0 * ln(u)) * cos(2.0 * PI * nextDouble())
}

internal fun Random.gamma(shape: Double): Double {
    if (shape < 1.0) {
        var u: Double
        do { u = nextDouble() } while (u <= 1e-12)
        return gamma(shape + 1.0) * u.pow(1.0 / shape)
    }
    val d = shape - 1.0 / 3.0
    val c = 1.0 / sqrt(9.0 * d)
    while (true) {
        val x = gaussian()
        val t = 1.0 + c * x
        if (t <= 0.0) continue
        val v = t * t * t
        val u = nextDouble()
        if (u < 1.0 - 0.0331 * x * x * x * x || ln(u) < 0.5 * x * x + d * (1.0 - v + ln(v))) return d * v
    }
}

internal fun Random.beta(a: Double, b: Double): Double {
    val x = gamma(a.coerceAtLeast(0.1))
    val y = gamma(b.coerceAtLeast(0.1))
    return if (x + y <= 0.0) 0.5 else x / (x + y)
}

class Bandit(
    private val name: String,
    private val store: LearnStore,
    private val rng: Random = Random.Default,
    private val priorA: Double = 3.0,
    private val priorB: Double = 3.0,
    private val window: Double = 80.0,
) {
    private class Arm(var a: Double, var b: Double)
    private val arms = HashMap<String, Arm>()

    init { load() }

    /**
     * Contextual Thompson sampling: blends global belief for `key` with context-specific
     * belief `contextKey::key` (e.g., Daypart.NIGHT::Origin.TASTE_TAG).
     */
    @Synchronized
    fun sampleContextual(key: String, context: String? = null): Double {
        val globalDraw = arm(key).let { rng.beta(it.a, it.b) }
        if (context.isNullOrBlank()) return globalDraw
        val ctxArm = arms[clean("$context::$key")] ?: return globalDraw
        val ctxConfidence = ((ctxArm.a + ctxArm.b - (priorA + priorB)) / 20.0).coerceIn(0.0, 0.7)
        val ctxDraw = rng.beta(ctxArm.a, ctxArm.b)
        return (1.0 - ctxConfidence) * globalDraw + ctxConfidence * ctxDraw
    }

    @Synchronized
    fun sample(key: String): Double = sampleContextual(key, null)

    @Synchronized
    fun mean(key: String): Double = arm(key).let { it.a / (it.a + it.b) }

    fun update(key: String, reward: Double, weight: Double = 1.0, context: String? = null) {
        val r = reward.coerceIn(0.0, 1.0)
        observe(key, weight * r, weight * (1.0 - r))
        if (!context.isNullOrBlank()) {
            observe("$context::$key", weight * r, weight * (1.0 - r))
        }
    }

    @Synchronized
    fun observe(key: String, success: Double, failure: Double) {
        val x = arm(key)
        x.a += success.coerceAtLeast(0.0)
        x.b += failure.coerceAtLeast(0.0)
        val n = x.a + x.b
        if (n > window) {
            val s = window / n
            x.a *= s
            x.b *= s
        }
        save()
    }

    private fun arm(key: String): Arm = arms.getOrPut(clean(key)) { Arm(priorA, priorB) }
    private fun clean(k: String) = k.replace('\t', ' ').replace('\n', ' ')

    private fun load() {
        val raw = store.get("bandit:$name") ?: return
        for (line in raw.lineSequence()) {
            val p = line.split('\t')
            if (p.size != 3) continue
            val a = p[1].toDoubleOrNull() ?: continue
            val b = p[2].toDoubleOrNull() ?: continue
            arms[p[0]] = Arm(a, b)
        }
    }

    private fun save() {
        if (arms.size > 250) {
            val keep = arms.entries.sortedByDescending { it.value.a + it.value.b }.take(180).map { it.key }.toSet()
            arms.keys.retainAll(keep)
        }
        store.put("bandit:$name", arms.entries.joinToString("\n") { "${it.key}\t${it.value.a}\t${it.value.b}" })
    }
}

/** Central learning coordinator shared across QueueEngine, Ranker, and HomeBuilder. */
class Learner(store: LearnStore = MemoryLearnStore(), rng: Random = Random.Default) {
    val session = SessionModel()
    val sources = Bandit("sources", store, rng, priorA = 3.5, priorB = 2.5, window = 90.0)
    val shelves = Bandit("shelves", store, rng, priorA = 2.5, priorB = 10.0, window = 140.0)

    /** Contextual Thompson draw per candidate Origin for the current Daypart. */
    fun sampleTrust(daypart: Daypart = Daypart.current()): Map<Origin, Double> =
        Origin.entries.associateWith { sources.sampleContextual(it.name, daypart.name) }

    /** Called when a track finishes or is skipped. Updates RAM session + contextual source bandits. */
    fun onFinished(track: Track, artistKey: String, origins: Set<Origin>, listenedMs: Long): Double {
        val s = Feedback.signal(listenedMs, track.durationMs)
        session.record(track, artistKey, s)
        creditOrigins(origins, s)
        return s
    }

    /** Called on explicit Like (+1), Search Pick (+0.85), or Dislike (-1). */
    fun onExplicit(track: Track, artistKey: String, origins: Set<Origin>, signal: Double, weight: Double = 1.6) {
        session.record(track, artistKey, signal, weight)
        creditOrigins(origins, signal)
    }

    /** Called when a Home shelf is rendered vs. tapped by the user. */
    fun onShelfImpression(shelfId: String) {
        shelves.observe(shelfId, success = 0.0, failure = 0.15)
    }

    fun onShelfClick(shelfId: String) {
        shelves.observe(shelfId, success = 1.0, failure = 0.0)
    }

    private fun creditOrigins(origins: Set<Origin>, signal: Double) {
        if (origins.isEmpty()) return
        val reward = ((signal + 1.0) / 2.0).coerceIn(0.0, 1.0)
        val ctx = Daypart.current().name
        val share = 1.0 / origins.size
        for (o in origins) {
            sources.update(o.name, reward, share, ctx)
        }
    }
}
