package com.kairo.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kairo.player.data.local.entity.PlaybackHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Insert
    suspend fun insert(entry: PlaybackHistoryEntity): Long

    @Query("SELECT * FROM playback_history ORDER BY playedAtMs DESC, entryId DESC")
    fun observeRecent(): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE trackKey = :trackKey ORDER BY playedAtMs DESC")
    fun observeForTrack(trackKey: String): Flow<List<PlaybackHistoryEntity>>

    @Query("DELETE FROM playback_history")
    suspend fun clear()
}