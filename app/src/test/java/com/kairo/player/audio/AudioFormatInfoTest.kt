package com.kairo.player.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioFormatInfoTest {
    @Test
    fun retainsOnlyProvidedFormatValues() {
        val info = AudioFormatInfo(
            codec = "flac",
            mimeType = "audio/flac",
            sampleRateHz = 96_000,
            channelCount = 2,
            lossless = true,
        )

        assertEquals("flac", info.codec)
        assertEquals(96_000, info.sampleRateHz)
        assertNull(info.bitrateBitsPerSecond)
        assertNull(info.bitDepth)
        assertNull(info.decoderName)
    }
}