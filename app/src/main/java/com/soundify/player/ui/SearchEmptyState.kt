package com.soundify.player.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.soundify.player.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.pow

/* ------------------------------------------------------------------ */
/*  Models                                                            */
/* ------------------------------------------------------------------ */

data class PlayEvent(
    val track: Track,
    val completed: Boolean,    // true = listened to the end, false = skipped early
    val timestamp: Long = System.currentTimeMillis(),
)

/* ------------------------------------------------------------------ */
/*  UI Components                                                     */
/* ------------------------------------------------------------------ */



@Composable
fun SearchEmptyState(
    recentSearches: List<String>,
    recommendations: List<Track>,
    onRecentClick: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearRecents: () -> Unit,
    onTrackClick: (Track) -> Unit,
    onTrackOptionsClick: ((Track) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (recentSearches.isNotEmpty()) {
            item(key = "recent_header") {
                SectionHeader("Recent searches", actionLabel = "Clear", onAction = onClearRecents)
            }
            items(recentSearches, key = { "recent_$it" }) { query ->
                RecentRow(query, onClick = { onRecentClick(query) }, onRemove = { onRemoveRecent(query) })
            }
            item(key = "recent_gap") { Spacer(Modifier.height(24.dp)) }
        }

        if (recommendations.isNotEmpty()) {
            item(key = "rec_header") { SectionHeader("Recommended for you") }
            items(recommendations, key = { "rec_${it.id}" }) { track ->
                TrackRow(
                    track = track,
                    onClick = { onTrackClick(track) },
                    onOptionsClick = onTrackOptionsClick?.let { { it(track) } }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val colors = DriftTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (actionLabel != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(actionLabel, color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun RecentRow(query: String, onClick: () -> Unit, onRemove: () -> Unit) {
    val colors = DriftTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.History, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            query,
            color = colors.textPrimary,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TrackRow(track: Track, onClick: () -> Unit, onOptionsClick: (() -> Unit)? = null) {
    val colors = DriftTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SafeTrackArtwork(
            artworkUrl = track.artworkUrl,
            modifier = Modifier.size(48.dp),
            contentDescription = track.title,
            shape = RoundedCornerShape(6.dp),
            iconSize = 22.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = colors.textPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = colors.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onOptionsClick != null) {
            IconButton(onClick = onOptionsClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Search History Management                                         */
/* ------------------------------------------------------------------ */

class SearchHistory(context: Context, private val max: Int = 10) {
    private val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<String>> = _items.asStateFlow()

    fun add(query: String) {
        val q = query.trim().replace("\n", " ")
        if (q.isEmpty()) return
        update { listOf(q) + it.filterNot { s -> s.equals(q, ignoreCase = true) } }
    }

    fun remove(query: String) = update { it - query }
    fun clear() = update { emptyList() }

    private fun update(transform: (List<String>) -> List<String>) {
        val next = transform(_items.value).take(max)
        _items.value = next
        prefs.edit().putString("items", next.joinToString("\n")).apply()
    }

    private fun load(): List<String> =
        prefs.getString("items", "").orEmpty().split("\n").filter { it.isNotBlank() }
}

/* ------------------------------------------------------------------ */
/*  Lightweight Kotlin Recommender Engine                             */
/* ------------------------------------------------------------------ */

object Recommender {
    private const val HALF_LIFE_MS = 30L * 24 * 3600 * 1000

    fun rank(
        candidates: List<Track>,
        plays: List<PlayEvent>,
        limit: Int = 10,
        seed: Track? = null,
    ): List<Track> {
        val unique = candidates.distinctBy { it.id }
        if (plays.isEmpty() && seed == null) return unique.take(limit)

        val now = System.currentTimeMillis()
        val artistScore = HashMap<String, Double>()
        for (p in plays) {
            val decay = 0.5.pow((now - p.timestamp).toDouble() / HALF_LIFE_MS)
            val weight = (if (p.completed) 1.0 else -0.5) * decay
            artistScore.merge(p.track.artist, weight, Double::plus)
        }
        val maxAbs = artistScore.values.maxOfOrNull { abs(it) }?.takeIf { it > 0 } ?: 1.0
        val playedIds = plays.map { p -> p.track.id }.toSet()

        val ranked = unique
            .map { t ->
                var s = (artistScore[t.artist] ?: 0.0) / maxAbs
                if (t.id !in playedIds) s += 0.15
                if (seed != null && t.artist == seed.artist) s += 0.25
                t to s
            }
            .sortedByDescending { it.second }
            .map { it.first }

        val perArtist = HashMap<String, Int>()
        return ranked.filter { t -> perArtist.merge(t.artist, 1, Int::plus)!! <= 2 }.take(limit)
    }
}
