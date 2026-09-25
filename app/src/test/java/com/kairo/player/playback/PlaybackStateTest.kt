package com.kairo.player.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackStateTest {
    @Test
    fun eachStateCarriesPlaybackMetricsAndCurrentTrack() {
        val track = Track(id = "track-1", uri = "file:///music/track.flac", title = "Track")
        val states = listOf(
            PlaybackState.Idle(currentTrack = track),
            PlaybackState.Loading(currentTrack = track),
            PlaybackState.Playing(currentTrack = track),
            PlaybackState.Paused(currentTrack = track),
            PlaybackState.Buffering(currentTrack = track),
            PlaybackState.Completed(currentTrack = track),
            PlaybackState.Error(message = "decode failed", errorCode = 1, causeType = null, currentTrack = track),
        )

        states.forEach { state ->
            assertEquals(track, state.currentTrack)
            assertEquals(0L, state.currentPositionMs)
            assertEquals(null, state.durationMs)
            assertEquals(0L, state.bufferedPositionMs)
        }
    }
}