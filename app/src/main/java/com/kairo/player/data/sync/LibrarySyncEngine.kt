package com.kairo.player.data.sync

import androidx.room.withTransaction
import com.kairo.player.data.local.KairoDatabase
import com.kairo.player.data.local.entity.AlbumEntity
import com.kairo.player.data.local.entity.ArtistEntity
import com.kairo.player.data.local.entity.SyncStateEntity
import com.kairo.player.data.local.entity.TrackEntity
import com.kairo.player.server.AlbumDto
import com.kairo.player.server.ArtistDto
import com.kairo.player.server.NavidromeApiService
import com.kairo.player.server.ServerConfig
import com.kairo.player.server.SongDto
import com.kairo.player.server.SubsonicEnvelope
import com.kairo.player.server.SubsonicResponse
import com.kairo.player.server.SubsonicUrlBuilder
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class LibrarySyncEngine @Inject constructor(
    private val api: NavidromeApiService,
    private val config: ServerConfig,
    private val database: KairoDatabase,
    private val urls: SubsonicUrlBuilder,
) {
    private val mutex = Mutex()
    private val mutableProgress = MutableStateFlow<SyncProgress>(SyncProgress.Idle)
    val progress: StateFlow<SyncProgress> = mutableProgress.asStateFlow()
    val isConfigured: Boolean get() = config.isConfigured

    suspend fun syncAll(): SyncResult = mutex.withLock {
        val startedAt = System.currentTimeMillis()
        val stateDao = database.syncStateDao()
        val previous = stateDao.getSyncStateOnce(SOURCE_ID)
        stateDao.upsertSyncState(
            SyncStateEntity(
                sourceId = SOURCE_ID,
                lastSyncStartedAt = startedAt,
                lastSyncCompletedAt = previous?.lastSyncCompletedAt,
                lastSyncStatus = "running",
                lastSyncError = null,
                artistCount = previous?.artistCount ?: 0,
                albumCount = previous?.albumCount ?: 0,
                trackCount = previous?.trackCount ?: 0,
            ),
        )
        var artistCount = 0
        var albumCount = 0
        var trackCount = 0
        val failures = mutableListOf<String>()
        mutableProgress.value = SyncProgress.Running(SyncPhase.Artists, 0, 0)

        try {
            check(config.isConfigured) { "Navidrome is not configured." }
            val artistDtos = api.getArtists().requireOk().artists?.index.orEmpty()
                .flatMap { it.artist }
                .distinctBy { it.id }
            mutableProgress.value = SyncProgress.Running(SyncPhase.Artists, 0, artistDtos.size)

            val artists = buildList {
                artistDtos.forEachIndexed { index, dto ->
                    val biography = fetchBiography(dto.id)
                    add(dto.toEntity(startedAt, biography))
                    mutableProgress.value = SyncProgress.Running(SyncPhase.Artists, index + 1, artistDtos.size)
                }
            }
            database.withTransaction { database.artistDao().upsertAll(artists) }
            artistCount = artists.size

            val albumDtos = mutableListOf<AlbumDto>()
            mutableProgress.value = SyncProgress.Running(SyncPhase.Albums, 0, artistDtos.size)
            artistDtos.forEachIndexed { index, artist ->
                try {
                    val artistDetails = api.getArtist(artist.id).requireOk().artist
                        ?: throw IOException("Artist response was empty.")
                    albumDtos += artistDetails.album
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (exception: Exception) {
                    recordFailure(failures, "${artist.name}: ${exception.message ?: "albums unavailable"}")
                }
                mutableProgress.value = SyncProgress.Running(SyncPhase.Albums, index + 1, artistDtos.size)
            }
            val uniqueAlbums = albumDtos.distinctBy { it.id }
            val albumEntities = uniqueAlbums.map { it.toEntity(startedAt) }
            val impliedArtists = uniqueAlbums.mapNotNull { it.toArtistEntity(startedAt) }
            database.withTransaction {
                database.artistDao().upsertAll((artists + impliedArtists).distinctBy { "${it.sourceId}:${it.remoteId}" })
                database.albumDao().upsertAll(albumEntities)
            }
            albumCount = albumEntities.size

            val detailedAlbums = mutableListOf<AlbumEntity>()
            val tracks = mutableListOf<TrackEntity>()
            mutableProgress.value = SyncProgress.Running(SyncPhase.Tracks, 0, uniqueAlbums.size)
            uniqueAlbums.forEachIndexed { index, listedAlbum ->
                try {
                    val details = api.getAlbum(listedAlbum.id).requireOk().album
                        ?: throw IOException("Album response was empty.")
                    val album = details.toEntity(startedAt)
                    detailedAlbums += album
                    details.song.forEach { song ->
                        song.toEntity(album, startedAt)?.let(tracks::add)
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (exception: Exception) {
                    recordFailure(failures, "${listedAlbum.name}: ${exception.message ?: "tracks unavailable"}")
                }
                mutableProgress.value = SyncProgress.Running(SyncPhase.Tracks, index + 1, uniqueAlbums.size)
            }
            database.withTransaction {
                database.albumDao().upsertAll(detailedAlbums.distinctBy { it.remoteId })
                database.trackDao().upsertAll(tracks.distinctBy { it.remoteId })
            }
            trackCount = tracks.distinctBy { it.remoteId }.size

            val finishedAt = System.currentTimeMillis()
            val status = if (failures.isEmpty()) "success" else "partial"
            val message = failures.take(MAX_FAILURE_MESSAGES).joinToString("\n").ifBlank { null }
            database.withTransaction {
                stateDao.upsertSyncState(
                    SyncStateEntity(
                        sourceId = SOURCE_ID,
                        lastSyncStartedAt = startedAt,
                        lastSyncCompletedAt = finishedAt,
                        lastSyncStatus = status,
                        lastSyncError = message,
                        artistCount = artistCount,
                        albumCount = albumCount,
                        trackCount = trackCount,
                    ),
                )
            }
            mutableProgress.value = SyncProgress.Completed(finishedAt, artistCount, albumCount, trackCount)
            SyncResult(artistCount, albumCount, trackCount, status, message)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            val finishedAt = System.currentTimeMillis()
            val reason = exception.message ?: "Library sync failed."
            stateDao.upsertSyncState(
                SyncStateEntity(
                    sourceId = SOURCE_ID,
                    lastSyncStartedAt = startedAt,
                    lastSyncCompletedAt = finishedAt,
                    lastSyncStatus = "failed",
                    lastSyncError = reason,
                    artistCount = artistCount,
                    albumCount = albumCount,
                    trackCount = trackCount,
                ),
            )
            mutableProgress.value = SyncProgress.Failed(reason)
            SyncResult(artistCount, albumCount, trackCount, "failed", reason)
        }
    }

    private suspend fun fetchBiography(artistId: String): String? = try {
        api.getArtistInfo2(artistId).requireOk().artistInfo2?.biography?.takeIf(String::isNotBlank)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        null
    }

    private fun recordFailure(failures: MutableList<String>, message: String) {
        if (failures.size < MAX_FAILURE_MESSAGES) failures += message
    }

    private fun ArtistDto.toEntity(syncedAt: Long, biography: String? = null) = ArtistEntity(
        key = entityKey(id),
        id = id,
        sourceId = SOURCE_ID,
        name = name,
        remoteId = id,
        lastSyncedAt = syncedAt,
        artworkUri = coverArt?.let(urls::coverArtUrl),
        biography = biography,
    )

    private fun AlbumDto.toEntity(syncedAt: Long) = AlbumEntity(
        key = entityKey(id),
        id = id,
        sourceId = SOURCE_ID,
        title = name,
        artistKeys = listOfNotNull(artist?.let { entityKey(artistId ?: it) }),
        artworkUri = coverArt?.let(urls::coverArtUrl),
        artworkMimeType = null,
        artworkData = null,
        releaseYear = year,
        remoteId = id,
        lastSyncedAt = syncedAt,
    )

    private fun AlbumDto.toArtistEntity(syncedAt: Long): ArtistEntity? {
        val artistName = artist ?: return null
        val remoteId = artistId ?: artistName
        return ArtistEntity(
            key = entityKey(remoteId),
            id = remoteId,
            sourceId = SOURCE_ID,
            name = artistName,
            remoteId = remoteId,
            lastSyncedAt = syncedAt,
        )
    }

    private fun SongDto.toEntity(album: AlbumEntity, syncedAt: Long): TrackEntity? {
        val artistName = artist
        val artistIdValue = artistId ?: artistName
        val artistKeys = listOfNotNull(artistIdValue?.let(::entityKey))
        return TrackEntity(
            key = entityKey(id),
            id = id,
            sourceId = SOURCE_ID,
            title = title,
            artistKeys = artistKeys,
            albumKey = album.key,
            durationMs = duration?.times(1_000L),
            remoteId = id,
            lastSyncedAt = syncedAt,
            artistNames = listOfNotNull(artistName),
            trackNumber = track,
            discNumber = discNumber,
            year = year,
            genre = genre,
            coverArtId = coverArt,
        )
    }

    private fun SubsonicEnvelope.requireOk(): SubsonicResponse {
        if (!response.isOk) throw IOException(response.error?.message ?: "Subsonic request failed")
        return response
    }

    private fun entityKey(remoteId: String) = "$SOURCE_ID$KEY_SEPARATOR$remoteId"

    companion object {
        const val SOURCE_ID = "navidrome"
        private const val KEY_SEPARATOR = '\u001f'
        private const val MAX_FAILURE_MESSAGES = 20
    }
}

enum class SyncPhase { Artists, Albums, Tracks }

sealed interface SyncProgress {
    data object Idle : SyncProgress
    data class Running(val phase: SyncPhase, val progress: Int, val total: Int) : SyncProgress
    data class Completed(val timestamp: Long, val artistsCount: Int, val albumsCount: Int, val tracksCount: Int) : SyncProgress
    data class Failed(val reason: String) : SyncProgress
}

data class SyncResult(
    val artistsCount: Int,
    val albumsCount: Int,
    val tracksCount: Int,
    val status: String,
    val error: String?,
) {
    val succeeded: Boolean get() = status != "failed"
}
