package com.kairo.player.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kairo.player.data.local.entity.AlbumEntity

@Dao
interface AlbumDao {
    @Upsert
    suspend fun upsert(album: AlbumEntity)

    @Query("SELECT * FROM albums WHERE key = :key LIMIT 1")
    suspend fun getByKey(key: String): AlbumEntity?

    @Query("SELECT * FROM albums WHERE key IN (:keys)")
    suspend fun getByKeys(keys: List<String>): List<AlbumEntity>
}