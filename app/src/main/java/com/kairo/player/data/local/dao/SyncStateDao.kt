package com.kairo.player.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kairo.player.data.local.entity.SyncStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM sync_state WHERE sourceId = :sourceId LIMIT 1")
    fun getSyncState(sourceId: String): Flow<SyncStateEntity?>

    @Query("SELECT * FROM sync_state WHERE sourceId = :sourceId LIMIT 1")
    suspend fun getSyncStateOnce(sourceId: String): SyncStateEntity?

    @Upsert
    suspend fun upsertSyncState(entity: SyncStateEntity)
}