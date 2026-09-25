package com.kairo.player.audio

import androidx.media3.common.C
import androidx.media3.common.Format
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioQualityManagerTest {
    @Test
    fun exposesKnownEncodedFormatWithoutInferringBitDepth() {
        val manager = AudioQualityManager()
        val format = Format.Builder()
            .setSampleMimeType("audio/flac")
            .setCodecs("flac")
            .setAverageBitrate(900_000)
            .setSampleRate(96_000)
            .setChannelCount(2)
            .build()

        manager.updateFromFormat(format)

        val info = manager.currentFormat.value
        assertEquals("flac", info?.codec)
        assertEquals("audio/flac", info?.mimeType)
        assertEquals(900_000, info?.bitrateBitsPerSecond)
        assertEquals(96_000, info?.sampleRateHz)
        assertEquals(2, info?.channelCount)
        assertEquals(true, info?.lossless)
        assertNull(info?.bitDepth)
        assertNull(info?.decoderName)
    }

    @Test
    fun reportsRawPcmBitDepthOnlyWhenEncodingIsKnown() {
        val manager = AudioQualityManager()
        manager.updateFromFormat(
            Format.Builder()
                .setSampleMimeType("audio/raw")
                .setPcmEncoding(C.ENCODING_PCM_24BIT)
                .build(),
        )

        assertEquals(24, manager.currentFormat.value?.bitDepth)
    }

    @Test
    fun leavesUnknownPropertiesNull() {
        val manager = AudioQualityManager()
        manager.updateFromFormat(Format.Builder().setSampleMimeType("audio/unknown").build())

        val info = manager.currentFormat.value
        assertNull(info?.codec)
        assertNull(info?.bitrateBitsPerSecond)
        assertNull(info?.sampleRateHz)
        assertNull(info?.channelCount)
        assertNull(info?.bitDepth)
        assertNull(info?.lossless)
    }

    @Test
    fun identifiesAacAsLossy() {
        val manager = AudioQualityManager()
        manager.updateFromFormat(Format.Builder().setSampleMimeType("audio/mp4a-latm").build())

        assertEquals(false, manager.currentFormat.value?.lossless)
    }
}