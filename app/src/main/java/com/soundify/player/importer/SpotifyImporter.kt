package com.soundify.player.importer

import com.soundify.player.model.PlaylistModel
import com.soundify.player.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit

object SpotifyImporter {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Extracts playlist title, cover, and track list from a public Spotify playlist URL
     * using Spotify's public embed endpoint (no login or API key needed).
     */
    suspend fun importPlaylist(spotifyUrl: String): Result<PlaylistModel> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = spotifyUrl.trim()
            val playlistId = cleanUrl
                .substringAfter("playlist/", "")
                .substringBefore("?")
                .substringBefore("/")
                .trim()

            if (playlistId.isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid Spotify Playlist link. Expected format: https://open.spotify.com/playlist/...")
                )
            }

            val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
            val req = Request.Builder()
                .url(embedUrl)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                )
                .build()

            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to fetch Spotify playlist (HTTP ${res.code})"))
                }

                val html = res.body?.string() ?: ""
                val nextDataJson = html
                    .substringAfter("<script id=\"__NEXT_DATA__\" type=\"application/json\">", "")
                    .substringBefore("</script>")
                    .trim()

                if (nextDataJson.isNotBlank()) {
                    val root = json.parseToJsonElement(nextDataJson).jsonObject
                    val entity = root["props"]?.jsonObject
                        ?.get("pageProps")?.jsonObject
                        ?.get("state")?.jsonObject
                        ?.get("data")?.jsonObject
                        ?.get("entity")?.jsonObject

                    if (entity != null) {
                        val name = entity["name"]?.jsonPrimitive?.content
                            ?: entity["title"]?.jsonPrimitive?.content
                            ?: "Imported Spotify Playlist"

                        val coverUrl = entity["coverArt"]?.jsonObject
                            ?.get("sources")?.jsonArray
                            ?.firstOrNull()?.jsonObject
                            ?.get("url")?.jsonPrimitive?.content
                            ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80"

                        val trackListArray = entity["trackList"]?.jsonArray
                        val tracks = mutableListOf<Track>()

                        if (trackListArray != null) {
                            for (item in trackListArray) {
                                val tObj = item.jsonObject
                                val title = tObj["title"]?.jsonPrimitive?.content ?: continue
                                val subtitle = tObj["subtitle"]?.jsonPrimitive?.content ?: "Unknown Artist"
                                val durationMs = tObj["duration"]?.jsonPrimitive?.content?.toLongOrNull() ?: 180000L
                                val uid = tObj["uid"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString()

                                // Ensure this points specifically to the song's thumbnail,
                                // fallback to default placeholder if blank (never use playlist coverUrl!)
                                val songImage = tObj["album"]?.jsonObject
                                    ?.get("coverArt")?.jsonObject
                                    ?.get("sources")?.jsonArray
                                    ?.firstOrNull()?.jsonObject
                                    ?.get("url")?.jsonPrimitive?.content
                                    ?: tObj["coverArt"]?.jsonObject
                                        ?.get("sources")?.jsonArray
                                        ?.firstOrNull()?.jsonObject
                                        ?.get("url")?.jsonPrimitive?.content
                                    ?: tObj["image"]?.jsonPrimitive?.content
                                    ?: tObj["artworkUrl"]?.jsonPrimitive?.content
                                    ?: tObj["thumbnail"]?.jsonPrimitive?.content

                                val artworkUrl = songImage ?: "default_fallback_url"

                                tracks.add(
                                    Track(
                                        id = "sp-$uid",
                                        title = title,
                                        artist = subtitle.replace("\u00a0", " "),
                                        artworkUrl = artworkUrl,
                                        durationSec = (durationMs / 1000L).toInt().coerceAtLeast(30)
                                    )
                                )
                            }
                        }

                        if (tracks.isNotEmpty()) {
                            return@withContext Result.success(
                                PlaylistModel(
                                    id = "pl-sp-$playlistId",
                                    name = name,
                                    description = "Imported from Spotify (${tracks.size} tracks)",
                                    coverUrl = coverUrl,
                                    tracks = tracks
                                )
                            )
                        }
                    }
                }

                return@withContext Result.failure(
                    Exception("Could not parse tracks from Spotify link. Make sure the playlist is set to Public.")
                )
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
}
