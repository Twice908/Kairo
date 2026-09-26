package com.kairo.player.source.spotiflac

import retrofit2.http.GET
import retrofit2.http.Query

interface SpotiFlacApiService {
    @GET("search/")
    suspend fun search(
        @Query("s") query: String,
        @Query("limit") limit: Int = DEFAULT_SEARCH_LIMIT,
    ): SpotiFlacSearchResponse

    @GET("info/")
    suspend fun getTrack(@Query("id") trackId: Long): SpotiFlacTrackResponse

    @GET("track/")
    suspend fun resolveTrack(
        @Query("id") trackId: Long,
        @Query("quality") quality: String = LOSSLESS_QUALITY,
    ): SpotiFlacStreamResponse

    companion object {
        const val DEFAULT_SEARCH_LIMIT = 25
        const val LOSSLESS_QUALITY = "LOSSLESS"
    }
}