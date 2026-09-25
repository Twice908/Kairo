package com.kairo.player.source

import com.kairo.player.domain.model.ResolvedTrack
import com.kairo.player.domain.model.StreamInfo
import com.kairo.player.domain.model.Track
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamResolver @Inject constructor(
    private val registry: MusicSourceRegistry,
    private val qualityResolver: QualityResolver,
) {
    suspend fun resolve(
        track: Track,
        isCompatible: (StreamInfo) -> Boolean? = { null },
    ): StreamInfo? = qualityResolver.select(registry.resolveStreams(track), isCompatible)

    suspend fun resolveTrack(
        track: Track,
        isCompatible: (StreamInfo) -> Boolean? = { null },
    ): ResolvedTrack? = resolve(track, isCompatible)?.let { ResolvedTrack(track, it) }
}