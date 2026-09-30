package com.kairo.player.data.repository

import com.kairo.player.data.local.KairoDatabase
import com.kairo.player.data.local.entity.AlbumEntity
import com.kairo.player.data.local.entity.ArtistEntity
import com.kairo.player.data.local.entity.SyncStateEntity
import com.kairo.player.data.local.entity.TrackEntity
import com.kairo.player.domain.model.Album
import com.kairo.player.domain.model.Artist
import com.kairo.player.domain.model.MusicLibrary
import com.kairo.player.domain.model.Track
import com.kairo.player.data.sync.LibrarySyncEngine
import com.kairo.player.data.sync.SyncProgress
import com.kairo.player.data.sync.SyncResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@Singleton
class LibraryRepository @Inject constructor(
    private val database: KairoDatabase,
    private val trackRepository: TrackRepository,
    private val syncEngine: LibrarySyncEngine,
) {
    private val artistDao = database.artistDao()
    private val albumDao = database.albumDao()
    private val trackDao = database.trackDao()
    private val syncStateDao = database.syncStateDao()

    val syncState: Flow<SyncStateEntity?> = syncStateDao.getSyncState(NAVIDROME_SOURCE_ID)
    val syncProgress: Flow<SyncProgress> = syncEngine.progress

    fun getTracks(sourceId: String): Flow<List<Track>> = trackRepository.observeTracks(sourceId)

    fun getAlbums(sourceId: String): Flow<List<Album>> = albumDao.observeBySource(sourceId)
        .map { entities -> entities.map { trackRepository.mapAlbum(it) } }
        .flowOn(Dispatchers.IO)

    fun getArtists(sourceId: String): Flow<List<Artist>> = artistDao.observeBySource(sourceId)
        .map { entities -> entities.map(trackRepository::mapArtist) }
        .flowOn(Dispatchers.IO)

    fun getTracksForAlbum(sourceId: String, albumRemoteId: String): Flow<List<Track>> =
        trackRepository.observeTracksForAlbum(sourceId, albumRemoteId)

    fun observeArtist(artistId: String): Flow<ArtistEntity?> =
        artistDao.observeArtist(NAVIDROME_SOURCE_ID, artistId)

    fun observeAlbumsForArtist(artistId: String): Flow<List<AlbumEntity>> =
        albumDao.observeBySource(NAVIDROME_SOURCE_ID)
            .map { albums ->
                albums.filter { album ->
                    album.artistKeys.any { it == "$NAVIDROME_SOURCE_ID\u001f$artistId" }
                }
            }

    fun observeAlbum(albumId: String): Flow<AlbumEntity?> =
        albumDao.observeAlbum(NAVIDROME_SOURCE_ID, albumId)

    fun observeTracksForAlbum(albumId: String): Flow<List<TrackEntity>> =
        trackDao.observeBySource(NAVIDROME_SOURCE_ID)
            .map { tracks -> tracks.filter { it.albumKey == "$NAVIDROME_SOURCE_ID\u001f$albumId" } }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeArtistForAlbum(albumId: String): Flow<ArtistEntity?> =
        albumDao.observeAlbum(NAVIDROME_SOURCE_ID, albumId)
            .flatMapLatest { album ->
                val artistKey = album?.artistKeys?.firstOrNull() ?: return@flatMapLatest flowOf(null)
                artistDao.observeArtistByKey(artistKey)
            }

    fun observeLibrary(sourceId: String): Flow<MusicLibrary> = combine(
        getTracks(sourceId),
        getAlbums(sourceId),
        getArtists(sourceId),
    ) { tracks, albums, artists -> MusicLibrary(tracks, albums, artists) }

    suspend fun searchTracks(sourceId: String, query: String): List<Track> =
        trackRepository.search(sourceId, query)

    suspend fun getTrack(sourceId: String, remoteId: String): Track? =
        trackRepository.getTrack(sourceId, remoteId)

    suspend fun getArtist(sourceId: String, remoteId: String): Artist? =
        trackRepository.getArtist(sourceId, remoteId)

    suspend fun getAlbum(sourceId: String, remoteId: String): Album? =
        trackRepository.getAlbum(sourceId, remoteId)

    suspend fun getAlbumTracksOnce(sourceId: String, albumRemoteId: String): List<Track> =
        getTracksForAlbum(sourceId, albumRemoteId).first()

    suspend fun getArtistAlbumsOnce(sourceId: String, artistRemoteId: String): List<Album> =
        getAlbums(sourceId).first().filter { album ->
            album.artists.any { it.sourceId == sourceId && it.id == artistRemoteId }
        }

    suspend fun triggerSync(): SyncResult = syncEngine.syncAll()

    suspend fun resyncIfStale(maxAgeMs: Long = DEFAULT_MAX_AGE_MS) {
        if (!syncEngine.isConfigured) return
        val state = syncState.first()
        val completedAt = state?.lastSyncCompletedAt
        val isFresh = state?.lastSyncStatus == "success" && completedAt != null &&
            System.currentTimeMillis() - completedAt < maxAgeMs
        if (!isFresh) syncEngine.syncAll()
    }

    companion object {
        const val NAVIDROME_SOURCE_ID = "navidrome"
        const val DEFAULT_MAX_AGE_MS = 24 * 60 * 60 * 1000L
    }
}
