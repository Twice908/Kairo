package com.kairo.player.playback

sealed interface PlaybackState {
    val currentPositionMs: Long
    val durationMs: Long?
    val bufferedPositionMs: Long
    val currentTrack: Track?

    data class Idle(
        override val currentPositionMs: Long = 0L,
        override val durationMs: Long? = null,
        override val bufferedPositionMs: Long = 0L,
        override val currentTrack: Track? = null,
    ) : PlaybackState

    data class Loading(
        override val currentPositionMs: Long = 0L,
        override val durationMs: Long? = null,
        override val bufferedPositionMs: Long = 0L,
        override val currentTrack: Track? = null,
    ) : PlaybackState

    data class Playing(
        override val currentPositionMs: Long = 0L,
        override val durationMs: Long? = null,
        override val bufferedPositionMs: Long = 0L,
        override val currentTrack: Track? = null,
    ) : PlaybackState

    data class Paused(
        override val currentPositionMs: Long = 0L,
        override val durationMs: Long? = null,
        override val bufferedPositionMs: Long = 0L,
        override val currentTrack: Track? = null,
    ) : PlaybackState

    data class Buffering(
        override val currentPositionMs: Long = 0L,
        override val durationMs: Long? = null,
        override val bufferedPositionMs: Long = 0L,
        override val currentTrack: Track? = null,
    ) : PlaybackState

    data class Completed(
        override val currentPositionMs: Long = 0L,
        override val durationMs: Long? = null,
        override val bufferedPositionMs: Long = 0L,
        override val currentTrack: Track? = null,
    ) : PlaybackState

    data class Error(
        val message: String?,
        val errorCode: Int,
        val causeType: String?,
        override val currentPositionMs: Long = 0L,
        override val durationMs: Long? = null,
        override val bufferedPositionMs: Long = 0L,
        override val currentTrack: Track? = null,
    ) : PlaybackState
}