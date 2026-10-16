package com.soundify.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundify.player.model.Track
import com.soundify.player.music.Shelf
import com.soundify.player.music.SmartArtwork

@Composable
fun CleanHomeScreen(
    shelves: List<Shelf>, // Comes directly from HomeBuilder.build() in HomeBuilder.kt
    onTrackClick: (Track) -> Unit,
    fetchRealCover: suspend (String) -> String? = { null },
    modifier: Modifier = Modifier
) {
    val colors = DriftTheme.colors
    val jumpShelf = shelves.firstOrNull { it.id == "jump" }
    val carouselShelves = shelves.filter { it.id != "jump" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        // 1. Compact 2-Column Quick Grid ("Jump back in") right at the top — zero header clutter!
        if (jumpShelf != null && jumpShelf.tracks.isNotEmpty()) {
            item(key = "quick_grid") {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    val rows = jumpShelf.tracks.take(6).chunked(2)
                    rows.forEach { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { track ->
                                QuickJumpCard(
                                    track = track,
                                    fetchRealCover = fetchRealCover,
                                    onClick = { onTrackClick(track) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 2. Horizontal Shelves (Made for you, Because you listened to..., Discover)
        items(carouselShelves, key = { it.id }) { shelf ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = shelf.title,
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(shelf.tracks, key = { "${shelf.id}_${it.id}" }) { track ->
                        Column(
                            modifier = Modifier
                                .width(142.dp)
                                .clickable { onTrackClick(track) }
                        ) {
                            SmartArtwork(
                                track = track,
                                sizeDp = 142.dp,
                                cornerRadius = 10.dp,
                                fetchRealCover = fetchRealCover
                            )
                            Spacer(Modifier.height(8.dp))
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
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickJumpCard(
    track: Track,
    fetchRealCover: suspend (String) -> String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DriftTheme.colors

    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.card)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SmartArtwork(
            track = track,
            sizeDp = 56.dp,
            cornerRadius = 0.dp,
            fetchRealCover = fetchRealCover
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = track.title,
            color = colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}
