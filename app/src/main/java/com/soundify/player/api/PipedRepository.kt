package com.soundify.player.api

import android.util.Base64
import com.soundify.player.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object PipedRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private val PIPED_INSTANCES = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.ducks.party",
        "https://pipedapi.ngn.tf",
        "https://api.piped.privacydev.net"
    )

    val FALLBACK_TRACKS = emptyList<Track>()

    /**
     * Decrypts JioSaavn's official encrypted_media_url into a direct full-length 320kbps MP4/M4A stream.
     */
    private fun decryptSaavnUrl(encryptedUrl: String): String? {
        return try {
            val keySpec = SecretKeySpec("38346591".toByteArray(Charsets.UTF_8), "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decodedBytes = Base64.decode(encryptedUrl.trim(), Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            val rawUrl = String(decryptedBytes, Charsets.UTF_8).trim()
            rawUrl
                .replace("http://", "https://")
                .replace("_96.mp4", "_320.mp4")
                .replace("_160.mp4", "_320.mp4")
        } catch (_: Exception) {
            null
        }
    }

    private fun cleanHtmlEntities(text: String): String {
        return text
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
    }

    suspend fun searchTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext FALLBACK_TRACKS
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")

        // 1. Primary: Official JioSaavn API (Direct full-length 320kbps streams from start to finish)
        val officialSaavn = fetchOfficialSaavnTracks(encodedQuery)
        if (officialSaavn.isNotEmpty()) return@withContext officialSaavn

        // 2. Secondary: Community JioSaavn Mirrors (Full-length streams)
        val mirrorSaavn = fetchMirrorSaavnTracks(encodedQuery)
        if (mirrorSaavn.isNotEmpty()) return@withContext mirrorSaavn

        // 3. Tertiary: Piped YouTube Music Search (Full-length streams)
        val pipedResults = coroutineScope {
            PIPED_INSTANCES.map { instance ->
                async { fetchPipedSearch(instance, encodedQuery) }
            }.awaitAll().firstOrNull { it.isNotEmpty() }.orEmpty()
        }
        if (pipedResults.isNotEmpty()) return@withContext pipedResults

        return@withContext FALLBACK_TRACKS.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
        }
    }

    private fun fetchOfficialSaavnTracks(encodedQuery: String): List<Track> {
        try {
            val url =
                "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&n=25&p=1&q=$encodedQuery"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()

            val res = client.newCall(req).execute()
            try {
                if (!res.isSuccessful) return emptyList()
                val body = res.body?.string() ?: return emptyList()
                val root = json.parseToJsonElement(body).jsonObject
                val resultsArr = root["results"]?.jsonArray ?: return emptyList()
                val tracks = mutableListOf<Track>()

                for (item in resultsArr) {
                    val obj = item.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.content ?: continue
                    val title = cleanHtmlEntities(obj["title"]?.jsonPrimitive?.content ?: "Unknown Title")

                    val moreInfo = obj["more_info"]?.jsonObject
                    val encryptedMediaUrl = moreInfo?.get("encrypted_media_url")?.jsonPrimitive?.content
                        ?: obj["encrypted_media_url"]?.jsonPrimitive?.content
                        ?: continue

                    val streamUrl = decryptSaavnUrl(encryptedMediaUrl) ?: continue

                    val artistMap = moreInfo?.get("artistMap")?.jsonObject
                    val primaryArtistsArr = artistMap?.get("primary_artists")?.jsonArray
                    val artistFromMap = primaryArtistsArr
                        ?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
                        ?.joinToString(", ")

                    val rawArtist = if (!artistFromMap.isNullOrBlank()) {
                        artistFromMap
                    } else {
                        moreInfo?.get("music")?.jsonPrimitive?.content
                            ?: obj["subtitle"]?.jsonPrimitive?.content
                            ?: "Unknown Artist"
                    }
                    val artist = cleanHtmlEntities(rawArtist)

                    val rawImage = obj["image"]?.jsonPrimitive?.content
                        ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80"
                    val artworkUrl = rawImage
                        .replace("http://", "https://")
                        .replace("150x150", "500x500")
                        .replace("50x50", "500x500")

                    val duration = moreInfo?.get("duration")?.jsonPrimitive?.content?.toIntOrNull()
                        ?: obj["duration"]?.jsonPrimitive?.content?.toIntOrNull()
                        ?: 200

                    tracks.add(
                        Track(
                            id = "saavn-$id",
                            title = title,
                            artist = artist,
                            artworkUrl = artworkUrl,
                            durationSec = if (duration > 0) duration else 200,
                            streamUrl = streamUrl
                        )
                    )
                }
                return tracks
            } finally {
                res.close()
            }
        } catch (_: Exception) {
            return emptyList()
        }
    }

    private fun fetchMirrorSaavnTracks(encodedQuery: String): List<Track> {
        val endpoints = listOf(
            "https://saavn.sumit.co/api/search/songs?query=$encodedQuery&limit=25",
            "https://jiosaavn-api-privatecvc2.vercel.app/search/songs?query=$encodedQuery&limit=25",
            "https://saavn.dev/api/search/songs?query=$encodedQuery&limit=25"
        )
        for (url in endpoints) {
            try {
                val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
                val res = client.newCall(req).execute()
                try {
                    if (!res.isSuccessful) continue
                    val body = res.body?.string() ?: continue
                    val root = json.parseToJsonElement(body).jsonObject
                    val dataElement = root["data"] ?: continue

                    val resultsArr = try {
                        dataElement.jsonObject["results"]?.jsonArray
                    } catch (_: Exception) {
                        dataElement.jsonArray
                    } ?: continue

                    val tracks = mutableListOf<Track>()

                    for (item in resultsArr) {
                        val obj = item.jsonObject
                        val id = obj["id"]?.jsonPrimitive?.content ?: continue
                        val rawTitle = obj["name"]?.jsonPrimitive?.content
                            ?: obj["title"]?.jsonPrimitive?.content
                            ?: "Unknown Title"
                        val title = cleanHtmlEntities(rawTitle)

                        val artist = obj["primaryArtists"]?.jsonPrimitive?.content
                            ?: obj["artists"]?.jsonObject?.get("primary")?.jsonArray
                                ?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
                                ?.joinToString(", ")
                            ?: obj["subtitle"]?.jsonPrimitive?.content
                            ?: "Unknown Artist"

                        val images = obj["image"]?.jsonArray
                        val thumb = images?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                            ?: images?.lastOrNull()?.jsonObject?.get("link")?.jsonPrimitive?.content
                            ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80"

                        val downloads = obj["downloadUrl"]?.jsonArray
                        val streamUrl = downloads?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                            ?: downloads?.lastOrNull()?.jsonObject?.get("link")?.jsonPrimitive?.content
                            ?: continue

                        val duration = obj["duration"]?.jsonPrimitive?.content?.toIntOrNull() ?: 200

                        tracks.add(
                            Track(
                                id = "saavn-$id",
                                title = title,
                                artist = cleanHtmlEntities(artist),
                                artworkUrl = thumb.replace("http://", "https://"),
                                durationSec = if (duration > 0) duration else 200,
                                streamUrl = streamUrl.replace("http://", "https://")
                            )
                        )
                    }
                    if (tracks.isNotEmpty()) return tracks
                } finally {
                    res.close()
                }
            } catch (_: Exception) {
            }
        }
        return emptyList()
    }

    private fun fetchPipedSearch(instance: String, encodedQuery: String): List<Track> {
        try {
            val url = "$instance/search?q=$encodedQuery&filter=music_songs"
            val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val res = client.newCall(req).execute()
            try {
                if (!res.isSuccessful) return emptyList()
                val body = res.body?.string() ?: return emptyList()
                val root = json.parseToJsonElement(body).jsonObject
                val items = root["items"]?.jsonArray ?: return emptyList()
                val results = mutableListOf<Track>()

                for (item in items) {
                    val obj = item.jsonObject
                    val rawUrl = obj["url"]?.jsonPrimitive?.content ?: continue
                    val videoId = rawUrl.substringAfter("v=", "").substringBefore("&")
                    if (videoId.isBlank()) continue

                    val title = obj["title"]?.jsonPrimitive?.content ?: "Unknown Title"
                    val artist = obj["uploaderName"]?.jsonPrimitive?.content ?: "Unknown Artist"
                    val thumb = obj["thumbnail"]?.jsonPrimitive?.content
                        ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                    val duration = obj["duration"]?.jsonPrimitive?.content?.toIntOrNull() ?: 200

                    results.add(
                        Track(
                            id = videoId,
                            title = title,
                            artist = artist,
                            artworkUrl = thumb,
                            durationSec = if (duration > 0) duration else 200
                        )
                    )
                }
                return results
            } finally {
                res.close()
            }
        } catch (_: Exception) {
            return emptyList()
        }
    }

    suspend fun resolveStreamUrl(track: Track): String = withContext(Dispatchers.IO) {
        // 1. Play immediately if it's already a full-length direct stream (and NOT an Apple 30-sec preview)
        if (track.streamUrl.startsWith("http") && !track.streamUrl.contains("apple.com")) {
            return@withContext track.streamUrl
        }

        // 2. Resolve full-length song via Official JioSaavn or Mirrors
        val cleanQuery = URLEncoder.encode("${track.title} ${track.artist}".trim(), "UTF-8")
        val directMatch = fetchOfficialSaavnTracks(cleanQuery).firstOrNull()
            ?: fetchMirrorSaavnTracks(cleanQuery).firstOrNull()
        if (directMatch != null && directMatch.streamUrl.startsWith("http")) {
            return@withContext directMatch.streamUrl
        }

        // 3. Try Piped stream extraction if it's a YouTube videoId
        if (!track.id.startsWith("sp-") && !track.id.startsWith("saavn-")) {
            val ytUrl = resolveYoutubeVideoId(track.id)
            if (!ytUrl.isNullOrBlank()) return@withContext ytUrl
        }

        return@withContext FALLBACK_TRACKS.firstOrNull()?.streamUrl.orEmpty()
    }

    private suspend fun resolveYoutubeVideoId(videoId: String): String? = coroutineScope {
        val jobs = PIPED_INSTANCES.map { instance ->
            async {
                try {
                    val url = "$instance/streams/$videoId"
                    val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
                    val res = client.newCall(req).execute()
                    try {
                        if (!res.isSuccessful) return@async null
                        val body = res.body?.string() ?: return@async null
                        val root = json.parseToJsonElement(body).jsonObject
                        val audioStreams = root["audioStreams"]?.jsonArray ?: return@async null

                        var bestUrl: String? = null
                        var bestBitrate = -1

                        for (stream in audioStreams) {
                            val sObj = stream.jsonObject
                            val sUrl = sObj["url"]?.jsonPrimitive?.content ?: continue
                            val mimeType = sObj["mimeType"]?.jsonPrimitive?.content ?: ""
                            val bitrate = sObj["bitrate"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0

                            if (mimeType.contains("audio/mp4") || mimeType.contains("audio/webm")) {
                                if (bitrate > bestBitrate) {
                                    bestBitrate = bitrate
                                    bestUrl = sUrl
                                }
                            } else if (bestUrl == null) {
                                bestUrl = sUrl
                            }
                        }
                        bestUrl
                    } finally {
                        res.close()
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }
        jobs.awaitAll().firstOrNull { !it.isNullOrBlank() }
    }
}
