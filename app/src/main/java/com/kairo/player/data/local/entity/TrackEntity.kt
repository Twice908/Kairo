package com.kairo.player.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(
    tableName = "tracks",
    indices = [Index(value = ["sourceId", "remoteId"], unique = true)],
)
data class TrackEntity(
    @PrimaryKey val key: String,
    val id: String,
    val sourceId: String,
    val title: String,
    val artistKeys: List<String>,
    val albumKey: String?,
    val durationMs: Long?,
    @ColumnInfo(defaultValue = "''") val remoteId: String = id,
    @ColumnInfo(defaultValue = "0") val lastSyncedAt: Long = 0L,
    @ColumnInfo(defaultValue = "'[]'") val artistNames: List<String> = emptyList(),
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val coverArtId: String? = null,
)