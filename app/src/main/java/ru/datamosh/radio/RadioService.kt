package ru.datamosh.radio

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.MediaSessionService
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val ACTION_LIKE = "ru.datamosh.radio.LIKE"

/** Whether the radio should be playing (playWhenReady). Read by the home screen widget. */
object RadioPlayback {
    val playing = MutableStateFlow(false)
}

class RadioService : MediaSessionService() {
    private var session: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        AppLog.d("PLAYER", "Service created")
        RadioRepo.acquire()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.setMediaItem(item(null))

        // After a pause the buffer is stale: reconnect so playback returns to the live edge.
        val live = object : ForwardingPlayer(player) {
            private var pausedAt = 0L

            override fun play() {
                val stale = pausedAt != 0L && System.currentTimeMillis() - pausedAt > 3_000
                AppLog.d("PLAYER", "play() stale=$stale state=${stateName(wrappedPlayer.playbackState)}")
                if (wrappedPlayer.playbackState == Player.STATE_IDLE || stale) {
                    wrappedPlayer.stop()
                    wrappedPlayer.prepare()
                }
                pausedAt = 0L
                super.play()
            }

            override fun pause() {
                pausedAt = System.currentTimeMillis()
                AppLog.d("PLAYER", "pause()")
                super.pause()
            }
        }

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                AppLog.d("PLAYER", "State: ${stateName(state)}")
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                RadioPlayback.playing.value = playWhenReady
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                AppLog.d("PLAYER", "isPlaying=$isPlaying")
            }

            override fun onPlayerError(error: PlaybackException) {
                AppLog.e("PLAYER", "Playback error: ${error.errorCodeName}", error)
                scope.launch {
                    delay(3_000)
                    if (player.playWhenReady) {
                        player.stop()
                        player.prepare()
                    }
                }
            }
        })

        session = MediaSession.Builder(this, live)
            .setCallback(sessionCallback)
            .setMediaButtonPreferences(ImmutableList.of(likeButton(isCurrentLiked())))
            .build()

        // Keep the heart in the system player in sync with the current track and the likes list.
        scope.launch {
            combine(
                RadioRepo.state.map { it.now?.id }.distinctUntilChanged(),
                LikesStore.likes,
            ) { id, likes -> id != null && likes.any { it.track.id == id } }
                .distinctUntilChanged()
                .collect { liked ->
                    session?.setMediaButtonPreferences(ImmutableList.of(likeButton(liked)))
                }
        }

        // Same URI + id, only metadata changes: ExoPlayer updates it without interrupting playback.
        scope.launch {
            RadioRepo.state.map { it.now }.distinctUntilChanged().collect { t ->
                if (t != null) player.replaceMediaItem(0, item(t))
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        if (!player.playWhenReady) stopSelf()
    }

    override fun onDestroy() {
        AppLog.d("PLAYER", "Service destroyed")
        RadioPlayback.playing.value = false
        scope.cancel()
        session?.release()
        session = null
        player.release()
        RadioRepo.release()
        super.onDestroy()
    }

    private val likeCommand = SessionCommand(ACTION_LIKE, Bundle.EMPTY)

    private fun likeButton(liked: Boolean): CommandButton =
        CommandButton.Builder(if (liked) CommandButton.ICON_HEART_FILLED else CommandButton.ICON_HEART_UNFILLED)
            .setDisplayName(if (liked) "Убрать из понравившихся" else "Нравится")
            .setSessionCommand(likeCommand)
            .setSlots(CommandButton.SLOT_FORWARD)
            .build()

    private fun isCurrentLiked(): Boolean {
        val id = RadioRepo.state.value.now?.id ?: return false
        return LikesStore.likes.value.any { it.track.id == id }
    }

    private fun toggleCurrentLike() {
        val t = RadioRepo.state.value.now ?: return
        scope.launch {
            val liked = LikesStore.likes.value.any { it.track.id == t.id }
            AppLog.d("PLAYER", (if (liked) "Unlike" else "Like") + " from the system player: ${t.artist} - ${t.title}")
            if (liked) LikesStore.unlike(t.id) else LikesStore.like(t)
        }
    }

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(likeCommand)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .setMediaButtonPreferences(ImmutableList.of(likeButton(isCurrentLiked())))
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == ACTION_LIKE) toggleCurrentLike()
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private fun stateName(s: Int) = when (s) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> s.toString()
    }

    private fun item(t: Track?): MediaItem = MediaItem.Builder()
        .setMediaId("radio")
        .setUri(STREAM_URL)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(t?.title ?: "DATAMOSH FM")
                .setArtist(t?.artist ?: "")
                .setArtworkUri(t?.art?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) })
                .build(),
        )
        .build()
}
