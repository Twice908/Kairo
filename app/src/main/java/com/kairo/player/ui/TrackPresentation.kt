package com.kairo.player.ui

import com.kairo.player.domain.model.Track as LibraryTrack
import com.kairo.player.playback.Track as PlaybackTrack

data class TrackPresentation(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long? = null,
    val artworkUri: String? = null,
)

fun LibraryTrack.toPresentation(): TrackPresentation = TrackPresentation(
    id = "$sourceId:$id",
    title = title,
    artist = artists.joinToString { it.name }.ifBlank { "Unknown artist" },
    durationMs = durationMs,
    artworkUri = album?.artwork?.uri,
)

fun PlaybackTrack.toPresentation(durationMs: Long? = null): TrackPresentation = TrackPresentation(
    id = id,
    title = title ?: "Unknown track",
    artist = artist?.takeIf(String::isNotBlank) ?: "Unknown artist",
    durationMs = durationMs,
    artworkUri = artworkUri,
)
