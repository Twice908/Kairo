package com.kairo.player.playback

import android.app.PendingIntent
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.kairo.player.audio.AudioQualityManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
@UnstableApi
class PlaybackService : MediaSessionService() {
    @Inject
    lateinit var audioQualityManager: AudioQualityManager

    private var audioPlayer: AudioPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        audioPlayer = AudioPlayer(player, audioQualityManager)

        val sessionBuilder = MediaSession.Builder(this, player)
        createSessionActivity()?.let(sessionBuilder::setSessionActivity)
        mediaSession = sessionBuilder.build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        audioPlayer?.release()
        audioPlayer = null
        super.onDestroy()
    }

    private fun createSessionActivity(): PendingIntent? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        return PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}