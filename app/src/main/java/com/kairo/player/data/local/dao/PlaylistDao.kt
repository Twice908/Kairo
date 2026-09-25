package com.kairo.player.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.kairo.player.data.local.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlaylistDao {
    @Upsert
    abstract suspend fun upsert(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    abstract suspend fun getById(id: String): PlaylistEntity?

    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("DELETE FROM playlists WHERE id = :id")
    abstract suspend fun delete(id: String)

    @Transaction
    open suspend fun addTrack(id: String, trackKey: String, updatedAtMs: Long) {
        val playlist = getById(id) ?: return
        if (trackKey !in playlist.trackKeys) {
            upsert(playlist.copy(trackKeys = playlist.trackKeys + trackKey, updatedAtMs = updatedAtMs))
        }
    }

    @Transaction
    open suspend fun removeTrack(id: String, trackKey: String, updatedAtMs: Long) {
        val playlist = getById(id) ?: return
        upsert(playlist.copy(trackKeys = playlist.trackKeys - trackKey, updatedAtMs = updatedAtMs))
    }
}