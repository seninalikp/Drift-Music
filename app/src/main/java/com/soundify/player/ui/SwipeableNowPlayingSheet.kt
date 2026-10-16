package com.soundify.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundify.player.model.Track
import com.soundify.player.music.SmartArtwork
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Wrap your main app content with this container, or place it at the bottom of your root Box.
 * Swiping up on the MiniBar smoothly drags the full NowPlaying screen up with your finger;
 * swiping down drags it right back into the MiniBar.
 */
@Composable
fun SwipeableNowPlayingSheet(
    track: Track?,
    isPlaying: Boolean,
    currentPositionMs: () -> Long,
    durationMs: () -> Long,
    isLiked: Boolean,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleLike: (Track) -> Unit,
    onMoreOptions: (Track) -> Unit,
    fetchRealCover: suspend (String) -> String? = { null },
    bottomNavHeight: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    if (track == null) return

    val scope = rememberCoroutineScope()
    val config = LocalConfiguration.current
    val density = LocalDensity.current

    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    val miniHeightPx = with(density) { 68.dp.toPx() }
    val bottomNavPx = with(density) { bottomNavHeight.toPx() }
    val maxCollapseOffset = (screenHeightPx - miniHeightPx - bottomNavPx).coerceAtLeast(1f)

    // 0f = Collapsed (Mini Player), 1f = Expanded (Full Now Playing)
    val expansion = remember { Animatable(0f) }
    val isExpanded by remember { derivedStateOf { expansion.value > 0.5f } }

    fun expand() {
        scope.launch {
            expansion.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow))
        }
    }

    fun collapse() {
        scope.launch {
            expansion.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
        }
    }

    BackHandler(enabled = expansion.value > 0.1f) {
        collapse()
    }

    val dragState = rememberDraggableState { deltaY ->
        val progressDelta = -deltaY / maxCollapseOffset
        scope.launch {
            expansion.snapTo((expansion.value + progressDelta).coerceIn(0f, 1f))
        }
    }

    val currentOffsetY = ((1f - expansion.value) * maxCollapseOffset).roundToInt()

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, currentOffsetY) }
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    val shouldExpand = when {
                        velocity < -450f -> true   // Fast swipe up
                        velocity > 450f -> false   // Fast swipe down
                        else -> expansion.value > 0.35f
                    }
                    if (shouldExpand) expand() else collapse()
                }
            )
    ) {
        // 1. Full Screen Now Playing Layer (Fades in as you drag up)
        if (expansion.value > 0.02f) {
            FullNowPlayingContent(
                track = track,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                isLiked = isLiked,
                onPlayPause = onPlayPause,
                onSeekTo = onSeekTo,
                onNext = onNext,
                onPrevious = onPrevious,
                onToggleLike = { onToggleLike(track) },
                onMoreOptions = { onMoreOptions(track) },
                onCollapse = ::collapse,
                fetchRealCover = fetchRealCover,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = ((expansion.value - 0.1f) / 0.9f).coerceIn(0f, 1f) }
            )
        }

        // 2. Mini Bar Layer (Fades out as you drag up)
        if (expansion.value < 0.85f) {
            MiniPlayerBarContent(
                track = track,
                isPlaying = isPlaying,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onExpand = ::expand,
                fetchRealCover = fetchRealCover,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .graphicsLayer { alpha = (1f - (expansion.value * 2.2f)).coerceIn(0f, 1f) }
            )
        }
    }
}

@Composable
private fun MiniPlayerBarContent(
    track: Track,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    fetchRealCover: suspend (String) -> String?,
    modifier: Modifier = Modifier
) {
    val colors = DriftTheme.colors

    Row(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (colors.isDark) Color(0xFF181818) else Color(0xFFFFFFFF))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onExpand
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SmartArtwork(
            track = track,
            sizeDp = 44.dp,
            cornerRadius = 8.dp,
            fetchRealCover = fetchRealCover
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
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
        IconButton(onClick = onPlayPause) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                tint = colors.textPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = colors.accent,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun FullNowPlayingContent(
    track: Track,
    isPlaying: Boolean,
    currentPositionMs: () -> Long,
    durationMs: () -> Long,
    isLiked: Boolean,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleLike: () -> Unit,
    onMoreOptions: () -> Unit,
    onCollapse: () -> Unit,
    fetchRealCover: suspend (String) -> String?,
    modifier: Modifier = Modifier
) {
    val colors = DriftTheme.colors

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var posMs by remember { mutableLongStateOf(0L) }
    var durMs by remember { mutableLongStateOf(1L) }

    // Poll progress cleanly on UI only when expanded, without triggering parent recompositions
    LaunchedEffect(track.id, isPlaying, isDraggingSlider) {
        while (true) {
            if (!isDraggingSlider) {
                posMs = currentPositionMs().coerceAtLeast(0L)
                durMs = durationMs().coerceAtLeast(1L)
                sliderValue = (posMs.toFloat() / durMs.toFloat()).coerceIn(0f, 1f)
            }
            delay(250L)
        }
    }

    val bgGradient = if (colors.isDark) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF1A2833), Color(0xFF0E0E0E), Color(0xFF0E0E0E))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFE0F2FE), Color(0xFFF8FAFC), Color(0xFFF8FAFC))
        )
    }

    Column(
        modifier = modifier
            .background(bgGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Pill Grabber + Collapse & More Menu
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(colors.textSecondary.copy(alpha = 0.35f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCollapse) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Minimize",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = "NOW PLAYING",
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            IconButton(onClick = onMoreOptions) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "Track Options",
                    tint = colors.textPrimary
                )
            }
        }

        Spacer(Modifier.weight(0.5f))

        // High-Res Smart Artwork
        SmartArtwork(
            track = track,
            sizeDp = 310.dp,
            cornerRadius = 16.dp,
            fetchRealCover = fetchRealCover,
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .aspectRatio(1f)
        )

        Spacer(Modifier.weight(0.5f))

        // Track Title, Artist & Favorite Heart
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = colors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = track.artist,
                    color = colors.textSecondary,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggleLike) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) colors.accent else colors.textPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Smooth Seek Bar
        Slider(
            value = sliderValue,
            onValueChange = {
                isDraggingSlider = true
                sliderValue = it
            },
            onValueChangeFinished = {
                val target = (sliderValue * durMs).toLong()
                onSeekTo(target)
                posMs = target
                isDraggingSlider = false
            },
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.border
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val shownPos = if (isDraggingSlider) (sliderValue * durMs).toLong() else posMs
            Text(formatTimeMs(shownPos), color = colors.textSecondary, fontSize = 12.sp)
            Text(formatTimeMs(durMs), color = colors.textSecondary, fontSize = 12.sp)
        }

        Spacer(Modifier.height(20.dp))

        // Main Transport Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(38.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(38.dp)
                )
            }

            IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

private fun formatTimeMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0L)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
