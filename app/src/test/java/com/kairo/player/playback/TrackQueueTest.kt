package com.kairo.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackQueueTest {
    @Test
    fun supportsSetAddRemoveAndClear() {
        val first = Track(id = "first", uri = "file:///first.flac")
        val second = Track(id = "second", uri = "file:///second.flac")
        val third = Track(id = "third", uri = "file:///third.flac")
        val queue = TrackQueue()

        queue.set(listOf(first, second))
        queue.add(third)
        assertEquals(listOf(first, second, third), queue.snapshot())

        assertEquals(second, queue.removeAt(1))
        assertEquals(listOf(first, third), queue.snapshot())

        queue.clear()
        assertTrue(queue.isEmpty)
        assertEquals(emptyList<Track>(), queue.snapshot())
    }

    @Test
    fun setReplacesTheExistingQueue() {
        val first = Track(id = "first", uri = "file:///first.flac")
        val replacement = Track(id = "replacement", uri = "file:///replacement.flac")
        val queue = TrackQueue().apply { set(listOf(first)) }

        queue.set(listOf(replacement))

        assertEquals(listOf(replacement), queue.snapshot())
    }
}