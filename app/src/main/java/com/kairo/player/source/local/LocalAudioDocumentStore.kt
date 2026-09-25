package com.kairo.player.source.local

import com.kairo.player.domain.model.AlbumArt

data class LocalAudioDocument(
    val uri: String,
    val displayName: String,
    val mimeType: String?,
)

data class LocalAudioMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
    val albumArt: AlbumArt? = null,
)

interface LocalAudioDocumentStore {
    suspend fun persistTree(uri: String)
    suspend fun persistDocument(uri: String)
    suspend fun removeSelection(uri: String)
    suspend fun listDocuments(): List<LocalAudioDocument>
    suspend fun getDocument(uri: String): LocalAudioDocument?
    suspend fun getMetadata(uri: String): LocalAudioMetadata?
}