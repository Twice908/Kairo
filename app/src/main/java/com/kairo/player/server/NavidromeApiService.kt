package com.kairo.player.server

import retrofit2.http.GET
import retrofit2.http.Query

interface NavidromeApiService {
    @GET("rest/ping")
    suspend fun ping(): SubsonicEnvelope

    @GET("rest/getArtists")
    suspend fun getArtists(): SubsonicEnvelope

    @GET("rest/getArtist")
    suspend fun getArtist(@Query("id") id: String): SubsonicEnvelope

    @GET("rest/getAlbum")
    suspend fun getAlbum(@Query("id") id: String): SubsonicEnvelope

    @GET("rest/getSong")
    suspend fun getSong(@Query("id") id: String): SubsonicEnvelope

    @GET("rest/search3")
    suspend fun search3(
        @Query("query") query: String,
        @Query("artistCount") artistCount: Int = 10,
        @Query("albumCount") albumCount: Int = 10,
        @Query("songCount") songCount: Int = 30,
    ): SubsonicEnvelope

    @GET("rest/getAlbumList2")
    suspend fun getAlbumList2(
        @Query("type") type: String = "alphabeticalByName",
        @Query("size") size: Int = 50,
        @Query("offset") offset: Int = 0,
    ): SubsonicEnvelope

    @GET("rest/startScan")
    suspend fun startScan(): SubsonicEnvelope

    @GET("rest/getScanStatus")
    suspend fun getScanStatus(): SubsonicEnvelope
}