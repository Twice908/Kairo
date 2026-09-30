package com.kairo.player.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(
    tableName = "albums",
    indices = [Index(value = ["sourceId", "remoteId"], unique = true)],
)
data class AlbumEntity(
    @PrimaryKey val key: String,
    val id: String,
    val sourceId: String,
    val title: String,
    val artistKeys: List<String>,
    val artworkUri: String?,
    val artworkMimeType: String?,
    val artworkData: ByteArray?,
    val releaseYear: Int?,
    @ColumnInfo(defaultValue = "''") val remoteId: String = id,
    @ColumnInfo(defaultValue = "0") val lastSyncedAt: Long = 0L,
)