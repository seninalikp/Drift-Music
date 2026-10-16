package com.soundify.player.api

import com.soundify.player.model.Track
import kotlin.math.abs

object TrackDeduplicator {

    // Strips YouTube noise like "(Official Audio)", "[Official Video]", "(Lyrics)", etc.
    private val noiseBracketsRegex = Regex(
        """(?i)\s*[\(\[]\s*(official\s*(music\s*)?(video|audio|lyric\s*video|visualizer)|lyrics?|audio|hd|hq|4k|explicit|clean|remastered(\s*\d{4})?|full\s*song|original\s*mix)\s*[\)\]]"""
    )

    // Strips trailing "- Topic", "VEVO", "Official" from uploader/artist names
    private val artistNoiseRegex = Regex("""(?i)(\s*-\s*topic|\s*vevo|\s*official\s*channel|\s*official)$""")

    // Preserves special edition tags in the fingerprint so actual remixes/slowed versions aren't merged with the original
    private val variantKeywords = listOf(
        "remix", "slowed", "reverb", "sped up", "nightcore", "acoustic", "live", "instrumental", "cover", "lofi", "lo-fi", "edit", "vip"
    )

    /**
     * Cleans raw YouTube/Piped title and artist so they look like clean Spotify/Apple Music metadata.
     */
    fun cleanTrackMetadata(rawTrack: Track): Track {
        var cleanedArtist = rawTrack.artist
            .replace(artistNoiseRegex, "")
            .trim()

        var cleanedTitle = rawTrack.title
            .replace(noiseBracketsRegex, "")
            .trim()

        // If title is formatted as "Artist - Song Title", strip the redundant artist prefix
        val prefixPattern = Regex("""^(?i)${Regex.escape(cleanedArtist)}\s*[-–—:]\s*(.+)$""")
        val match = prefixPattern.find(cleanedTitle)
        if (match != null && match.groupValues[1].isNotBlank()) {
            cleanedTitle = match.groupValues[1].trim()
        }

        return rawTrack.copy(
            title = cleanedTitle.ifBlank { rawTrack.title },
            artist = cleanedArtist.ifBlank { rawTrack.artist }
        )
    }

    /**
     * Generates a canonical key for a track based on normalized title + primary artist + variant type.
     */
    private fun canonicalFingerprint(track: Track): String {
        val rawLowerTitle = track.title.lowercase()

        // Detect if this track is a specific remix/slowed/live variant
        val detectedVariants = variantKeywords.filter { rawLowerTitle.contains(it) }.sorted().joinToString("_")

        // Normalize title: remove parentheticals, feat. credits, punctuation, and extra spaces
        val baseTitle = rawLowerTitle
            .replace(Regex("""[\(\[].*?[\)\]]"""), " ")
            .replace(Regex("""\b(feat\.?|ft\.?|featuring|with|prod\.?\s*by)\b.*$"""), " ")
            .replace(Regex("""[^a-z0-9\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        // Normalize primary artist (first artist before comma, &, x, or feat)
        val primaryArtist = track.artist.lowercase()
            .replace(artistNoiseRegex, "")
            .split(",", "&", " x ", " feat", " ft", " with")[0]
            .replace(Regex("""[^a-z0-9\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        return "$primaryArtist|$baseTitle|$detectedVariants"
    }

    /**
     * Scores two duplicate candidates so we always keep the highest-quality official track
     * (prefers tracks from "- Topic" official channels, with valid artwork and reasonable duration).
     */
    private fun qualityScore(rawTrack: Track): Int {
        var score = 0
        if (rawTrack.artist.contains("- Topic", ignoreCase = true)) score += 50
        if (!rawTrack.artworkUrl.isNullOrBlank()) score += 20
        if (rawTrack.durationMs in 60_000L..600_000L) score += 15
        if (!rawTrack.title.contains("video", ignoreCase = true)) score += 10
        return score
    }

    /**
     * Deduplicates a list of tracks by:
     * 1. Exact ID match
     * 2. Canonical (Artist + Normalized Title + Variant) fingerprint
     * 3. Title-only + similar duration (within 5 seconds) when one upload has the artist in the channel name and another doesn't
     */
    fun List<Track>.deduplicateTracks(): List<Track> {
        if (this.isEmpty()) return emptyList()

        // Sort by quality score descending first so the best version (e.g., Official Topic Audio) wins
        val sortedByQuality = this.mapIndexed { index, track ->
            Triple(index, track, qualityScore(track))
        }.sortedWith(compareByDescending<Triple<Int, Track, Int>> { it.third }.thenBy { it.first })

        val seenIds = HashSet<String>()
        val seenFingerprints = HashSet<String>()
        val acceptedTracks = mutableListOf<Pair<Int, Track>>()

        for ((originalIndex, rawTrack, _) in sortedByQuality) {
            if (!seenIds.add(rawTrack.id)) continue

            val cleaned = cleanTrackMetadata(rawTrack)
            val fingerprint = canonicalFingerprint(cleaned)

            if (!seenFingerprints.add(fingerprint)) continue

            // Secondary check: same normalized title + duration within 5 seconds (catches re-uploads on random channels)
            val normTitleOnly = fingerprint.split("|").getOrNull(1) ?: ""
            val normVariant = fingerprint.split("|").getOrNull(2) ?: ""
            val isDuplicateByTitleAndDuration = acceptedTracks.any { (_, existing) ->
                val existingParts = canonicalFingerprint(existing).split("|")
                val existingTitle = existingParts.getOrNull(1) ?: ""
                val existingVariant = existingParts.getOrNull(2) ?: ""
                normTitleOnly.length >= 4 &&
                    normTitleOnly == existingTitle &&
                    normVariant == existingVariant &&
                    (cleaned.durationMs == 0L || existing.durationMs == 0L ||
                        abs(cleaned.durationMs - existing.durationMs) <= 5_000L)
            }

            if (isDuplicateByTitleAndDuration) continue

            acceptedTracks.add(originalIndex to cleaned)
        }

        // Restore original search relevance order
        return acceptedTracks
            .sortedBy { it.first }
            .map { it.second }
    }
}
