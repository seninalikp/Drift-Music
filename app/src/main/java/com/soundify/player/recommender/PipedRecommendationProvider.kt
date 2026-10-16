package com.soundify.player.recommender

import com.soundify.player.api.PipedRepository
import com.soundify.player.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PipedRecommendationProvider : RecommendationProvider {

    override suspend fun byArtist(artistId: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val query = artistId.replace("-", " ").trim()
        val results = PipedRepository.searchTracks(query)
        if (results.isNotEmpty()) {
            results.take(limit)
        } else {
            emptyList()
        }
    }

    override suspend fun byGenre(genre: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val query = "$genre hits".trim()
        val results = PipedRepository.searchTracks(query)
        if (results.isNotEmpty()) {
            results.take(limit)
        } else {
            emptyList()
        }
    }

    override suspend fun trending(limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val results = PipedRepository.searchTracks("Top Global Hits 2024")
        if (results.isNotEmpty()) {
            results.take(limit)
        } else {
            PipedRepository.searchTracks("Trending Hits").take(limit)
        }
    }

    override suspend fun newReleases(limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val results = PipedRepository.searchTracks("New Music 2024")
        if (results.isNotEmpty()) {
            results.take(limit)
        } else {
            PipedRepository.searchTracks("Latest Songs").take(limit)
        }
    }
}
