package com.soundify.player.music

import com.soundify.player.model.Track
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.Calendar

data class Shelf(val id: String, val title: String, val tracks: List<Track>, val score: Double = 0.0)

/**
 * Spotify BaRT-style Two-Level Home Builder:
 * Level 1: Generates personalized candidate shelves in parallel (Heavy Rotation, Made For You,
 *          Right Now Sonic Flow, Because You Listened, Daypart Mix, Genre Mixes, Discovery).
 * Level 2: Uses [Learner.shelves] Thompson-sampling bandit to dynamically order shelves based on
 *          which shelves the user actually taps vs. ignores.
 */
class HomeBuilder(
    private val source: MusicSource,
    private val repo: Repo,
    private val learner: Learner = Learner(),
    private val ranker: Ranker = Ranker(learner = learner),
) {
    private val gen = CandidateGenerator(source)

    suspend fun build(): List<Shelf> = coroutineScope {
        val logs = repo.logs(1500)
        val p = Taste.build(logs, repo.blocked())
        val sv = learner.session.view()
        val trust = learner.sampleTrust()

        // ---------- Cold start user ----------
        if (p.isCold) {
            val trending = async { gen.combine(listOf(Origin.TRENDING to safeList { source.trending(30) }), p) }
            val fresh = async { gen.combine(listOf(Origin.NEW to safeList { source.newReleases(30) }), p) }
            return@coroutineScope listOf(
                Shelf("trending", "Trending now", trending.await().map { it.track }.take(20)),
                Shelf("new", "New releases", fresh.await().map { it.track }.take(20)),
            ).filter { it.tracks.isNotEmpty() }
        }

        // ---------- Parallel candidate fetches ----------
        val tasteJob = async { gen.forTaste(p, sv) }

        val topKey = p.heavyRotationArtists.firstOrNull() ?: p.topArtists.firstOrNull()
        val topName = topKey?.let { p.artistNames[it] }
        val anchor = logs.firstOrNull { it.artistKey == topKey && Taste.weight(it) > 0.3 }?.toTrack()

        val becauseJob: Deferred<List<Candidate>>? = topName?.let { name ->
            async {
                val related = if (anchor != null) safeList { source.related(anchor, 25) } else emptyList()
                gen.combine(
                    listOf(
                        Origin.RELATED to related,
                        Origin.SAME_ARTIST to safeList { source.byArtist(name, 20) },
                    ),
                    p,
                )
            }
        }

        val mixes = p.topTags.take(2).map { tag ->
            tag to async { gen.combine(listOf(Origin.TASTE_TAG to safeList { source.byTag(tag, 25) }), p) }
        }

        // ---------- Assemble shelves ----------
        val pinnedTop = ArrayList<Shelf>()
        val dynamicShelves = ArrayList<Shelf>()
        val used = HashSet<String>()

        fun buildShelf(
            id: String,
            title: String,
            r: Ranker,
            cands: List<Candidate>,
            seed: Track?,
            n: Int,
            pinToTop: Boolean = false
        ) {
            val picked = r.select(cands, p, seed, n, exclude = used, sessionOverride = sv, trustOverride = trust)
            if (picked.size >= 4) {
                used += picked.map { it.id.dedupeKey }
                val shelfScore = learner.shelves.sampleContextual(id, Daypart.current().name)
                learner.onShelfImpression(id)
                val s = Shelf(id, title, picked.map { it.track }, shelfScore)
                if (pinToTop) pinnedTop += s else dynamicShelves += s
            }
        }

        // 1. "Jump back in" (Pinned top shelf if available)
        val jump = logs.asSequence()
            .filter { it.action == Action.PLAY.name && Taste.weight(it) > 0.25 }
            .distinctBy { it.dedupeKey }
            .take(10)
            .map { it.toTrack() }
            .toList()
        if (jump.isNotEmpty()) {
            pinnedTop += Shelf("jump", "Jump back in", jump, score = 1.0)
        }

        val tasteCands = tasteJob.await()

        // 2. "Made for you" (Core daily mix)
        buildShelf("foryou", "Made for you", ranker, tasteCands, sv.lastPositive, 20, pinToTop = true)

        // 3. "On Repeat / Heavy Rotation" (Short-term 4-day favorites)
        if (p.heavyRotationArtists.isNotEmpty()) {
            val heavyRanker = Ranker(ranker.cfg.copy(wArtistShort = 0.65, noveltyBonus = -0.1, exploration = 0.0), learner)
            buildShelf("heavy_rotation", "On Repeat", heavyRanker, tasteCands, null, 15)
        }

        // 4. "Because you listened to <Artist>"
        becauseJob?.let {
            buildShelf("because", "Because you listened to $topName", ranker, it.await(), anchor, 15)
        }

        // 5. Time-of-day contextual shelf
        if (p.daypartArtist.isNotEmpty()) {
            val dayRanker = Ranker(ranker.cfg.copy(wDaypart = 0.65, wSonicFlow = 0.45, wArtistLong = 0.20), learner)
            buildShelf("daypart", daypartTitle(), dayRanker, tasteCands, null, 15)
        }

        // 6. Genre / Tag mixes
        for ((tag, job) in mixes) {
            buildShelf("mix-$tag", "${tag.replaceFirstChar { it.uppercase() }} mix", ranker, job.await(), null, 15)
        }

        // 7. "Discover something new" (Unfamiliar artists with high sonic & bandit alignment)
        val discoverRanker = Ranker(
            ranker.cfg.copy(
                noveltyBonus = 0.45,
                wArtistLong = 0.02,
                wArtistShort = 0.0,
                wArtistSession = 0.05,
                wSonicFlow = 0.45,
                exploration = 0.25
            ),
            learner
        )
        val unfamiliar = tasteCands.filter { (p.artist[it.id.artistKey] ?: 0.0) < 0.08 }
        buildShelf("discover", "Discover something new", discoverRanker, unfamiliar, sv.lastPositive, 15)

        // Sort dynamic shelves by their Contextual Thompson-sampled Bandit score
        pinnedTop + dynamicShelves.sortedByDescending { it.score }
    }

    private fun daypartTitle(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Morning flow"
        in 12..16 -> "Afternoon focus"
        in 17..21 -> "Evening wind-down"
        else -> "Late night vibes"
    }
}
