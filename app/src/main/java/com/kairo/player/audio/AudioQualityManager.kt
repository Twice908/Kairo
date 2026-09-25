package com.kairo.player.audio

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@UnstableApi
@Singleton
class AudioQualityManager @Inject constructor() : AnalyticsListener {
    private val mutableCurrentFormat = MutableStateFlow<AudioFormatInfo?>(null)
    private var decoderName: String? = null

    val currentFormat: StateFlow<AudioFormatInfo?> = mutableCurrentFormat.asStateFlow()

    override fun onAudioInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?,
    ) {
        updateFromFormat(format)
    }

    override fun onAudioDecoderInitialized(
        eventTime: AnalyticsListener.EventTime,
        decoderName: String,
        initializedTimestampMs: Long,
        initializationDurationMs: Long,
    ) {
        this.decoderName = decoderName
        mutableCurrentFormat.value = mutableCurrentFormat.value?.copy(decoderName = decoderName)
    }

    fun clear() {
        decoderName = null
        mutableCurrentFormat.value = null
    }

    internal fun updateFromFormat(format: Format) {
        decoderName = null
        val mimeType = format.sampleMimeType
        mutableCurrentFormat.value = AudioFormatInfo(
            codec = format.codecs?.takeIf(String::isNotBlank),
            mimeType = mimeType,
            bitrateBitsPerSecond = format.averageBitrate.takeIf { it > 0 }
                ?: format.bitrate.takeIf { it > 0 },
            sampleRateHz = format.sampleRate.takeIf { it > 0 },
            channelCount = format.channelCount.takeIf { it > 0 },
            bitDepth = bitDepth(format),
            lossless = losslessFrom(mimeType, format.codecs),
        )
    }

    private fun bitDepth(format: Format): Int? {
        if (format.sampleMimeType != "audio/raw") return null
        return when (format.pcmEncoding) {
            C.ENCODING_PCM_8BIT -> 8
            C.ENCODING_PCM_16BIT -> 16
            C.ENCODING_PCM_24BIT -> 24
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 32
            else -> null
        }
    }

    private fun losslessFrom(mimeType: String?, codecs: String?): Boolean? {
        val codec = codecs?.lowercase().orEmpty()
        val mime = mimeType?.lowercase()
        return when {
            codec.startsWith("flac") || codec.startsWith("alac") -> true
            mime == "audio/flac" || mime == "audio/alac" -> true
            mime == "audio/mpeg" || mime == "audio/mp4a-latm" || mime == "audio/aac" ||
                mime == "audio/opus" || mime == "audio/vorbis" ||
                mime == "audio/ac3" || mime == "audio/eac3" || mime == "audio/ac4" ||
                mime == "audio/amr-nb" || mime == "audio/amr-wb" -> false
            else -> null
        }
    }
}