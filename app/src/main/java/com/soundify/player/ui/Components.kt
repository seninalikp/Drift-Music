package com.soundify.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
                    .allowHardware(true)
                    .memoryCacheKey(artworkUrl)
                    .diskCacheKey(artworkUrl)
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
 * Standard MiniPlayer composable adapter that wraps the new Floating Liquid-Glass PillMiniPlayer
 * and passes the PlayerViewModel state with isolated progress polling.
 */
@Composable
fun MiniPlayer(
    viewModel: PlayerViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val likedTracks by viewModel.likedTracks.collectAsState()

    val track = currentTrack ?: return
    val isLiked = remember(track.id, likedTracks) {
        likedTracks.any { it.id == track.id }
    }

    PillMiniPlayer(
        track = track,
        isPlaying = isPlaying,
        isLiked = isLiked,
        progressProvider = {
            val cur = viewModel.currentPositionMs.value
            val dur = viewModel.durationMs.value
            if (dur > 0L) (cur.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
        },
        onTogglePlayPause = { viewModel.togglePlayPause() },
        onPlayNext = { viewModel.nextTrack() },
        onToggleLike = { viewModel.toggleLike(track) },
        onExpandFullPlayer = onClick,
        modifier = modifier
    )
}

/**
 * Samsung One UI "Now Bar" & Apple Dynamic Island style Floating Liquid-Glass Pill MiniPlayer.
 * - Tap the pill to open Fullscreen Now Playing.
 * - Long-press or swipe horizontally to collapse/expand into a compact corner capsule!
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PillMiniPlayer(
    track: Track,
    isPlaying: Boolean,
    isLiked: Boolean,
    progressProvider: () -> Float, // 0f..1f lambda to avoid full-pill recomposition
    onTogglePlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    onToggleLike: () -> Unit,
    onExpandFullPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Allows the user to collapse the full pill into a tiny One UI / Dynamic Island corner capsule
    var isCompactCapsule by rememberSaveable { mutableStateOf(false) }

    // Liquid Glass Gradient & Specular Rim Brush
    val glassBackgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2A2D35).copy(alpha = 0.94f),
                Color(0xFF16181D).copy(alpha = 0.97f)
            )
        )
    }

    val specularBorderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.06f),
                Color(0xFF38BDF8).copy(alpha = 0.18f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = if (isCompactCapsule) Alignment.CenterEnd else Alignment.Center
    ) {
        AnimatedContent(
            targetState = isCompactCapsule,
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(
                    initialScale = 0.88f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )).togetherWith(
                    fadeOut(tween(150)) + scaleOut(targetScale = 0.88f)
                )
            },
            label = "PillMorphTransition"
        ) { compact ->
            if (compact) {
                // ============================================================
                // STATE 1: MINIMIZED COMPACT CAPSULE (Samsung Now Bar / Island)
                // ============================================================
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .height(54.dp)
                        .shadow(
                            elevation = 20.dp,
                            shape = CircleShape,
                            ambientColor = Color.Black,
                            spotColor = Color(0xFF38BDF8).copy(alpha = 0.35f)
                        )
                        .clip(CircleShape)
                        .background(glassBackgroundBrush)
                        .border(width = 1.dp, brush = specularBorderBrush, shape = CircleShape)
                        .clickable { isCompactCapsule = false }
                        .padding(horizontal = 8.dp)
                ) {
                    // Spinning Circular Artwork + Progress Ring
                    ArtworkWithProgressRing(
                        artworkUrl = track.artworkUrl,
                        title = track.title,
                        isPlaying = isPlaying,
                        progressProvider = progressProvider,
                        sizeDp = 40
                    )

                    // Animated Equalizer Bars
                    MiniEqualizerBars(isPlaying = isPlaying)

                    // Play/Pause Pill Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8))
                            .clickable(
                                onClick = onTogglePlayPause
                            )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF0A0A0E),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Expand chevron hint
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Expand Pill",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                // ============================================================
                // STATE 2: FULL FLOATING LIQUID-GLASS PILL
                // ============================================================
                var horizontalDragAccum by remember { mutableStateOf(0f) }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .height(62.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = CircleShape,
                            ambientColor = Color.Black,
                            spotColor = Color(0xFF38BDF8).copy(alpha = 0.28f)
                        )
                        .clip(CircleShape)
                        .background(glassBackgroundBrush)
                        .border(width = 1.dp, brush = specularBorderBrush, shape = CircleShape)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { horizontalDragAccum = 0f },
                                onDragEnd = {
                                    // Swipe right > 90px collapses into compact corner capsule
                                    if (horizontalDragAccum > 90f) {
                                        isCompactCapsule = true
                                    } else if (horizontalDragAccum < -90f) {
                                        onPlayNext()
                                    }
                                    horizontalDragAccum = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    horizontalDragAccum += dragAmount
                                }
                            )
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onExpandFullPlayer
                        )
                        .padding(start = 8.dp, end = 10.dp)
                ) {
                    // Left: Circular Vinyl Artwork + Isolated Progress Ring
                    ArtworkWithProgressRing(
                        artworkUrl = track.artworkUrl,
                        title = track.title,
                        isPlaying = isPlaying,
                        progressProvider = progressProvider,
                        sizeDp = 46
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Center: Marquee Track Title + Artist + Live Waveform Dot
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MiniEqualizerBars(isPlaying = isPlaying)

                            Text(
                                text = track.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = track.artist,
                            color = Color(0xFFA8B0C0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Right Controls: Like, Play/Pause Glowing Circle, Next, & Minimize-to-Capsule
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onToggleLike,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Tactile Glowing Play/Pause Pill Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF38BDF8),
                                            Color(0xFF0EA5E9)
                                        )
                                    )
                                )
                                .clickable(onClick = onTogglePlayPause)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color(0xFF080A0F),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = onPlayNext,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next Track",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Isolated Circular Artwork + Progress Ring so 500ms playback updates
 * only redraw the 2.dp ring and never recompose the entire pill.
 */
@Composable
private fun ArtworkWithProgressRing(
    artworkUrl: String?,
    title: String,
    isPlaying: Boolean,
    progressProvider: () -> Float,
    sizeDp: Int
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.90f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ArtworkPulseScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(sizeDp.dp)
    ) {
        // Circular Artwork inside the ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
        ) {
            SafeTrackArtwork(
                artworkUrl = artworkUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Isolated Progress Ring
        CircularTrackProgressRing(
            progressProvider = progressProvider,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun CircularTrackProgressRing(
    progressProvider: () -> Float,
    modifier: Modifier = Modifier
) {
    val progress = progressProvider().coerceIn(0f, 1f)
    CircularProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = Color(0xFF38BDF8),
        trackColor = Color.White.copy(alpha = 0.12f),
        strokeWidth = 2.2.dp,
        strokeCap = StrokeCap.Round
    )
}

/**
 * Subtle animated 3-bar audio waveform indicator (Apple Dynamic Island / Samsung Now Bar style)
 */
@Composable
private fun MiniEqualizerBars(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "EqTransition")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isPlaying) 1.0f else 0.3f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = if (isPlaying) 0.25f else 0.3f,
        animationSpec = infiniteRepeatable(tween(340, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = if (isPlaying) 0.95f else 0.3f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.height(12.dp)
    ) {
        listOf(bar1, bar2, bar3).forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height((12f * if (isPlaying) fraction else 0.28f).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF38BDF8))
            )
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
