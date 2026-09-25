package com.kairo.player.playback

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.kairo.player.audio.AudioQualityManager
import com.kairo.player.domain.model.ResolvedTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface PlaybackConnectionState {
    data object Connecting : PlaybackConnectionState
    data object Connected : PlaybackConnectionState
    data class Failed(val message: String?) : PlaybackConnectionState
}

@UnstableApi
@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext context: Context,
    private val audioQualityManager: AudioQualityManager,
) : Player.Listener {
    private val handler = Handler(Looper.getMainLooper())
    private val stateMapper = PlaybackStateMapper()
    private val mutablePlaybackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle())
    private val mutableConnectionState = MutableStateFlow<PlaybackConnectionState>(
        PlaybackConnectionState.Connecting,
    )
    private val mutableQueueState = MutableStateFlow(PlaybackQueueState())
    private var mediaController: MediaController? = null
    private val controllerFuture = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, PlaybackService::class.java)),
    ).buildAsync()

    val playbackState: StateFlow<PlaybackState> = mutablePlaybackState.asStateFlow()
    val connectionState: StateFlow<PlaybackConnectionState> = mutableConnectionState.asStateFlow()
    val queueState: StateFlow<PlaybackQueueState> = mutableQueueState.asStateFlow()
    val currentTrack: Track?
        get() = playbackState.value.currentTrack

    private val positionUpdater = object : Runnable {
        override fun run() {
            mediaController?.let(::publishPlaybackState)
        }
    }

    init {
        controllerFuture.addListener(
            {
                try {
                    val controller = controllerFuture.get()
                    mediaController = controller
                    controller.addListener(this)
                    mutableConnectionState.value = PlaybackConnectionState.Connected
                    publishPlaybackState(controller, updateQueue = true)
                } catch (exception: Exception) {
                    mutableConnectionState.value = PlaybackConnectionState.Failed(
                        exception.cause?.message ?: exception.message,
                    )
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    @MainThread
    fun play() {
        val controller = requireController()
        stateMapper.clearError()
        if (controller.playbackState == Player.STATE_IDLE && controller.mediaItemCount > 0) {
            controller.prepare()
        }
        controller.play()
        publishPlaybackState(controller)
    }

    @MainThread
    fun pause() {
        val controller = requireController()
        controller.pause()
        publishPlaybackState(controller)
    }

    @MainThread
    fun seekTo(positionMs: Long) {
        require(positionMs >= 0L) { "Position must not be negative" }
        val controller = requireController()
        controller.seekTo(positionMs)
        publishPlaybackState(controller)
    }

    @MainThread
    fun next() {
        val controller = requireController()
        if (controller.hasNextMediaItem()) controller.seekToNextMediaItem()
    }

    @MainThread
    fun previous() {
        val controller = requireController()
        if (controller.hasPreviousMediaItem()) controller.seekToPreviousMediaItem()
    }

    @MainThread
    fun selectQueueItem(index: Int) {
        val controller = requireController()
        require(index in 0 until controller.mediaItemCount) { "Queue index is outside the queue" }
        controller.seekToDefaultPosition(index)
        controller.play()
        publishPlaybackState(controller, updateQueue = true)
    }

    @MainThread
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val controller = requireController()
        require(fromIndex in 0 until controller.mediaItemCount) { "Queue index is outside the queue" }
        require(toIndex in 0 until controller.mediaItemCount) { "Queue index is outside the queue" }
        if (fromIndex != toIndex) controller.moveMediaItem(fromIndex, toIndex)
        publishPlaybackState(controller, updateQueue = true)
    }

    @MainThread
    fun setShuffleEnabled(enabled: Boolean) {
        val controller = requireController()
        controller.shuffleModeEnabled = enabled
        publishPlaybackState(controller, updateQueue = true)
    }

    @MainThread
    fun cycleRepeatMode() {
        val controller = requireController()
        controller.repeatMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        publishPlaybackState(controller, updateQueue = true)
    }

    @MainThread
    fun setDeviceVolume(volume: Int) {
        val controller = requireController()
        controller.deviceVolume = volume.coerceIn(
            controller.deviceInfo.minVolume,
            controller.deviceInfo.maxVolume,
        )
        publishPlaybackState(controller, updateQueue = true)
    }

    @MainThread
    fun setDeviceMuted(muted: Boolean) {
        val controller = requireController()
        controller.setDeviceMuted(muted)
        publishPlaybackState(controller, updateQueue = true)
    }

    @MainThread
    fun setQueue(tracks: List<Track>, startIndex: Int = 0, startPositionMs: Long = 0L) {
        require(startPositionMs >= 0L) { "Start position must not be negative" }
        if (tracks.isNotEmpty()) {
            require(startIndex in tracks.indices) { "Start index is outside the queue" }
        }
        val controller = requireController()
        stateMapper.reset()
        audioQualityManager.clear()
        if (tracks.isEmpty()) {
            controller.clearMediaItems()
        } else {
            controller.setMediaItems(tracks.map(Track::toMediaItem), startIndex, startPositionMs)
            controller.prepare()
        }
        publishPlaybackState(controller, updateQueue = true)
    }

    @MainThread
    fun setResolvedQueue(
        tracks: List<ResolvedTrack>,
        startIndex: Int = 0,
        startPositionMs: Long = 0L,
    ) {
        setQueue(
            tracks.map { resolved ->
                Track(
                    id = "${resolved.track.sourceId}:${resolved.track.id}",
                    uri = resolved.stream.url,
                    title = resolved.track.title,
                    mimeType = resolved.stream.mimeType,
                    artist = resolved.track.artists.joinToString { it.name },
                    artworkUri = resolved.track.album?.artwork?.uri,
                    sourceId = resolved.track.sourceId,
                    sourceTrackId = resolved.track.id,
                )
            },
            startIndex,
            startPositionMs,
        )
    }

    @MainThread
    fun addToQueue(track: Track) {
        val controller = requireController()
        val wasEmpty = controller.mediaItemCount == 0
        controller.addMediaItem(track.toMediaItem())
        if (wasEmpty) {
            stateMapper.reset()
            controller.prepare()
        }
        publishPlaybackState(controller, updateQueue = true)
    }

    @MainThread
    fun addResolvedTrack(track: ResolvedTrack) {
        addToQueue(
            Track(
                id = "${track.track.sourceId}:${track.track.id}",
                uri = track.stream.url,
                title = track.track.title,
                mimeType = track.stream.mimeType,
                artist = track.track.artists.joinToString { it.name },
                artworkUri = track.track.album?.artwork?.uri,
                sourceId = track.track.sourceId,
                sourceTrackId = track.track.id,
            ),
        )
    }

    @MainThread
    fun removeFromQueue(index: Int) {
        val controller = requireController()
        require(index in 0 until controller.mediaItemCount) { "Queue index is outside the queue" }
        controller.removeMediaItem(index)
        if (controller.mediaItemCount == 0) audioQualityManager.clear()
        publishPlaybackState(controller, updateQueue = true)
    }

    @MainThread
    fun clearQueue() {
        val controller = requireController()
        stateMapper.reset()
        audioQualityManager.clear()
        controller.clearMediaItems()
        publishPlaybackState(controller, updateQueue = true)
    }

    override fun onEvents(player: Player, events: Player.Events) {
        publishPlaybackState(player, updateQueue = true)
    }

    override fun onPlayerError(error: PlaybackException) {
        stateMapper.recordError(error)
        mediaController?.let { publishPlaybackState(it, updateQueue = true) }
    }

    private fun requireController(): MediaController = checkNotNull(mediaController) {
        "Playback service is not connected: ${connectionState.value}"
    }

    private fun publishPlaybackState(player: Player, updateQueue: Boolean = false) {
        mutablePlaybackState.value = stateMapper.map(
            player,
            Track.fromMediaItem(player.currentMediaItem),
        )
        if (updateQueue) {
            val tracks = List(player.mediaItemCount) { index ->
                Track.fromMediaItem(player.getMediaItemAt(index))
            }.filterNotNull()
            val queue = PlaybackQueueState(
                tracks = tracks,
                currentIndex = player.currentMediaItemIndex.takeIf { it in tracks.indices } ?: -1,
                shuffleEnabled = player.shuffleModeEnabled,
                repeatMode = player.repeatMode,
                deviceVolume = player.deviceVolume,
                maxDeviceVolume = player.deviceInfo.maxVolume,
                deviceMuted = player.isDeviceMuted,
            )
            if (queue != mutableQueueState.value) mutableQueueState.value = queue
        }
        handler.removeCallbacks(positionUpdater)
        if (player.isPlaying) handler.postDelayed(positionUpdater, POSITION_UPDATE_INTERVAL_MS)
    }

    private companion object {
        const val POSITION_UPDATE_INTERVAL_MS = 250L
    }
}