package com.kairo.player.data.sync

import androidx.room.Room
import com.kairo.player.data.local.KairoDatabase
import com.kairo.player.server.AlbumDto
import com.kairo.player.server.ArtistInfoDto
import com.kairo.player.server.ArtistDto
import com.kairo.player.server.ArtistIndexDto
import com.kairo.player.server.ArtistsDto
import com.kairo.player.server.NavidromeApiService
import com.kairo.player.server.ScanStatusDto
import com.kairo.player.server.SearchResultDto
import com.kairo.player.server.ServerConfig
import com.kairo.player.server.SongDto
import com.kairo.player.server.SubsonicEnvelope
import com.kairo.player.server.SubsonicResponse
import com.kairo.player.server.SubsonicUrlBuilder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LibrarySyncEngineTest {
    private lateinit var database: KairoDatabase
    private lateinit var serverConfig: ServerConfig

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            KairoDatabase::class.java,
        ).allowMainThreadQueries().build()
        serverConfig = ServerConfig(RuntimeEnvironment.getApplication()).apply {
            serverUrl = "http://100.64.0.1:4533"
            username = "kairo-test"
            password = "test-password"
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun continuesAfterArtistFailureAndPersistsPartialCatalog() = runBlocking {
        val album = AlbumDto(
            id = "album-1",
            name = "Album One",
            artist = "Artist One",
            artistId = "artist-1",
            coverArt = "cover-1",
            song = listOf(
                SongDto(
                    id = "track-1",
                    title = "Track One",
                    album = "Album One",
                    albumId = "album-1",
                    artist = "Artist One",
                    artistId = "artist-1",
                    track = 1,
                    discNumber = 1,
                    year = 2024,
                    duration = 180,
                    coverArt = "cover-1",
                ),
            ),
        )
        val api = object : NavidromeApiService {
            override suspend fun ping() = envelope()
            override suspend fun getArtists() = envelope(
                SubsonicResponse(
                    status = "ok",
                    artists = ArtistsDto(
                        index = listOf(
                            ArtistIndexDto("A", listOf(
                                ArtistDto("artist-1", "Artist One", coverArt = "artist-cover"),
                                ArtistDto("artist-2", "Artist Two"),
                            )),
                        ),
                    ),
                ),
            )

            override suspend fun getArtist(id: String): SubsonicEnvelope {
                if (id == "artist-2") throw IOException("temporary artist failure")
                return envelope(SubsonicResponse(status = "ok", artist = ArtistDto("artist-1", "Artist One", album = listOf(album))))
            }

            override suspend fun getArtistInfo2(id: String) = envelope(
                SubsonicResponse(status = "ok", artistInfo2 = ArtistInfoDto("Artist biography")),
            )
            override suspend fun getAlbum(id: String) = envelope(SubsonicResponse(status = "ok", album = album))
            override suspend fun getSong(id: String) = envelope()
            override suspend fun search3(query: String, artistCount: Int, albumCount: Int, songCount: Int) =
                envelope(SubsonicResponse(status = "ok", searchResult3 = SearchResultDto()))
            override suspend fun getAlbumList2(type: String, size: Int, offset: Int) = envelope()
            override suspend fun startScan() = envelope()
            override suspend fun getScanStatus() = envelope(SubsonicResponse(status = "ok", scanStatus = ScanStatusDto()))
        }
        val engine = LibrarySyncEngine(api, serverConfig, database, SubsonicUrlBuilder(serverConfig))

        val result = engine.syncAll()

        assertEquals("partial", result.status)
        assertEquals(2, result.artistsCount)
        assertEquals(1, result.albumsCount)
        assertEquals(1, result.tracksCount)
        assertTrue(result.error.orEmpty().contains("Artist Two"))
        assertEquals("track-1", database.trackDao().getByKey("navidrome\u001ftrack-1")?.remoteId)
        assertEquals(1, database.trackDao().getByKey("navidrome\u001ftrack-1")?.trackNumber)
        assertNotNull(database.albumDao().getByRemoteId("navidrome", "album-1"))
        val savedArtist = database.artistDao().getByRemoteId("navidrome", "artist-1")
        assertTrue(savedArtist?.artworkUri.orEmpty().contains("getCoverArt"))
        assertEquals("Artist biography", savedArtist?.biography)
        assertEquals("partial", database.syncStateDao().getSyncStateOnce("navidrome")?.lastSyncStatus)
        assertTrue(engine.progress.value is SyncProgress.Completed)
    }

    private fun envelope(response: SubsonicResponse = SubsonicResponse(status = "ok")) =
        SubsonicEnvelope(response)
}
