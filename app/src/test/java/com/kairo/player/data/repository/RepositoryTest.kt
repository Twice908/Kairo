package com.kairo.player.data.repository

import androidx.room.Room
import com.kairo.player.data.local.KairoDatabase
import com.kairo.player.domain.model.Album
import com.kairo.player.domain.model.AlbumArt
import com.kairo.player.domain.model.Artist
import com.kairo.player.domain.model.Track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RepositoryTest {
    private lateinit var database: KairoDatabase
    private lateinit var trackRepository: TrackRepository
    private lateinit var playlistRepository: PlaylistRepository
    private lateinit var historyRepository: HistoryRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            KairoDatabase::class.java,
        ).allowMainThreadQueries().build()
        trackRepository = TrackRepository(database)
        playlistRepository = PlaylistRepository(database.playlistDao(), trackRepository)
        historyRepository = HistoryRepository(database.playbackHistoryDao(), trackRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun trackRepositoryRoundTripsNestedMetadata() = runBlocking {
        val artist = Artist("artist-1", "local", "Artist")
        val album = Album(
            id = "album-1",
            sourceId = "local",
            title = "Album",
            artists = listOf(artist),
            artwork = AlbumArt("content://art/1", "image/jpeg", byteArrayOf(1, 2, 3)),
            releaseYear = 2022,
        )
        val track = Track("track-1", "local", "Track", listOf(artist), album, 80_000L)

        trackRepository.saveTrack(track)

        val loaded = requireNotNull(trackRepository.getTrack("local", "track-1"))
        assertEquals(track.copy(album = album.copy(artwork = album.artwork?.copy(data = null))), loaded.copy(album = loaded.album?.copy(artwork = loaded.album.artwork?.copy(data = null))))
        assertArrayEquals(album.artwork?.data, loaded.album?.artwork?.data)
        assertEquals(track, trackRepository.observeTracks().first().single().copy(album = album))
    }

    @Test
    fun playlistRepositoryPreservesOrderAndAvoidsDuplicates() = runBlocking {
        val track = Track("track-1", "local", "Track")
        playlistRepository.createPlaylist("favorites", " Favorites ", 100L)
        playlistRepository.addTrack("favorites", track, 200L)
        playlistRepository.addTrack("favorites", track, 300L)

        val playlist = requireNotNull(playlistRepository.getPlaylist("favorites"))
        assertEquals("Favorites", playlist.name)
        assertEquals(listOf("local\u001ftrack-1"), playlist.trackKeys)
        assertEquals(200L, playlist.updatedAtMs)

        playlistRepository.removeTrack("favorites", track, 400L)
        assertEquals(emptyList<String>(), playlistRepository.getPlaylist("favorites")?.trackKeys)
    }

    @Test
    fun historyRepositoryPersistsTrackAndPlaybackPosition() = runBlocking {
        val track = Track("track-2", "local", "History track")

        val entryId = historyRepository.recordPlayback(track, playedAtMs = 500L, positionMs = 12_000L)

        assertNotNull(trackRepository.getTrack("local", track.id))
        val history = historyRepository.observeRecent().first().single()
        assertEquals(entryId, history.entryId)
        assertEquals("local\u001ftrack-2", history.trackKey)
        assertEquals(500L, history.playedAtMs)
        assertEquals(12_000L, history.positionMs)
    }
}