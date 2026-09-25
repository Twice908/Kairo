package com.kairo.player.source.local

import com.kairo.player.domain.model.AlbumArt
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMusicSourceTest {
    @Test
    fun exposesBundledDemoAudioWithExactFilenamesAndNativeMimeTypes() = runBlocking {
        val documents = listOf(
            "Raatein_Guzaari_Aditya_Rikhari.flac",
            "Suroor.flac",
            "Ultrafunk.wav",
            "dhurandhar_baloch.wav",
        ).map { name ->
            LocalAudioDocument("asset:///demo_music/$name", name, null)
        }
        val source = LocalMusicSource(FakeLocalAudioDocumentStore(documents))
        val tracks = source.search("").sortedBy { it.title }

        assertEquals(documents.map { it.displayName }.sorted(), tracks.map { it.title })
        assertTrue(tracks.all { it.artists.isEmpty() && it.album == null && it.durationMs == null })
        tracks.forEach { track ->
            val stream = source.resolveStream(track.id).single()
            assertEquals(track.id, stream.url)
            assertEquals(if (track.title.endsWith(".flac")) "audio/flac" else "audio/wav", stream.mimeType)
        }
    }

    @Test
    fun searchReturnsSupportedUserFilesWithAvailableMetadata() = runBlocking {
        val document = LocalAudioDocument(
            uri = "content://music/track-1",
            displayName = "fallback.mp3",
            mimeType = "audio/mpeg",
        )
        val source = LocalMusicSource(
            FakeLocalAudioDocumentStore(
                documents = listOf(document),
                metadata = mapOf(
                    document.uri to LocalAudioMetadata(
                        title = "Blue Hour",
                        artist = "Kairo Test Artist",
                        album = "Open Sessions",
                        durationMs = 184_000,
                        albumArt = AlbumArt(data = byteArrayOf(1, 2, 3)),
                    ),
                ),
            ),
        )

        val results = source.search("test artist")

        assertEquals(1, results.size)
        assertEquals("Blue Hour", results.single().title)
        assertEquals("Kairo Test Artist", results.single().artists.single().name)
        assertEquals("Open Sessions", results.single().album?.title)
        assertEquals(184_000L, results.single().durationMs)
        assertEquals(document.uri, results.single().id)
    }

    @Test
    fun resolvesLocalStreamWithoutInventingUnavailableQuality() = runBlocking {
        val document = LocalAudioDocument(
            uri = "content://music/track.flac",
            displayName = "track.flac",
            mimeType = "application/octet-stream",
        )
        val source = LocalMusicSource(FakeLocalAudioDocumentStore(documents = listOf(document)))

        val stream = source.resolveStream(document.uri).single()

        assertEquals(document.uri, stream.url)
        assertEquals("audio/flac", stream.mimeType)
        assertEquals("Local files", stream.sourceName)
        assertNull(stream.codec)
        assertNull(stream.bitrate)
        assertNull(stream.sampleRate)
        assertNull(stream.bitDepth)
        assertNull(stream.channels)
        assertNull(stream.lossless)
    }

    @Test
    fun excludesAndRejectsUnsupportedFiles() {
        val document = LocalAudioDocument(
            uri = "content://music/cover.pdf",
            displayName = "cover.pdf",
            mimeType = "application/pdf",
        )
        val source = LocalMusicSource(FakeLocalAudioDocumentStore(documents = listOf(document)))
        val results = runBlocking { source.search("") }
        val track = runBlocking { source.getTrack(document.uri) }

        assertTrue(results.isEmpty())
        assertNull(track)
        assertThrows(UnsupportedAudioFormatException::class.java) {
            runBlocking { source.resolveStream(document.uri) }
        }
    }

    @Test
    fun supportsCommonAudioExtensionsWhenProviderMimeIsGeneric() {
        assertEquals("audio/mpeg", SupportedAudioFormats.mimeType("song.mp3", "application/octet-stream"))
        assertEquals("audio/opus", SupportedAudioFormats.mimeType("song.opus", null))
        assertEquals("audio/aac", SupportedAudioFormats.mimeType("song.aac", "audio/mp4a-latm"))
        assertFalse(SupportedAudioFormats.mimeType("video.mp4", "video/mp4") != null)
    }

    private class FakeLocalAudioDocumentStore(
        private val documents: List<LocalAudioDocument> = emptyList(),
        private val metadata: Map<String, LocalAudioMetadata> = emptyMap(),
    ) : LocalAudioDocumentStore {
        override suspend fun persistTree(uri: String) = Unit

        override suspend fun persistDocument(uri: String) = Unit

        override suspend fun removeSelection(uri: String) = Unit

        override suspend fun listDocuments(): List<LocalAudioDocument> = documents

        override suspend fun getDocument(uri: String): LocalAudioDocument? =
            documents.firstOrNull { it.uri == uri }

        override suspend fun getMetadata(uri: String): LocalAudioMetadata? = metadata[uri]
    }
}