package com.kairo.player.di

import android.content.Context
import com.kairo.player.source.MusicSourceRegistry
import com.kairo.player.source.local.LocalAudioDocumentStore
import com.kairo.player.source.local.LocalMusicSource
import com.kairo.player.source.local.SafLocalAudioDocumentStore
import com.kairo.player.source.mock.MockMusicSource
import com.kairo.player.source.spotiflac.SpotiFlacMusicSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MusicSourceModule {
    @Provides
    @Singleton
    fun provideLocalAudioDocumentStore(
        @ApplicationContext context: Context,
    ): LocalAudioDocumentStore = SafLocalAudioDocumentStore(context)

    @Provides
    @Singleton
    fun provideLocalMusicSource(store: LocalAudioDocumentStore): LocalMusicSource =
        LocalMusicSource(store)

    @Provides
    @Singleton
    fun provideMockMusicSource(): MockMusicSource = MockMusicSource()

    @Provides
    @Singleton
    fun provideMusicSourceRegistry(
        localMusicSource: LocalMusicSource,
        mockMusicSource: MockMusicSource,
        spotiFlacMusicSource: SpotiFlacMusicSource,
    ): MusicSourceRegistry =
        MusicSourceRegistry(listOf(localMusicSource, mockMusicSource, spotiFlacMusicSource))
}