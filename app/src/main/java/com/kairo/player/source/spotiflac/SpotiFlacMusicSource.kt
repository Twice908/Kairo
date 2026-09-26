package com.kairo.player.source.spotiflac

import android.util.Base64
import com.kairo.player.domain.model.Album
import com.kairo.player.domain.model.AlbumArt
import com.kairo.player.domain.model.Artist
import com.kairo.player.domain.model.StreamInfo
import com.kairo.player.domain.model.Track
import com.kairo.player.source.MusicSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.util.concurrent.ConcurrentHashMap

class SpotiFlacMusicSource(
    private val api: SpotiFlacApiService,
) : MusicSource {
    private val tracks = ConcurrentHashMap<String, Track>()
    private val json = Json { ignoreUnknownKeys = true }

    override val sourceId: String = SOURCE_ID
    override val sourceName: String = "SpotiFLAC (Tidal)"

    override suspend fun search(query: String): List<Track> {
        val term = query.trim()
        if (term.isEmpty()) return emptyList()

        return api.search(term)
            .data.items
            .map(::toTrack)
            .onEach { track -> tracks[track.id] = track }
    }

    override suspend fun getTrack(trackId: String): Track? {
        tracks[trackId]?.let { return it }
        val numericId = trackId.toLongOrNull() ?: return null
        return api.getTrack(numericId).data?.let(::toTrack)?.also { tracks[it.id] = it }
    }

    override suspend fun resolveStream(trackId: String): List<StreamInfo> {
        val numericId = trackId.toLongOrNull() ?: return emptyList()
        val stream = api.resolveTrack(numericId).data ?: return emptyList()
        val encodedManifest = stream.manifest ?: return emptyList()
        val decodedManifest = runCatching {
            val bytes = Base64.decode(encodedManifest, Base64.DEFAULT)
            json.decodeFromString<SpotiFlacManifest>(bytes.toString(Charsets.UTF_8))
        }.getOrNull() ?: return emptyList()

        if (!decodedManifest.codecs.orEmpty().contains("flac", ignoreCase = true) &&
            decodedManifest.mimeType != FLAC_MIME_TYPE
        ) {
            return emptyList()
        }

        return decodedManifest.urls
            .filter { url -> url.startsWith(HTTPS_PREFIX) && url.substringBefore('?').endsWith(".flac", true) }
            .map { url ->
                StreamInfo(
                    url = url,
                    mimeType = FLAC_MIME_TYPE,
                    codec = FLAC_CODEC,
                    sampleRate = stream.sampleRate,
                    bitDepth = stream.bitDepth,
                    lossless = true,
                    sourceName = sourceName,
                )
            }
    }

    override suspend fun getArtist(artistId: String): Artist? = tracks.values
        .asSequence()
        .flatMap { it.artists.asSequence() }
        .firstOrNull { it.id == artistId }

    override suspend fun getAlbum(albumId: String): Album? = tracks.values
        .asSequence()
        .mapNotNull { it.album }
        .firstOrNull { it.id == albumId }

    private fun toTrack(dto: SpotiFlacTrackDto): Track {
        val artists = dto.artists.map { artist ->
            Artist(id = artist.id.toString(), sourceId = SOURCE_ID, name = artist.name)
        }
        val album = dto.album?.let { album ->
            Album(
                id = album.id.toString(),
                sourceId = SOURCE_ID,
                title = album.title,
                artists = artists,
                artwork = album.cover?.let { cover ->
                    AlbumArt(uri = TIDAL_ARTWORK_URL.format(cover.replace('-', '/')))
                },
                releaseYear = album.releaseDate?.take(4)?.toIntOrNull(),
            )
        }
        return Track(
            id = dto.id.toString(),
            sourceId = SOURCE_ID,
            title = dto.title,
            artists = artists,
            album = album,
            durationMs = dto.duration?.times(1000),
        )
    }

    companion object {
        const val SOURCE_ID = "spotiflac"
        private const val HTTPS_PREFIX = "https://"
        private const val FLAC_CODEC = "flac"
        private const val FLAC_MIME_TYPE = "audio/flac"
        private const val TIDAL_ARTWORK_URL = "https://resources.tidal.com/images/%s/640x640.jpg"
    }
}