package com.kairo.player.network

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkClientFactory @Inject constructor() {
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    fun createRetrofit(baseUrl: String): Retrofit {
        val parsedUrl = baseUrl.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Base URL must be a valid HTTPS URL")
        require(parsedUrl.isHttps) { "Only HTTPS endpoints are allowed" }
        require(baseUrl.endsWith('/')) { "Retrofit base URL must end with '/'" }
        require(parsedUrl.username.isEmpty() && parsedUrl.password.isEmpty()) {
            "Credentials must not be embedded in the base URL"
        }

        return Retrofit.Builder()
            .baseUrl(parsedUrl)
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
    }

    fun <T : Any> createService(baseUrl: String, serviceClass: Class<T>): T =
        createRetrofit(baseUrl).create(serviceClass)

    private companion object {
        const val CONNECT_TIMEOUT_SECONDS = 15L
        const val READ_TIMEOUT_SECONDS = 30L
        const val WRITE_TIMEOUT_SECONDS = 15L
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}