package com.kairo.player.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class Track(
    val id: String,
    val uri: String,
    val title: String? = null,
    val mimeType: String? = null,
) {
    init {
        require(id.isNotBlank()) { "Track id must not be blank" }
        require(uri.isNotBlank()) { "Track URI must not be blank" }
    }

    internal fun toMediaItem(): MediaItem {
        val builder = MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
        mimeType?.let(builder::setMimeType)
        return builder.build()
    }

    internal companion object {
        fun fromMediaItem(mediaItem: MediaItem?): Track? {
            val uri = mediaItem?.localConfiguration?.uri?.toString() ?: return null
            return Track(
                id = mediaItem.mediaId.ifBlank { uri },
                uri = uri,
                title = mediaItem.mediaMetadata.title?.toString(),
                mimeType = mediaItem.localConfiguration?.mimeType,
            )
        }
    }
}