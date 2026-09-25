package com.kairo.player.audio

data class AudioFormatInfo(
    val codec: String? = null,
    val mimeType: String? = null,
    val bitrateBitsPerSecond: Int? = null,
    val sampleRateHz: Int? = null,
    val channelCount: Int? = null,
    val bitDepth: Int? = null,
    val lossless: Boolean? = null,
    val decoderName: String? = null,
)