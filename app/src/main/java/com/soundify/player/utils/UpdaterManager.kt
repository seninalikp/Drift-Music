package com.soundify.player.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

data class ReleaseInfo(
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String
)

object UpdaterManager {

    private const val GITHUB_REPO = "seninalikp/Drift-Music"
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdate(currentVersionName: String): Result<ReleaseInfo?> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "DriftMusicUpdater")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@withContext Result.success(null)
                val body = res.body?.string() ?: return@withContext Result.success(null)
                val root = json.parseToJsonElement(body).jsonObject

                val tagName = root["tag_name"]?.jsonPrimitive?.content ?: return@withContext Result.success(null)
                val bodyNotes = root["body"]?.jsonPrimitive?.content ?: "No release notes provided."
                val assets = root["assets"]?.jsonArray ?: return@withContext Result.success(null)

                val apkAsset = assets.firstOrNull { 
                    it.jsonObject["name"]?.jsonPrimitive?.content?.endsWith(".apk", ignoreCase = true) == true 
                }?.jsonObject

                val downloadUrl = apkAsset?.get("browser_download_url")?.jsonPrimitive?.content ?: return@withContext Result.success(null)

                val remoteVersion = tagName.removePrefix("v").trim()
                val localVersion = currentVersionName.removePrefix("v").trim()

                if (remoteVersion != localVersion) {
                    Result.success(ReleaseInfo(tagName, bodyNotes, downloadUrl))
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadAndInstall(context: Context, downloadUrl: String, onProgress: (Float) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "DriftMusicUpdater")
                .build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@withContext Result.failure(Exception("Download failed: HTTP ${res.code}"))
                val body = res.body ?: return@withContext Result.failure(Exception("Empty body"))

                val file = File(context.cacheDir, "update.apk")
                if (file.exists()) file.delete()
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L

                FileOutputStream(file).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytes = input.read(buffer)
                        while (bytes >= 0) {
                            output.write(buffer, 0, bytes)
                            downloadedBytes += bytes
                            if (totalBytes > 0) {
                                onProgress(downloadedBytes.toFloat() / totalBytes.toFloat())
                            }
                            bytes = input.read(buffer)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    installApk(context, file)
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun installApk(context: Context, file: File) {
        val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
