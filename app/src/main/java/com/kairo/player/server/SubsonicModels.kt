package com.kairo.player.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubsonicEnvelope(
    @SerialName("subsonic-response") val response: SubsonicResponse,
)

@Serializable
data class SubsonicResponse(
    val status: String,
    val version: String? = null,
    val error: SubsonicError? = null,
    val artists: ArtistsDto? = null,
    val artist: ArtistDto? = null,
    val album: AlbumDto? = null,
    val song: SongDto? = null,
    val searchResult3: SearchResultDto? = null,
    val albumList2: AlbumListDto? = null,
    val scanStatus: ScanStatusDto? = null,
) {
    val isOk: Boolean get() = status == "ok"
}

@Serializable
data class SubsonicError(val code: Int, val message: String? = null)

@Serializable
data class ArtistsDto(val index: List<ArtistIndexDto> = emptyList())

@Serializable
data class ArtistIndexDto(val name: String, val artist: List<ArtistDto> = emptyList())

@Serializable
data class ArtistDto(
    val id: String,
    val name: String,
    val coverArt: String? = null,
    val albumCount: Int? = null,
    val album: List<AlbumDto> = emptyList(),
)

@Serializable
data class AlbumDto(
    val id: String,
    val name: String,
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int? = null,
    val year: Int? = null,
    val duration: Int? = null,
    val song: List<SongDto> = emptyList(),
)

@Serializable
data class SongDto(
    val id: String,
    val title: String,
    val album: String? = null,
    val albumId: String? = null,
    val artist: String? = null,
    val artistId: String? = null,
    val track: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val coverArt: String? = null,
    val duration: Int? = null,
    val suffix: String? = null,
    val contentType: String? = null,
    val bitRate: Int? = null,
    val samplingRate: Int? = null,
    val bitDepth: Int? = null,
    val channelCount: Int? = null,
    val size: Long? = null,
)

@Serializable
data class SearchResultDto(
    val artist: List<ArtistDto> = emptyList(),
    val album: List<AlbumDto> = emptyList(),
    val song: List<SongDto> = emptyList(),
)

@Serializable
data class AlbumListDto(val album: List<AlbumDto> = emptyList())

@Serializable
data class ScanStatusDto(val scanning: Boolean = false, val count: Long? = null)