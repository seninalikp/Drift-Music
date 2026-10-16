package com.soundify.player.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.soundify.player.model.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsModal(
    track: Track,
    isLiked: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleLike: () -> Unit,
    onGoToArtist: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color.Gray)
        }
    ) {
        TrackOptionsBottomSheet(
            track = track,
            isLiked = isLiked,
            onDismiss = onDismiss,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onAddToPlaylist = onAddToPlaylist,
            onToggleLike = onToggleLike,
            onGoToArtist = onGoToArtist
        )
    }
}

@Composable
fun TrackOptionsBottomSheet(
    track: Track,
    isLiked: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleLike: () -> Unit,
    onGoToArtist: () -> Unit,
) {
    val context = LocalContext.current

    val colors = DriftTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(vertical = 16.dp)
    ) {
        // Track Header Info (Artwork, Title, Artist)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SafeTrackArtwork(
                artworkUrl = track.artworkUrl,
                modifier = Modifier.size(50.dp),
                contentDescription = track.title,
                shape = RoundedCornerShape(8.dp),
                iconSize = 24.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = colors.border, thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // Options List
        OptionRow(
            icon = Icons.Default.PlayArrow,
            title = "Play next",
            tint = colors.textPrimary,
            onClick = { onPlayNext(); onDismiss() }
        )
        OptionRow(
            icon = Icons.AutoMirrored.Filled.QueueMusic,
            title = "Add to queue",
            tint = colors.textPrimary,
            onClick = { onAddToQueue(); onDismiss() }
        )
        OptionRow(
            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
            title = "Add to playlist",
            tint = colors.textPrimary,
            onClick = { onAddToPlaylist(); onDismiss() }
        )
        OptionRow(
            icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            title = if (isLiked) "Remove from Liked Songs" else "Save to Liked Songs",
            tint = if (isLiked) colors.accent else colors.textPrimary,
            onClick = { onToggleLike(); onDismiss() }
        )
        OptionRow(
            icon = Icons.Default.Person,
            title = "Go to artist",
            tint = colors.textPrimary,
            onClick = { onGoToArtist(); onDismiss() }
        )
        OptionRow(
            icon = Icons.Default.Share,
            title = "Share",
            tint = colors.textPrimary,
            onClick = {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "Listening to ${track.title} by ${track.artist}")
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, null))
                onDismiss()
            }
        )
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    title: String,
    tint: Color = DriftTheme.colors.textPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = title,
            color = tint,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
