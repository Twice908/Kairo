package com.kairo.player.domain.model

data class Artist(
    val id: String,
    val sourceId: String,
    val name: String,
    val artwork: AlbumArt? = null,
    val biography: String? = null,
)