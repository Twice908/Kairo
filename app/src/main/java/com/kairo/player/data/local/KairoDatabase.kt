package com.kairo.player.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kairo.player.data.local.dao.AlbumDao
import com.kairo.player.data.local.dao.ArtistDao
import com.kairo.player.data.local.dao.PlaybackHistoryDao
import com.kairo.player.data.local.dao.PlaylistDao
import com.kairo.player.data.local.dao.TrackDao
import com.kairo.player.data.local.dao.SyncStateDao
import com.kairo.player.data.local.entity.AlbumEntity
import com.kairo.player.data.local.entity.ArtistEntity
import com.kairo.player.data.local.entity.PlaybackHistoryEntity
import com.kairo.player.data.local.entity.PlaylistEntity
import com.kairo.player.data.local.entity.TrackEntity
import com.kairo.player.data.local.entity.SyncStateEntity

@Database(
    entities = [
        TrackEntity::class,
        ArtistEntity::class,
        AlbumEntity::class,
        PlaybackHistoryEntity::class,
        PlaylistEntity::class,
        SyncStateEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(RoomConverters::class)
abstract class KairoDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun artistDao(): ArtistDao
    abstract fun albumDao(): AlbumDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun syncStateDao(): SyncStateDao

    companion object {
        // Preserve v1 user data and backfill remote IDs rather than rebuilding the database.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tracks ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tracks ADD COLUMN lastSyncedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tracks ADD COLUMN artistNames TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE tracks ADD COLUMN trackNumber INTEGER")
                db.execSQL("ALTER TABLE tracks ADD COLUMN discNumber INTEGER")
                db.execSQL("ALTER TABLE tracks ADD COLUMN year INTEGER")
                db.execSQL("ALTER TABLE tracks ADD COLUMN genre TEXT")
                db.execSQL("ALTER TABLE tracks ADD COLUMN coverArtId TEXT")
                db.execSQL("UPDATE tracks SET remoteId = id")

                db.execSQL("ALTER TABLE albums ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE albums ADD COLUMN lastSyncedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE albums SET remoteId = id")

                db.execSQL("ALTER TABLE artists ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE artists ADD COLUMN lastSyncedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE artists ADD COLUMN artworkUri TEXT")
                db.execSQL("ALTER TABLE artists ADD COLUMN biography TEXT")
                db.execSQL("UPDATE artists SET remoteId = id")

                db.execSQL("DROP INDEX IF EXISTS index_tracks_sourceId_id")
                db.execSQL("DROP INDEX IF EXISTS index_albums_sourceId_id")
                db.execSQL("DROP INDEX IF EXISTS index_artists_sourceId_id")
                db.execSQL("CREATE UNIQUE INDEX index_tracks_sourceId_remoteId ON tracks(sourceId, remoteId)")
                db.execSQL("CREATE UNIQUE INDEX index_albums_sourceId_remoteId ON albums(sourceId, remoteId)")
                db.execSQL("CREATE UNIQUE INDEX index_artists_sourceId_remoteId ON artists(sourceId, remoteId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_state (
                        sourceId TEXT NOT NULL PRIMARY KEY,
                        lastSyncStartedAt INTEGER,
                        lastSyncCompletedAt INTEGER,
                        lastSyncStatus TEXT NOT NULL,
                        lastSyncError TEXT,
                        artistCount INTEGER NOT NULL,
                        albumCount INTEGER NOT NULL,
                        trackCount INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}