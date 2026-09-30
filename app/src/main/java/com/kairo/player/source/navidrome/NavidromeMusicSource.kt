package com.kairo.player.source.navidrome

import com.kairo.player.domain.model.Album
import com.kairo.player.domain.model.AlbumArt
import com.kairo.player.domain.model.Artist
import com.kairo.player.domain.model.MusicLibrary
import com.kairo.player.domain.model.StreamInfo
import com.kairo.player.domain.model.Track
import com.kairo.player.data.repository.LibraryRepository
import com.kairo.player.server.AlbumDto
import com.kairo.player.server.ArtistDto
import com.kairo.player.server.NavidromeApiService
import com.kairo.player.server.ServerConfig
import com.kairo.player.server.SongDto
import com.kairo.player.server.SubsonicEnvelope
import com.kairo.player.server.SubsonicResponse
import com.kairo.player.server.SubsonicUrlBuilder
import com.kairo.player.source.MusicSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavidromeMusicSource @Inject constructor(
    private val api: NavidromeApiService,
    private val config: ServerConfig,
    private val urls: SubsonicUrlBuilder,
    private val libraryRepository: LibraryRepository,
) : MusicSource {

    override val sourceId: String = SOURCE_ID
    override val sourceName: String = "Navidrome"

    override suspend fun search(query: String): List<Track> {
        val cached = libraryRepository.searchTracks(sourceId, query)
        if (cached.isNotEmpty() || !config.isConfigured) return cached
        return call(emptyList()) {
            val result = api.search3(query.trim()).ok().searchResult3
            result?.song.orEmpty().map { it.toTrack() }
        }
    }

    override suspend fun getTrack(trackId: String): Track? =
        libraryRepository.getTrack(sourceId, trackId)
            ?: call(null) { api.getSong(trackId).ok().song?.toTrack() }

    override suspend fun resolveStream(trackId: String): List<StreamInfo> =
        call(emptyList()) {
            val song = api.getSong(trackId).ok().song ?: return@call emptyList()
            val url = urls.streamUrl(trackId) ?: return@call emptyList()
            val suffix = song.suffix?.lowercase()
            listOf(
                StreamInfo(
                    url = url,
                    mimeType = song.contentType,
                    codec = suffix,
                    bitrate = song.bitRate,
                    sampleRate = song.samplingRate,
                    bitDepth = song.bitDepth,
                    channels = song.channelCount,
                    lossless = suffix?.let { it in LOSSLESS_SUFFIXES },
                    sourceName = sourceName,
                    cacheAllowed = true,
                ),
            )
        }

    override suspend fun getArtist(artistId: String): Artist? =
        libraryRepository.getArtist(sourceId, artistId)
            ?: call(null) { api.getArtist(artistId).ok().artist?.toArtist() }

    override suspend fun getAlbum(albumId: String): Album? =
        libraryRepository.getAlbum(sourceId, albumId)
            ?: call(null) { api.getAlbum(albumId).ok().album?.toAlbum() }

    override suspend fun browseLibrary(): MusicLibrary =
        libraryRepository.observeLibrary(sourceId).first()

    override suspend fun getAlbumTracks(albumId: String): List<Track> =
        libraryRepository.getAlbumTracksOnce(sourceId, albumId)

    override suspend fun getArtistAlbums(artistId: String): List<Album> =
        libraryRepository.getArtistAlbumsOnce(sourceId, artistId)

    override suspend fun getArtistBiography(artistId: String): String? =
        libraryRepository.getArtist(sourceId, artistId)?.biography

    // ---- helpers ----

    private suspend fun <T> call(fallback: T, block: suspend () -> T): T {
        if (!config.isConfigured) return fallback
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fallback
        }
    }

    // Subsonic returns HTTP 200 even on failure, so check the body status.
    private fun SubsonicEnvelope.ok(): SubsonicResponse {
        if (!response.isOk) {
            throw IOException(response.error?.message ?: "Subsonic request failed")
        }
        return response
    }

    private fun ArtistDto.toArtist() = Artist(
        id = id,
        sourceId = sourceId,
        name = name,
        artwork = coverArt?.let { AlbumArt(uri = urls.coverArtUrl(it)) },
    )

    private fun AlbumDto.toAlbum() = Album(
        id = id,
        sourceId = sourceId,
        title = name,
        artists = if (artist != null) {
            listOf(Artist(id = artistId ?: artist, sourceId = sourceId, name = artist))
        } else {
            emptyList()
        },
        artwork = (coverArt)?.let { AlbumArt(uri = urls.coverArtUrl(it)) },
        releaseYear = year,
    )

    private fun SongDto.toTrack(albumOverride: Album? = null) = Track(
        id = id,
        sourceId = sourceId,
        title = title,
        artists = if (artist != null) {
            listOf(Artist(id = artistId ?: artist, sourceId = sourceId, name = artist))
        } else {
            emptyList()
        },
        album = albumOverride ?: albumId?.let {
            Album(
                id = it,
                sourceId = sourceId,
                title = album ?: "",
                artwork = coverArt?.let { art -> AlbumArt(uri = urls.coverArtUrl(art)) },
                releaseYear = year,
            )
        },
        durationMs = duration?.let { it * 1000L },
        trackNumber = track,
        discNumber = discNumber,
        year = year,
        genre = genre,
        coverArtId = coverArt,
    )

    companion object {
        const val SOURCE_ID = "navidrome"
        private val LOSSLESS_SUFFIXES = setOf("flac", "wav", "alac", "ape", "wv", "aiff", "aif")
    }
}