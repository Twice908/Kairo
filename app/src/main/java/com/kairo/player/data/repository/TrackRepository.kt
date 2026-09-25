package com.kairo.player.data.repository

import androidx.room.withTransaction
import com.kairo.player.data.local.KairoDatabase
import com.kairo.player.data.local.entity.AlbumEntity
import com.kairo.player.data.local.entity.ArtistEntity
import com.kairo.player.data.local.entity.TrackEntity
import com.kairo.player.domain.model.Album
import com.kairo.player.domain.model.AlbumArt
import com.kairo.player.domain.model.Artist
import com.kairo.player.domain.model.Track
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class TrackRepository @Inject constructor(
    private val database: KairoDatabase,
) {
    private val trackDao = database.trackDao()
    private val artistDao = database.artistDao()
    private val albumDao = database.albumDao()

    fun observeTracks(): Flow<List<Track>> = trackDao.observeAll()
        .map { entities -> entities.map { loadTrack(it) } }
        .flowOn(Dispatchers.IO)

    suspend fun getTrack(sourceId: String, trackId: String): Track? = withContext(Dispatchers.IO) {
        trackDao.getByKey(entityKey(sourceId, trackId))?.let { loadTrack(it) }
    }

    suspend fun saveTrack(track: Track) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val trackArtists = track.artists.map(::toEntity)
            val albumArtists = track.album?.artists.orEmpty().map(::toEntity)
            artistDao.upsertAll((trackArtists + albumArtists).distinctBy { it.key })
            val albumEntity = track.album?.let(::toEntity)
            albumEntity?.let { albumDao.upsert(it) }
            trackDao.upsert(
                TrackEntity(
                    key = entityKey(track.sourceId, track.id),
                    id = track.id,
                    sourceId = track.sourceId,
                    title = track.title,
                    artistKeys = track.artists.map { entityKey(it.sourceId, it.id) },
                    albumKey = albumEntity?.key,
                    durationMs = track.durationMs,
                ),
            )
        }
    }

    suspend fun removeTrack(sourceId: String, trackId: String) = withContext(Dispatchers.IO) {
        trackDao.delete(entityKey(sourceId, trackId))
    }

    private suspend fun loadTrack(entity: TrackEntity): Track {
        val artists = if (entity.artistKeys.isEmpty()) emptyList() else
            artistDao.getByKeys(entity.artistKeys).associateBy { it.key }
                .let { byKey -> entity.artistKeys.mapNotNull(byKey::get).map(::toDomain) }
        val albumEntity = entity.albumKey?.let { albumDao.getByKey(it) }
        val album = albumEntity?.let { stored ->
            val albumArtists = if (stored.artistKeys.isEmpty()) emptyList() else
                artistDao.getByKeys(stored.artistKeys).associateBy { it.key }
                    .let { byKey -> stored.artistKeys.mapNotNull(byKey::get).map(::toDomain) }
            Album(
                id = stored.id,
                sourceId = stored.sourceId,
                title = stored.title,
                artists = albumArtists,
                artwork = if (stored.artworkUri == null && stored.artworkData == null) null else {
                    AlbumArt(stored.artworkUri, stored.artworkMimeType, stored.artworkData)
                },
                releaseYear = stored.releaseYear,
            )
        }
        return Track(
            id = entity.id,
            sourceId = entity.sourceId,
            title = entity.title,
            artists = artists,
            album = album,
            durationMs = entity.durationMs,
        )
    }

    private fun toEntity(artist: Artist) = ArtistEntity(
        key = entityKey(artist.sourceId, artist.id),
        id = artist.id,
        sourceId = artist.sourceId,
        name = artist.name,
    )

    private fun toEntity(album: Album): AlbumEntity {
        val artwork = album.artwork
        return AlbumEntity(
            key = entityKey(album.sourceId, album.id),
            id = album.id,
            sourceId = album.sourceId,
            title = album.title,
            artistKeys = album.artists.map { entityKey(it.sourceId, it.id) },
            artworkUri = artwork?.uri,
            artworkMimeType = artwork?.mimeType,
            artworkData = artwork?.data,
            releaseYear = album.releaseYear,
        )
    }

    private fun toDomain(artist: ArtistEntity) = Artist(
        id = artist.id,
        sourceId = artist.sourceId,
        name = artist.name,
    )

    private fun entityKey(sourceId: String, id: String) = "$sourceId\u001f$id"
}