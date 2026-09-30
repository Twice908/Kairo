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
import kotlinx.coroutines.flow.first
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

    fun observeTracks(sourceId: String): Flow<List<Track>> = trackDao.observeBySource(sourceId)
        .map { entities -> entities.map { loadTrack(it) } }
        .flowOn(Dispatchers.IO)

    suspend fun search(sourceId: String, query: String): List<Track> = withContext(Dispatchers.IO) {
        trackDao.search(sourceId, query.trim()).map { loadTrack(it) }
    }

    suspend fun getTrack(sourceId: String, trackId: String): Track? = withContext(Dispatchers.IO) {
        trackDao.getByKey(entityKey(sourceId, trackId))?.let { loadTrack(it) }
    }

    suspend fun getArtist(sourceId: String, artistId: String): Artist? = withContext(Dispatchers.IO) {
        artistDao.getByRemoteId(sourceId, artistId)?.let(::mapArtist)
    }

    suspend fun getAlbum(sourceId: String, albumId: String): Album? = withContext(Dispatchers.IO) {
        albumDao.getByRemoteId(sourceId, albumId)?.let { mapAlbum(it) }
    }

    suspend fun getTracksForAlbum(sourceId: String, albumId: String): List<Track> = withContext(Dispatchers.IO) {
        trackDao.observeForAlbum(sourceId, entityKey(sourceId, albumId)).first().map { loadTrack(it) }
    }

    fun observeTracksForAlbum(sourceId: String, albumId: String): Flow<List<Track>> =
        trackDao.observeForAlbum(sourceId, entityKey(sourceId, albumId))
            .map { entities -> entities.map { loadTrack(it) } }
            .flowOn(Dispatchers.IO)

    suspend fun saveTrack(track: Track, lastSyncedAt: Long = 0L) = withContext(Dispatchers.IO) {
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
                    remoteId = track.id,
                    lastSyncedAt = lastSyncedAt,
                    artistNames = track.artists.map { it.name },
                    trackNumber = track.trackNumber,
                    discNumber = track.discNumber,
                    year = track.year,
                    genre = track.genre,
                    coverArtId = track.coverArtId,
                ),
            )
        }
    }

    suspend fun removeTrack(sourceId: String, trackId: String) = withContext(Dispatchers.IO) {
        trackDao.delete(entityKey(sourceId, trackId))
    }

    internal suspend fun loadTrack(entity: TrackEntity): Track {
        val artists = if (entity.artistKeys.isEmpty()) emptyList() else
            artistDao.getByKeys(entity.artistKeys).associateBy { it.key }
                .let { byKey -> entity.artistKeys.mapNotNull(byKey::get).map(::mapArtist) }
        val albumEntity = entity.albumKey?.let { albumDao.getByKey(it) }
        val album = albumEntity?.let { mapAlbum(it) }
        return Track(
            id = entity.remoteId,
            sourceId = entity.sourceId,
            title = entity.title,
            artists = artists,
            album = album,
            durationMs = entity.durationMs,
            trackNumber = entity.trackNumber,
            discNumber = entity.discNumber,
            year = entity.year,
            genre = entity.genre,
            coverArtId = entity.coverArtId,
        )
    }

    internal suspend fun mapAlbum(album: AlbumEntity): Album {
        val albumArtists = artistDao.getByKeys(album.artistKeys).associateBy { it.key }
        return Album(
            id = album.remoteId,
            sourceId = album.sourceId,
            title = album.title,
            artists = album.artistKeys.mapNotNull(albumArtists::get).map(::mapArtist),
            artwork = if (album.artworkUri == null && album.artworkData == null) null else {
                AlbumArt(album.artworkUri, album.artworkMimeType, album.artworkData)
            },
            releaseYear = album.releaseYear,
        )
    }

    private fun toEntity(artist: Artist) = ArtistEntity(
        key = entityKey(artist.sourceId, artist.id),
        id = artist.id,
        sourceId = artist.sourceId,
        name = artist.name,
        remoteId = artist.id,
        lastSyncedAt = 0L,
        artworkUri = artist.artwork?.uri,
        biography = artist.biography,
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
            remoteId = album.id,
            lastSyncedAt = 0L,
        )
    }

    internal fun mapArtist(artist: ArtistEntity) = Artist(
        id = artist.id,
        sourceId = artist.sourceId,
        name = artist.name,
        artwork = artist.artworkUri?.let { AlbumArt(uri = it) },
        biography = artist.biography,
    )

    private fun entityKey(sourceId: String, id: String) = "$sourceId\u001f$id"
}