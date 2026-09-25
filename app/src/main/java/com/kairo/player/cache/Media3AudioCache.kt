package com.kairo.player.cache

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheSpan
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.ContentMetadataMutations
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.kairo.player.domain.model.StreamInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@UnstableApi
@Singleton
class Media3AudioCache @Inject constructor(
    @ApplicationContext context: Context,
) : AudioCache {
    private val context = context.applicationContext
    private val cacheMutex = Mutex()
    private val mutationMutex = Mutex()
    private val closed = AtomicBoolean(false)
    @Volatile
    private var mediaCache: SimpleCache? = null

    override suspend fun get(stream: StreamInfo): AudioCacheEntry? = withContext(Dispatchers.IO) {
        if (!stream.cacheAllowed) return@withContext null
        val cache = cache()
        val contentLength = contentLength(cache, stream.url) ?: return@withContext null
        if (!cache.isCached(stream.url, 0L, contentLength)) return@withContext null
        val span = cache.getCachedSpans(stream.url).firstOrNull {
            it.position == 0L && it.length >= contentLength && it.isCached
        } ?: return@withContext null
        span.file?.let { AudioCacheEntry(stream.url, it, contentLength) }
    }

    override suspend fun put(
        stream: StreamInfo,
        input: InputStream,
        contentLength: Long,
    ): AudioCacheEntry = withContext(Dispatchers.IO) {
        require(stream.cacheAllowed) { "The source does not permit caching this stream" }
        require(contentLength > 0L) { "Content length must be positive" }
        mutationMutex.withLock {
            val cache = cache()
            get(stream)?.let { return@withLock it }

            cache.removeResource(stream.url)
            val holeSpan = cache.startReadWrite(stream.url, 0L, contentLength)
            check(!holeSpan.isCached) { "Stream is already cached" }
            var cacheFile: File? = null
            var committed = false
            var removeCommittedFile = false
            try {
                cacheFile = cache.startFile(stream.url, 0L, contentLength)
                val written = input.use { source ->
                    cacheFile.outputStream().buffered().use { target -> source.copyTo(target) }
                }
                require(written == contentLength) {
                    "Expected $contentLength bytes but received $written"
                }
                cache.commitFile(cacheFile, written)
                committed = true
                cache.applyContentMetadataMutations(
                    stream.url,
                    ContentMetadataMutations.setContentLength(ContentMetadataMutations(), written),
                )
                get(stream) ?: throw IOException("Media3 did not expose the completed cache entry")
            } catch (exception: Exception) {
                if (committed) removeCommittedFile = true else cacheFile?.delete()
                throw exception
            } finally {
                cache.releaseHoleSpan(holeSpan)
                if (removeCommittedFile) cache.removeResource(stream.url)
            }
        }
    }

    override suspend fun contains(stream: StreamInfo): Boolean = withContext(Dispatchers.IO) {
        if (!stream.cacheAllowed) return@withContext false
        val cache = cache()
        val length = contentLength(cache, stream.url) ?: return@withContext false
        cache.isCached(stream.url, 0L, length)
    }

    override suspend fun remove(stream: StreamInfo) = withContext(Dispatchers.IO) {
        mutationMutex.withLock { cache().removeResource(stream.url) }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        mutationMutex.withLock {
            val cache = cache()
            cache.keys.toList().forEach(cache::removeResource)
        }
    }

    override suspend fun createDataSourceFactory(
        stream: StreamInfo,
        upstreamFactory: DataSource.Factory?,
    ): CacheDataSource.Factory = withContext(Dispatchers.IO) {
        val factory = CacheDataSource.Factory()
            .setCache(cache())
            .setUpstreamDataSourceFactory(upstreamFactory)
        if (!stream.cacheAllowed) factory.setCacheWriteDataSinkFactory(null)
        factory
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        mutationMutex.withLock {
            if (closed.compareAndSet(false, true)) {
                mediaCache?.release()
                mediaCache = null
            }
        }
    }

    private suspend fun cache(): SimpleCache {
        check(!closed.get()) { "Audio cache has been closed" }
        mediaCache?.let { return it }
        return cacheMutex.withLock {
            mediaCache?.let { return@withLock it }
            withContext(Dispatchers.IO) {
                SimpleCache(
                    File(context.cacheDir, CACHE_DIRECTORY),
                    LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
                    StandaloneDatabaseProvider(context),
                ).also { mediaCache = it }
            }
        }
    }

    private fun contentLength(cache: SimpleCache, key: String): Long? {
        val length = ContentMetadata.getContentLength(cache.getContentMetadata(key))
        return length.takeIf { it > 0L }
    }

    private companion object {
        const val CACHE_DIRECTORY = "audio-media3-cache"
        const val MAX_CACHE_BYTES = 512L * 1024L * 1024L
    }
}