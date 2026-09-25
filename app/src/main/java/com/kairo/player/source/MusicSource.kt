package com.kairo.player.source

import com.kairo.player.domain.model.Album
import com.kairo.player.domain.model.Artist
import com.kairo.player.domain.model.StreamInfo
import com.kairo.player.domain.model.Track

interface MusicSource {
    val sourceId: String
    val sourceName: String

    suspend fun search(query: String): List<Track>
    suspend fun getTrack(trackId: String): Track?
    suspend fun resolveStream(trackId: String): List<StreamInfo>
    suspend fun getArtist(artistId: String): Artist?
    suspend fun getAlbum(albumId: String): Album?
}