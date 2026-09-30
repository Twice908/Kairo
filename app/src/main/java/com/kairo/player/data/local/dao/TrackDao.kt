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

    @Query("SELECT * FROM tracks WHERE sourceId = :sourceId ORDER BY title COLLATE NOCASE")
    fun observeBySource(sourceId: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE sourceId = :sourceId AND albumKey = :albumKey ORDER BY discNumber, trackNumber, title COLLATE NOCASE")
    fun observeForAlbum(sourceId: String, albumKey: String): Flow<List<TrackEntity>>

    @Query("""
        SELECT DISTINCT tracks.* FROM tracks
        LEFT JOIN albums ON tracks.albumKey = albums.key
        WHERE tracks.sourceId = :sourceId AND (
            tracks.title LIKE '%' || :query || '%' COLLATE NOCASE OR
            tracks.artistNames LIKE '%' || :query || '%' COLLATE NOCASE OR
            albums.title LIKE '%' || :query || '%' COLLATE NOCASE
        )
        ORDER BY tracks.title COLLATE NOCASE
    """)
    suspend fun search(sourceId: String, query: String): List<TrackEntity>

    @Query("DELETE FROM tracks WHERE key = :key")
    suspend fun delete(key: String)
}