package com.soundify.player.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class AudioFeatures(
    val energy: Float = 0.5f,
    val valence: Float = 0.5f,
    val danceability: Float = 0.5f,
    val tempo: Float = 0.5f
)

@Immutable
@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String = "",
    val durationSec: Int = 0,
    val streamUrl: String = "",
    val isLocalFallback: Boolean = false,
    val artistId: String = "",
    val genres: List<String> = emptyList(),
    val popularity: Int = 50,
    val features: AudioFeatures? = null,
    val durationMs: Long = (durationSec.toLong() * 1000L),
    val releaseYear: Int? = null
) {
    val resolvedArtistId: String
        get() = if (artistId.isNotBlank()) artistId else artist.trim().lowercase()

    val tags: List<String>
        get() = genres

    val sonic: com.soundify.player.music.SonicVector
        get() = com.soundify.player.music.SonicVector.infer(title, tags, durationMs)
}

@Immutable
@Serializable
data class PlaylistModel(
    val id: String,
    val name: String,
    val description: String = "",
    val coverUrl: String = "",
    val tracks: List<Track> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
