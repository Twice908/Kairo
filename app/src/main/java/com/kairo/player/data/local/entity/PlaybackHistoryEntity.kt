package com.kairo.player.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playback_history",
    indices = [Index(value = ["playedAtMs"])],
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val entryId: Long = 0,
    val trackKey: String,
    val playedAtMs: Long,
    val positionMs: Long,
)