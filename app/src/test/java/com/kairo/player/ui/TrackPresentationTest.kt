package com.kairo.player.ui

import com.kairo.player.domain.model.Artist
import com.kairo.player.domain.model.Track as LibraryTrack
import com.kairo.player.playback.Track as PlaybackTrack
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackPresentationTest {
    @Test
    fun mapsLibraryTrackIdentityAndMetadata() {
        val track = LibraryTrack(
            id = "track-1",
            sourceId = "local",
            title = "Evening Light",
            artists = listOf(Artist(id = "artist-1", sourceId = "local", name = "Kairo Ensemble")),
            durationMs = 185_000L,
        )

        assertEquals(
            TrackPresentation("local:track-1", "Evening Light", "Kairo Ensemble", 185_000L),
            track.toPresentation(),
        )
    }

    @Test
    fun mapsPlaybackMetadataAndFallbackLabels() {
        val track = PlaybackTrack(id = "remote:track-2", uri = "https://example.test/audio", title = "Signal", artist = "North")

        assertEquals(TrackPresentation("remote:track-2", "Signal", "North"), track.toPresentation())
        assertEquals(
            TrackPresentation("unknown", "Unknown track", "Unknown artist"),
            PlaybackTrack(id = "unknown", uri = "file:///audio").toPresentation(),
        )
    }
}
