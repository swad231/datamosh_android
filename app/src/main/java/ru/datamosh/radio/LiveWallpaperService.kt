package ru.datamosh.radio

import android.app.WallpaperColors
import android.net.Uri
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import android.graphics.Color as AndroidColor

/**
 * Live wallpaper with the same looping background video as the player screen.
 * The video plays only while the wallpaper is visible (home or lock screen, screen on),
 * is silent and never requests audio focus, so it does not interfere with the radio.
 */
class LiveWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VideoEngine()

    private inner class VideoEngine : Engine() {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private var player: ExoPlayer? = null
        private var visible = false

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
            AppLog.d("WALL", "Engine created (preview=$isPreview)")
            val exo = ExoPlayer.Builder(this@LiveWallpaperService).build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                // Fill the whole screen keeping the aspect ratio; the sides are cropped,
                // like resize_mode="zoom" on the player screen.
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .build()
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        AppLog.e("WALL", "Wallpaper video error: ${error.errorCodeName}", error)
                    }
                })
            }
            player = exo
            scope.launch {
                // Same source as the player screen: cached copy first, stream as a fallback.
                val src = ensureBackground(applicationContext)?.let { Uri.fromFile(it).toString() }
                    ?: BACKGROUND_URL
                exo.setMediaItem(MediaItem.fromUri(src))
                exo.prepare()
                exo.playWhenReady = visible
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            player?.setVideoSurface(holder.surface)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            player?.clearVideoSurface(holder.surface)
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            player?.playWhenReady = visible
        }

        // The video is dark: tell the system so the clock and icons on top stay light.
        @RequiresApi(Build.VERSION_CODES.O_MR1)
        override fun onComputeColors(): WallpaperColors =
            WallpaperColors(android.graphics.Color.valueOf(AndroidColor.rgb(18, 20, 40)), null, null)

        override fun onDestroy() {
            AppLog.d("WALL", "Engine destroyed (preview=$isPreview)")
            scope.cancel()
            player?.release()
            player = null
            super.onDestroy()
        }
    }
}
