package com.soundify.player.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.soundify.player.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object ArtworkCache {
    private val map = ConcurrentHashMap<String, String>()

    suspend fun resolve(
        track: Track,
        playlistCoverUrl: String? = null,
        fetcher: suspend (String) -> String? // e.g., { query -> source.searchTrackArtwork(query) }
    ): String? {
        val raw = track.artworkUrl
        // Valid if non-empty and NOT identical to the parent playlist cover
        if (!raw.isNullOrBlank() && raw != playlistCoverUrl) return raw

        val key = "${track.title}_${track.artist}".lowercase()
        map[key]?.let { return it }

        return withContext(Dispatchers.IO) {
            runCatching { fetcher("${track.title} ${track.artist}") }
                .getOrNull()
                ?.also { if (it.isNotBlank()) map[key] = it }
        }
    }
}

@Composable
fun SmartArtwork(
    track: Track,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 48.dp,
    cornerRadius: Dp = 6.dp,
    playlistCoverUrl: String? = null,
    fetchRealCover: suspend (String) -> String? = { null }
) {
    val context = LocalContext.current
    var resolvedUrl by remember(track.id, track.artworkUrl) {
        val initial = track.artworkUrl?.takeIf { it.isNotBlank() && it != playlistCoverUrl }
        mutableStateOf(initial)
    }
    var loadFailed by remember(track.id) { mutableStateOf(false) }

    LaunchedEffect(track.id, loadFailed) {
        if (resolvedUrl == null || loadFailed) {
            val fresh = ArtworkCache.resolve(track, playlistCoverUrl, fetchRealCover)
            if (fresh != null && fresh != resolvedUrl) {
                resolvedUrl = fresh
                loadFailed = false
            }
        }
    }

    if (resolvedUrl == null || loadFailed) {
        Box(
            modifier = modifier
                .size(sizeDp)
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color(0xFF1E1E1E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(sizeDp * 0.45f)
            )
        }
    } else {
        val px = (sizeDp.value * context.resources.displayMetrics.density).toInt().coerceAtLeast(120)
        val request = remember(resolvedUrl, px) {
            ImageRequest.Builder(context)
                .data(resolvedUrl)
                .size(px, px) // Downsample to exact view size -> kills scroll lag!
                .crossfade(150)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        }

        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onError = { loadFailed = true },
            modifier = modifier
                .size(sizeDp)
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color(0xFF1E1E1E))
        )
    }
}
