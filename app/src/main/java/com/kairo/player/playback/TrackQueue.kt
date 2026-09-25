package com.kairo.player.playback

internal class TrackQueue {
    private val items = mutableListOf<Track>()

    val size: Int
        get() = items.size

    val isEmpty: Boolean
        get() = items.isEmpty()

    fun snapshot(): List<Track> = items.toList()

    fun getOrNull(index: Int): Track? = items.getOrNull(index)

    fun set(tracks: List<Track>) {
        items.clear()
        items.addAll(tracks)
    }

    fun add(track: Track) {
        items.add(track)
    }

    fun removeAt(index: Int): Track = items.removeAt(index)

    fun clear() {
        items.clear()
    }
}