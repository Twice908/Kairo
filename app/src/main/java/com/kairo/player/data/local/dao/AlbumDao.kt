package com.kairo.player.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kairo.player.data.local.entity.AlbumEntity
import com.kairo.player.data.local.entity.ArtistEntity
import com.kairo.player.data.local.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Upsert
    suspend fun upsert(album: AlbumEntity)

    @Query("SELECT * FROM albums WHERE key = :key LIMIT 1")
    suspend fun getByKey(key: String): AlbumEntity?

    @Query("SELECT * FROM albums WHERE key IN (:keys)")
    suspend fun getByKeys(keys: List<String>): List<AlbumEntity>

    @Query("SELECT * FROM albums WHERE sourceId = :sourceId ORDER BY title COLLATE NOCASE")
    fun observeBySource(sourceId: String): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE sourceId = :sourceId AND remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(sourceId: String, remoteId: String): AlbumEntity?

    @Query("SELECT * FROM albums WHERE sourceId = :sourceId AND remoteId = :albumId LIMIT 1")
    fun observeAlbum(sourceId: String, albumId: String): Flow<AlbumEntity?>

    @Query("SELECT * FROM tracks WHERE sourceId = :sourceId AND albumKey = :albumKey ORDER BY discNumber, trackNumber, title COLLATE NOCASE")
    fun observeTracksForAlbum(sourceId: String, albumKey: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM artists WHERE sourceId = :sourceId AND key = :artistKey LIMIT 1")
    fun observeArtistForAlbum(sourceId: String, artistKey: String): Flow<ArtistEntity?>

    @Upsert
    suspend fun upsertAll(albums: List<AlbumEntity>)
}