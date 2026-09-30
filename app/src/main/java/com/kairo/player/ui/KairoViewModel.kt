package com.kairo.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kairo.player.audio.AudioDiagnostics
import com.kairo.player.audio.AudioDiagnosticsSnapshot
import com.kairo.player.data.local.entity.PlaylistEntity
import com.kairo.player.data.local.entity.SyncStateEntity
import com.kairo.player.data.repository.HistoryRepository
import com.kairo.player.data.repository.LibraryRepository
import com.kairo.player.data.repository.PlaylistRepository
import com.kairo.player.data.repository.TrackRepository
import com.kairo.player.data.sync.SyncProgress
import com.kairo.player.domain.model.Album
import com.kairo.player.domain.model.Artist
import com.kairo.player.domain.model.Track
import com.kairo.player.playback.PlaybackConnectionState
import com.kairo.player.playback.PlaybackController
import com.kairo.player.playback.PlaybackQueueState
import com.kairo.player.playback.PlaybackState
import com.kairo.player.source.MusicSourceRegistry
import com.kairo.player.source.StreamResolver
import com.kairo.player.source.local.LocalMusicSource
import com.kairo.player.ui.components.PlaylistVisual
import com.kairo.player.ui.screens.SearchContentState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kairo.player.server.NavidromeApiService
import com.kairo.player.server.ServerConfig
import kotlinx.coroutines.flow.update

@HiltViewModel
class KairoViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val musicSources: MusicSourceRegistry,
    private val streamResolver: StreamResolver,
    private val trackRepository: TrackRepository,
    private val libraryRepository: LibraryRepository,
    private val historyRepository: HistoryRepository,
    private val playlistRepository: PlaylistRepository,
    private val audioDiagnostics: AudioDiagnostics,
    private val localMusicSource: LocalMusicSource,
    private val serverConfig: ServerConfig,
    private val navidromeApi: NavidromeApiService,
) : ViewModel() {
    private val mutableSearchState = MutableStateFlow(SearchUiState())
    private val mutableMessage = MutableStateFlow<String?>(null)
    private val mutableArtistDetail = MutableStateFlow(ArtistDetailUiState())
    private val mutableAlbumDetail = MutableStateFlow(AlbumDetailUiState())
    private var searchJob: Job? = null
    private var artistDetailJob: Job? = null
    private var albumDetailJob: Job? = null

    val searchState: StateFlow<SearchUiState> = mutableSearchState.asStateFlow()
    val message: StateFlow<String?> = mutableMessage.asStateFlow()
    val artistDetailState: StateFlow<ArtistDetailUiState> = mutableArtistDetail.asStateFlow()
    val albumDetailState: StateFlow<AlbumDetailUiState> = mutableAlbumDetail.asStateFlow()
    val syncState: StateFlow<SyncStateEntity?> = libraryRepository.syncState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )
    val syncProgress: StateFlow<SyncProgress> = libraryRepository.syncProgress.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SyncProgress.Idle,
    )
    val playbackState: StateFlow<PlaybackState> = playbackController.playbackState
    val connectionState: StateFlow<PlaybackConnectionState> = playbackController.connectionState
    val queueState: StateFlow<PlaybackQueueState> = playbackController.queueState
    val diagnostics: StateFlow<AudioDiagnosticsSnapshot> = audioDiagnostics.snapshots.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        audioDiagnostics.currentSnapshot(),
    )

    private val libraryData = combine(
        trackRepository.observeTracks(),
        libraryRepository.getAlbums(LibraryRepository.NAVIDROME_SOURCE_ID),
        libraryRepository.getArtists(LibraryRepository.NAVIDROME_SOURCE_ID),
        playlistRepository.observePlaylists(),
        historyRepository.observeRecent(),
    ) { tracks, albums, artists, playlists, history ->
        LibraryData(tracks, albums, artists, playlists, history)
    }

    val libraryState: StateFlow<LibraryUiState> = libraryData
        .map { data -> loadLibraryState(data) }
        .catch { exception ->
            emit(LibraryUiState(isLoading = false, error = exception.message ?: "Library could not be loaded."))
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            LibraryUiState(),
        )

    init {
        viewModelScope.launch {
            playbackController.playbackState
                .mapNotNull { it.currentTrack }
                .distinctUntilChangedBy { "${it.sourceId}:${it.sourceTrackId}" }
                .collect { playbackTrack ->
                    val sourceId = playbackTrack.sourceId ?: return@collect
                    val sourceTrackId = playbackTrack.sourceTrackId ?: return@collect
                    try {
                        val track = withContext(Dispatchers.IO) {
                            musicSources.getTrack(
                                Track(
                                    id = sourceTrackId,
                                    sourceId = sourceId,
                                    title = playbackTrack.title ?: "Unknown track",
                                ),
                            )
                        } ?: return@collect
                        historyRepository.recordPlayback(
                            track = track,
                            playedAtMs = System.currentTimeMillis(),
                            positionMs = playbackController.playbackState.value.currentPositionMs,
                        )
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (exception: Exception) {
                        mutableMessage.value = exception.message ?: "Playback history could not be saved."
                    }
                }
        }
    }

    fun setSearchQuery(query: String) {
        searchJob?.cancel()
        mutableSearchState.update {
            SearchUiState(query = query, status = SearchContentState.IDLE)
        }
    }

    fun search() {
        val query = mutableSearchState.value.query.trim()
        if (query.isEmpty()) {
            mutableSearchState.update { it.copy(status = SearchContentState.IDLE, tracks = emptyList(), playlists = emptyList()) }
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            mutableSearchState.update { it.copy(status = SearchContentState.LOADING, error = null) }
            try {
                val tracks = withContext(Dispatchers.IO) { musicSources.search(query) }
                    .distinctBy { "${it.sourceId}\u001f${it.id}" }
                val playlists = withContext(Dispatchers.IO) {
                    playlistRepository.observePlaylists().first()
                        .filter { it.name.contains(query, ignoreCase = true) }
                }
                mutableSearchState.update {
                    it.copy(
                        status = if (tracks.isEmpty() && playlists.isEmpty()) SearchContentState.EMPTY else SearchContentState.RESULTS,
                        tracks = tracks,
                        playlists = playlists.map { playlist ->
                            PlaylistVisual(playlist.id, playlist.name, playlist.trackKeys.size)
                        },
                        error = null,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                mutableSearchState.update {
                    it.copy(
                        status = SearchContentState.ERROR,
                        error = exception.message ?: "Search is temporarily unavailable.",
                    )
                }
            }
        }
    }

    fun triggerLibrarySync() {
        viewModelScope.launch {
            try {
                val result = libraryRepository.triggerSync()
                if (!result.succeeded) mutableMessage.value = result.error ?: "Library sync failed."
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                mutableMessage.value = exception.message ?: "Library sync failed."
            }
        }
    }

    fun loadArtistDetail(sourceId: String, artistId: String) {
        val current = mutableArtistDetail.value
        if (current.sourceId == sourceId && current.artistId == artistId &&
            (current.isLoading || current.artist != null || current.error != null)
        ) return
        artistDetailJob?.cancel()
        mutableArtistDetail.value = ArtistDetailUiState(sourceId = sourceId, artistId = artistId, isLoading = true)
        artistDetailJob = viewModelScope.launch {
            try {
                combine(
                    libraryRepository.getArtists(sourceId),
                    libraryRepository.getAlbums(sourceId),
                ) { artists, albums ->
                    artists.firstOrNull { it.id == artistId } to albums.filter { album ->
                        album.artists.any { it.sourceId == sourceId && it.id == artistId }
                    }
                }.collect { (artist, albums) ->
                    mutableArtistDetail.value = ArtistDetailUiState(
                        sourceId = sourceId,
                        artistId = artistId,
                        artist = artist,
                        albums = albums,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                mutableArtistDetail.value = ArtistDetailUiState(
                    sourceId = sourceId,
                    artistId = artistId,
                    error = exception.message ?: "Artist details could not be loaded.",
                )
            }
        }
    }

    fun loadAlbumDetail(sourceId: String, albumId: String) {
        val current = mutableAlbumDetail.value
        if (current.sourceId == sourceId && current.albumId == albumId &&
            (current.isLoading || current.album != null || current.error != null)
        ) return
        albumDetailJob?.cancel()
        mutableAlbumDetail.value = AlbumDetailUiState(sourceId = sourceId, albumId = albumId, isLoading = true)
        albumDetailJob = viewModelScope.launch {
            try {
                combine(
                    libraryRepository.getAlbums(sourceId),
                    libraryRepository.getTracksForAlbum(sourceId, albumId),
                ) { albums, tracks ->
                    albums.firstOrNull { it.id == albumId } to tracks.sortedWith(
                        compareBy<Track>({ it.discNumber ?: 0 }, { it.trackNumber ?: Int.MAX_VALUE }, { it.title }),
                    )
                }.collect { (album, tracks) ->
                    mutableAlbumDetail.value = AlbumDetailUiState(
                        sourceId = sourceId,
                        albumId = albumId,
                        album = album,
                        tracks = tracks,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                mutableAlbumDetail.value = AlbumDetailUiState(
                    sourceId = sourceId,
                    albumId = albumId,
                    error = exception.message ?: "Album details could not be loaded.",
                )
            }
        }
    }

    fun playTrack(track: Track) {
        if (track.sourceId != LocalMusicSource.SOURCE_ID) {
            playTracks(listOf(track), selectedIndex = 0)
            return
        }
        runPlaybackCommand {
            val localTracks = withContext(Dispatchers.IO) { localMusicSource.search("") }
            val selectedIndex = localTracks.indexOfFirst { it.id == track.id }
            check(selectedIndex >= 0) { "This local track is no longer available." }
            val resolved = withContext(Dispatchers.IO) {
                localTracks.mapIndexedNotNull { index, localTrack ->
                    streamResolver.resolveTrack(localTrack)?.let { index to it }
                }
            }
            val startIndex = resolved.indexOfFirst { it.first == selectedIndex }
            check(startIndex >= 0) { "No playable stream is available for this track." }
            playbackController.setResolvedQueue(resolved.map { it.second }, startIndex)
            playbackController.play()
        }
    }

    fun playTracks(tracks: List<Track>, selectedIndex: Int) {
        if (tracks.isEmpty() || selectedIndex !in tracks.indices) return
        runPlaybackCommand {
            val resolved = withContext(Dispatchers.IO) {
                tracks.mapIndexedNotNull { index, track ->
                    streamResolver.resolveTrack(track)?.let { index to it }
                }
            }
            val startIndex = resolved.indexOfFirst { it.first == selectedIndex }
            check(startIndex >= 0) { "No playable stream is available for this track." }
            playbackController.setResolvedQueue(resolved.map { it.second }, startIndex)
            playbackController.play()
        }
    }

    fun playAlbum(album: Album) {
        val tracks = libraryState.value.tracks.filter { it.album?.let { item -> item.sourceId == album.sourceId && item.id == album.id } == true }
        playTracks(tracks, 0)
    }

    fun playArtist(artist: Artist) {
        val tracks = libraryState.value.tracks.filter { track ->
            track.artists.any { it.sourceId == artist.sourceId && it.id == artist.id }
        }
        playTracks(tracks, 0)
    }

    fun playPlaylist(playlistId: String) {
        runPlaybackCommand {
            val tracks: List<Track> = withContext(Dispatchers.IO) {
                val playlist = playlistRepository.getPlaylist(playlistId) ?: return@withContext emptyList()
                buildList {
                    for (trackKey in playlist.trackKeys) {
                        getTrackForKey(trackKey)?.let(::add)
                    }
                }
            }
            check(tracks.isNotEmpty()) { "This playlist has no saved tracks." }
            val resolved = withContext(Dispatchers.IO) {
                tracks.mapNotNull { streamResolver.resolveTrack(it) }
            }
            check(resolved.isNotEmpty()) { "No playable streams are available for this playlist." }
            playbackController.setResolvedQueue(resolved)
            playbackController.play()
        }
    }

    fun togglePlayPause() = runPlaybackCommand {
        when (playbackController.playbackState.value) {
            is PlaybackState.Playing, is PlaybackState.Buffering -> playbackController.pause()
            else -> playbackController.play()
        }
    }

    fun previous() = runPlaybackCommand { playbackController.previous() }
    fun next() = runPlaybackCommand { playbackController.next() }
    fun seekTo(positionMs: Long) = runPlaybackCommand { playbackController.seekTo(positionMs) }
    fun setShuffleEnabled(enabled: Boolean) = runPlaybackCommand { playbackController.setShuffleEnabled(enabled) }
    fun cycleRepeatMode() = runPlaybackCommand { playbackController.cycleRepeatMode() }
    fun selectQueueItem(index: Int) = runPlaybackCommand { playbackController.selectQueueItem(index) }
    fun removeQueueItem(index: Int) = runPlaybackCommand { playbackController.removeFromQueue(index) }
    fun moveQueueItem(fromIndex: Int, toIndex: Int) = runPlaybackCommand { playbackController.moveQueueItem(fromIndex, toIndex) }
    fun clearQueue() = runPlaybackCommand { playbackController.clearQueue() }
    fun setDeviceVolume(volume: Int) = runPlaybackCommand { playbackController.setDeviceVolume(volume) }
    fun setDeviceMuted(muted: Boolean) = runPlaybackCommand { playbackController.setDeviceMuted(muted) }

    fun addLocalFolder(uri: String) {
        viewModelScope.launch {
            try {
                val importedCount = withContext(Dispatchers.IO) {
                    localMusicSource.addTree(uri)
                    val tracks = localMusicSource.search("")
                    tracks.forEach { trackRepository.saveTrack(it) }
                    tracks.size
                }
                mutableMessage.value = "Music folder added. $importedCount tracks added to your library."
            } catch (exception: Exception) {
                mutableMessage.value = exception.message ?: "The music folder could not be added."
            }
        }
    }

        val savedServerUrl: String get() = serverConfig.serverUrl
    val savedServerUser: String get() = serverConfig.username
    val savedServerPassword: String get() = serverConfig.password

    fun saveServerAndTest(url: String, user: String, password: String) {
        if (!url.trim().startsWith("http://") && !url.trim().startsWith("https://")) {
            mutableMessage.value = "Server URL must start with http:// or https://"
            return
        }
        serverConfig.serverUrl = url
        serverConfig.username = user
        serverConfig.password = password
        viewModelScope.launch {
            mutableMessage.value = try {
                val response = navidromeApi.ping().response
                if (response.isOk) {
                    triggerLibrarySync()
                    "Connected to Navidrome."
                } else {
                    "Login rejected: ${response.error?.message ?: "unknown error"}"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Could not reach server: ${e.message ?: "unknown error"}"
            }
        }
    }
    fun dismissMessage() {
        mutableMessage.value = null
    }

    private fun runPlaybackCommand(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                when (val connection = playbackController.connectionState.first { it !is PlaybackConnectionState.Connecting }) {
                    PlaybackConnectionState.Connected -> action()
                    is PlaybackConnectionState.Failed -> error(connection.message ?: "Playback service is unavailable.")
                    PlaybackConnectionState.Connecting -> Unit
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                mutableMessage.value = exception.message ?: "Playback action failed."
            }
        }
    }

    private suspend fun loadLibraryState(data: LibraryData): LibraryUiState = withContext(Dispatchers.IO) {
        val tracks = (data.tracks + localMusicSource.search(""))
            .distinctBy { "${it.sourceId}\u001f${it.id}" }
        val recentTracks = buildList<Track> {
            for (entry in data.history) {
                val track = getTrackForKey(entry.trackKey) ?: continue
                if (none { it.sourceId == track.sourceId && it.id == track.id }) add(track)
            }
        }
        LibraryUiState(
            isLoading = false,
            tracks = tracks,
            albums = (data.albums + tracks.mapNotNull { it.album })
                .distinctBy { "${it.sourceId}\u001f${it.id}" },
            artists = (data.artists + tracks.flatMap { it.artists })
                .distinctBy { "${it.sourceId}\u001f${it.id}" },
            playlists = data.playlists,
            recentlyPlayed = recentTracks,
        )
    }

    private suspend fun getTrackForKey(trackKey: String): Track? {
        val separator = trackKey.indexOf('\u001f')
        if (separator <= 0 || separator == trackKey.lastIndex) return null
        return trackRepository.getTrack(trackKey.substring(0, separator), trackKey.substring(separator + 1))
    }

    private data class LibraryData(
        val tracks: List<Track>,
        val albums: List<Album>,
        val artists: List<Artist>,
        val playlists: List<PlaylistEntity>,
        val history: List<com.kairo.player.data.local.entity.PlaybackHistoryEntity>,
    )
}

data class ArtistDetailUiState(
    val sourceId: String? = null,
    val artistId: String? = null,
    val artist: Artist? = null,
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class AlbumDetailUiState(
    val sourceId: String? = null,
    val albumId: String? = null,
    val album: Album? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class SearchUiState(
    val query: String = "",
    val status: SearchContentState = SearchContentState.IDLE,
    val tracks: List<Track> = emptyList(),
    val playlists: List<PlaylistVisual> = emptyList(),
    val error: String? = null,
)

data class LibraryUiState(
    val isLoading: Boolean = true,
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val recentlyPlayed: List<Track> = emptyList(),
    val error: String? = null,
)
