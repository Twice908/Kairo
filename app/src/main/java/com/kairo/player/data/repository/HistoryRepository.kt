package com.kairo.player.data.repository

import com.kairo.player.data.local.dao.PlaybackHistoryDao
import com.kairo.player.data.local.entity.PlaybackHistoryEntity
import com.kairo.player.domain.model.Track
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: PlaybackHistoryDao,
    private val trackRepository: TrackRepository,
) {
    fun observeRecent(): Flow<List<PlaybackHistoryEntity>> = historyDao.observeRecent().flowOn(Dispatchers.IO)

    fun observeForTrack(track: Track): Flow<List<PlaybackHistoryEntity>> =
        historyDao.observeForTrack(trackKey(track)).flowOn(Dispatchers.IO)

    suspend fun recordPlayback(
        track: Track,
        playedAtMs: Long,
        positionMs: Long,
    ): Long = withContext(Dispatchers.IO) {
        require(playedAtMs >= 0L) { "Played-at time must not be negative" }
        require(positionMs >= 0L) { "Playback position must not be negative" }
        trackRepository.saveTrack(track)
        historyDao.insert(
            PlaybackHistoryEntity(
                trackKey = trackKey(track),
                playedAtMs = playedAtMs,
                positionMs = positionMs,
            ),
        )
    }

    suspend fun clear() = withContext(Dispatchers.IO) { historyDao.clear() }

    private fun trackKey(track: Track) = "${track.sourceId}\u001f${track.id}"
}