package com.kairo.player.di

import com.kairo.player.network.NetworkClientFactory
import com.kairo.player.server.NavidromeApiService
import com.kairo.player.server.SubsonicAuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NavidromeModule {
    // Placeholder only. SubsonicAuthInterceptor swaps in the real host at request time.
    private const val PLACEHOLDER_BASE_URL = "http://localhost/"

    @Provides
    @Singleton
    fun provideNavidromeApiService(
        factory: NetworkClientFactory,
        authInterceptor: SubsonicAuthInterceptor,
    ): NavidromeApiService {
        val client = factory.httpClient.newBuilder()
            .addInterceptor(authInterceptor)
            .build()
        return factory.createRetrofit(PLACEHOLDER_BASE_URL, client)
            .create(NavidromeApiService::class.java)
    }
}