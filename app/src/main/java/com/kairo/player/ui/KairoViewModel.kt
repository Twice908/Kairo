package com.kairo.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kairo.player.audio.AudioDiagnostics
import com.kairo.player.audio.AudioDiagnosticsSnapshot
import com.kairo.player.data.local.entity.PlaylistEntity
import com.kairo.player.data.repository.HistoryRepository
import com.kairo.player.data.repository.PlaylistRepository
import com.kairo.player.data.repository.TrackRepository
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
import kotlinx.coroutines.flow.update

@HiltViewModel
class KairoViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val musicSources: MusicSourceRegistry,
    private val streamResolver: StreamResolver,
    private val trackRepository: TrackRepository,
    private val historyRepository: HistoryRepository,
    private val playlistRepository: PlaylistRepository,
    private val audioDiagnostics: AudioDiagnostics,
    private val localMusicSource: LocalMusicSource,
) : ViewModel() {
    private val mutableSearchState = MutableStateFlow(SearchUiState())
    private val mutableMessage = MutableStateFlow<String?>(null)
    private var searchJob: Job? = null

    val searchState: StateFlow<SearchUiState> = mutableSearchState.asStateFlow()
    val message: StateFlow<String?> = mutableMessage.asStateFlow()
    val playbackState: StateFlow<PlaybackState> = playbackController.playbackState
    val connectionState: StateFlow<PlaybackConnectionState> = playbackController.connectionState
    val queueState: StateFlow<PlaybackQueueState> = playbackController.queueState
    val diagnostics: StateFlow<AudioDiagnosticsSnapshot> = audioDiagnostics.snapshots.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        audioDiagnostics.currentSnapshot(),
    )

    val libraryState: StateFlow<LibraryUiState> = combine(
        trackRepository.observeTracks(),
        playlistRepository.observePlaylists(),
        historyRepository.observeRecent(),
    ) { tracks, playlists, history -> LibraryData(tracks, playlists, history) }
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
            albums = tracks.mapNotNull { it.album }.distinctBy { "${it.sourceId}\u001f${it.id}" },
            artists = tracks.flatMap { it.artists }.distinctBy { "${it.sourceId}\u001f${it.id}" },
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
        val playlists: List<PlaylistEntity>,
        val history: List<com.kairo.player.data.local.entity.PlaybackHistoryEntity>,
    )
}

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
