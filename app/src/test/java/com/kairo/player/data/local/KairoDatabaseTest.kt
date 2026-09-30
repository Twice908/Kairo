package com.kairo.player.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.kairo.player.data.local.entity.AlbumEntity
import com.kairo.player.data.local.entity.ArtistEntity
import com.kairo.player.data.local.entity.PlaybackHistoryEntity
import com.kairo.player.data.local.entity.PlaylistEntity
import com.kairo.player.data.local.entity.TrackEntity
import com.kairo.player.data.local.entity.SyncStateEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class KairoDatabaseTest {
    private lateinit var database: KairoDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            KairoDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun persistsAllMetadataHistoryAndPlaylistEntities() = runBlocking {
        val artist = ArtistEntity("local\u001fartist-1", "artist-1", "local", "Artist")
        val album = AlbumEntity(
            key = "local\u001falbum-1",
            id = "album-1",
            sourceId = "local",
            title = "Album",
            artistKeys = listOf(artist.key),
            artworkUri = "content://art/1",
            artworkMimeType = "image/jpeg",
            artworkData = byteArrayOf(1, 2),
            releaseYear = 2024,
        )
        val track = TrackEntity(
            key = "local\u001ftrack-1",
            id = "track-1",
            sourceId = "local",
            title = "Track",
            artistKeys = listOf(artist.key),
            albumKey = album.key,
            durationMs = 90_000,
        )
        val playlist = PlaylistEntity("playlist-1", "Favorites", listOf(track.key), 10L, 10L)
        val syncState = SyncStateEntity("navidrome", 10L, 20L, "success", null, 1, 1, 1)

        database.artistDao().upsertAll(listOf(artist))
        database.albumDao().upsert(album)
        database.trackDao().upsert(track)
        database.playbackHistoryDao().insert(PlaybackHistoryEntity(trackKey = track.key, playedAtMs = 20L, positionMs = 5_000L))
        database.playlistDao().upsert(playlist)
        database.syncStateDao().upsertSyncState(syncState)

        assertEquals(artist, database.artistDao().getByKeys(listOf(artist.key)).single())
        assertEquals(album.key, database.albumDao().getByKey(album.key)?.key)
        assertEquals(listOf(artist.key), database.trackDao().getByKey(track.key)?.artistKeys)
        assertEquals(20L, database.playbackHistoryDao().observeRecent().first().single().playedAtMs)
        assertEquals(playlist, database.playlistDao().getById(playlist.id))
        assertEquals(syncState, database.syncStateDao().getSyncState("navidrome").first())
    }

    @Test
    fun migrationOneToTwoPreservesRowsAndBackfillsRemoteIds() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "kairo-v1-migration-test.db"
        context.deleteDatabase(name)
        val legacy = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null)
        legacy.execSQL("CREATE TABLE tracks (\"key\" TEXT NOT NULL, id TEXT NOT NULL, sourceId TEXT NOT NULL, title TEXT NOT NULL, artistKeys TEXT NOT NULL, albumKey TEXT, durationMs INTEGER, PRIMARY KEY(\"key\"))")
        legacy.execSQL("CREATE TABLE artists (\"key\" TEXT NOT NULL, id TEXT NOT NULL, sourceId TEXT NOT NULL, name TEXT NOT NULL, PRIMARY KEY(\"key\"))")
        legacy.execSQL("CREATE TABLE albums (\"key\" TEXT NOT NULL, id TEXT NOT NULL, sourceId TEXT NOT NULL, title TEXT NOT NULL, artistKeys TEXT NOT NULL, artworkUri TEXT, artworkMimeType TEXT, artworkData BLOB, releaseYear INTEGER, PRIMARY KEY(\"key\"))")
        legacy.execSQL("CREATE TABLE playlists (id TEXT NOT NULL, name TEXT NOT NULL, trackKeys TEXT NOT NULL, createdAtMs INTEGER NOT NULL, updatedAtMs INTEGER NOT NULL, PRIMARY KEY(id))")
        legacy.execSQL("CREATE TABLE playback_history (entryId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, trackKey TEXT NOT NULL, playedAtMs INTEGER NOT NULL, positionMs INTEGER NOT NULL)")
        legacy.execSQL("CREATE UNIQUE INDEX index_tracks_sourceId_id ON tracks(sourceId, id)")
        legacy.execSQL("CREATE UNIQUE INDEX index_artists_sourceId_id ON artists(sourceId, id)")
        legacy.execSQL("CREATE UNIQUE INDEX index_albums_sourceId_id ON albums(sourceId, id)")
        legacy.execSQL("CREATE INDEX index_playback_history_playedAtMs ON playback_history(playedAtMs)")
        legacy.execSQL(
            "INSERT INTO tracks (\"key\", id, sourceId, title, artistKeys, albumKey, durationMs) VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>("navidrome\u001ftrack-1", "track-1", "navidrome", "Track", "[]", null, 90_000),
        )
        legacy.version = 1
        legacy.close()

        val migrated = Room.databaseBuilder(context, KairoDatabase::class.java, name)
            .addMigrations(KairoDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val track = migrated.trackDao().getByKey("navidrome\u001ftrack-1")
            assertEquals("track-1", track?.remoteId)
            assertEquals(0L, track?.lastSyncedAt)
            assertEquals("Track", track?.title)
            assertEquals(null, migrated.syncStateDao().getSyncStateOnce("navidrome"))
        } finally {
            migrated.close()
            context.deleteDatabase(name)
        }
    }
}