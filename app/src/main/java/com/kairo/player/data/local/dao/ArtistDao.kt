package com.kairo.player.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kairo.player.data.local.entity.ArtistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {
    @Upsert
    suspend fun upsertAll(artists: List<ArtistEntity>)

    @Query("SELECT * FROM artists WHERE key IN (:keys)")
    suspend fun getByKeys(keys: List<String>): List<ArtistEntity>

    @Query("SELECT * FROM artists WHERE sourceId = :sourceId ORDER BY name COLLATE NOCASE")
    fun observeBySource(sourceId: String): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists WHERE sourceId = :sourceId AND remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(sourceId: String, remoteId: String): ArtistEntity?

    @Query("SELECT * FROM artists WHERE sourceId = :sourceId AND remoteId = :artistId LIMIT 1")
    fun observeArtist(sourceId: String, artistId: String): Flow<ArtistEntity?>

    @Query("SELECT * FROM artists WHERE key = :artistKey LIMIT 1")
    fun observeArtistByKey(artistKey: String): Flow<ArtistEntity?>

    @Query("SELECT * FROM albums WHERE sourceId = :sourceId AND artistKeys LIKE '%' || :artistKey || '%' ORDER BY title COLLATE NOCASE")
    fun observeAlbumsForArtist(sourceId: String, artistKey: String): Flow<List<com.kairo.player.data.local.entity.AlbumEntity>>

    @Query("SELECT COUNT(*) FROM tracks WHERE sourceId = :sourceId AND artistKeys LIKE '%' || :artistKey || '%'")
    fun getTrackCountByArtist(sourceId: String, artistKey: String): Flow<Int>
}