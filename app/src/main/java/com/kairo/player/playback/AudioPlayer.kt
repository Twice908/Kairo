package com.kairo.player.playback

import android.os.Handler
import androidx.annotation.MainThread
import androidx.media3.common.C
import androidx.media3.common.AudioAttributes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.kairo.player.audio.AudioQualityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@UnstableApi
class AudioPlayer internal constructor(
    internal val exoPlayer: ExoPlayer,
    private val audioQualityManager: AudioQualityManager,
) : Player.Listener {
    private val queue = TrackQueue()
    private val handler = Handler(exoPlayer.applicationLooper)
    private val mutablePlaybackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle())
    private val stateMapper = PlaybackStateMapper()
    private var released = false

    val playbackState: StateFlow<PlaybackState> = mutablePlaybackState.asStateFlow()
    val currentTrack: Track?
        get() = playbackState.value.currentTrack

    private val positionUpdater = object : Runnable {
        override fun run() {
            publishPlaybackState()
        }
    }

    init {
        exoPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true,
        )
        exoPlayer.setHandleAudioBecomingNoisy(true)
        exoPlayer.addListener(this)
        exoPlayer.addAnalyticsListener(audioQualityManager)
        audioQualityManager.clear()
        publishPlaybackState()
    }

    @MainThread
    fun play() {
        stateMapper.clearError()
        if (exoPlayer.playbackState == Player.STATE_IDLE && !queue.isEmpty) {
            exoPlayer.prepare()
        }
        exoPlayer.play()
        publishPlaybackState()
    }

    @MainThread
    fun pause() {
        exoPlayer.pause()
        publishPlaybackState()
    }

    @MainThread
    fun seekTo(positionMs: Long) {
        require(positionMs >= 0L) { "Position must not be negative" }
        exoPlayer.seekTo(positionMs)
        publishPlaybackState()
    }

    @MainThread
    fun next() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        }
    }

    @MainThread
    fun previous() {
        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        }
    }

    @MainThread
    fun setQueue(tracks: List<Track>, startIndex: Int = 0, startPositionMs: Long = 0L) {
        require(startPositionMs >= 0L) { "Start position must not be negative" }
        if (tracks.isNotEmpty()) {
            require(startIndex in tracks.indices) { "Start index is outside the queue" }
        }

        queue.set(tracks)
        stateMapper.reset()
        audioQualityManager.clear()
        if (tracks.isEmpty()) {
            exoPlayer.clearMediaItems()
        } else {
            exoPlayer.setMediaItems(tracks.map(Track::toMediaItem), startIndex, startPositionMs)
            exoPlayer.prepare()
        }
        publishPlaybackState()
    }

    @MainThread
    fun addToQueue(track: Track) {
        val wasEmpty = queue.isEmpty
        queue.add(track)
        exoPlayer.addMediaItem(track.toMediaItem())
        if (wasEmpty) {
            stateMapper.reset()
            exoPlayer.prepare()
        }
        publishPlaybackState()
    }

    @MainThread
    fun removeFromQueue(index: Int) {
        require(index in 0 until queue.size) { "Queue index is outside the queue" }
        queue.removeAt(index)
        exoPlayer.removeMediaItem(index)
        if (queue.isEmpty) {
            audioQualityManager.clear()
        }
        publishPlaybackState()
    }

    @MainThread
    fun clearQueue() {
        queue.clear()
        stateMapper.reset()
        audioQualityManager.clear()
        exoPlayer.clearMediaItems()
        publishPlaybackState()
    }

    @MainThread
    fun release() {
        if (released) return
        released = true
        handler.removeCallbacks(positionUpdater)
        exoPlayer.removeListener(this)
        exoPlayer.removeAnalyticsListener(audioQualityManager)
        audioQualityManager.clear()
        exoPlayer.release()
    }

    override fun onEvents(player: Player, events: Player.Events) {
        publishPlaybackState()
    }

    override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
        audioQualityManager.clear()
        publishPlaybackState()
    }

    override fun onPlayerError(error: PlaybackException) {
        stateMapper.recordError(error)
        publishPlaybackState()
    }

    private fun publishPlaybackState() {
        mutablePlaybackState.value = stateMapper.map(
            exoPlayer,
            queue.getOrNull(exoPlayer.currentMediaItemIndex),
        )

        handler.removeCallbacks(positionUpdater)
        if (exoPlayer.isPlaying) {
            handler.postDelayed(positionUpdater, POSITION_UPDATE_INTERVAL_MS)
        }
    }

    private companion object {
        const val POSITION_UPDATE_INTERVAL_MS = 250L
    }
}