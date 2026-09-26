package com.kairo.player.di

import com.kairo.player.network.NetworkClientFactory
import com.kairo.player.source.spotiflac.SpotiFlacApiService
import com.kairo.player.source.spotiflac.SpotiFlacMusicSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SpotiFlacModule {
    @Provides
    @Singleton
    fun provideSpotiFlacRetrofit(networkClientFactory: NetworkClientFactory): Retrofit =
        networkClientFactory.createRetrofit(BASE_URL)

    @Provides
    @Singleton
    fun provideSpotiFlacApiService(retrofit: Retrofit): SpotiFlacApiService =
        retrofit.create(SpotiFlacApiService::class.java)

    @Provides
    @Singleton
    fun provideSpotiFlacMusicSource(api: SpotiFlacApiService): SpotiFlacMusicSource =
        SpotiFlacMusicSource(api)

    private const val BASE_URL = "https://triton.squid.wtf/"
}