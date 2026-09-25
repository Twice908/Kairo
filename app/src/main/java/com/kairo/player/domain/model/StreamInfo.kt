package com.kairo.player.domain.model

data class StreamInfo(
    val url: String,
    val mimeType: String? = null,
    val codec: String? = null,
    val bitrate: Int? = null,
    val sampleRate: Int? = null,
    val bitDepth: Int? = null,
    val channels: Int? = null,
    val lossless: Boolean? = null,
    val sourceName: String,
    val cacheAllowed: Boolean = false,
)