package com.kairo.player.source

import com.kairo.player.domain.model.Album
import com.kairo.player.domain.model.Artist
import com.kairo.player.domain.model.StreamInfo
import com.kairo.player.domain.model.Track
import com.kairo.player.source.mock.MockMusicSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MusicSourceRegistryTest {
    @Test
    fun registryRoutesSearchTrackMetadataAndStreamsBySource() = runBlocking {
        val track = Track(id = "track-1", sourceId = MockMusicSource.SOURCE_ID, title = "Track")
        val stream = StreamInfo(url = "mock://track-1", sourceName = "Mock source")
        val source = MockMusicSource(listOf(track), mapOf(track.id to listOf(stream)))
        val registry = MusicSourceRegistry(listOf(source))

        assertEquals(listOf(track), registry.search("track"))
        assertEquals(track, registry.getTrack(track))
        assertEquals(listOf(stream), registry.resolveStreams(track))
        assertNull(registry.getSource("unknown"))
    }

    @Test
    fun registryBuildsCatalogFromRegisteredSource() = runBlocking {
        val artist = Artist(id = "artist-1", sourceId = MockMusicSource.SOURCE_ID, name = "Artist")
        val album = Album(id = "album-1", sourceId = MockMusicSource.SOURCE_ID, title = "Album", artists = listOf(artist))
        val track = Track(id = "track-1", sourceId = MockMusicSource.SOURCE_ID, title = "Track", artists = listOf(artist), album = album)
        val registry = MusicSourceRegistry(listOf(MockMusicSource(listOf(track))))

        val library = registry.getSource(MockMusicSource.SOURCE_ID)?.browseLibrary()

        assertEquals(listOf(track), library?.tracks)
        assertEquals(listOf(album), library?.albums)
        assertEquals(listOf(artist), library?.artists)
    }

    @Test
    fun rejectsDuplicateSourceIds() {
        val first = MockMusicSource()
        val second = MockMusicSource()

        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            MusicSourceRegistry(listOf(first, second))
        }
    }
}