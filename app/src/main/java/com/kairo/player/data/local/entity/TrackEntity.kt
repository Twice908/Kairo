package com.kairo.player.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracks",
    indices = [Index(value = ["sourceId", "id"], unique = true)],
)
data class TrackEntity(
    @PrimaryKey val key: String,
    val id: String,
    val sourceId: String,
    val title: String,
    val artistKeys: List<String>,
    val albumKey: String?,
    val durationMs: Long?,
)