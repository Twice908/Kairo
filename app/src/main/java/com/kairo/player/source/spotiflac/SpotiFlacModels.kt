package com.kairo.player.source.spotiflac

import kotlinx.serialization.Serializable

@Serializable
data class SpotiFlacSearchResponse(
    val data: SpotiFlacSearchData = SpotiFlacSearchData(),
)

@Serializable
data class SpotiFlacSearchData(
    val items: List<SpotiFlacTrackDto> = emptyList(),
)

@Serializable
data class SpotiFlacTrackResponse(
    val data: SpotiFlacTrackDto? = null,
)

@Serializable
data class SpotiFlacTrackDto(
    val id: Long,
    val title: String,
    val duration: Long? = null,
    val artists: List<SpotiFlacArtistDto> = emptyList(),
    val album: SpotiFlacAlbumDto? = null,
)

@Serializable
data class SpotiFlacArtistDto(
    val id: Long,
    val name: String,
)

@Serializable
data class SpotiFlacAlbumDto(
    val id: Long,
    val title: String,
    val cover: String? = null,
    val releaseDate: String? = null,
)

@Serializable
data class SpotiFlacStreamResponse(
    val data: SpotiFlacStreamData? = null,
)

@Serializable
data class SpotiFlacStreamData(
    val audioQuality: String? = null,
    val manifestMimeType: String? = null,
    val manifest: String? = null,
    val bitDepth: Int? = null,
    val sampleRate: Int? = null,
)

@Serializable
data class SpotiFlacManifest(
    val mimeType: String? = null,
    val codecs: String? = null,
    val urls: List<String> = emptyList(),
)