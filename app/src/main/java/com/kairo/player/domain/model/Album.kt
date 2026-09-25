package com.kairo.player.domain.model

data class Album(
    val id: String,
    val sourceId: String,
    val title: String,
    val artists: List<Artist> = emptyList(),
    val artwork: AlbumArt? = null,
    val releaseYear: Int? = null,
)