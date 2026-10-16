package com.soundify.player.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundify.player.model.Track

enum class FullscreenPanel { ART, LYRICS, QUEUE }

@Composable
fun NowPlayingScreen(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val likedTracks by viewModel.likedTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsState()

    var activePanel by remember { mutableStateOf(FullscreenPanel.ART) }
    var showPlaylistModal by remember { mutableStateOf(false) }
    var showOptionsModal by remember { mutableStateOf(false) }

    val track = currentTrack ?: return
    val isLiked = remember(likedTracks, track.id) { likedTracks.any { it.id == track.id } }

    if (showOptionsModal) {
        TrackOptionsModal(
            track = track,
            isLiked = isLiked,
            onDismiss = { showOptionsModal = false },
            onPlayNext = { viewModel.playNext(track) },
            onAddToQueue = { viewModel.addToQueue(track) },
            onAddToPlaylist = {
                showOptionsModal = false
                showPlaylistModal = true
            },
            onToggleLike = { viewModel.toggleLike(track) },
            onGoToArtist = { viewModel.goToArtist(track.artist) }
        )
    }

    if (showPlaylistModal) {
        AddToPlaylistModal(
            track = track,
            playlists = playlists,
            onDismiss = { showPlaylistModal = false },
            onSelectPlaylist = { plId ->
                viewModel.addTrackToPlaylist(plId, track)
                showPlaylistModal = false
            },
            onCreateNewPlaylist = { name ->
                viewModel.createPlaylist(name)
            }
        )
    }

    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF162534),
                Color(0xFF0F151C),
                Color(0xFF090A0C)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(backgroundBrush)
            .systemBarsPadding()
    ) {
        // Subtle ambient atmospheric glow
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.TopCenter)
                .offset(y = 60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.16f),
                            Color(0xFF0284C7).copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Grip Handle & Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.28f))
                        .clickable(onClick = onDismiss)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimize",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "DRIFT MUSIC",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.5.sp
                        )
                        Text(
                            text = when (activePanel) {
                                FullscreenPanel.LYRICS -> "LYRICS & NOTES"
                                FullscreenPanel.QUEUE -> "PLAYING FROM QUEUE"
                                FullscreenPanel.ART -> "NOW PLAYING"
                            },
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.5.sp
                        )
                    }
                    IconButton(onClick = { showOptionsModal = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Middle Interactive Content Area (Artwork / Lyrics / Queue)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                when (activePanel) {
                    FullscreenPanel.ART -> {
                        val artworkScale by animateFloatAsState(
                            targetValue = if (isPlaying) 1.0f else 0.87f,
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "artworkScale"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .aspectRatio(1f)
                                .graphicsLayer {
                                    scaleX = artworkScale
                                    scaleY = artworkScale
                                }
                                .clip(RoundedCornerShape(24.dp))
                                .border(
                                    BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                                    RoundedCornerShape(24.dp)
                                )
                                .background(Color(0xFF1E1E24))
                                .clickable { viewModel.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            SafeTrackArtwork(
                                artworkUrl = track.artworkUrl,
                                modifier = Modifier.fillMaxSize(),
                                contentDescription = track.title,
                                shape = RoundedCornerShape(24.dp),
                                iconSize = 72.dp
                            )
                        }
                    }

                    FullscreenPanel.LYRICS -> {
                        val currentPosMs by viewModel.currentPositionMs.collectAsState()
                        val durationMs by viewModel.durationMs.collectAsState()
                        val maxDur = if (durationMs > 0) durationMs else (track.durationSec * 1000L).coerceAtLeast(1000L)

                        val lyrics = remember(track.id) {
                            listOf(
                                "♪ ${track.title} — ${track.artist}",
                                "Feel the rhythm drifting through the night",
                                "Lost in the frequency, floating in blue light",
                                "Bass resonance echoes deep in the soul",
                                "The melody drifts, taking full control",
                                "Pure studio stream • Drift Audio Engine",
                                "Turn the volume up and let the sound drift"
                            )
                        }

                        val activeIndex = if (maxDur > 0) {
                            ((currentPosMs.toFloat() / maxDur) * lyrics.size).toInt().coerceIn(0, lyrics.size - 1)
                        } else 0

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(
                                    BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                    RoundedCornerShape(22.dp)
                                )
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "SYNCED AUDIO NOTES & LYRICS",
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(lyrics.size) { idx ->
                                    val isCurrentLine = idx == activeIndex
                                    Text(
                                        text = lyrics[idx],
                                        color = if (isCurrentLine) Color.White else Color.White.copy(alpha = 0.35f),
                                        fontSize = if (isCurrentLine) 20.sp else 16.sp,
                                        fontWeight = if (isCurrentLine) FontWeight.ExtraBold else FontWeight.Medium,
                                        lineHeight = if (isCurrentLine) 26.sp else 22.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val targetMs = ((idx.toFloat() / lyrics.size) * maxDur).toLong()
                                                viewModel.seekTo(targetMs)
                                            }
                                    )
                                }
                            }
                        }
                    }

                    FullscreenPanel.QUEUE -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(
                                    BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                    RoundedCornerShape(22.dp)
                                )
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "UP NEXT (${queue.size})",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                                Text(
                                    text = "Tap track to play",
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontSize = 11.sp
                                )
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(queue, key = { it.id }) { qTrack ->
                                    val isCurrent = qTrack.id == track.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isCurrent) Color.White.copy(alpha = 0.14f)
                                                else Color.White.copy(alpha = 0.04f)
                                            )
                                            .clickable { viewModel.playTrack(qTrack, queue) }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        SafeTrackArtwork(
                                            artworkUrl = qTrack.artworkUrl,
                                            modifier = Modifier.size(38.dp),
                                            contentDescription = qTrack.title,
                                            shape = RoundedCornerShape(8.dp),
                                            iconSize = 18.dp
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = qTrack.title,
                                                color = if (isCurrent) Color(0xFF38BDF8) else Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = qTrack.artist,
                                                color = Color.White.copy(alpha = 0.55f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Default.GraphicEq,
                                                contentDescription = "Playing",
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Track Info & Like Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = track.artist,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { viewModel.toggleLike(track) }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color(0xFFFA2D48) else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Isolated Seek Bar
            PlayerSeekBarSection(
                viewModel = viewModel,
                fallbackDurationSec = track.durationSec
            )

            // Transport Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffleEnabled) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = { viewModel.previousTrack() }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8))
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color.Black,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                IconButton(onClick = { viewModel.nextTrack() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                IconButton(onClick = { showPlaylistModal = true }) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Apple Music Bottom Pill Toolbar: Lyrics, Speed, Sleep Timer, Up Next
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppleMusicPillButton(
                    icon = Icons.Default.FormatQuote,
                    text = "Lyrics",
                    isActive = activePanel == FullscreenPanel.LYRICS,
                    onClick = {
                        activePanel = if (activePanel == FullscreenPanel.LYRICS) FullscreenPanel.ART else FullscreenPanel.LYRICS
                    }
                )

                AppleMusicPillButton(
                    icon = Icons.Default.Speed,
                    text = "${playbackSpeed}x",
                    isActive = playbackSpeed != 1f,
                    activeColor = Color(0xFF38BDF8),
                    onClick = { viewModel.cyclePlaybackSpeed() }
                )

                AppleMusicPillButton(
                    icon = Icons.Default.Bedtime,
                    text = if (sleepTimerMinutes != null) "${sleepTimerMinutes}m" else "Sleep",
                    isActive = sleepTimerMinutes != null,
                    activeColor = Color(0xFFA855F7),
                    onClick = { viewModel.cycleSleepTimer() }
                )

                AppleMusicPillButton(
                    icon = Icons.Default.QueueMusic,
                    text = "Up Next",
                    isActive = activePanel == FullscreenPanel.QUEUE,
                    onClick = {
                        activePanel = if (activePanel == FullscreenPanel.QUEUE) FullscreenPanel.ART else FullscreenPanel.QUEUE
                    }
                )
            }
        }
    }
}

@Composable
private fun AppleMusicPillButton(
    icon: ImageVector,
    text: String,
    isActive: Boolean,
    activeColor: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isActive) activeColor else Color.White.copy(alpha = 0.08f)
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (isActive) Color.Transparent else Color.White.copy(alpha = 0.12f)
                ),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isActive) Color.Black else Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = text,
            color = if (isActive) Color.Black else Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PlayerSeekBarSection(
    viewModel: PlayerViewModel,
    fallbackDurationSec: Int
) {
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderTempPosition by remember { mutableFloatStateOf(0f) }

    val maxDuration = if (durationMs > 0) {
        durationMs.toFloat()
    } else {
        (fallbackDurationSec * 1000f).coerceAtLeast(1000f)
    }
    val sliderValue = if (isDraggingSlider) {
        sliderTempPosition
    } else {
        currentPositionMs.toFloat().coerceIn(0f, maxDuration)
    }
    val remainingMs = (maxDuration - sliderValue).coerceAtLeast(0f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = sliderValue,
            onValueChange = {
                isDraggingSlider = true
                sliderTempPosition = it
            },
            onValueChangeFinished = {
                isDraggingSlider = false
                viewModel.seekTo(sliderTempPosition.toLong())
            },
            valueRange = 0f..maxDuration,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF38BDF8),
                inactiveTrackColor = Color.White.copy(alpha = 0.18f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-4).dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(sliderValue.toLong()),
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "-${formatTime(remainingMs.toLong())}",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun NowPlayingDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    NowPlayingScreen(viewModel = viewModel, onDismiss = onDismiss)
}
