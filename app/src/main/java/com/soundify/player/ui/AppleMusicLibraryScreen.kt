package com.soundify.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MicExternalOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundify.player.model.Track
import com.soundify.player.music.SmartArtwork

enum class LibrarySection(val title: String, val icon: ImageVector) {
    PLAYLISTS("Playlists", Icons.AutoMirrored.Filled.QueueMusic),
    FAVORITES("Favorite Songs", Icons.Default.FavoriteBorder),
    ARTISTS("Artists", Icons.Default.MicExternalOn),
    RECENT_PLAYS("History", Icons.Default.History)
}

@Composable
fun AppleMusicLibraryScreen(
    recentlyAddedTracks: List<Track>,
    onCategoryClick: (LibrarySection) -> Unit,
    onTrackClick: (Track) -> Unit,
    fetchRealCover: suspend (String) -> String? = { null },
    modifier: Modifier = Modifier
) {
    val colors = DriftTheme.colors
    val dividerColor = if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Large Apple Music Header
        item(span = { GridItemSpan(2) }) {
            Text(
                text = "Library",
                color = colors.textPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // 2. Category Menu Rows (Playlists, Favorite Songs, Artists, History)
        item(span = { GridItemSpan(2) }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                LibrarySection.entries.forEach { section ->
                    AppleLibraryCategoryRow(
                        title = section.title,
                        icon = section.icon,
                        onClick = { onCategoryClick(section) }
                    )
                    HorizontalDivider(color = dividerColor, thickness = 0.7.dp)
                }
            }
        }

        // 3. "Recently Added" Section Title
        if (recentlyAddedTracks.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Recently Added",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 4. 2-Column Cover Art Grid
            items(
                items = recentlyAddedTracks,
                key = { "lib_grid_${it.id}" }
            ) { track ->
                AppleGridCard(
                    track = track,
                    fetchRealCover = fetchRealCover,
                    onClick = { onTrackClick(track) }
                )
            }
        }
    }
}

@Composable
private fun AppleLibraryCategoryRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colors = DriftTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = colors.accent,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun AppleGridCard(
    track: Track,
    fetchRealCover: suspend (String) -> String?,
    onClick: () -> Unit
) {
    val colors = DriftTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        SmartArtwork(
            track = track,
            sizeDp = 165.dp,
            cornerRadius = 10.dp,
            fetchRealCover = fetchRealCover,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title,
            color = colors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            color = colors.textSecondary,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
