package com.kairo.player.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class Track(
    val id: String,
    val uri: String,
    val title: String? = null,
    val mimeType: String? = null,
    val artist: String? = null,
    val artworkUri: String? = null,
    val sourceId: String? = null,
    val sourceTrackId: String? = null,
) {
    init {
        require(id.isNotBlank()) { "Track id must not be blank" }
        require(uri.isNotBlank()) { "Track URI must not be blank" }
    }

    internal fun toMediaItem(): MediaItem {
        val builder = MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(artworkUri?.let(Uri::parse))
                    .setExtras(Bundle().apply {
                        sourceId?.let { putString(METADATA_SOURCE_ID, it) }
                        sourceTrackId?.let { putString(METADATA_TRACK_ID, it) }
                    })
                    .build(),
            )
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
                artist = mediaItem.mediaMetadata.artist?.toString(),
                artworkUri = mediaItem.mediaMetadata.artworkUri?.toString(),
                sourceId = mediaItem.mediaMetadata.extras?.getString(METADATA_SOURCE_ID),
                sourceTrackId = mediaItem.mediaMetadata.extras?.getString(METADATA_TRACK_ID),
            )
        }

        private const val METADATA_SOURCE_ID = "com.kairo.player.source_id"
        private const val METADATA_TRACK_ID = "com.kairo.player.source_track_id"
    }
}