package com.kairo.player.source

import com.kairo.player.domain.model.StreamInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QualityResolver @Inject constructor() {
    fun select(
        streams: List<StreamInfo>,
        isCompatible: (StreamInfo) -> Boolean? = { null },
    ): StreamInfo? {
        if (streams.isEmpty()) return null

        var candidates = streams
        val lossless = candidates.filter { it.lossless == true }
        if (lossless.isNotEmpty()) candidates = lossless

        candidates = retainHighestKnown(candidates) { it.sampleRate }
        candidates = retainHighestKnown(candidates) { it.bitDepth }

        if (candidates.size > 1 && candidates.all { it.lossless == false }) {
            val codecs = candidates.map(::codecKey).distinct()
            if (codecs.size == 1 && codecs.single() != null) {
                candidates = retainHighestKnown(candidates) { it.bitrate }
            }
        }

        val compatible = candidates.filter { isCompatible(it) == true }
        if (compatible.isNotEmpty()) return compatible.first()

        val unknownCompatibility = candidates.filter { isCompatible(it) == null }
        return (unknownCompatibility.ifEmpty { candidates }).firstOrNull()
    }

    private fun codecKey(stream: StreamInfo): String? =
        stream.codec?.lowercase()?.substringBefore('.')?.takeIf(String::isNotBlank)
            ?: stream.mimeType?.lowercase()?.takeIf(String::isNotBlank)

    private fun retainHighestKnown(
        streams: List<StreamInfo>,
        value: (StreamInfo) -> Int?,
    ): List<StreamInfo> {
        val highest = streams.mapNotNull(value).maxOrNull() ?: return streams
        return streams.filter { value(it) == highest }
    }
}