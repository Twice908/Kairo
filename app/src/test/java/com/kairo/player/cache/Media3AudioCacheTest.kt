package com.kairo.player.cache

import com.kairo.player.domain.model.StreamInfo
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class Media3AudioCacheTest {
    @Test
    fun putsGetsChecksRemovesAndClearsPermittedContent() = runBlocking {
        val cache = Media3AudioCache(RuntimeEnvironment.getApplication())
        val first = StreamInfo(
            url = "https://open.example.invalid/audio/first",
            sourceName = "fixture source",
            cacheAllowed = true,
        )
        val second = first.copy(url = "https://open.example.invalid/audio/second")
        val content = byteArrayOf(4, 8, 15, 16, 23, 42)

        try {
            val entry = cache.put(first, ByteArrayInputStream(content), content.size.toLong())

            assertTrue(cache.contains(first))
            assertArrayEquals(content, entry.file.readBytes())
            assertNotNull(cache.get(first))

            cache.remove(first)
            assertFalse(cache.contains(first))

            cache.put(first, ByteArrayInputStream(content), content.size.toLong())
            cache.put(second, ByteArrayInputStream(content), content.size.toLong())
            cache.clear()
            assertFalse(cache.contains(first))
            assertFalse(cache.contains(second))
        } finally {
            cache.close()
        }
    }

    @Test
    fun refusesToCacheContentWithoutSourcePermission() {
        val cache = Media3AudioCache(RuntimeEnvironment.getApplication())
        val stream = StreamInfo(
            url = "https://source.example.invalid/protected",
            sourceName = "fixture source",
            cacheAllowed = false,
        )

        try {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { cache.put(stream, ByteArrayInputStream(byteArrayOf(1)), 1L) }
            }
            assertFalse(runBlocking { cache.contains(stream) })
        } finally {
            runBlocking { cache.close() }
        }
    }
}