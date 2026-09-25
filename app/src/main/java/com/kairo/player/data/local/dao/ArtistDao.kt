package com.kairo.player.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kairo.player.data.local.entity.ArtistEntity

@Dao
interface ArtistDao {
    @Upsert
    suspend fun upsertAll(artists: List<ArtistEntity>)

    @Query("SELECT * FROM artists WHERE key IN (:keys)")
    suspend fun getByKeys(keys: List<String>): List<ArtistEntity>
}