package ru.datamosh.radio

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.KeyEvent
import android.widget.RemoteViews
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.transform.RoundedCornersTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val ACTION_WIDGET_LIKE = "ru.datamosh.radio.widget.LIKE"
private const val COVER_PX = 256

/** Home screen widget: the track on air, a like button and play/pause. */
class PlayerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        PlayerWidgets.refresh(context.applicationContext) { pending.finish() }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_WIDGET_LIKE) {
            val pending = goAsync()
            PlayerWidgets.toggleLike(context.applicationContext) { pending.finish() }
        } else {
            super.onReceive(context, intent)
        }
    }
}

object PlayerWidgets {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val renderLock = Mutex()

    /** Last track shown; persisted so a fresh process can redraw the widget correctly. */
    @Volatile private var known: Track? = null
    @Volatile private var coverUrl: String? = null
    @Volatile private var cover: Bitmap? = null

    /**
     * Called from [RadioApplication]: while the process is alive, redraw the widgets whenever the
     * track, the likes or the play state change. The first redraw also fixes a stale "pause" icon
     * left from a process that was killed while playing.
     */
    fun start(ctx: Context) {
        known = loadTrack(ctx)
        scope.launch {
            combine(
                RadioRepo.state.map { it.now }.distinctUntilChanged(),
                LikesStore.likes,
                RadioPlayback.playing,
            ) { now, _, _ -> now }
                .collectLatest { now ->
                    if (now != null) remember(ctx, now)
                    render(ctx)
                }
        }
    }

    /** Widget added or periodic update: show what is on air right now. */
    fun refresh(ctx: Context, done: () -> Unit) {
        scope.launch {
            try {
                // A request right after the process starts sometimes fails; retry a couple of times.
                var track = currentTrack()
                repeat(2) {
                    if (track == null) {
                        delay(3_000)
                        track = currentTrack()
                    }
                }
                track?.let { remember(ctx, it) }
                render(ctx)
            } finally {
                done()
            }
        }
    }

    fun toggleLike(ctx: Context, done: () -> Unit) {
        scope.launch {
            try {
                val t = currentTrack() ?: known ?: return@launch
                remember(ctx, t)
                val liked = LikesStore.likes.value.any { it.track.id == t.id }
                AppLog.d("WIDGET", (if (liked) "Unlike" else "Like") + " from the widget: ${t.artist} - ${t.title}")
                if (liked) LikesStore.unlike(t.id) else LikesStore.like(t)
                render(ctx)
            } finally {
                done()
            }
        }
    }

    /** The track on air: from the running poller if it is fresh, otherwise a one-off request. */
    private fun currentTrack(): Track? {
        val s = RadioRepo.state.value
        if (s.now != null && System.currentTimeMillis() - s.fetchedAt < 60_000) return s.now
        return fetchNowPlayingOnce()
    }

    private suspend fun render(ctx: Context) = renderLock.withLock {
        val manager = AppWidgetManager.getInstance(ctx)
        val ids = manager.getAppWidgetIds(ComponentName(ctx, PlayerWidget::class.java))
        if (ids.isEmpty()) return@withLock

        val t = known
        val liked = t != null && LikesStore.likes.value.any { it.track.id == t.id }
        val playing = RadioPlayback.playing.value
        val art = t?.art?.takeIf { it.isNotBlank() }?.let { loadCover(ctx, it) }

        val views = RemoteViews(ctx.packageName, R.layout.widget_player).apply {
            setTextViewText(R.id.widget_title, t?.title ?: "DATAMOSH FM")
            setTextViewText(R.id.widget_artist, t?.artist ?: "Радио")
            if (art != null) setImageViewBitmap(R.id.widget_cover, art)
            else setImageViewResource(R.id.widget_cover, R.drawable.logo_apk)

            setImageViewResource(
                R.id.widget_like_icon,
                if (liked) R.drawable.ic_widget_heart else R.drawable.ic_widget_heart_border,
            )
            setContentDescription(R.id.widget_like, if (liked) "Убрать из понравившихся" else "Лайк")
            setImageViewResource(
                R.id.widget_play_icon,
                if (playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            )
            setContentDescription(R.id.widget_play, if (playing) "Пауза" else "Слушать")

            setOnClickPendingIntent(android.R.id.background, openAppIntent(ctx))
            setOnClickPendingIntent(R.id.widget_like, likeIntent(ctx))
            setOnClickPendingIntent(R.id.widget_play, playPauseIntent(ctx))
        }
        manager.updateAppWidget(ids, views)
    }

    private suspend fun loadCover(ctx: Context, url: String): Bitmap? {
        if (url == coverUrl) cover?.let { return it }
        val request = ImageRequest.Builder(ctx)
            .data(url)
            .size(COVER_PX)
            .allowHardware(false) // RemoteViews can't carry hardware bitmaps
            .transformations(RoundedCornersTransformation(COVER_PX * 0.16f))
            .build()
        val bitmap = (ctx.imageLoader.execute(request) as? SuccessResult)?.drawable?.toBitmap()
        if (bitmap != null) {
            coverUrl = url
            cover = bitmap
        } else {
            AppLog.d("WIDGET", "Cover not loaded: $url")
        }
        return bitmap
    }

    private fun openAppIntent(ctx: Context): PendingIntent {
        // The launcher intent brings an existing task to the front instead of stacking a new activity.
        val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
            ?: Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun likeIntent(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx,
        0,
        Intent(ctx, PlayerWidget::class.java).setAction(ACTION_WIDGET_LIKE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /**
     * A media button event straight to [RadioService], the same way Media3's own notification does it.
     * PLAY_PAUSE (not PLAY/PAUSE) so a stale icon after the process was killed still does the right thing.
     */
    private fun playPauseIntent(ctx: Context): PendingIntent {
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
            .setComponent(ComponentName(ctx, RadioService::class.java))
            .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        return PendingIntent.getForegroundService(
            ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun remember(ctx: Context, t: Track) {
        if (t == known) return
        known = t
        ctx.getSharedPreferences("widget", Context.MODE_PRIVATE).edit {
            putString("id", t.id)
            putString("title", t.title)
            putString("artist", t.artist)
            putString("art", t.art)
        }
    }

    private fun loadTrack(ctx: Context): Track? {
        val p = ctx.getSharedPreferences("widget", Context.MODE_PRIVATE)
        val id = p.getString("id", null) ?: return null
        return Track(id, p.getString("title", "").orEmpty(), p.getString("artist", "").orEmpty(), p.getString("art", "").orEmpty())
    }
}
