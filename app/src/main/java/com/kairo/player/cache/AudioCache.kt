package com.kairo.player.cache

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import com.kairo.player.domain.model.StreamInfo
import java.io.File
import java.io.InputStream

data class AudioCacheEntry(
    val key: String,
    val file: File,
    val contentLength: Long,
)

interface AudioCache {
    suspend fun get(stream: StreamInfo): AudioCacheEntry?

    suspend fun put(stream: StreamInfo, input: InputStream, contentLength: Long): AudioCacheEntry

    suspend fun contains(stream: StreamInfo): Boolean

    suspend fun remove(stream: StreamInfo)

    suspend fun clear()

    suspend fun createDataSourceFactory(
        stream: StreamInfo,
        upstreamFactory: DataSource.Factory? = null,
    ): CacheDataSource.Factory
}