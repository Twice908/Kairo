package com.kairo.player.source.local

import com.kairo.player.domain.model.Album
import com.kairo.player.domain.model.Artist
import com.kairo.player.domain.model.StreamInfo
import com.kairo.player.domain.model.Track
import com.kairo.player.source.MusicSource

class LocalMusicSource(
    private val documentStore: LocalAudioDocumentStore,
) : MusicSource {
    override val sourceId: String = SOURCE_ID
    override val sourceName: String = "Local files"

    override suspend fun search(query: String): List<Track> {
        val term = query.trim()
        return documentStore.listDocuments()
            .mapNotNull { toTrack(it) }
            .filter { track ->
                term.isEmpty() || track.title.contains(term, ignoreCase = true) ||
                    track.artists.any { it.name.contains(term, ignoreCase = true) } ||
                    track.album?.title?.contains(term, ignoreCase = true) == true
            }
    }

    override suspend fun getTrack(trackId: String): Track? =
        documentStore.getDocument(trackId)?.let { toTrack(it) }

    override suspend fun resolveStream(trackId: String): List<StreamInfo> {
        val document = documentStore.getDocument(trackId) ?: return emptyList()
        val mimeType = SupportedAudioFormats.mimeType(document.displayName, document.mimeType)
            ?: throw UnsupportedAudioFormatException(document.displayName)
        return listOf(
            StreamInfo(
                url = document.uri,
                mimeType = mimeType,
                sourceName = sourceName,
            ),
        )
    }

    override suspend fun getArtist(artistId: String): Artist? = search("")
        .asSequence()
        .flatMap { it.artists.asSequence() }
        .firstOrNull { it.id == artistId }

    override suspend fun getAlbum(albumId: String): Album? = search("")
        .firstNotNullOfOrNull { it.album?.takeIf { album -> album.id == albumId } }

    suspend fun addTree(uri: String) = documentStore.persistTree(uri)

    suspend fun addDocument(uri: String) = documentStore.persistDocument(uri)

    suspend fun removeSelection(uri: String) = documentStore.removeSelection(uri)

    private suspend fun toTrack(document: LocalAudioDocument): Track? {
        if (SupportedAudioFormats.mimeType(document.displayName, document.mimeType) == null) return null
        val metadata = documentStore.getMetadata(document.uri)
        val title = metadata?.title?.takeIf(String::isNotBlank)
            ?: document.displayName.substringBeforeLast('.', document.displayName)
        val artist = metadata?.artist?.takeIf(String::isNotBlank)?.let { name ->
            Artist(id = "$SOURCE_ID:artist:${name.lowercase()}", sourceId = SOURCE_ID, name = name)
        }
        val album = metadata?.album?.takeIf(String::isNotBlank)?.let { name ->
            Album(
                id = "$SOURCE_ID:album:${artist?.id.orEmpty()}:$name",
                sourceId = SOURCE_ID,
                title = name,
                artists = listOfNotNull(artist),
                artwork = metadata.albumArt,
            )
        }
        return Track(
            id = document.uri,
            sourceId = SOURCE_ID,
            title = title,
            artists = listOfNotNull(artist),
            album = album,
            durationMs = metadata?.durationMs,
        )
    }

    companion object {
        const val SOURCE_ID = "local"
    }
}

class UnsupportedAudioFormatException(fileName: String) :
    IllegalArgumentException("Unsupported audio format: $fileName")

internal object SupportedAudioFormats {
    private val mimeTypes = setOf(
        "audio/aac",
        "audio/amr",
        "audio/flac",
        "audio/mp4",
        "audio/mpeg",
        "audio/ogg",
        "audio/opus",
        "audio/wav",
        "audio/3gpp",
    )
    private val mimeAliases = mapOf(
        "audio/mp4a-latm" to "audio/aac",
        "audio/x-aac" to "audio/aac",
        "audio/amr-nb" to "audio/amr",
        "audio/amr-wb" to "audio/amr",
        "audio/x-flac" to "audio/flac",
        "audio/x-wav" to "audio/wav",
        "audio/wave" to "audio/wav",
    )
    private val extensionMimeTypes = mapOf(
        "aac" to "audio/aac",
        "amr" to "audio/amr",
        "flac" to "audio/flac",
        "m4a" to "audio/mp4",
        "m4b" to "audio/mp4",
        "mp3" to "audio/mpeg",
        "oga" to "audio/ogg",
        "ogg" to "audio/ogg",
        "opus" to "audio/opus",
        "wav" to "audio/wav",
        "wave" to "audio/wav",
        "3gp" to "audio/3gpp",
    )

    fun mimeType(displayName: String, declaredMimeType: String?): String? {
        val mimeType = declaredMimeType?.substringBefore(';')?.lowercase()
        val normalizedMimeType = mimeAliases[mimeType] ?: mimeType
        if (normalizedMimeType in mimeTypes) return normalizedMimeType
        if (mimeType != null && mimeType != "application/octet-stream" && mimeType != "application/unknown") {
            return null
        }
        val extension = displayName.substringAfterLast('.', "").lowercase()
        return extensionMimeTypes[extension]
    }
}