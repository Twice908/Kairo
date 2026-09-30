package com.kairo.player.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(
    tableName = "artists",
    indices = [Index(value = ["sourceId", "remoteId"], unique = true)],
)
data class ArtistEntity(
    @PrimaryKey val key: String,
    val id: String,
    val sourceId: String,
    val name: String,
    @ColumnInfo(defaultValue = "''") val remoteId: String = id,
    @ColumnInfo(defaultValue = "0") val lastSyncedAt: Long = 0L,
    val artworkUri: String? = null,
    val biography: String? = null,
)