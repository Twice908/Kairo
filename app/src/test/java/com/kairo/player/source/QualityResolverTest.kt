package com.kairo.player.source

import com.kairo.player.domain.model.StreamInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QualityResolverTest {
    private val resolver = QualityResolver()

    @Test
    fun losslessOutranksHigherBitrateLossyStream() {
        val lossy = stream("lossy", codec = "mp3", bitrate = 320_000, lossless = false)
        val lossless = stream("lossless", codec = "flac", bitrate = 900_000, lossless = true)

        assertEquals(lossless, resolver.select(listOf(lossy, lossless)))
    }

    @Test
    fun usesSampleRateThenBitDepthForLosslessStreams() {
        val lowRate = stream("44k", sampleRate = 44_100, bitDepth = 24, lossless = true)
        val highRate = stream("96k", sampleRate = 96_000, bitDepth = 16, lossless = true)

        assertEquals(highRate, resolver.select(listOf(lowRate, highRate)))
    }

    @Test
    fun usesBitDepthWhenSampleRatesAreEqual() {
        val lowDepth = stream("16-bit", sampleRate = 96_000, bitDepth = 16, lossless = true)
        val highDepth = stream("24-bit", sampleRate = 96_000, bitDepth = 24, lossless = true)

        assertEquals(highDepth, resolver.select(listOf(lowDepth, highDepth)))
    }

    @Test
    fun comparesBitrateOnlyForEquivalentLossyCodecs() {
        val lowAac = stream("aac-low", codec = "mp4a.40.2", bitrate = 128_000, lossless = false)
        val highAac = stream("aac-high", codec = "mp4a.40.2", bitrate = 256_000, lossless = false)

        assertEquals(highAac, resolver.select(listOf(lowAac, highAac)))
    }

    @Test
    fun doesNotCompareBitrateAcrossDifferentLossyCodecs() {
        val first = stream("mp3", codec = "mp3", bitrate = 128_000, lossless = false)
        val higherBitrateDifferentCodec = stream("opus", codec = "opus", bitrate = 256_000, lossless = false)

        assertEquals(first, resolver.select(listOf(first, higherBitrateDifferentCodec)))
    }

    @Test
    fun prefersCompatibleCandidateWhenQualityIsOtherwiseEqual() {
        val first = stream("first", lossless = true)
        val compatible = stream("compatible", lossless = true)

        assertEquals(
            compatible,
            resolver.select(listOf(first, compatible)) { it.url == "compatible" },
        )
    }

    @Test
    fun returnsNullWhenNoStreamsExist() {
        assertNull(resolver.select(emptyList()))
    }

    private fun stream(
        url: String,
        codec: String? = null,
        bitrate: Int? = null,
        sampleRate: Int? = null,
        bitDepth: Int? = null,
        lossless: Boolean? = null,
    ) = StreamInfo(
        url = url,
        codec = codec,
        bitrate = bitrate,
        sampleRate = sampleRate,
        bitDepth = bitDepth,
        lossless = lossless,
        sourceName = "test",
    )
}