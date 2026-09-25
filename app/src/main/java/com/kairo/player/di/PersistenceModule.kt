package com.kairo.player.di

import android.content.Context
import androidx.room.Room
import com.kairo.player.cache.AudioCache
import com.kairo.player.cache.Media3AudioCache
import com.kairo.player.data.local.dao.AlbumDao
import com.kairo.player.data.local.dao.ArtistDao
import com.kairo.player.data.local.dao.PlaybackHistoryDao
import com.kairo.player.data.local.dao.PlaylistDao
import com.kairo.player.data.local.dao.TrackDao
import com.kairo.player.data.local.KairoDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioCacheBindingModule {
    @Binds
    @Singleton
    abstract fun bindAudioCache(implementation: Media3AudioCache): AudioCache
}

@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {
    @Provides
    @Singleton
    fun provideKairoDatabase(@ApplicationContext context: Context): KairoDatabase =
        Room.databaseBuilder(context, KairoDatabase::class.java, DATABASE_NAME).build()

    @Provides
    fun provideTrackDao(database: KairoDatabase): TrackDao = database.trackDao()

    @Provides
    fun provideArtistDao(database: KairoDatabase): ArtistDao = database.artistDao()

    @Provides
    fun provideAlbumDao(database: KairoDatabase): AlbumDao = database.albumDao()

    @Provides
    fun providePlaybackHistoryDao(database: KairoDatabase): PlaybackHistoryDao =
        database.playbackHistoryDao()

    @Provides
    fun providePlaylistDao(database: KairoDatabase): PlaylistDao = database.playlistDao()

    private const val DATABASE_NAME = "kairo.db"
}