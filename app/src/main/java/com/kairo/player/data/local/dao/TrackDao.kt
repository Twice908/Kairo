package com.kairo.player.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kairo.player.data.local.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Upsert
    suspend fun upsert(track: TrackEntity)

    @Upsert
    suspend fun upsertAll(tracks: List<TrackEntity>)

    @Query("SELECT * FROM tracks WHERE key = :key LIMIT 1")
    suspend fun getByKey(key: String): TrackEntity?

    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<TrackEntity>>

    @Query("DELETE FROM tracks WHERE key = :key")
    suspend fun delete(key: String)
}