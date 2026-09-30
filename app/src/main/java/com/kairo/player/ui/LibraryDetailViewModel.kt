package com.kairo.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kairo.player.data.local.entity.AlbumEntity
import com.kairo.player.data.local.entity.ArtistEntity
import com.kairo.player.data.local.entity.TrackEntity
import com.kairo.player.data.repository.LibraryRepository
import com.kairo.player.playback.PlaybackConnectionState
import com.kairo.player.playback.PlaybackController
import com.kairo.player.playback.Track as PlaybackTrack
import com.kairo.player.source.StreamResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryDetailViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val streamResolver: StreamResolver,
    private val playbackController: PlaybackController,
) : ViewModel() {
    private val mutableArtist = MutableStateFlow<ArtistEntity?>(null)
    val artist: StateFlow<ArtistEntity?> = mutableArtist.asStateFlow()

    private val mutableAlbums = MutableStateFlow<List<AlbumEntity>>(emptyList())
    val albums: StateFlow<List<AlbumEntity>> = mutableAlbums.asStateFlow()

    private val mutableAlbum = MutableStateFlow<AlbumEntity?>(null)
    val album: StateFlow<AlbumEntity?> = mutableAlbum.asStateFlow()

    private val mutableTracks = MutableStateFlow<List<TrackEntity>>(emptyList())
    val tracks: StateFlow<List<TrackEntity>> = mutableTracks.asStateFlow()

    private val mutableAlbumArtist = MutableStateFlow<ArtistEntity?>(null)
    val albumArtist: StateFlow<ArtistEntity?> = mutableAlbumArtist.asStateFlow()

    private var artistJob: Job? = null
    private var albumJob: Job? = null

    fun loadArtist(artistId: String) {
        artistJob?.cancel()
        artistJob = viewModelScope.launch {
            combine(
                libraryRepository.observeArtist(artistId),
                libraryRepository.observeAlbumsForArtist(artistId),
            ) { artistEntity, artistAlbums ->
                artistEntity to artistAlbums
            }.collect { (artistEntity, artistAlbums) ->
                mutableArtist.value = artistEntity
                mutableAlbums.value = artistAlbums
            }
        }
    }

    fun loadAlbum(albumId: String) {
        albumJob?.cancel()
        albumJob = viewModelScope.launch {
            combine(
                libraryRepository.observeAlbum(albumId),
                libraryRepository.observeTracksForAlbum(albumId),
                libraryRepository.observeArtistForAlbum(albumId),
            ) { albumEntity, albumTracks, artistEntity ->
                Triple(albumEntity, albumTracks, artistEntity)
            }.collect { (albumEntity, albumTracks, artistEntity) ->
                mutableAlbum.value = albumEntity
                mutableTracks.value = albumTracks
                mutableAlbumArtist.value = artistEntity
            }
        }
    }

    fun playAlbum(tracks: List<TrackEntity>) {
        if (tracks.isEmpty()) return
        val connection = playbackController.connectionState.value
        if (connection is PlaybackConnectionState.Failed || connection is PlaybackConnectionState.Connecting) return

        val queue = tracks.map { entity ->
            PlaybackTrack(
                id = entity.remoteId.ifBlank { entity.id },
                uri = "content://kairo/${entity.sourceId}/${entity.id}",
                title = entity.title,
                artist = entity.artistNames.joinToString(),
                sourceId = entity.sourceId,
                sourceTrackId = entity.id,
            )
        }
        playbackController.setQueue(queue)
        playbackController.play()
    }
}
