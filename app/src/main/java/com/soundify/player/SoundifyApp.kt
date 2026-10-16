package com.soundify.player

import android.app.Application
import com.soundify.player.api.PipedRepository
import com.soundify.player.music.Graph
import com.soundify.player.music.PipedMusicSource
import com.soundify.player.music.StreamResolver

class SoundifyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val source = PipedMusicSource()
        val resolver = StreamResolver { track ->
            PipedRepository.resolveStreamUrl(track)
        }
        Graph.init(this, source, resolver)
    }
}
