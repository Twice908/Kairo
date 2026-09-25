package com.kairo.player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kairo.player.data.local.dao.AlbumDao
import com.kairo.player.data.local.dao.ArtistDao
import com.kairo.player.data.local.dao.PlaybackHistoryDao
import com.kairo.player.data.local.dao.PlaylistDao
import com.kairo.player.data.local.dao.TrackDao
import com.kairo.player.data.local.entity.AlbumEntity
import com.kairo.player.data.local.entity.ArtistEntity
import com.kairo.player.data.local.entity.PlaybackHistoryEntity
import com.kairo.player.data.local.entity.PlaylistEntity
import com.kairo.player.data.local.entity.TrackEntity

@Database(
    entities = [
        TrackEntity::class,
        ArtistEntity::class,
        AlbumEntity::class,
        PlaybackHistoryEntity::class,
        PlaylistEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(RoomConverters::class)
abstract class KairoDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun artistDao(): ArtistDao
    abstract fun albumDao(): AlbumDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun playlistDao(): PlaylistDao
}