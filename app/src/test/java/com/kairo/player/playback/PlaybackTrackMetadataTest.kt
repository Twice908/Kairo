package com.kairo.player.playback

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaybackTrackMetadataTest {
    @Test
    fun sourceAndPresentationMetadataSurviveMediaItemConversion() {
        val track = Track(
            id = "queue-entry-1",
            uri = "content://music/track-1",
            title = "Evening Light",
            artist = "Kairo Ensemble",
            sourceId = "local",
            sourceTrackId = "content://music/track-1",
        )

        val restored = Track.fromMediaItem(track.toMediaItem())

        assertEquals(track.id, restored?.id)
        assertEquals(track.title, restored?.title)
        assertEquals(track.artist, restored?.artist)
        assertEquals(track.sourceId, restored?.sourceId)
        assertEquals(track.sourceTrackId, restored?.sourceTrackId)
    }
}
