package com.kairo.player.source.mock

import com.kairo.player.domain.model.Album
import com.kairo.player.domain.model.Artist
import com.kairo.player.domain.model.StreamInfo
import com.kairo.player.domain.model.Track
import com.kairo.player.source.MusicSource

class MockMusicSource(
    private val tracks: List<Track> = emptyList(),
    private val streamsByTrackId: Map<String, List<StreamInfo>> = emptyMap(),
) : MusicSource {
    override val sourceId: String = SOURCE_ID
    override val sourceName: String = "Mock source"

    override suspend fun search(query: String): List<Track> {
        val term = query.trim()
        return tracks.filter { track ->
            term.isEmpty() || track.title.contains(term, ignoreCase = true) ||
                track.artists.any { it.name.contains(term, ignoreCase = true) } ||
                track.album?.title?.contains(term, ignoreCase = true) == true
        }
    }

    override suspend fun getTrack(trackId: String): Track? = tracks.firstOrNull { it.id == trackId }

    override suspend fun resolveStream(trackId: String): List<StreamInfo> =
        streamsByTrackId[trackId].orEmpty()

    override suspend fun getArtist(artistId: String): Artist? =
        tracks.asSequence().flatMap { it.artists.asSequence() }.firstOrNull { it.id == artistId }

    override suspend fun getAlbum(albumId: String): Album? =
        tracks.firstNotNullOfOrNull { track -> track.album?.takeIf { it.id == albumId } }

    companion object {
        const val SOURCE_ID = "mock"
    }
}