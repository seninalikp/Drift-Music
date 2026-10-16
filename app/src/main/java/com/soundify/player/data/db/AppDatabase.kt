package com.soundify.player.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "liked_tracks")
data class LikedTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val durationSec: Int,
    val streamUrl: String,
    val isLocalFallback: Boolean,
    val likedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history_tracks")
data class HistoryTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val durationSec: Int,
    val streamUrl: String,
    val isLocalFallback: Boolean,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val coverUrl: String,
    val tracksJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface SoundifyDao {
    @Query("SELECT * FROM liked_tracks ORDER BY likedAt DESC")
    fun getLikedTracks(): Flow<List<LikedTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiked(track: LikedTrackEntity)

    @Query("DELETE FROM liked_tracks WHERE id = :trackId")
    suspend fun deleteLiked(trackId: String)

    @Query("SELECT * FROM history_tracks ORDER BY playedAt DESC LIMIT 30")
    fun getHistoryTracks(): Flow<List<HistoryTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(track: HistoryTrackEntity)

    @Query("DELETE FROM history_tracks")
    suspend fun clearHistory()

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)
}

@Database(
    entities = [LikedTrackEntity::class, HistoryTrackEntity::class, PlaylistEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): SoundifyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "soundify_native.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
