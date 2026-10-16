package com.soundify.player.music

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.room.Room
import com.soundify.player.model.Track
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Upgraded QueuePlayer:
 * 1. Tracks TRUE accumulated listening time via SystemClock.elapsedRealtime() while `isPlaying` is true
 *    (so scrubbing to the end of a track and skipping doesn't fake a full listen).
 * 2. Routes all hardware/Bluetooth/lock-screen Next/Prev commands through [QueueEngine].
 * 3. Prefetches the top 2 upcoming tracks in the queue for zero-latency transitions.
 */
class QueuePlayer(
    private val exo: ExoPlayer,
    private val engine: QueueEngine,
    private val resolver: StreamResolver,
    private val scope: CoroutineScope,
) : ForwardingPlayer(exo) {

    private var playing: Track? = null
    private var started = false
    private var loadJob: Job? = null
    private var failures = 0
    private val urls = ConcurrentHashMap<String, Pair<String, Long>>()

    // True wall-clock play time accumulator
    private var accumulatedPlayMs = 0L
    private var playResumedAtMs = 0L

    init {
        exo.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val now = SystemClock.elapsedRealtime()
                if (isPlaying) {
                    playResumedAtMs = now
                } else if (playResumedAtMs > 0L) {
                    accumulatedPlayMs += (now - playResumedAtMs).coerceAtLeast(0L)
                    playResumedAtMs = 0L
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) failures = 0
                if (state == Player.STATE_ENDED) skipNext()
            }

            override fun onPlayerError(error: PlaybackException) {
                if (++failures <= 3) skipNext(failed = true) else failures = 0
            }
        })
    }

    private fun snapshotTrueListenedMs(): Long {
        val now = SystemClock.elapsedRealtime()
        val activeDelta = if (playResumedAtMs > 0L) (now - playResumedAtMs).coerceAtLeast(0L) else 0L
        val total = accumulatedPlayMs + activeDelta
        accumulatedPlayMs = 0L
        playResumedAtMs = if (exo.isPlaying) now else 0L
        return total
    }

    // ---------- Public API ----------

    fun playFromUser(track: Track, fromSearch: Boolean = false, shelfId: String? = null) {
        val prev = playing
        val wasStarted = started
        val listened = snapshotTrueListenedMs()
        val prevDuration = exo.duration.takeIf { it > 0 } ?: prev?.durationMs ?: 0L

        loadJob?.cancel()
        loadJob = scope.launch {
            if (prev != null && wasStarted && listened > 1_000L) {
                engine.reportFinished(prev.copy(durationMs = prevDuration), listened)
            }
            if (fromSearch) engine.logAction(track, Action.SEARCH_PICK)
            engine.start(track, shelfId)
            playResolved(track)
        }
    }

    fun skipNext(failed: Boolean = false) {
        val cur = playing
        val wasStarted = started
        val listened = snapshotTrueListenedMs()
        val duration = exo.duration.takeIf { it > 0 } ?: cur?.durationMs ?: 0L

        loadJob?.cancel()
        loadJob = scope.launch {
            if (cur != null && wasStarted && !failed) {
                engine.reportFinished(cur.copy(durationMs = duration), listened)
            }
            val next = engine.awaitNext() ?: return@launch
            playResolved(next)
        }
    }

    fun skipPrevious() {
        if (exo.currentPosition > 3_000) {
            exo.seekTo(0)
            return
        }
        val prev = engine.previous() ?: run {
            exo.seekTo(0)
            return
        }
        snapshotTrueListenedMs()
        loadJob?.cancel()
        loadJob = scope.launch { playResolved(prev) }
    }

    // ---------- ForwardingPlayer Command Overrides ----------

    override fun getAvailableCommands(): Player.Commands = super.getAvailableCommands().buildUpon()
        .addAll(
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        ).build()

    override fun isCommandAvailable(command: Int) = availableCommands.contains(command)
    override fun hasNextMediaItem() = true
    override fun hasPreviousMediaItem() = true
    override fun seekToNext() = skipNext()
    override fun seekToNextMediaItem() = skipNext()
    override fun seekToPrevious() = skipPrevious()
    override fun seekToPreviousMediaItem() = skipPrevious()

    // ---------- Internals ----------

    private suspend fun playResolved(first: Track) {
        var track = first
        repeat(3) {
            playing = track
            started = false
            accumulatedPlayMs = 0L
            playResumedAtMs = 0L

            val url = urlFor(track)
            if (url != null) {
                exo.setMediaItem(
                    MediaItem.Builder()
                        .setMediaId(track.id)
                        .setUri(url)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(track.title)
                                .setArtist(track.artist)
                                .setArtworkUri(track.artworkUrl.ifBlank { null }?.let(Uri::parse))
                                .build()
                        ).build()
                )
                exo.prepare()
                exo.play()
                started = true
                prefetchUpcoming()
                return
            }
            track = engine.awaitNext() ?: return
        }
    }

    private suspend fun urlFor(t: Track): String? {
        urls[t.id]?.let { (url, at) ->
            if (System.currentTimeMillis() - at < 20 * 60_000L) return url
        }
        return try {
            resolver.resolve(t)?.also { resolved ->
                if (urls.size > 60) urls.clear()
                urls[t.id] = resolved to System.currentTimeMillis()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /** Prefetches the next 2 tracks in the queue so skips are instantaneous. */
    private fun prefetchUpcoming() {
        scope.launch {
            engine.upcoming.value.take(2).forEach { urlFor(it) }
        }
    }
}

class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        session = MediaSession.Builder(this, Graph.player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!Graph.player.playWhenReady) stopSelf()
    }

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }
}

object Graph {
    lateinit var repo: Repo
    lateinit var learner: Learner
    lateinit var engine: QueueEngine
    lateinit var player: QueuePlayer
    lateinit var home: HomeBuilder

    fun init(app: Application, source: MusicSource, resolver: StreamResolver) {
        repo = Repo(Room.databaseBuilder(app, MusicDb::class.java, "music.db").fallbackToDestructiveMigration().build().dao())
        val prefs = app.getSharedPreferences("soundify_learner_prefs", Context.MODE_PRIVATE)
        learner = Learner(store = PrefsLearnStore(prefs))

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val ranker = Ranker(learner = learner)
        engine = QueueEngine(source, repo, scope, learner, ranker)
        home = HomeBuilder(source, repo, learner, ranker)

        val exo = ExoPlayer.Builder(app)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player = QueuePlayer(exo, engine, resolver, scope)
    }

    fun play(context: Context, track: Track, fromSearch: Boolean = false, shelfId: String? = null) {
        context.startService(Intent(context, PlaybackService::class.java))
        player.playFromUser(track, fromSearch, shelfId)
    }
}
