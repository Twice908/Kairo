package com.kairo.player.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "artists",
    indices = [Index(value = ["sourceId", "id"], unique = true)],
)
data class ArtistEntity(
    @PrimaryKey val key: String,
    val id: String,
    val sourceId: String,
    val name: String,
)