package com.kairo.player.source

import com.kairo.player.domain.model.StreamInfo
import com.kairo.player.domain.model.Track
import com.kairo.player.source.mock.MockMusicSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamResolverTest {
    @Test
    fun resolvesAndSelectsBestStreamForTrackSource() = runBlocking {
        val track = Track(id = "track-1", sourceId = MockMusicSource.SOURCE_ID, title = "Track")
        val lossy = StreamInfo(
            url = "mock://lossy",
            codec = "mp3",
            bitrate = 320_000,
            lossless = false,
            sourceName = "Mock source",
        )
        val lossless = StreamInfo(
            url = "mock://lossless",
            codec = "flac",
            lossless = true,
            sourceName = "Mock source",
        )
        val source = MockMusicSource(listOf(track), mapOf(track.id to listOf(lossy, lossless)))
        val resolver = StreamResolver(MusicSourceRegistry(listOf(source)), QualityResolver())

        assertEquals(lossless, resolver.resolve(track))
        assertEquals(track, resolver.resolveTrack(track)?.track)
        assertEquals(lossless, resolver.resolveTrack(track)?.stream)
    }

    @Test
    fun returnsNullWhenSourceCannotResolveAStream() = runBlocking {
        val track = Track(id = "missing", sourceId = MockMusicSource.SOURCE_ID, title = "Missing")
        val resolver = StreamResolver(
            MusicSourceRegistry(listOf(MockMusicSource())),
            QualityResolver(),
        )

        assertNull(resolver.resolve(track))
    }
}
