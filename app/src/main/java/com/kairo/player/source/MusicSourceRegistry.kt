package com.kairo.player.source

import com.kairo.player.domain.model.Album
import com.kairo.player.domain.model.Artist
import com.kairo.player.domain.model.StreamInfo
import com.kairo.player.domain.model.Track

class MusicSourceRegistry(sources: Collection<MusicSource>) {
    private val sourcesById: Map<String, MusicSource> = sources.associateBy { it.sourceId }

    init {
        require(sourcesById.size == sources.size) { "Music source IDs must be unique" }
    }

    val sources: List<MusicSource>
        get() = sourcesById.values.toList()

    fun getSource(sourceId: String): MusicSource? = sourcesById[sourceId]

    suspend fun search(query: String): List<Track> = sourcesById.values.flatMap { it.search(query) }

    suspend fun getTrack(track: Track): Track? = sourcesById[track.sourceId]?.getTrack(track.id)

    suspend fun getArtist(sourceId: String, artistId: String): Artist? =
        sourcesById[sourceId]?.getArtist(artistId)

    suspend fun getAlbum(sourceId: String, albumId: String): Album? =
        sourcesById[sourceId]?.getAlbum(albumId)

    suspend fun resolveStreams(track: Track): List<StreamInfo> =
        sourcesById[track.sourceId]?.resolveStream(track.id).orEmpty()
}