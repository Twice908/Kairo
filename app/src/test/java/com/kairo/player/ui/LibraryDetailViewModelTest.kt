package com.kairo.player.ui

import com.kairo.player.data.local.entity.AlbumEntity
import com.kairo.player.data.local.entity.ArtistEntity
import com.kairo.player.data.local.entity.TrackEntity
import com.kairo.player.data.repository.LibraryRepository
import com.kairo.player.playback.PlaybackConnectionState
import com.kairo.player.playback.PlaybackController
import com.kairo.player.source.StreamResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.eq
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LibraryDetailViewModelTest {
    private lateinit var libraryRepository: LibraryRepository
    private lateinit var streamResolver: StreamResolver
    private lateinit var playbackController: PlaybackController
    private lateinit var viewModel: LibraryDetailViewModel

    @Before
    fun setUp() = runBlocking {
        libraryRepository = mock(LibraryRepository::class.java)
        streamResolver = mock(StreamResolver::class.java)
        playbackController = mock(PlaybackController::class.java)

        val connectionState = MutableStateFlow(PlaybackConnectionState.Connected)
        doReturn(connectionState).`when`(playbackController).connectionState

        val artist = ArtistEntity(
            key = "navidrome\u001fartist-1",
            id = "artist-1",
            sourceId = "navidrome",
            name = "Artist Uno",
            remoteId = "artist-1",
            artworkUri = "https://example.com/artist.jpg",
            biography = "Bio",
        )
        val album = AlbumEntity(
            key = "navidrome\u001falbum-1",
            id = "album-1",
            sourceId = "navidrome",
            title = "Album One",
            artistKeys = listOf("navidrome\u001fartist-1"),
            artworkUri = "https://example.com/cover.jpg",
            artworkMimeType = "image/jpeg",
            artworkData = byteArrayOf(1, 2, 3),
            releaseYear = 2024,
            remoteId = "album-1",
        )
        val track = TrackEntity(
            key = "navidrome\u001ftrack-1",
            id = "track-1",
            sourceId = "navidrome",
            title = "Track One",
            artistKeys = listOf("navidrome\u001fartist-1"),
            albumKey = "navidrome\u001falbum-1",
            durationMs = 90_000L,
            remoteId = "track-1",
            trackNumber = 1,
            artistNames = listOf("Artist Uno"),
            coverArtId = "cover-1",
        )

        doReturn(flowOf(artist)).`when`(libraryRepository).observeArtist("artist-1")
        doReturn(flowOf(listOf(album))).`when`(libraryRepository).observeAlbumsForArtist("artist-1")
        doReturn(flowOf(album)).`when`(libraryRepository).observeAlbum("album-1")
        doReturn(flowOf(listOf(track))).`when`(libraryRepository).observeTracksForAlbum("album-1")
        doReturn(flowOf(artist)).`when`(libraryRepository).observeArtistForAlbum("album-1")

        viewModel = LibraryDetailViewModel(libraryRepository, streamResolver, playbackController)
    }

    @Test
    fun loadArtist_emitsCorrectArtistAndAlbums() = runBlocking {
        viewModel.loadArtist("artist-1")

        val artist = viewModel.artist.first()
        val albums = viewModel.albums.first()

        assertNotNull(artist)
        assertEquals("Artist Uno", artist?.name)
        assertEquals(1, albums.size)
        assertEquals("Album One", albums.first().title)
    }

    @Test
    fun loadAlbum_emitsCorrectAlbumTracksAndArtist() = runBlocking {
        viewModel.loadAlbum("album-1")

        val album = viewModel.album.first()
        val tracks = viewModel.tracks.first()
        val albumArtist = viewModel.albumArtist.first()

        assertNotNull(album)
        assertEquals("Album One", album?.title)
        assertEquals(1, tracks.size)
        assertEquals("Track One", tracks.first().title)
        assertEquals("Artist Uno", albumArtist?.name)
    }

    @Test
    fun playAlbum_handlesEmptyListWithoutCrashing() = runBlocking {
        viewModel.playAlbum(emptyList())
        verify(playbackController, never()).setQueue(anyList(), eq(0), eq(0L))
    }

    @Test
    fun playAlbum_callsPlaybackControllerWithMappedTracks() = runBlocking {
        val track = TrackEntity(
            key = "navidrome\u001ftrack-1",
            id = "track-1",
            sourceId = "navidrome",
            title = "Track One",
            artistKeys = listOf("navidrome\u001fartist-1"),
            albumKey = "navidrome\u001falbum-1",
            durationMs = 90_000L,
            remoteId = "track-1",
            trackNumber = 1,
            artistNames = listOf("Artist Uno"),
            coverArtId = "cover-1",
        )
        viewModel.playAlbum(listOf(track))
        verify(playbackController).setQueue(anyList(), eq(0), eq(0L))
        verify(playbackController).play()
    }
}
