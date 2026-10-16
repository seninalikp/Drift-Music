package com.soundify.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.soundify.player.api.PipedRepository
import com.soundify.player.model.PlaylistModel
import com.soundify.player.model.Track
import com.soundify.player.recommender.HomeSection

@Composable
fun AddToPlaylistModal(
    track: Track,
    playlists: List<PlaylistModel>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (String) -> Unit,
    onCreateNewPlaylist: (String) -> Unit
) {
    var newPlaylistName by remember { mutableStateOf("") }
    val colors = DriftTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add \"${track.title}\" to Playlist",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("New playlist name...", fontSize = 12.sp, color = colors.textSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border
                        )
                    )
                    IconButton(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                onCreateNewPlaylist(newPlaylistName)
                                newPlaylistName = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Create", tint = colors.accent)
                    }
                }

                if (playlists.isEmpty()) {
                    Text(
                        text = "No playlists yet. Create one above!",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                } else {
                    playlists.forEach { pl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.card)
                                .clickable { onSelectPlaylist(pl.id) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = pl.name, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                            Text(text = "${pl.tracks.size} songs", color = colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = colors.textSecondary)
            }
        },
        containerColor = colors.surface
    )
}

@Composable
fun RecommendedTrackCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    onOptionsClick: (() -> Unit)? = null
) {
    val colors = DriftTheme.colors

    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .clickable(onClick = onPlay)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(124.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            SafeTrackArtwork(
                artworkUrl = track.artworkUrl,
                modifier = Modifier.fillMaxSize(),
                contentDescription = track.title,
                shape = RoundedCornerShape(8.dp),
                iconSize = 36.dp
            )
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.PlayArrow,
                        contentDescription = "Playing",
                        tint = colors.accent,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title,
            color = if (isCurrent) colors.accent else colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = track.artist,
                color = colors.textSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { (onOptionsClick ?: onAddToPlaylist)() },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: PlayerViewModel) {
    val homeSections by viewModel.homeSections.collectAsState()
    val isHomeLoading by viewModel.isHomeLoading.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val likedTracks by viewModel.likedTracks.collectAsState()
    var trackForPlaylistModal by remember { mutableStateOf<Track?>(null) }
    var trackForOptionsModal by remember { mutableStateOf<Track?>(null) }

    if (trackForOptionsModal != null) {
        val modalTrack = trackForOptionsModal!!
        val isLiked = likedTracks.any { it.id == modalTrack.id }
        TrackOptionsModal(
            track = modalTrack,
            isLiked = isLiked,
            onDismiss = { trackForOptionsModal = null },
            onPlayNext = { viewModel.playNext(modalTrack) },
            onAddToQueue = { viewModel.addToQueue(modalTrack) },
            onAddToPlaylist = {
                val target = modalTrack
                trackForOptionsModal = null
                trackForPlaylistModal = target
            },
            onToggleLike = { viewModel.toggleLike(modalTrack) },
            onGoToArtist = { viewModel.goToArtist(modalTrack.artist) }
        )
    }

    if (trackForPlaylistModal != null) {
        AddToPlaylistModal(
            track = trackForPlaylistModal!!,
            playlists = playlists,
            onDismiss = { trackForPlaylistModal = null },
            onSelectPlaylist = { plId ->
                viewModel.addTrackToPlaylist(plId, trackForPlaylistModal!!)
                trackForPlaylistModal = null
            },
            onCreateNewPlaylist = { name ->
                viewModel.createPlaylist(name)
            }
        )
    }

    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userAvatarUrl by viewModel.userAvatarUrl.collectAsState()
    var showProfileSettings by remember { mutableStateOf(false) }
    val colors = DriftTheme.colors

    if (showProfileSettings) {
        ProfileSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showProfileSettings = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(colors.accent, Color(0xFF6366F1))
                                )
                            )
                            .clickable { showProfileSettings = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (userAvatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = userAvatarUrl,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = userName.take(1).uppercase().ifBlank { "D" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    DriftAppLogo(modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Drift ",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Music",
                        color = colors.accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.toggleDarkMode() }) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle theme",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {

        if (isHomeLoading && homeSections.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.accent)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tuning your personalized feed...",
                            color = colors.textSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else if (homeSections.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Welcome to Drift ",
                                color = colors.textPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Music",
                                color = colors.accent,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Discover songs in Search, or tap below to load trending tracks.",
                            color = colors.textSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.refreshRecommendations() },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Explore Music", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(homeSections, key = { it.id }) { section ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = section.title,
                            color = colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${section.tracks.size} tracks",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(section.tracks, key = { it.id }) { track ->
                            val isCurrent = currentTrack?.id == track.id
                            RecommendedTrackCard(
                                track = track,
                                isCurrent = isCurrent,
                                isPlaying = isPlaying,
                                onPlay = { viewModel.playTrack(track, section.tracks) },
                                onAddToPlaylist = { trackForPlaylistModal = track },
                                onOptionsClick = { trackForOptionsModal = track }
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun SearchScreen(viewModel: PlayerViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    var query by remember { mutableStateOf(searchQuery) }
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val likedTracks by viewModel.likedTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val searchRecommendations by viewModel.searchRecommendations.collectAsState()
    var trackForPlaylistModal by remember { mutableStateOf<Track?>(null) }
    var trackForOptionsModal by remember { mutableStateOf<Track?>(null) }

    LaunchedEffect(searchQuery) {
        if (query != searchQuery) {
            query = searchQuery
        }
    }

    if (trackForOptionsModal != null) {
        val modalTrack = trackForOptionsModal!!
        val isLiked = likedTracks.any { it.id == modalTrack.id }
        TrackOptionsModal(
            track = modalTrack,
            isLiked = isLiked,
            onDismiss = { trackForOptionsModal = null },
            onPlayNext = { viewModel.playNext(modalTrack) },
            onAddToQueue = { viewModel.addToQueue(modalTrack) },
            onAddToPlaylist = {
                val target = modalTrack
                trackForOptionsModal = null
                trackForPlaylistModal = target
            },
            onToggleLike = { viewModel.toggleLike(modalTrack) },
            onGoToArtist = { viewModel.goToArtist(modalTrack.artist) }
        )
    }

    if (trackForPlaylistModal != null) {
        AddToPlaylistModal(
            track = trackForPlaylistModal!!,
            playlists = playlists,
            onDismiss = { trackForPlaylistModal = null },
            onSelectPlaylist = { plId ->
                viewModel.addTrackToPlaylist(plId, trackForPlaylistModal!!)
                trackForPlaylistModal = null
            },
            onCreateNewPlaylist = { name ->
                viewModel.createPlaylist(name)
            }
        )
    }

    val colors = DriftTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.search(it)
            },
            placeholder = { Text("Search songs, artists, albums...", color = colors.textSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.textSecondary) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        viewModel.search("")
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = colors.textSecondary)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (query.isNotBlank()) {
                    viewModel.addRecentSearch(query)
                    viewModel.search(query)
                }
            }),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(50.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (query.isBlank()) {
            SearchEmptyState(
                recentSearches = recentSearches,
                recommendations = searchRecommendations,
                onRecentClick = { clickedQuery ->
                    query = clickedQuery
                    viewModel.search(clickedQuery)
                    viewModel.addRecentSearch(clickedQuery)
                },
                onRemoveRecent = { q -> viewModel.removeRecentSearch(q) },
                onClearRecents = { viewModel.clearRecentSearches() },
                onTrackClick = { track ->
                    viewModel.playTrack(track, searchRecommendations)
                },
                onTrackOptionsClick = { trackForOptionsModal = it }
            )
        } else if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        } else if (searchResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No songs found for \"$query\"", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults, key = { it.id }) { track ->
                    val isCurrent = currentTrack?.id == track.id
                    val isLiked = likedTracks.any { it.id == track.id }
                    TrackListItem(
                        track = track,
                        isPlaying = isPlaying,
                        isCurrent = isCurrent,
                        onClick = {
                            viewModel.addRecentSearch(query)
                            viewModel.playFromSearch(track)
                        },
                        onLikeClick = { viewModel.toggleLike(track) },
                        onAddToPlaylistClick = { trackForPlaylistModal = track },
                        isLiked = isLiked,
                        onOptionsClick = { trackForOptionsModal = track }
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryScreen(viewModel: PlayerViewModel) {
    val likedTracks by viewModel.likedTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showSpotifyDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var spotifyUrl by remember { mutableStateOf("") }
    var trackForPlaylistModal by remember { mutableStateOf<Track?>(null) }
    var trackForOptionsModal by remember { mutableStateOf<Track?>(null) }

    val userName by viewModel.userName.collectAsState()
    val userAvatarUrl by viewModel.userAvatarUrl.collectAsState()
    var showProfileSettings by remember { mutableStateOf(false) }

    if (showProfileSettings) {
        ProfileSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showProfileSettings = false }
        )
    }

    if (trackForOptionsModal != null) {
        val modalTrack = trackForOptionsModal!!
        val isLiked = likedTracks.any { it.id == modalTrack.id }
        TrackOptionsModal(
            track = modalTrack,
            isLiked = isLiked,
            onDismiss = { trackForOptionsModal = null },
            onPlayNext = { viewModel.playNext(modalTrack) },
            onAddToQueue = { viewModel.addToQueue(modalTrack) },
            onAddToPlaylist = {
                val target = modalTrack
                trackForOptionsModal = null
                trackForPlaylistModal = target
            },
            onToggleLike = { viewModel.toggleLike(modalTrack) },
            onGoToArtist = { viewModel.goToArtist(modalTrack.artist) }
        )
    }

    if (trackForPlaylistModal != null) {
        AddToPlaylistModal(
            track = trackForPlaylistModal!!,
            playlists = playlists,
            onDismiss = { trackForPlaylistModal = null },
            onSelectPlaylist = { plId ->
                viewModel.addTrackToPlaylist(plId, trackForPlaylistModal!!)
                trackForPlaylistModal = null
            },
            onCreateNewPlaylist = { name ->
                viewModel.createPlaylist(name)
            }
        )
    }

    val colors = DriftTheme.colors

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist", color = colors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Playlist Name", color = colors.textSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createPlaylist(newPlaylistName)
                    newPlaylistName = ""
                    showCreateDialog = false
                }) {
                    Text("Create", color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }

    if (showSpotifyDialog) {
        AlertDialog(
            onDismissRequest = { showSpotifyDialog = false },
            title = { Text("Import Spotify Playlist", color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste public Spotify playlist link:", color = colors.textSecondary, fontSize = 12.sp)
                    OutlinedTextField(
                        value = spotifyUrl,
                        onValueChange = { spotifyUrl = it },
                        placeholder = { Text("https://open.spotify.com/playlist/...", color = colors.textSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importSpotifyPlaylist(spotifyUrl)
                    spotifyUrl = ""
                    showSpotifyDialog = false
                }) {
                    Text("Import", color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSpotifyDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(colors.accent, Color(0xFF6366F1))
                            )
                        )
                        .clickable { showProfileSettings = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (userAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = userAvatarUrl,
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = userName.take(1).uppercase().ifBlank { "D" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Your Library",
                    color = colors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Playlist",
                        tint = colors.textPrimary
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.card),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = colors.accent)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Playlist", color = colors.textPrimary)
                }

                Button(
                    onClick = { showSpotifyDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import Spotify", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Liked Songs (${likedTracks.size})",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (likedTracks.isNotEmpty()) {
                    PlaylistShuffleButton(
                        tracks = likedTracks,
                        onShufflePlay = { first, rest ->
                            viewModel.handleShufflePlay(first, rest)
                        }
                    )
                }
            }
        }

        if (likedTracks.isEmpty()) {
            item {
                Text("No liked songs yet. Tap the heart icon on any track!", color = colors.textSecondary, fontSize = 12.sp)
            }
        } else {
            items(likedTracks, key = { it.id }) { track ->
                val isCurrent = currentTrack?.id == track.id
                TrackListItem(
                    track = track,
                    isPlaying = isPlaying,
                    isCurrent = isCurrent,
                    onClick = { viewModel.playTrack(track, likedTracks) },
                    onLikeClick = { viewModel.toggleLike(track) },
                    onAddToPlaylistClick = { trackForPlaylistModal = track },
                    isLiked = true,
                    onOptionsClick = { trackForOptionsModal = track }
                )
            }
        }

        item {
            Text(
                text = "Custom Playlists (${playlists.size})",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (playlists.isEmpty()) {
            item {
                Text(
                    text = "No custom playlists yet. Tap \"New Playlist\" above to create one!",
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
        } else {
            items(playlists, key = { it.id }) { pl ->
                CollapsiblePlaylistCard(
                    playlist = pl,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    likedTracks = likedTracks,
                    onTrackClick = { track -> viewModel.playTrack(track, pl.tracks) },
                    onShufflePlay = { first, rest -> viewModel.handleShufflePlay(first, rest) },
                    onLikeClick = { track -> viewModel.toggleLike(track) },
                    onAddToPlaylistClick = { track -> trackForPlaylistModal = track },
                    onOptionsClick = { track -> trackForOptionsModal = track },
                    onDeletePlaylist = { viewModel.deletePlaylist(pl.id) }
                )
            }
        }
    }
}

@Composable
fun CollapsiblePlaylistCard(
    playlist: PlaylistModel,
    currentTrack: Track?,
    isPlaying: Boolean,
    likedTracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    onLikeClick: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit,
    onOptionsClick: (Track) -> Unit,
    onDeletePlaylist: () -> Unit,
    onShufflePlay: ((Track, List<Track>) -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    val artworkUrl = playlist.coverUrl.ifBlank {
        playlist.tracks.firstOrNull {
            it.artworkUrl.isNotBlank() &&
            it.artworkUrl != "playlist_cover_placeholder" &&
            it.artworkUrl != "default_fallback_url"
        }?.artworkUrl
    }

    val colors = DriftTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .padding(12.dp)
    ) {
        // Playlist Header (Clicking this toggles expansion)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { isExpanded = !isExpanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface),
                contentAlignment = Alignment.Center
            ) {
                if (!artworkUrl.isNullOrBlank() && artworkUrl != "playlist_cover_placeholder" && artworkUrl != "default_fallback_url") {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = playlist.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (playlist.tracks.size == 1) "1 song" else "${playlist.tracks.size} songs",
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
            }

            IconButton(onClick = onDeletePlaylist) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Playlist",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = { isExpanded = !isExpanded }) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = colors.border, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                if (playlist.tracks.isEmpty()) {
                    Text(
                        text = "No songs in this playlist yet. Add songs via track options menu (⋮).",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlaylistShuffleButton(
                            tracks = playlist.tracks,
                            onShufflePlay = onShufflePlay ?: { first, _ -> onTrackClick(first) }
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        playlist.tracks.forEach { track ->
                            val isCurrent = currentTrack?.id == track.id
                            val isLiked = likedTracks.any { it.id == track.id }
                            TrackListItem(
                                track = track, // Individual track artworkUrl rendered here
                                isPlaying = isPlaying,
                                isCurrent = isCurrent,
                                onClick = { onTrackClick(track) },
                                onLikeClick = { onLikeClick(track) },
                                onAddToPlaylistClick = { onAddToPlaylistClick(track) },
                                isLiked = isLiked,
                                onOptionsClick = { onOptionsClick(track) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CollapsiblePlaylistCard(
    playlistName: String,
    playlistArtwork: String?,
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    onShufflePlay: ((Track, List<Track>) -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    val colors = DriftTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { isExpanded = !isExpanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface),
                contentAlignment = Alignment.Center
            ) {
                if (!playlistArtwork.isNullOrBlank() && playlistArtwork != "playlist_cover_placeholder" && playlistArtwork != "default_fallback_url") {
                    AsyncImage(
                        model = playlistArtwork,
                        contentDescription = playlistName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlistName,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (tracks.size == 1) "1 song" else "${tracks.size} songs",
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
            }

            IconButton(onClick = { isExpanded = !isExpanded }) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = colors.border, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                if (tracks.isEmpty()) {
                    Text(
                        text = "No songs in this playlist yet.",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlaylistShuffleButton(
                            tracks = tracks,
                            onShufflePlay = onShufflePlay ?: { first, _ -> onTrackClick(first) }
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        tracks.forEach { track ->
                            TrackListItem(
                                track = track,
                                isPlaying = false,
                                isCurrent = false,
                                onClick = { onTrackClick(track) },
                                onLikeClick = {},
                                onAddToPlaylistClick = {},
                                isLiked = false
                            )
                        }
                    }
                }
            }
        }
    }
}
