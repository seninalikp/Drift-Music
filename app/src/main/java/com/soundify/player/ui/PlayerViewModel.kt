package com.soundify.player.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soundify.player.music.Graph
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.soundify.player.api.PipedRepository
import com.soundify.player.api.TrackDeduplicator.deduplicateTracks
import com.soundify.player.data.PlayerPreferences
import com.soundify.player.data.db.AppDatabase
import com.soundify.player.data.db.HistoryTrackEntity
import com.soundify.player.data.db.LikedTrackEntity
import com.soundify.player.data.db.PlaylistEntity
import com.soundify.player.importer.SpotifyImporter
import com.soundify.player.model.PlaylistModel
import com.soundify.player.model.Track
import com.soundify.player.queue.PipedMusicSource
import com.soundify.player.queue.PlayQueue
import com.soundify.player.recommender.FileRecommenderStorage
import com.soundify.player.recommender.HomeSection
import com.soundify.player.recommender.PipedRecommendationProvider
import com.soundify.player.recommender.RecommenderEngine
import com.soundify.player.service.AudioPlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application).dao()
    private val prefs = PlayerPreferences(application)
    private val json = Json { ignoreUnknownKeys = true }

    private val recommenderStorage = FileRecommenderStorage(
        File(application.filesDir, "soundify_recommender_events.json")
    )
    private val recommendationProvider = PipedRecommendationProvider()
    val recommender = RecommenderEngine(
        provider = recommendationProvider,
        storage = recommenderStorage
    )

    private val musicSource = PipedMusicSource(recommendationProvider)
    private val playHistory = mutableListOf<PlayEvent>()

    val playQueue = PlayQueue(
        source = musicSource,
        history = { synchronized(playHistory) { playHistory.toList() } },
        scope = viewModelScope
    )

    val audioSettingsManager = AudioSettingsManager(application)

    fun applyAudioQuality(url: String): String {
        if (!url.contains("saavncdn.com")) return url
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val isWifi = cm?.activeNetwork?.let { net ->
            cm.getNetworkCapabilities(net)?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
        } ?: true
        val quality = audioSettingsManager.resolveActiveQuality(isWifi)
        return when (quality) {
            AudioQuality.NORMAL -> url.replace("_320.", "_96.").replace("_160.", "_96.")
            AudioQuality.HIGH -> url.replace("_320.", "_160.").replace("_96.", "_160.")
            AudioQuality.UHD -> url.replace("_96.", "_320.").replace("_160.", "_320.")
        }
    }

    private var mediaController: MediaController? = null
    private var progressJob: Job? = null
    private var searchJob: Job? = null

    private val _activeTab = MutableStateFlow("home")
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(PipedRepository.FALLBACK_TRACKS)
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(180000L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(PipedRepository.FALLBACK_TRACKS)
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()
    private var sleepTimerJob: Job? = null

    private val _homeSections = MutableStateFlow<List<HomeSection>>(emptyList())
    val homeSections: StateFlow<List<HomeSection>> = _homeSections.asStateFlow()

    private val _isHomeLoading = MutableStateFlow(false)
    val isHomeLoading: StateFlow<Boolean> = _isHomeLoading.asStateFlow()

    private val _userName = MutableStateFlow("Drifter")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userAvatarUrl = MutableStateFlow("")
    val userAvatarUrl: StateFlow<String> = _userAvatarUrl.asStateFlow()

    fun updateProfile(name: String, avatarUrl: String) {
        val trimmed = name.trim().ifBlank { "Drifter" }
        _userName.value = trimmed
        _userAvatarUrl.value = avatarUrl.trim()
        viewModelScope.launch {
            prefs.saveUserProfile(trimmed, avatarUrl.trim())
        }
    }

    private val searchHistory = SearchHistory(application)
    val recentSearches: StateFlow<List<String>> = searchHistory.items

    val searchRecommendations: StateFlow<List<Track>> = _homeSections
        .map { sections ->
            sections.firstOrNull { it.id == "foryou" }?.tracks
                ?: sections.firstOrNull { it.id == "trending" }?.tracks
                ?: sections.firstOrNull()?.tracks
                ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRecentSearch(query: String) {
        if (query.isNotBlank()) {
            searchHistory.add(query)
        }
    }

    fun removeRecentSearch(query: String) = searchHistory.remove(query)

    fun clearRecentSearches() = searchHistory.clear()

    val likedTracks: StateFlow<List<Track>> = db.getLikedTracks()
        .map { list ->
            list.map {
                Track(
                    id = it.id,
                    title = it.title,
                    artist = it.artist,
                    artworkUrl = it.artworkUrl,
                    durationSec = it.durationSec,
                    streamUrl = it.streamUrl,
                    isLocalFallback = it.isLocalFallback
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyTracks: StateFlow<List<Track>> = db.getHistoryTracks()
        .map { list ->
            list.map {
                Track(
                    id = it.id,
                    title = it.title,
                    artist = it.artist,
                    artworkUrl = it.artworkUrl,
                    durationSec = it.durationSec,
                    streamUrl = it.streamUrl,
                    isLocalFallback = it.isLocalFallback
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistModel>> = db.getPlaylists()
        .map { list ->
            list.map { entity ->
                val parsedTracks = try {
                    json.decodeFromString<List<Track>>(entity.tracksJson)
                } catch (_: Exception) {
                    emptyList()
                }
                val cleanedTracks = parsedTracks.map { track ->
                    // Fix legacy tracks where the playlist cover was mistakenly assigned to individual tracks
                    val isFaultyCover = entity.coverUrl.isNotBlank() && track.artworkUrl == entity.coverUrl
                    val cleanArt = if (isFaultyCover || track.artworkUrl.isBlank() || track.artworkUrl == "playlist_cover_placeholder") {
                        "default_fallback_url"
                    } else {
                        track.artworkUrl
                    }
                    track.copy(artworkUrl = cleanArt)
                }
                PlaylistModel(
                    id = entity.id,
                    name = entity.name,
                    description = entity.description,
                    coverUrl = entity.coverUrl,
                    tracks = cleanedTracks,
                    createdAt = entity.createdAt
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        restoreColdStartSession()
        connectToService()
        refreshRecommendations()
        loadHistoryEvents()
    }

    private fun loadHistoryEvents() {
        viewModelScope.launch {
            db.getHistoryTracks().collect { list ->
                synchronized(playHistory) {
                    if (playHistory.isEmpty() && list.isNotEmpty()) {
                        list.take(50).forEach { entity ->
                            playHistory.add(
                                PlayEvent(
                                    track = Track(
                                        id = entity.id,
                                        title = entity.title,
                                        artist = entity.artist,
                                        artworkUrl = entity.artworkUrl,
                                        durationSec = entity.durationSec,
                                        streamUrl = entity.streamUrl,
                                        isLocalFallback = entity.isLocalFallback
                                    ),
                                    completed = true
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun refreshRecommendations() {
        viewModelScope.launch {
            _isHomeLoading.value = true
            val sections = recommender.buildHome()
            _homeSections.value = sections
            _isHomeLoading.value = false
        }
    }

    private fun restoreColdStartSession() {
        viewModelScope.launch {
            val (savedName, savedAvatar) = prefs.loadUserProfile()
            _userName.value = savedName
            _userAvatarUrl.value = savedAvatar

            val saved = prefs.loadSession()
            _activeTab.value = saved.activeTab
            if (saved.queue.isNotEmpty()) {
                _queue.value = saved.queue
                _queueIndex.value = saved.queueIndex.coerceIn(0, saved.queue.lastIndex)
            }
            if (saved.currentTrack != null) {
                _currentTrack.value = saved.currentTrack
                _currentPositionMs.value = saved.positionMs
                _durationMs.value = (saved.currentTrack.durationSec * 1000L).coerceAtLeast(1000L)
            } else {
                val first = _queue.value.firstOrNull()
                _currentTrack.value = first
                _durationMs.value = ((first?.durationSec ?: 180) * 1000L)
            }
        }
    }

    private fun connectToService() {
        val context = getApplication<Application>()
        val sessionToken = SessionToken(context, ComponentName(context, AudioPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            val controller = try {
                controllerFuture.get()
            } catch (_: Exception) {
                null
            } ?: return@addListener

            mediaController = controller

            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    if (mediaItem != null) {
                        val trackId = mediaItem.mediaId
                        val q = _queue.value
                        val idx = q.indexOfFirst { it.id == trackId }
                        if (idx >= 0) {
                            val prev = _currentTrack.value
                            if (prev != null && prev.id != trackId) {
                                recommender.log("complete", prev)
                            }
                            _queueIndex.value = idx
                            _currentTrack.value = q[idx]
                            _currentPositionMs.value = 0L
                            _durationMs.value = (q[idx].durationSec * 1000L).coerceAtLeast(1000L)
                            prefetchUpcomingTrack(idx + 1)
                        }
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _isBuffering.value = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_READY && controller.duration > 0) {
                        _durationMs.value = controller.duration
                    } else if (playbackState == Player.STATE_ENDED) {
                        val finished = _currentTrack.value
                        if (finished != null) {
                            recommender.log("complete", finished)
                        }
                        nextTrack(isUserSkipped = false)
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    android.util.Log.e("PlayerViewModel", "Playback error: ${error.message}", error)
                    viewModelScope.launch {
                        delay(600L)
                        nextTrack(isUserSkipped = false)
                    }
                }
            })

            startProgressObserver()
        }, MoreExecutors.directExecutor())
    }

    private var prefetchJob: Job? = null

    private fun prefetchUpcomingTrack(nextIdx: Int) {
        prefetchJob?.cancel()
        val q = _queue.value
        if (nextIdx !in q.indices) return
        val nextTrack = q[nextIdx]
        prefetchJob = viewModelScope.launch {
            try {
                val resolvedUrl = applyAudioQuality(PipedRepository.resolveStreamUrl(nextTrack))
                if (resolvedUrl.isNotBlank()) {
                    val resolvedTrack = nextTrack.copy(streamUrl = resolvedUrl)
                    val updatedQueue = _queue.value.toMutableList()
                    if (nextIdx in updatedQueue.indices && updatedQueue[nextIdx].id == nextTrack.id) {
                        updatedQueue[nextIdx] = resolvedTrack
                        _queue.value = updatedQueue
                    }
                    val controller = mediaController
                    if (controller != null && controller.mediaItemCount <= 1) {
                        val metadata = MediaMetadata.Builder()
                            .setTitle(resolvedTrack.title)
                            .setArtist(resolvedTrack.artist)
                            .setArtworkUri(Uri.parse(resolvedTrack.artworkUrl))
                            .build()
                        val mediaItem = MediaItem.Builder()
                            .setMediaId(resolvedTrack.id)
                            .setUri(resolvedUrl)
                            .setMediaMetadata(metadata)
                            .setRequestMetadata(
                                MediaItem.RequestMetadata.Builder()
                                    .setMediaUri(Uri.parse(resolvedUrl))
                                    .build()
                            )
                            .build()
                        controller.addMediaItem(mediaItem)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startProgressObserver() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            var tickCount = 0
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    _currentPositionMs.value = pos
                    if (controller.duration > 0) {
                        _durationMs.value = controller.duration
                    }
                    tickCount++
                    if (tickCount % 5 == 0) {
                        prefs.savePosition(pos)
                    }
                }
                delay(1000L)
            }
        }
    }

    fun setActiveTab(tab: String) {
        _activeTab.value = tab
        viewModelScope.launch {
            prefs.saveActiveTab(tab)
        }
    }

    fun dismissStatusMessage() {
        _statusMessage.value = null
    }

    fun search(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = PipedRepository.FALLBACK_TRACKS
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(350L) // Debounce typing
            val tracks = PipedRepository.searchTracks(query).deduplicateTracks()
            _searchResults.value = tracks
            _isSearching.value = false
        }
    }

    fun playFromSearch(track: Track) {
        recommender.log("search", track)
        playTrack(track, _searchResults.value)
    }

    fun showStatusMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun handleShufflePlay(playlistTracks: List<Track>) {
        if (playlistTracks.isEmpty()) return
        val shuffled = playlistTracks.shuffled()
        handleShufflePlay(shuffled.first(), shuffled.drop(1))
    }

    fun handleShufflePlay(firstTrack: Track, remainingQueue: List<Track>) {
        _isShuffleEnabled.value = true
        val fullShuffledList = listOf(firstTrack) + remainingQueue

        // 1. Start playback through the ViewModel's main player pipeline
        playTrack(firstTrack, fullShuffledList)

        // 2. Sync with QueueEngine safely without wiping the queue
        try {
            Graph.engine.start(firstTrack)
            remainingQueue.forEach { track ->
                Graph.engine.enqueue(track)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleShuffle() {
        val newState = !_isShuffleEnabled.value
        _isShuffleEnabled.value = newState
        if (newState) {
            val upcoming = Graph.engine.upcoming.value
            if (upcoming.size > 1) {
                val reshuffled = upcoming.shuffled()
                val current = _currentTrack.value
                if (current != null) {
                    Graph.engine.start(current)
                    reshuffled.forEach { Graph.engine.enqueue(it) }
                }
            }
        }
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun cyclePlaybackSpeed() {
        val speeds = listOf(0.85f, 1f, 1.25f, 1.5f)
        val currentIndex = speeds.indexOf(_playbackSpeed.value).takeIf { it >= 0 } ?: 1
        val nextSpeed = speeds[(currentIndex + 1) % speeds.size]
        _playbackSpeed.value = nextSpeed
        mediaController?.setPlaybackSpeed(nextSpeed)
    }

    fun cycleSleepTimer() {
        val options = listOf(null, 15, 30, 60)
        val currentIndex = options.indexOf(_sleepTimerMinutes.value).takeIf { it >= 0 } ?: 0
        val nextOption = options[(currentIndex + 1) % options.size]
        setSleepTimer(nextOption)
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = minutes
        if (minutes != null && minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                delay(minutes * 60 * 1000L)
                mediaController?.pause()
                _sleepTimerMinutes.value = null
            }
        }
    }

    private fun normalizeTitle(title: String?): String =
        title.orEmpty()
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()

    fun playSongFromAnyScreen(song: Track, contextQueue: List<Track>? = null) {
        val currentTitle = normalizeTitle(
            mediaController?.currentMediaItem?.mediaMetadata?.title?.toString()
                ?: _currentTrack.value?.title
        )

        val requestedTitle = normalizeTitle(song.title)

        // Don't restart a song with the same title.
        if (requestedTitle.isNotBlank() &&
            requestedTitle == currentTitle
        ) {
            val controller = mediaController
            if (controller != null && !controller.isPlaying) {
                controller.play()
            }
            return
        }

        playTrack(song, contextQueue, forceRestart = true)
    }

    fun playTrack(
        track: Track,
        contextQueue: List<Track>? = null,
        startPositionMs: Long = 0L,
        forceRestart: Boolean = false
    ) {
        if (!forceRestart && startPositionMs == 0L) {
            val currentTitle = normalizeTitle(
                mediaController?.currentMediaItem?.mediaMetadata?.title?.toString()
                    ?: _currentTrack.value?.title
            )
            val requestedTitle = normalizeTitle(track.title)

            // Don't restart a song with the same title.
            if (requestedTitle.isNotBlank() && requestedTitle == currentTitle) {
                val controller = mediaController
                if (controller != null && !controller.isPlaying) {
                    controller.play()
                }
                return
            }
        }

        viewModelScope.launch {
            _isBuffering.value = true

            // Recommendation Event Tracking
            val prevTrack = _currentTrack.value
            val prevPosMs = _currentPositionMs.value
            if (prevTrack != null && prevTrack.id != track.id) {
                val listenedSec = prevPosMs / 1000L
                recommender.logPlaybackEnd(prevTrack, listenedSec, prevTrack.durationSec.toLong())
            }
            if (prevTrack?.id == track.id) {
                recommender.log("replay", track)
            } else {
                recommender.log("play", track)
            }

            playQueue.start(track)

            val newQueue = contextQueue ?: _queue.value
            val idx = newQueue.indexOfFirst { it.id == track.id }
            val updatedQueue = if (idx >= 0) newQueue else listOf(track) + newQueue
            val activeIdx = if (idx >= 0) idx else 0

            _queue.value = updatedQueue
            _queueIndex.value = activeIdx
            _currentTrack.value = track
            _currentPositionMs.value = startPositionMs
            _durationMs.value = (track.durationSec * 1000L).coerceAtLeast(1000L)

            db.insertHistory(
                HistoryTrackEntity(
                    id = track.id,
                    title = track.title,
                    artist = track.artist,
                    artworkUrl = track.artworkUrl,
                    durationSec = track.durationSec,
                    streamUrl = track.streamUrl,
                    isLocalFallback = track.isLocalFallback
                )
            )
            prefs.saveSession(track, updatedQueue, activeIdx, startPositionMs, _activeTab.value)

            val resolvedUrl = applyAudioQuality(PipedRepository.resolveStreamUrl(track))
            val resolvedTrack = track.copy(streamUrl = resolvedUrl)
            _currentTrack.value = resolvedTrack

            val controller = mediaController
            if (controller != null) {
                val mediaItem = resolvedTrack.toMediaItem(resolvedUrl)
                controller.setMediaItem(mediaItem, startPositionMs)
                controller.prepare()
                controller.play()
                prefetchUpcomingTrack(activeIdx + 1)
            }
            _isBuffering.value = false
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        val current = _currentTrack.value ?: return

        if (controller.mediaItemCount == 0) {
            playTrack(current, _queue.value, _currentPositionMs.value)
        } else if (controller.isPlaying) {
            controller.pause()
            viewModelScope.launch { prefs.savePosition(controller.currentPosition) }
        } else {
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _durationMs.value)
        _currentPositionMs.value = clamped
        mediaController?.seekTo(clamped)
        viewModelScope.launch {
            prefs.savePosition(clamped)
        }
    }

    fun nextTrack(isUserSkipped: Boolean = true) {
        val current = _currentTrack.value
        val durMs = _durationMs.value
        val posMs = _currentPositionMs.value
        if (current != null) {
            val completed = !isUserSkipped && (posMs >= (durMs * 0.75).toLong().coerceAtLeast(30000L))
            synchronized(playHistory) {
                playHistory.add(PlayEvent(current, completed))
                if (playHistory.size > 100) playHistory.removeAt(0)
            }
            if (isUserSkipped) {
                playQueue.onSkipped(current)
            }
        }

        val controller = mediaController
        if (isUserSkipped && controller != null && controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
            return
        }

        val q = _queue.value
        if (q.isEmpty()) return
        val nextIdx = _queueIndex.value + 1
        if (nextIdx < q.size) {
            playTrack(q[nextIdx], q, 0L, forceRestart = true)
        } else {
            // Radio / Dynamic queue via PlayQueue (waits up to 5s if still refilling)
            viewModelScope.launch {
                val next = playQueue.awaitNext()
                if (next != null) {
                    val updatedQueue = _queue.value + next
                    _queue.value = updatedQueue
                    playTrack(next, updatedQueue, 0L, forceRestart = true)
                } else {
                    // Fallback if awaitNext times out
                    val radioTracks = if (current != null) recommender.nextUp(current, 10) else emptyList()
                    if (radioTracks.isNotEmpty()) {
                        val combined = _queue.value + radioTracks
                        _queue.value = combined
                        playTrack(radioTracks.first(), combined, 0L, forceRestart = true)
                        return@launch
                    }
                    if (_queue.value.isNotEmpty()) {
                        playTrack(_queue.value[0], _queue.value, 0L, forceRestart = true)
                    }
                }
            }
        }
    }

    fun previousTrack() {
        if (_currentPositionMs.value > 3000L) {
            seekTo(0L)
            return
        }
        val q = _queue.value
        if (q.isEmpty()) return
        val prevIdx = if (_queueIndex.value - 1 < 0) q.lastIndex else _queueIndex.value - 1
        playTrack(q[prevIdx], q, 0L, forceRestart = true)
    }

    fun skipToNext() = nextTrack(isUserSkipped = true)

    fun skipToPrevious() = previousTrack()

    fun playNext(track: Track) {
        val q = _queue.value.toMutableList()
        val currentIdx = _queueIndex.value
        if (q.isEmpty()) {
            playTrack(track)
            return
        }
        val insertIdx = (currentIdx + 1).coerceAtMost(q.size)
        val existing = q.indexOfFirst { it.id == track.id }
        if (existing > insertIdx) {
            q.removeAt(existing)
        }
        q.add(insertIdx, track)
        _queue.value = q
        playQueue.playNext(track)
        _statusMessage.value = "Will play next: ${track.title}"
    }

    fun addToQueue(track: Track) {
        val q = _queue.value.toMutableList()
        if (q.isEmpty()) {
            playTrack(track)
            return
        }
        q.add(track)
        _queue.value = q
        _statusMessage.value = "Added to queue: ${track.title}"
    }

    fun goToArtist(artist: String) {
        if (artist.isNotBlank()) {
            setActiveTab("search")
            search(artist)
            addRecentSearch(artist)
        }
    }

    fun toggleLike(track: Track) {
        viewModelScope.launch {
            val isAlreadyLiked = likedTracks.value.any { it.id == track.id }
            if (isAlreadyLiked) {
                db.deleteLiked(track.id)
            } else {
                recommender.log("like", track)
                db.insertLiked(
                    LikedTrackEntity(
                        id = track.id,
                        title = track.title,
                        artist = track.artist,
                        artworkUrl = track.artworkUrl,
                        durationSec = track.durationSec,
                        streamUrl = track.streamUrl,
                        isLocalFallback = track.isLocalFallback
                    )
                )
            }
        }
    }

    fun createPlaylist(name: String, description: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            val entity = PlaylistEntity(
                id = "pl-${UUID.randomUUID()}",
                name = name.trim(),
                description = description.trim(),
                coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80",
                tracksJson = "[]"
            )
            db.insertPlaylist(entity)
            _statusMessage.value = "Created playlist \"${name.trim()}\""
        }
    }

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch {
            recommender.log("addToPlaylist", track)
            val currentPl = playlists.value.find { it.id == playlistId } ?: return@launch
            if (currentPl.tracks.any { it.id == track.id }) {
                _statusMessage.value = "Track already in \"${currentPl.name}\""
                return@launch
            }
            val updatedTracks = currentPl.tracks + track
            db.insertPlaylist(
                PlaylistEntity(
                    id = currentPl.id,
                    name = currentPl.name,
                    description = currentPl.description,
                    coverUrl = currentPl.coverUrl,
                    tracksJson = json.encodeToString(updatedTracks),
                    createdAt = currentPl.createdAt
                )
            )
            _statusMessage.value = "Added to \"${currentPl.name}\""
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            db.deletePlaylist(playlistId)
        }
    }

    fun importSpotifyPlaylist(spotifyUrl: String) {
        viewModelScope.launch {
            _statusMessage.value = "Importing Spotify playlist..."
            val result = SpotifyImporter.importPlaylist(spotifyUrl)
            result.onSuccess { pl ->
                db.insertPlaylist(
                    PlaylistEntity(
                        id = pl.id,
                        name = pl.name,
                        description = pl.description,
                        coverUrl = pl.coverUrl,
                        tracksJson = json.encodeToString(pl.tracks),
                        createdAt = pl.createdAt
                    )
                )
                _statusMessage.value = "Imported \"${pl.name}\" (${pl.tracks.size} tracks)!"
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Failed to import Spotify link"
            }
        }
    }

    override fun onCleared() {
        searchJob?.cancel()
        progressJob?.cancel()
        prefetchJob?.cancel()
        mediaController?.release()
        super.onCleared()
    }
}

fun Track.toMediaItem(resolvedUrlOverride: String? = null): MediaItem {
    val url = resolvedUrlOverride ?: streamUrl
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setArtworkUri(if (artworkUrl.isNotBlank()) Uri.parse(artworkUrl) else null)
        .build()

    val builder = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(metadata)

    if (url.isNotBlank()) {
        val uri = Uri.parse(url)
        builder.setUri(uri)
        builder.setRequestMetadata(
            MediaItem.RequestMetadata.Builder()
                .setMediaUri(uri)
                .build()
        )
    }

    return builder.build()
}

