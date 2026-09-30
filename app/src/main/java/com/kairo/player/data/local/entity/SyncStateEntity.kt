package com.kairo.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val sourceId: String,
    val lastSyncStartedAt: Long?,
    val lastSyncCompletedAt: Long?,
    val lastSyncStatus: String,
    val lastSyncError: String?,
    val artistCount: Int,
    val albumCount: Int,
    val trackCount: Int,
)