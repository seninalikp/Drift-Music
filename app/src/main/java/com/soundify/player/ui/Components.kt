package com.soundify.player.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.soundify.player.R
import com.soundify.player.model.Track

fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${if (seconds < 10) "0" else ""}$seconds"
}

@Composable
fun SafeTrackArtwork(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    shape: Shape = RoundedCornerShape(6.dp),
    iconSize: Dp = 24.dp
) {
    val isPlaceholder = artworkUrl.isNullOrBlank() ||
        artworkUrl == "playlist_cover_placeholder" ||
        artworkUrl == "default_fallback_url"

    var hasError by remember(artworkUrl) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF222226)),
        contentAlignment = Alignment.Center
    ) {
        if (isPlaceholder || hasError) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = contentDescription,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(iconSize)
            )
        } else {
            val context = LocalContext.current
            val imageRequest = remember(artworkUrl) {
                ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .crossfade(150)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                onError = { hasError = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun DriftAppLogo(modifier: Modifier = Modifier.size(28.dp)) {
    Image(
        painter = painterResource(R.drawable.ic_drift_logo),
        contentDescription = "Drift Logo",
        modifier = modifier.clip(CircleShape)
    )
}

@Composable
fun TrackListItem(
    track: Track,
    isPlaying: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    isLiked: Boolean,
    onOptionsClick: (() -> Unit)? = null
) {
    val colors = DriftTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) colors.card else colors.surface)
            .border(
                BorderStroke(
                    1.dp,
                    if (isCurrent) colors.accent.copy(alpha = 0.35f) else colors.border
                ),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.card),
            contentAlignment = Alignment.Center
        ) {
            SafeTrackArtwork(
                artworkUrl = track.artworkUrl,
                modifier = Modifier.fillMaxSize(),
                contentDescription = track.title,
                shape = RoundedCornerShape(10.dp),
                iconSize = 22.dp
            )
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                color = if (isCurrent) colors.accent else colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                color = colors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = { (onOptionsClick ?: onAddToPlaylistClick)() }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Track options",
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(onClick = onLikeClick) {
            Icon(
                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Like",
                tint = if (isLiked) Color(0xFFFA2D48) else colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Isolated progress bar composable so high-frequency currentPositionMs ticks
 * NEVER recompose the MiniPlayer artwork, title, or buttons!
 */
@Composable
private fun MiniPlayerProgressBar(viewModel: PlayerViewModel) {
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val progress = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(2.5.dp),
        color = Color(0xFF38BDF8),
        trackColor = DriftTheme.colors.border
    )
}

@Composable
fun MiniPlayer(
    viewModel: PlayerViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()

    val track = currentTrack ?: return
    val colors = DriftTheme.colors

    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.92f,
        label = "miniArtScale"
    )

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MiniPlayerProgressBar(viewModel = viewModel)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .graphicsLayer {
                            scaleX = artworkScale
                            scaleY = artworkScale
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    SafeTrackArtwork(
                        artworkUrl = track.artworkUrl,
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = track.title,
                        shape = RoundedCornerShape(10.dp),
                        iconSize = 22.dp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.previousTrack() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Song",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.size(38.dp)
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = colors.accent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = colors.accent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.nextTrack() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Song",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistShuffleButton(
    tracks: List<Track>,
    modifier: Modifier = Modifier,
    onShufflePlay: (Track, List<Track>) -> Unit
) {
    TextButton(
        onClick = {
            if (tracks.isNotEmpty()) {
                val shuffled = tracks.shuffled()
                val firstTrack = shuffled.first()
                val remainingQueue = shuffled.drop(1)
                onShufflePlay(firstTrack, remainingQueue)
            }
        },
        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF38BDF8)),
        modifier = modifier.height(36.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Shuffle,
            contentDescription = "Shuffle",
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Shuffle",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
