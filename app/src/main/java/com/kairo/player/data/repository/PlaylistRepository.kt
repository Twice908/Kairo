package com.kairo.player.data.repository

import com.kairo.player.data.local.dao.PlaylistDao
import com.kairo.player.data.local.entity.PlaylistEntity
import com.kairo.player.domain.model.Track
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val trackRepository: TrackRepository,
) {
    fun observePlaylists(): Flow<List<PlaylistEntity>> = playlistDao.observeAll().flowOn(Dispatchers.IO)

    suspend fun createPlaylist(
        id: String,
        name: String,
        createdAtMs: Long,
    ): PlaylistEntity = withContext(Dispatchers.IO) {
        require(id.isNotBlank()) { "Playlist ID must not be blank" }
        require(name.isNotBlank()) { "Playlist name must not be blank" }
        PlaylistEntity(id, name.trim(), emptyList(), createdAtMs, createdAtMs).also {
            playlistDao.upsert(it)
        }
    }

    suspend fun getPlaylist(id: String): PlaylistEntity? = withContext(Dispatchers.IO) {
        playlistDao.getById(id)
    }

    suspend fun addTrack(playlistId: String, track: Track, updatedAtMs: Long) = withContext(Dispatchers.IO) {
        trackRepository.saveTrack(track)
        playlistDao.addTrack(playlistId, trackKey(track), updatedAtMs)
    }

    suspend fun removeTrack(playlistId: String, track: Track, updatedAtMs: Long) = withContext(Dispatchers.IO) {
        playlistDao.removeTrack(playlistId, trackKey(track), updatedAtMs)
    }

    suspend fun deletePlaylist(id: String) = withContext(Dispatchers.IO) {
        playlistDao.delete(id)
    }

    private fun trackKey(track: Track) = "${track.sourceId}\u001f${track.id}"
}