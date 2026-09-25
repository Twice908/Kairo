package com.kairo.player.data.local

import androidx.room.Room
import com.kairo.player.data.local.entity.AlbumEntity
import com.kairo.player.data.local.entity.ArtistEntity
import com.kairo.player.data.local.entity.PlaybackHistoryEntity
import com.kairo.player.data.local.entity.PlaylistEntity
import com.kairo.player.data.local.entity.TrackEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class KairoDatabaseTest {
    private lateinit var database: KairoDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            KairoDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun persistsAllMetadataHistoryAndPlaylistEntities() = runBlocking {
        val artist = ArtistEntity("local\u001fartist-1", "artist-1", "local", "Artist")
        val album = AlbumEntity(
            key = "local\u001falbum-1",
            id = "album-1",
            sourceId = "local",
            title = "Album",
            artistKeys = listOf(artist.key),
            artworkUri = "content://art/1",
            artworkMimeType = "image/jpeg",
            artworkData = byteArrayOf(1, 2),
            releaseYear = 2024,
        )
        val track = TrackEntity(
            key = "local\u001ftrack-1",
            id = "track-1",
            sourceId = "local",
            title = "Track",
            artistKeys = listOf(artist.key),
            albumKey = album.key,
            durationMs = 90_000,
        )
        val playlist = PlaylistEntity("playlist-1", "Favorites", listOf(track.key), 10L, 10L)

        database.artistDao().upsertAll(listOf(artist))
        database.albumDao().upsert(album)
        database.trackDao().upsert(track)
        database.playbackHistoryDao().insert(PlaybackHistoryEntity(trackKey = track.key, playedAtMs = 20L, positionMs = 5_000L))
        database.playlistDao().upsert(playlist)

        assertEquals(artist, database.artistDao().getByKeys(listOf(artist.key)).single())
        assertEquals(album.key, database.albumDao().getByKey(album.key)?.key)
        assertEquals(listOf(artist.key), database.trackDao().getByKey(track.key)?.artistKeys)
        assertEquals(20L, database.playbackHistoryDao().observeRecent().first().single().playedAtMs)
        assertEquals(playlist, database.playlistDao().getById(playlist.id))
    }
}