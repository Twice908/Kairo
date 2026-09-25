package com.kairo.player.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "albums",
    indices = [Index(value = ["sourceId", "id"], unique = true)],
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
)