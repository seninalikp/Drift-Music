package com.soundify.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundify.player.model.PlaylistModel
import com.soundify.player.model.Track
import com.soundify.player.music.SmartArtwork

/* ------------------------------------------------------------------ */
/*  Models for Library State                                          */
/* ------------------------------------------------------------------ */

@Immutable
data class UserPlaylist(
    val id: String,
    val name: String,
    val coverUrl: String? = null,
    val tracks: List<Track> = emptyList()
)

fun PlaylistModel.toUserPlaylist(): UserPlaylist = UserPlaylist(
    id = id,
    name = name,
    coverUrl = coverUrl.ifBlank { null },
    tracks = tracks
)

enum class LibraryCategory(val title: String, val icon: ImageVector) {
    PLAYLISTS("Playlists", Icons.AutoMirrored.Filled.QueueMusic),
    FAVORITES("Favorite Songs", Icons.Default.FavoriteBorder),
    MOST_PLAYED("Most Played", Icons.Default.Whatshot),
    ARTISTS("Artists", Icons.Default.MicExternalOn),
    DOWNLOADED("Downloaded", Icons.Default.DownloadDone),
    HISTORY("Recently Played", Icons.Default.History),
    BLOCKED("Hidden & Blocked", Icons.Default.Block)
}

enum class SortOrder(val label: String) {
    RECENT("Recently Added"),
    TITLE("Title A–Z"),
    ARTIST("Artist A–Z")
}

private sealed interface LibRoute {
    data object Root : LibRoute
    data class Category(val section: LibraryCategory) : LibRoute
    data class PlaylistDetail(val playlist: UserPlaylist) : LibRoute
    data class ArtistDetail(val artistName: String, val tracks: List<Track>) : LibRoute
}

/* ------------------------------------------------------------------ */
/*  Root Host with Slide Transitions                                  */
/* ------------------------------------------------------------------ */

@Composable
fun AppleMusicLibraryHost(
    playlists: List<UserPlaylist>,
    favoriteTracks: List<Track>,
    mostPlayedTracks: List<Track>,
    historyTracks: List<Track>,
    downloadedTracks: List<Track>,
    blockedTracks: List<Track>,
    recentlyAdded: List<Track>,
    onPlayTracks: (List<Track>, Boolean) -> Unit, // (tracks, shuffle)
    onTrackClick: (Track) -> Unit,
    onTrackMoreClick: (Track) -> Unit,
    onCreatePlaylist: () -> Unit = {},
    onImportPlaylist: () -> Unit = {},
    onUnblockTrack: (Track) -> Unit = {},
    fetchRealCover: suspend (String) -> String? = { null },
    modifier: Modifier = Modifier
) {
    val backStack = remember { mutableStateListOf<LibRoute>(LibRoute.Root) }
    val currentRoute = backStack.last()
    val colors = DriftTheme.colors

    BackHandler(enabled = backStack.size > 1) {
        backStack.removeLastOrNull()
    }

    AnimatedContent(
        targetState = currentRoute,
        transitionSpec = {
            if (targetState !is LibRoute.Root) {
                (slideInHorizontally(tween(260)) { it / 3 } + fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(tween(260)) { -it / 4 } + fadeOut(tween(180)))
            } else {
                (slideInHorizontally(tween(260)) { -it / 4 } + fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(tween(260)) { it / 3 } + fadeOut(tween(180)))
            }
        },
        label = "LibraryNavigation",
        modifier = modifier.fillMaxSize().background(colors.background)
    ) { route ->
        when (route) {
            is LibRoute.Root -> LibraryMainScreen(
                recentlyAdded = recentlyAdded,
                onCategoryClick = { backStack.add(LibRoute.Category(it)) },
                onTrackClick = onTrackClick,
                fetchRealCover = fetchRealCover
            )

            is LibRoute.Category -> when (route.section) {
                LibraryCategory.PLAYLISTS -> PlaylistsSubScreen(
                    playlists = playlists,
                    onBack = { backStack.removeLastOrNull() },
                    onPlaylistClick = { backStack.add(LibRoute.PlaylistDetail(it)) },
                    onCreatePlaylist = onCreatePlaylist,
                    onImportPlaylist = onImportPlaylist,
                    fetchRealCover = fetchRealCover
                )

                LibraryCategory.ARTISTS -> {
                    val allTracks = (favoriteTracks + historyTracks + recentlyAdded).distinctBy { it.id }
                    ArtistsSubScreen(
                        allTracks = allTracks,
                        onBack = { backStack.removeLastOrNull() },
                        onArtistClick = { name, tracks -> backStack.add(LibRoute.ArtistDetail(name, tracks)) }
                    )
                }

                LibraryCategory.BLOCKED -> TrackListDrillDownScreen(
                    title = "Hidden & Blocked",
                    subtitle = "Tap unblock to restore songs to your radio recommendations",
                    tracks = blockedTracks,
                    parentCoverUrl = null,
                    isBlockedList = true,
                    onBack = { backStack.removeLastOrNull() },
                    onPlayAll = { onPlayTracks(it, false) },
                    onShuffleAll = { onPlayTracks(it, true) },
                    onTrackClick = onUnblockTrack,
                    onTrackMoreClick = onTrackMoreClick,
                    fetchRealCover = fetchRealCover
                )

                else -> {
                    val (title, list) = when (route.section) {
                        LibraryCategory.FAVORITES -> "Favorite Songs" to favoriteTracks
                        LibraryCategory.MOST_PLAYED -> "Most Played" to mostPlayedTracks
                        LibraryCategory.DOWNLOADED -> "Downloaded" to downloadedTracks
                        LibraryCategory.HISTORY -> "Recently Played" to historyTracks
                        else -> "" to emptyList()
                    }
                    TrackListDrillDownScreen(
                        title = title,
                        subtitle = "${list.size} songs",
                        tracks = list,
                        parentCoverUrl = null,
                        onBack = { backStack.removeLastOrNull() },
                        onPlayAll = { onPlayTracks(it, false) },
                        onShuffleAll = { onPlayTracks(it, true) },
                        onTrackClick = onTrackClick,
                        onTrackMoreClick = onTrackMoreClick,
                        fetchRealCover = fetchRealCover
                    )
                }
            }

            is LibRoute.PlaylistDetail -> TrackListDrillDownScreen(
                title = route.playlist.name,
                subtitle = "${route.playlist.tracks.size} songs",
                tracks = route.playlist.tracks,
                parentCoverUrl = route.playlist.coverUrl, // Prevents playlist art overriding song art!
                backLabel = "Playlists",
                onBack = { backStack.removeLastOrNull() },
                onPlayAll = { onPlayTracks(it, false) },
                onShuffleAll = { onPlayTracks(it, true) },
                onTrackClick = onTrackClick,
                onTrackMoreClick = onTrackMoreClick,
                fetchRealCover = fetchRealCover
            )

            is LibRoute.ArtistDetail -> TrackListDrillDownScreen(
                title = route.artistName,
                subtitle = "${route.tracks.size} songs in your library",
                tracks = route.tracks,
                parentCoverUrl = null,
                backLabel = "Artists",
                onBack = { backStack.removeLastOrNull() },
                onPlayAll = { onPlayTracks(it, false) },
                onShuffleAll = { onPlayTracks(it, true) },
                onTrackClick = onTrackClick,
                onTrackMoreClick = onTrackMoreClick,
                fetchRealCover = fetchRealCover
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  1. Main Library Screen (Categories + Recently Added Grid)         */
/* ------------------------------------------------------------------ */

@Composable
private fun LibraryMainScreen(
    recentlyAdded: List<Track>,
    onCategoryClick: (LibraryCategory) -> Unit,
    onTrackClick: (Track) -> Unit,
    fetchRealCover: suspend (String) -> String?
) {
    val colors = DriftTheme.colors
    val dividerLine = colors.border

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 110.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Text(
                text = "Library",
                color = colors.textPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item(span = { GridItemSpan(2) }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                LibraryCategory.entries.forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategoryClick(category) }
                            .padding(vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = category.title,
                            tint = colors.accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = category.title,
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
                    HorizontalDivider(color = dividerLine, thickness = 0.7.dp)
                }
            }
        }

        if (recentlyAdded.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Recently Added",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            items(recentlyAdded, key = { "rec_grid_${it.id}" }) { track ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTrackClick(track) }
                ) {
                    SmartArtwork(
                        track = track,
                        sizeDp = 165.dp,
                        cornerRadius = 10.dp,
                        fetchRealCover = fetchRealCover,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
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
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  2. Track List Drill-Down Page (Favorites, Playlist Detail, etc.)  */
/* ------------------------------------------------------------------ */

@Composable
private fun TrackListDrillDownScreen(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    parentCoverUrl: String?,
    backLabel: String = "Library",
    isBlockedList: Boolean = false,
    onBack: () -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    onShuffleAll: (List<Track>) -> Unit,
    onTrackClick: (Track) -> Unit,
    onTrackMoreClick: (Track) -> Unit,
    fetchRealCover: suspend (String) -> String?
) {
    val colors = DriftTheme.colors
    val dividerLine = colors.border

    var filterQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(SortOrder.RECENT) }
    var showSortMenu by remember { mutableStateOf(false) }

    val displayedTracks = remember(tracks, filterQuery, sortOrder) {
        val filtered = if (filterQuery.isBlank()) tracks else {
            tracks.filter {
                it.title.contains(filterQuery, ignoreCase = true) ||
                    it.artist.contains(filterQuery, ignoreCase = true)
            }
        }
        when (sortOrder) {
            SortOrder.RECENT -> filtered
            SortOrder.TITLE -> filtered.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // Top Navigation Bar (< Library + Sort Button)
        item {
            DrillDownTopBar(
                backLabel = backLabel,
                onBack = onBack,
                trailingContent = {
                    Box {
                        TextButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = colors.accent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(sortOrder.label, color = colors.accent, fontSize = 13.sp)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(colors.card)
                        ) {
                            SortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.label, color = if (order == sortOrder) colors.accent else colors.textPrimary) },
                                    onClick = { sortOrder = order; showSortMenu = false }
                                )
                            }
                        }
                    }
                }
            )
        }

        // Title & Subtitle
        item {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                Text(text = title, color = colors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(text = subtitle, color = colors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }

        // In-Page Search Filter Bar
        item {
            InPageSearchBar(
                query = filterQuery,
                onQueryChange = { filterQuery = it },
                placeholder = "Find in $title"
            )
        }

        // Big Play & Shuffle Pill Buttons
        if (!isBlockedList && displayedTracks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionPillButton(
                        icon = Icons.Default.PlayArrow,
                        label = "Play",
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                        onClick = { onPlayAll(displayedTracks) }
                    )
                    ActionPillButton(
                        icon = Icons.Default.Shuffle,
                        label = "Shuffle",
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        onClick = { onShuffleAll(displayedTracks) }
                    )
                }
            }
        }

        // Empty State
        if (displayedTracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (filterQuery.isNotBlank()) "No matching songs" else "No songs here yet",
                        color = colors.textSecondary,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Track Rows (Using SmartArtwork so playlist cover never overrides individual track art)
        items(displayedTracks, key = { "drill_${it.id}" }) { track ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTrackClick(track) }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmartArtwork(
                    track = track,
                    sizeDp = 50.dp,
                    cornerRadius = 6.dp,
                    playlistCoverUrl = parentCoverUrl, // Strips away inherited playlist cover
                    fetchRealCover = fetchRealCover
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = colors.textPrimary,
                        fontSize = 15.sp,
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
                if (isBlockedList) {
                    TextButton(onClick = { onTrackClick(track) }) {
                        Text("Unblock", color = colors.accent, fontSize = 13.sp)
                    }
                } else {
                    IconButton(onClick = { onTrackMoreClick(track) }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            HorizontalDivider(
                color = dividerLine,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 82.dp, end = 18.dp)
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  3. Playlists Drill-Down Page (Create, Import, & List)             */
/* ------------------------------------------------------------------ */

@Composable
private fun PlaylistsSubScreen(
    playlists: List<UserPlaylist>,
    onBack: () -> Unit,
    onPlaylistClick: (UserPlaylist) -> Unit,
    onCreatePlaylist: () -> Unit,
    onImportPlaylist: () -> Unit,
    fetchRealCover: suspend (String) -> String?
) {
    val colors = DriftTheme.colors
    val dividerLine = colors.border

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        item { DrillDownTopBar(backLabel = "Library", onBack = onBack) }

        item {
            Text(
                text = "Playlists",
                color = colors.textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
            )
        }

        // Quick Action Rows: New Playlist & Import External Playlist
        item {
            PlaylistActionRow(
                icon = Icons.Default.Add,
                title = "New Playlist...",
                onClick = onCreatePlaylist
            )
            PlaylistActionRow(
                icon = Icons.Default.CloudDownload,
                title = "Import Playlist...",
                onClick = onImportPlaylist
            )
            Spacer(Modifier.height(8.dp))
        }

        items(playlists, key = { "pl_${it.id}" }) { playlist ->
            val previewTrack = playlist.tracks.firstOrNull()
                ?: Track(id = playlist.id, title = playlist.name, artist = "", artworkUrl = playlist.coverUrl ?: "")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlaylistClick(playlist) }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmartArtwork(
                    track = previewTrack.copy(artworkUrl = playlist.coverUrl ?: previewTrack.artworkUrl),
                    sizeDp = 60.dp,
                    cornerRadius = 8.dp,
                    fetchRealCover = fetchRealCover
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${playlist.tracks.size} songs",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textSecondary
                )
            }
            HorizontalDivider(
                color = dividerLine,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 94.dp, end = 18.dp)
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  4. Artists Drill-Down Page                                        */
/* ------------------------------------------------------------------ */

@Composable
private fun ArtistsSubScreen(
    allTracks: List<Track>,
    onBack: () -> Unit,
    onArtistClick: (String, List<Track>) -> Unit
) {
    val colors = DriftTheme.colors
    val dividerLine = colors.border

    val grouped = remember(allTracks) {
        allTracks.groupBy { it.artist }.entries.sortedBy { it.key.lowercase() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        item { DrillDownTopBar(backLabel = "Library", onBack = onBack) }
        item {
            Text(
                text = "Artists",
                color = colors.textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
            )
        }

        items(grouped, key = { "art_${it.key}" }) { (artist, tracks) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onArtistClick(artist, tracks) }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(colors.card),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = artist.take(1).uppercase(),
                        color = colors.accent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = artist, color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(text = "${tracks.size} songs", color = colors.textSecondary, fontSize = 13.sp)
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textSecondary
                )
            }
            HorizontalDivider(color = dividerLine, thickness = 0.5.dp, modifier = Modifier.padding(start = 78.dp, end = 18.dp))
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Reusable Apple-Style UI Pieces                                    */
/* ------------------------------------------------------------------ */

@Composable
private fun DrillDownTopBar(
    backLabel: String,
    onBack: () -> Unit,
    trailingContent: @Composable () -> Unit = {}
) {
    val colors = DriftTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                contentDescription = "Back",
                tint = colors.accent,
                modifier = Modifier.size(18.dp)
            )
            Text(text = backLabel, color = colors.accent, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        trailingContent()
    }
}

@Composable
private fun InPageSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    val colors = DriftTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.card)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(text = placeholder, color = colors.textSecondary, fontSize = 14.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear",
                tint = colors.textSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onQueryChange("") }
            )
        }
    }
}

@Composable
private fun ActionPillButton(
    icon: ImageVector,
    label: String,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = DriftTheme.colors

    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPrimary) colors.accent else colors.card)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isPrimary) Color.Black else colors.accent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = if (isPrimary) Color.Black else colors.accent,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PlaylistActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    val colors = DriftTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.card),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = colors.accent, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(text = title, color = colors.accent, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}
