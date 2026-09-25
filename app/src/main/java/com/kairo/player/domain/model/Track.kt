package com.kairo.player.domain.model

data class Track(
    val id: String,
    val sourceId: String,
    val title: String,
    val artists: List<Artist> = emptyList(),
    val album: Album? = null,
    val durationMs: Long? = null,
)