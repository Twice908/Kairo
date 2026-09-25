package com.kairo.player.audio

import com.kairo.player.playback.PlaybackController
import com.kairo.player.playback.PlaybackState
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class AudioDiagnosticsSnapshot(
    val currentFormat: AudioFormatInfo?,
    val playbackState: PlaybackState,
    val playbackPositionMs: Long,
    val bufferedPositionMs: Long,
) {
    val codec: String?
        get() = currentFormat?.codec
    val mimeType: String?
        get() = currentFormat?.mimeType
    val bitrateBitsPerSecond: Int?
        get() = currentFormat?.bitrateBitsPerSecond
    val sampleRateHz: Int?
        get() = currentFormat?.sampleRateHz
    val channelCount: Int?
        get() = currentFormat?.channelCount
    val bitDepth: Int?
        get() = currentFormat?.bitDepth
    val lossless: Boolean?
        get() = currentFormat?.lossless
    val decoderName: String?
        get() = currentFormat?.decoderName
}

class AudioDiagnostics @Inject constructor(
    private val playbackController: PlaybackController,
    private val audioQualityManager: AudioQualityManager,
) {
    val snapshots: Flow<AudioDiagnosticsSnapshot> = combine(
        playbackController.playbackState,
        audioQualityManager.currentFormat,
        ::createSnapshot,
    )

    fun currentSnapshot(): AudioDiagnosticsSnapshot = createSnapshot(
        playbackController.playbackState.value,
        audioQualityManager.currentFormat.value,
    )

    private fun createSnapshot(
        playbackState: PlaybackState,
        format: AudioFormatInfo?,
    ) = AudioDiagnosticsSnapshot(
        currentFormat = format,
        playbackState = playbackState,
        playbackPositionMs = playbackState.currentPositionMs,
        bufferedPositionMs = playbackState.bufferedPositionMs,
    )
}