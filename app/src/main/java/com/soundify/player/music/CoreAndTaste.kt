package com.soundify.player.music

import androidx.room.*
import com.soundify.player.api.PipedRepository
import com.soundify.player.model.Track
import java.util.Calendar
import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

enum class Action { PLAY, SEARCH_PICK, LIKE, DISLIKE, IMPRESSION }

/**
 * 4D Continuous Sonic Vibe Vector (0.0 .. 1.0)
 * Models Spotify-style Audio Features on-device from metadata, tags, title cues, and duration.
 */
data class SonicVector(
    val energy: Double = 0.5,
    val valence: Double = 0.5,
    val warmth: Double = 0.5,
    val danceability: Double = 0.5,
) {
    fun similarity(other: SonicVector): Double {
        val de = energy - other.energy
        val dv = valence - other.valence
        val dw = warmth - other.warmth
        val dd = danceability - other.danceability
        val dist = sqrt(de * de + dv * dv + dw * dw + dd * dd) / 2.0 // normalized 0..1
        return (1.0 - dist).coerceIn(0.0, 1.0)
    }

    fun nudge(target: SonicVector, rate: Double): SonicVector = SonicVector(
        energy = (energy + rate * (target.energy - energy)).coerceIn(0.0, 1.0),
        valence = (valence + rate * (target.valence - valence)).coerceIn(0.0, 1.0),
        warmth = (warmth + rate * (target.warmth - warmth)).coerceIn(0.0, 1.0),
        danceability = (danceability + rate * (target.danceability - danceability)).coerceIn(0.0, 1.0),
    )

    companion object {
        private val HIGH_ENERGY = setOf("edm", "rock", "metal", "trap", "phonk", "workout", "gym", "hype", "techno", "dnb", "punk", "hardstyle", "club", "bass")
        private val LOW_ENERGY = setOf("ambient", "sleep", "chill", "lofi", "lo-fi", "acoustic", "piano", "study", "relax", "meditation", "ballad", "soft")
        private val HIGH_VALENCE = setOf("happy", "summer", "pop", "funk", "disco", "upbeat", "party", "tropical", "indie pop", "feel good", "dance")
        private val LOW_VALENCE = setOf("sad", "dark", "melancholy", "emo", "heartbreak", "slowed", "reverb", "doomer", "night", "rain", "blues")
        private val WARM_ORGANIC = setOf("acoustic", "folk", "jazz", "soul", "r&b", "indie", "classical", "piano", "guitar", "live", "unplugged", "vinyl")
        private val DANCEABLE = setOf("dance", "house", "hip hop", "hip-hop", "rap", "reggaeton", "afrobeats", "disco", "funk", "pop", "edm", "club", "techno")

        fun infer(title: String, tags: List<String>, durationMs: Long): SonicVector {
            val tokens = (tags.map { it.lowercase(Locale.ROOT) } +
                title.lowercase(Locale.ROOT).split(" ", "-", "(", ")", "[", "]")).toSet()

            var e = 0.52
            var v = 0.50
            var w = 0.45
            var d = 0.50

            for (t in tokens) {
                if (t in HIGH_ENERGY) e += 0.16
                if (t in LOW_ENERGY) e -= 0.18
                if (t in HIGH_VALENCE) v += 0.15
                if (t in LOW_VALENCE) v -= 0.16
                if (t in WARM_ORGANIC) w += 0.18
                if (t in DANCEABLE) d += 0.16
            }

            if (title.contains("slowed", ignoreCase = true) || title.contains("acoustic", ignoreCase = true)) {
                e -= 0.15; w += 0.15
            }
            if (title.contains("remix", ignoreCase = true) || title.contains("vip", ignoreCase = true)) {
                e += 0.12; d += 0.12
            }
            if (durationMs > 330_000L) { // > 5.5 mins tends to be progressive/ambient/extended
                d -= 0.06
            }

            return SonicVector(
                energy = e.coerceIn(0.05, 0.95),
                valence = v.coerceIn(0.05, 0.95),
                warmth = w.coerceIn(0.05, 0.95),
                danceability = d.coerceIn(0.05, 0.95)
            )
        }
    }
}

data class Identity(
    val dedupeKey: String,
    val artistKey: String,
    val artist: String,
    val cleanTitle: String,
)

object TrackIdentity {
    private val NOISE_REGEX = Regex("\\b(official|video|audio|lyrics|lyric|hd|hq|4k|visualizer|remastered|remaster|version|explicit|clean|mv)\\b", RegexOption.IGNORE_CASE)
    private val BRACKETS_REGEX = Regex("[(\\[][^)\\]]*(official|video|audio|lyric|visual|hd|4k)[^)\\]]*[)\\]]", RegexOption.IGNORE_CASE)
    private val FEAT_REGEX = Regex("\\b(feat\\.?|ft\\.?|featuring)\\b.*$", RegexOption.IGNORE_CASE)

    fun of(track: Track): Identity {
        val primaryArtist = track.artist
            .split(",", "&", " x ", " X ", " feat", " ft")
            .firstOrNull()?.trim().orEmpty().ifEmpty { track.artist.trim() }
        val artistKey = normalize(primaryArtist)
        val cleanTitle = normalize(
            track.title
                .replace(BRACKETS_REGEX, " ")
                .replace(FEAT_REGEX, " ")
                .replace(NOISE_REGEX, " ")
        )
        return Identity(
            dedupeKey = "$artistKey::$cleanTitle",
            artistKey = artistKey,
            artist = primaryArtist,
            cleanTitle = cleanTitle
        )
    }

    fun normalize(raw: String): String =
        raw.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}

enum class Daypart { MORNING, AFTERNOON, EVENING, NIGHT;
    companion object {
        fun current(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): Daypart = when (hour) {
            in 5..11 -> MORNING
            in 12..16 -> AFTERNOON
            in 17..21 -> EVENING
            else -> NIGHT
        }
    }
}

data class Profile(
    val artist: Map<String, Double> = emptyMap(),
    val artistShortTerm: Map<String, Double> = emptyMap(), // 7-day fast-moving heavy rotation
    val artistNames: Map<String, String> = emptyMap(),
    val tag: Map<String, Double> = emptyMap(),
    val daypartArtist: Map<String, Double> = emptyMap(),
    val daypartSonic: SonicVector = SonicVector(),
    val transitions: Map<String, Map<String, Double>> = emptyMap(), // prevArtistKey -> (nextArtistKey -> weight)
    val lastPlayed: Map<String, Long> = emptyMap(),
    val playCounts: Map<String, Int> = emptyMap(),
    val skipCount: Map<String, Int> = emptyMap(),
    val impressions: Map<String, Int> = emptyMap(), // tracks shown or queued without positive engagement
    val blockedArtists: Set<String> = emptySet(),
    val blockedKeys: Set<String> = emptySet(),
    val topArtists: List<String> = emptyList(),
    val heavyRotationArtists: List<String> = emptyList(),
    val topTags: List<String> = emptyList(),
    val isCold: Boolean = true,
)

object Taste {
    private const val HALF_LIFE_LONG_MS = 28L * 24 * 3600_000L   // 28 days
    private const val HALF_LIFE_SHORT_MS = 4L * 24 * 3600_000L   // 4 days (Heavy Rotation)

    fun weight(log: EventLog): Double = when (log.action) {
        Action.LIKE.name -> 1.6
        Action.SEARCH_PICK.name -> 1.25
        Action.DISLIKE.name -> -2.0
        Action.IMPRESSION.name -> -0.05
        Action.PLAY.name -> Feedback.signal(log.listenedMs, log.durationMs)
        else -> 0.0
    }

    fun build(logs: List<EventLog>, blocked: List<BlockedItem>, now: Long = System.currentTimeMillis()): Profile {
        val artistLong = HashMap<String, Double>()
        val artistShort = HashMap<String, Double>()
        val artistNames = HashMap<String, String>()
        val tagScores = HashMap<String, Double>()
        val daypartArtist = HashMap<String, Double>()
        val transitions = HashMap<String, HashMap<String, Double>>()
        val lastPlayed = HashMap<String, Long>()
        val playCounts = HashMap<String, Int>()
        val skipCounts = HashMap<String, Int>()
        val impressions = HashMap<String, Int>()

        val currentBucket = Daypart.current()
        var daySonicSum = SonicVector(0.5, 0.5, 0.5, 0.5)
        var daySonicWeight = 1.0

        val sortedChronological = logs.sortedBy { it.timestamp }
        var prevPositiveArtist: String? = null
        var prevPositiveTime = 0L

        for (log in sortedChronological) {
            val ageMs = max(0L, now - log.timestamp)
            val decayLong = exp(-ln(2.0) * ageMs / HALF_LIFE_LONG_MS)
            val decayShort = exp(-ln(2.0) * ageMs / HALF_LIFE_SHORT_MS)
            val w = weight(log)

            artistNames[log.artistKey] = log.artist

            if (log.action == Action.IMPRESSION.name) {
                impressions[log.dedupeKey] = (impressions[log.dedupeKey] ?: 0) + 1
                continue
            }

            if (log.action == Action.PLAY.name) {
                if (w > 0.15) {
                    lastPlayed[log.dedupeKey] = log.timestamp
                    playCounts[log.dedupeKey] = (playCounts[log.dedupeKey] ?: 0) + 1
                    impressions.remove(log.dedupeKey)
                } else if (Feedback.isSkip(w)) {
                    skipCounts[log.dedupeKey] = (skipCounts[log.dedupeKey] ?: 0) + 1
                } else {
                    // Reset consecutive skips if user listened moderately
                    skipCounts[log.dedupeKey] = max(0, (skipCounts[log.dedupeKey] ?: 0) - 1)
                }
            }

            artistLong[log.artistKey] = (artistLong[log.artistKey] ?: 0.0) + w * decayLong
            artistShort[log.artistKey] = (artistShort[log.artistKey] ?: 0.0) + w * decayShort

            val tags = log.tagsCsv.split(",").map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotEmpty() }
            for (t in tags) {
                tagScores[t] = (tagScores[t] ?: 0.0) + (w * decayLong) / sqrt(tags.size.toDouble().coerceAtLeast(1.0))
            }

            val cal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
            val logDaypart = Daypart.current(cal.get(Calendar.HOUR_OF_DAY))
            if (logDaypart == currentBucket) {
                daypartArtist[log.artistKey] = (daypartArtist[log.artistKey] ?: 0.0) + w * decayLong
                if (w > 0.2) {
                    val sv = SonicVector.infer(log.title, tags, log.durationMs)
                    daySonicSum = daySonicSum.nudge(sv, (w * decayShort).coerceIn(0.05, 0.35))
                    daySonicWeight += w * decayShort
                }
            }

            // Build Sequential Markov Transition Graph (Artist A -> Artist B within 20 mins)
            if (w >= 0.35) {
                val prev = prevPositiveArtist
                if (prev != null && prev != log.artistKey && (log.timestamp - prevPositiveTime) in 1_000L..(20 * 60_000L)) {
                    val row = transitions.getOrPut(prev) { HashMap() }
                    row[log.artistKey] = (row[log.artistKey] ?: 0.0) + w * decayLong
                }
                prevPositiveArtist = log.artistKey
                prevPositiveTime = log.timestamp
            }
        }

        fun normalizeMap(m: Map<String, Double>): Map<String, Double> {
            val maxAbs = m.values.maxOfOrNull { kotlin.math.abs(it) }?.takeIf { it > 1e-6 } ?: return emptyMap()
            return m.mapValues { (_, v) -> (v / maxAbs).coerceIn(-1.0, 1.0) }
        }

        val normLong = normalizeMap(artistLong)
        val normShort = normalizeMap(artistShort)
        val normTag = normalizeMap(tagScores)
        val normDay = normalizeMap(daypartArtist)

        val normTransitions = transitions.mapValues { (_, row) ->
            val total = row.values.sum().takeIf { it > 0.0 } ?: 1.0
            row.mapValues { (_, v) -> (v / total).coerceIn(0.0, 1.0) }
        }

        val blockedArtists = blocked.filter { it.isArtist }.map { it.key }.toSet()
        val blockedKeys = blocked.filter { !it.isArtist }.map { it.key }.toSet()

        val topArtists = normLong.entries
            .filter { it.value > 0.08 && it.key !in blockedArtists }
            .sortedByDescending { it.value }
            .map { it.key }

        val heavyRotation = normShort.entries
            .filter { it.value > 0.12 && it.key !in blockedArtists }
            .sortedByDescending { it.value }
            .map { it.key }

        val topTags = normTag.entries
            .filter { it.value > 0.08 }
            .sortedByDescending { it.value }
            .map { it.key }

        return Profile(
            artist = normLong,
            artistShortTerm = normShort,
            artistNames = artistNames,
            tag = normTag,
            daypartArtist = normDay,
            daypartSonic = daySonicSum,
            transitions = normTransitions,
            lastPlayed = lastPlayed,
            playCounts = playCounts,
            skipCount = skipCounts,
            impressions = impressions,
            blockedArtists = blockedArtists,
            blockedKeys = blockedKeys,
            topArtists = topArtists,
            heavyRotationArtists = heavyRotation,
            topTags = topTags,
            isCold = logs.count { it.action == Action.PLAY.name && weight(it) > 0 } < 3
        )
    }
}

// ======================= Persistence & Source Interfaces =======================

@Entity(tableName = "event_logs")
data class EventLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val dedupeKey: String,
    val title: String,
    val artist: String,
    val artistKey: String,
    val artworkUrl: String?,
    val durationMs: Long,
    val tagsCsv: String,
    val action: String,
    val listenedMs: Long,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toTrack() = Track(
        id = trackId,
        title = title,
        artist = artist,
        artworkUrl = artworkUrl ?: "",
        durationMs = durationMs,
        durationSec = (durationMs / 1000L).toInt(),
        genres = if (tagsCsv.isBlank()) emptyList() else tagsCsv.split(",")
    )
}

@Entity(tableName = "blocked_items")
data class BlockedItem(
    @PrimaryKey val key: String,
    val isArtist: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface MusicDao {
    @Query("SELECT * FROM event_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun logs(limit: Int): List<EventLog>

    @Insert
    suspend fun insertLog(log: EventLog)

    @Query("SELECT * FROM blocked_items")
    suspend fun blocked(): List<BlockedItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocked(item: BlockedItem)
}

@Database(entities = [EventLog::class, BlockedItem::class], version = 2, exportSchema = false)
abstract class MusicDb : RoomDatabase() {
    abstract fun dao(): MusicDao
}

class Repo(private val dao: MusicDao) {
    suspend fun logs(limit: Int = 1500): List<EventLog> = dao.logs(limit)
    suspend fun blocked(): List<BlockedItem> = dao.blocked()

    suspend fun log(track: Track, action: Action, listenedMs: Long = 0L) {
        val id = TrackIdentity.of(track)
        dao.insertLog(
            EventLog(
                trackId = track.id,
                dedupeKey = id.dedupeKey,
                title = track.title,
                artist = id.artist,
                artistKey = id.artistKey,
                artworkUrl = track.artworkUrl,
                durationMs = track.durationMs,
                tagsCsv = track.tags.joinToString(","),
                action = action.name,
                listenedMs = listenedMs
            )
        )
    }

    suspend fun blockTrack(track: Track) {
        dao.insertBlocked(BlockedItem(TrackIdentity.of(track).dedupeKey, isArtist = false))
    }

    suspend fun blockArtist(track: Track) {
        dao.insertBlocked(BlockedItem(TrackIdentity.of(track).artistKey, isArtist = true))
    }
}

interface MusicSource {
    suspend fun related(seed: Track, limit: Int): List<Track>
    suspend fun byArtist(artist: String, limit: Int): List<Track>
    suspend fun byTag(tag: String, limit: Int): List<Track>
    suspend fun trending(limit: Int): List<Track>
    suspend fun newReleases(limit: Int): List<Track>
}

fun interface StreamResolver {
    suspend fun resolve(track: Track): String?
}

class PipedMusicSource : MusicSource {
    private val currentYear: Int
        get() = Calendar.getInstance().get(Calendar.YEAR)

    override suspend fun related(seed: Track, limit: Int): List<Track> {
        val query = seed.artist.trim()
        val results = PipedRepository.searchTracks(query)
        return results.take(limit)
    }

    override suspend fun byArtist(artist: String, limit: Int): List<Track> {
        val results = PipedRepository.searchTracks(artist)
        return results.take(limit)
    }

    override suspend fun byTag(tag: String, limit: Int): List<Track> {
        val results = PipedRepository.searchTracks("$tag hits")
        return results.take(limit)
    }

    override suspend fun trending(limit: Int): List<Track> {
        val results = PipedRepository.searchTracks("Top Global Hits $currentYear")
        return results.take(limit)
    }

    override suspend fun newReleases(limit: Int): List<Track> {
        val results = PipedRepository.searchTracks("New Music $currentYear")
        return results.take(limit)
    }
}
