package com.soundify.player.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.soundify.player.model.Track
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "soundify_session_prefs")

data class PersistedSession(
    val currentTrack: Track?,
    val queue: List<Track>,
    val queueIndex: Int,
    val positionMs: Long,
    val activeTab: String
)

class PlayerPreferences(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val KEY_CURRENT_TRACK = stringPreferencesKey("current_track_json")
        private val KEY_QUEUE = stringPreferencesKey("queue_json")
        private val KEY_QUEUE_INDEX = intPreferencesKey("queue_index")
        private val KEY_POSITION_MS = longPreferencesKey("position_ms")
        private val KEY_ACTIVE_TAB = stringPreferencesKey("active_tab")
        private val KEY_USER_NAME = stringPreferencesKey("user_display_name")
        private val KEY_USER_AVATAR_URL = stringPreferencesKey("user_avatar_url")
    }

    suspend fun saveUserProfile(name: String, avatarUrl: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = name
            prefs[KEY_USER_AVATAR_URL] = avatarUrl
        }
    }

    suspend fun loadUserProfile(): Pair<String, String> {
        val prefs = context.dataStore.data.first()
        val name = prefs[KEY_USER_NAME] ?: "Drifter"
        val avatar = prefs[KEY_USER_AVATAR_URL] ?: ""
        return name to avatar
    }

    suspend fun saveSession(
        currentTrack: Track?,
        queue: List<Track>,
        queueIndex: Int,
        positionMs: Long,
        activeTab: String
    ) {
        context.dataStore.edit { prefs ->
            if (currentTrack != null) {
                prefs[KEY_CURRENT_TRACK] = json.encodeToString(currentTrack)
            }
            prefs[KEY_QUEUE] = json.encodeToString(queue)
            prefs[KEY_QUEUE_INDEX] = queueIndex
            prefs[KEY_POSITION_MS] = positionMs.coerceAtLeast(0L)
            prefs[KEY_ACTIVE_TAB] = activeTab
        }
    }

    suspend fun savePosition(positionMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_POSITION_MS] = positionMs.coerceAtLeast(0L)
        }
    }

    suspend fun saveActiveTab(tab: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_TAB] = tab
        }
    }

    suspend fun loadSession(): PersistedSession {
        val prefs = context.dataStore.data.first()
        val trackJson = prefs[KEY_CURRENT_TRACK]
        val queueJson = prefs[KEY_QUEUE]
        val index = prefs[KEY_QUEUE_INDEX] ?: 0
        val pos = prefs[KEY_POSITION_MS] ?: 0L
        val tab = prefs[KEY_ACTIVE_TAB] ?: "home"

        val track = try {
            if (!trackJson.isNullOrBlank()) json.decodeFromString<Track>(trackJson) else null
        } catch (_: Exception) {
            null
        }

        val queue = try {
            if (!queueJson.isNullOrBlank()) json.decodeFromString<List<Track>>(queueJson) else emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        return PersistedSession(
            currentTrack = track,
            queue = queue,
            queueIndex = index,
            positionMs = pos,
            activeTab = tab
        )
    }
}
