package com.kairo.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val trackKeys: List<String>,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)