package com.kairo.player.playback

import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player

internal class PlaybackStateMapper {
    private var hasReachedReady = false
    private var lastError: PlaybackException? = null

    fun reset() {
        hasReachedReady = false
        lastError = null
    }

    fun clearError() {
        lastError = null
    }

    fun recordError(error: PlaybackException) {
        lastError = error
    }

    fun map(player: Player, currentTrack: Track?): PlaybackState {
        val currentPositionMs = player.currentPosition.coerceAtLeast(0L)
        val durationMs = player.duration.takeIf { it != C.TIME_UNSET && it >= 0L }
        val bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L)
        val error = lastError

        if (error != null) {
            return PlaybackState.Error(
                message = error.message,
                errorCode = error.errorCode,
                causeType = error.cause?.javaClass?.name,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                bufferedPositionMs = bufferedPositionMs,
                currentTrack = currentTrack,
            )
        }

        if (player.mediaItemCount == 0) return PlaybackState.Idle()

        return when (player.playbackState) {
            Player.STATE_BUFFERING -> if (hasReachedReady) {
                PlaybackState.Buffering(
                    currentPositionMs,
                    durationMs,
                    bufferedPositionMs,
                    currentTrack,
                )
            } else {
                PlaybackState.Loading(
                    currentPositionMs,
                    durationMs,
                    bufferedPositionMs,
                    currentTrack,
                )
            }
            Player.STATE_READY -> {
                hasReachedReady = true
                if (player.isPlaying) {
                    PlaybackState.Playing(currentPositionMs, durationMs, bufferedPositionMs, currentTrack)
                } else {
                    PlaybackState.Paused(currentPositionMs, durationMs, bufferedPositionMs, currentTrack)
                }
            }
            Player.STATE_ENDED -> PlaybackState.Completed(
                currentPositionMs,
                durationMs,
                bufferedPositionMs,
                currentTrack,
            )
            else -> PlaybackState.Idle()
        }
    }
}