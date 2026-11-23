@file:OptIn(ExperimentalMaterial3Api::class)

package ru.datamosh.radio

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.rounded.Wallpaper
import android.os.Build
import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.os.SystemClock
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlin.random.Random
import coil.compose.AsyncImage
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun RadioTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val scheme = if (Build.VERSION.SDK_INT >= 31) dynamicDarkColorScheme(ctx) else darkColorScheme()
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Wavy progress line in the Material 3 Expressive style; the wave flattens when paused. */
@Composable
private fun WavyProgress(progress: Float, playing: Boolean, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "phase",
    )
    val k by animateFloatAsState(if (playing) 1f else 0f, spring(dampingRatio = 0.6f), label = "amp")
    Canvas(modifier.height(20.dp)) {
        val cy = size.height / 2f
        val stroke = 5.dp.toPx()
        val amp = 5.dp.toPx() * k
        val wavelength = 30.dp.toPx()
        val end = size.width * progress.coerceIn(0f, 1f)
        drawLine(track, Offset(end, cy), Offset(size.width, cy), stroke, StrokeCap.Round)
        if (end > 0f) {
            val path = Path()
            path.moveTo(0f, cy + amp * sin(phase))
            var x = 0f
            while (x < end) {
                x = (x + 3f).coerceAtMost(end)
                path.lineTo(x, cy + amp * sin(2f * PI.toFloat() * x / wavelength + phase))
            }
            drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun RadioRoot(vm: RadioViewModel = viewModel()) {
    val state by RadioRepo.state.collectAsStateWithLifecycle()
    RadioTheme { RadioContent(vm, state) }
}

@Composable
private fun RadioContent(vm: RadioViewModel, state: RadioState) {
    val likes by LikesStore.likes.collectAsStateWithLifecycle()
    val wantPlay by vm.wantPlay.collectAsStateWithLifecycle()
    val buffering by vm.buffering.collectAsStateWithLifecycle()
    var showLikes by rememberSaveable { mutableStateOf(false) }
    val likedIds = remember(likes) { likes.map { it.track.id }.toSet() }

    var clean by rememberSaveable { mutableStateOf(false) }
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var cleanLocked by rememberSaveable { mutableStateOf(false) }
    val view = LocalView.current

    // Clean screen: hide the status and navigation bars as well.
    DisposableEffect(clean) {
        val window = (view.context as? Activity)?.window
        val ctrl = window?.let { WindowCompat.getInsetsController(it, view) }
        if (clean) {
            ctrl?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            ctrl?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { ctrl?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    BackHandler(showLikes || (clean && !cleanLocked)) {
        AppLog.d("UI", "Back pressed (clean=$clean, likes=$showLikes)")
        if (clean) clean = false else showLikes = false
    }

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VideoBackground()
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0x80000000), Color(0xCC000000))),
                ),
            )
            if (showMenu) MoreSheet { showMenu = false }
            AnimatedContent(
                targetState = if (clean) 2 else if (showLikes) 1 else 0,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tabs",
            ) { screen ->
                if (screen == 2) {
                    CleanScreen(
                        state = state,
                        liked = state.now?.let { likedIds.contains(it.id) } == true,
                        wantPlay = wantPlay,
                        buffering = buffering,
                        locked = cleanLocked,
                        onToggle = vm::toggle,
                        onLike = { state.now?.let(vm::toggleLike) },
                        onToggleLock = {
                            cleanLocked = !cleanLocked
                            AppLog.d("UI", "Clean screen lock: $cleanLocked")
                        },
                        onExit = {
                            AppLog.d("UI", "Exit clean screen")
                            cleanLocked = false
                            clean = false
                        },
                    )
                } else if (screen == 1) {
                    LikesScreen(
                        likes,
                        onBack = {
                            AppLog.d("UI", "Close likes screen")
                            showLikes = false
                        },
                        onUnlike = vm::unlike,
                    )
                } else {
                    PlayerScreen(
                        state = state,
                        likedIds = likedIds,
                        likesCount = likes.size,
                        wantPlay = wantPlay,
                        buffering = buffering,
                        onToggle = vm::toggle,
                        onLike = vm::toggleLike,
                        onOpenLikes = {
                            AppLog.d("UI", "Open likes screen")
                            showLikes = true
                        },
                        onOpenMenu = {
                            AppLog.d("UI", "Open menu")
                            showMenu = true
                        },
                        onClean = {
                            AppLog.d("UI", "Enter clean screen")
                            cleanLocked = false
                            clean = true
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoBackground() {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Local copy first; if the download fails, fall back to streaming from the site.
    var source by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        source = ensureBackground(ctx)?.let { Uri.fromFile(it).toString() } ?: BACKGROUND_URL
    }
    val src = source ?: return

    val exo = remember(src) {
        ExoPlayer.Builder(ctx).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .build()
            setMediaItem(MediaItem.fromUri(src))
            addListener(object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    AppLog.e("BG", "Background video player error: ${error.errorCodeName}", error)
                }
            })
            prepare()
        }
    }
    DisposableEffect(exo, lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_START) exo.play()
            else if (e == Lifecycle.Event.ON_STOP) exo.pause()
        }
        lifecycle.addObserver(obs)
        onDispose {
            lifecycle.removeObserver(obs)
            exo.release()
        }
    }
    // resize_mode="zoom" (see res/layout/bg_player.xml): the video fills the screen height
    // and is centre-cropped on the sides to match the display width.
    AndroidView(
        factory = { c ->
            (LayoutInflater.from(c).inflate(R.layout.bg_player, null) as PlayerView).apply {
                player = exo
            }
        },
        update = { it.player = exo },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun TopBar(onOpenMenu: () -> Unit, onOpenLikes: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = onOpenMenu) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Меню")
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Image(
                painterResource(R.drawable.logo_fm),
                contentDescription = "DATAMOSH FM",
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(44.dp),
            )
        }
        FilledTonalIconButton(onClick = onOpenLikes) {
            Icon(Icons.Rounded.Favorite, contentDescription = "Понравившиеся треки")
        }
    }
}

private fun LazyListScope.historyItems(
    state: RadioState,
    likedIds: Set<String>,
    onLike: (Track) -> Unit,
    topPadding: Dp,
) {
    item {
        Text(
            "Недавно играло",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().padding(top = topPadding),
        )
    }
    items(state.history, key = { it.shId }) { h ->
        TrackRow(h.track, null, likedIds.contains(h.track.id)) { onLike(h.track) }
    }
    if (state.history.isEmpty()) {
        item {
            Text(
                "Список появится, когда доиграет первый трек",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PlayerScreen(
    state: RadioState,
    likedIds: Set<String>,
    likesCount: Int,
    wantPlay: Boolean,
    buffering: Boolean,
    onToggle: () -> Unit,
    onLike: (Track) -> Unit,
    onOpenLikes: () -> Unit,
    onOpenMenu: () -> Unit,
    onClean: () -> Unit,
) {
    val sys = WindowInsets.systemBars.asPaddingValues()
    val config = LocalConfiguration.current
    val wide = config.screenWidthDp >= 600
    val landscape = config.screenWidthDp > config.screenHeightDp
    val nowLiked = state.now?.let { likedIds.contains(it.id) } == true

    if (wide && landscape) {
        // Landscape: a large cover fills the left half; the controls sit on top of the right
        // half with the history below them, and the whole right column scrolls together.
        val dir = LocalLayoutDirection.current
        val safe = WindowInsets.safeDrawing.asPaddingValues()
        Column(
            Modifier.fillMaxSize().padding(
                start = safe.calculateStartPadding(dir) + 24.dp,
                end = safe.calculateEndPadding(dir) + 24.dp,
                top = sys.calculateTopPadding() + 8.dp,
                bottom = sys.calculateBottomPadding() + 16.dp,
            ),
        ) {
            TopBar(onOpenMenu, onOpenLikes)
            Row(
                Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Cover(state.now?.art, Modifier.size(minOf(maxWidth, maxHeight)))
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        NowPlayingControls(state, nowLiked, wantPlay, buffering, onToggle, onLike, onClean)
                    }
                    historyItems(state, likedIds, onLike, 20.dp)
                }
            }
        }
    } else if (wide) {
        // Tablet / wide screen: player on the left, history on the right.
        Column(
            Modifier.fillMaxSize().padding(
                start = 24.dp,
                end = 24.dp,
                top = sys.calculateTopPadding() + 8.dp,
                bottom = sys.calculateBottomPadding() + 16.dp,
            ),
        ) {
            TopBar(onOpenMenu, onOpenLikes)
            Row(
                Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
                    val cover = minOf(maxWidth * 0.9f, maxHeight * 0.45f)
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        NowPlaying(state, nowLiked, wantPlay, buffering, onToggle, onLike, onClean, coverSize = cover)
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    historyItems(state, likedIds, onLike, 8.dp)
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = sys.calculateTopPadding() + 8.dp,
                bottom = sys.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { TopBar(onOpenMenu, onOpenLikes) }
            item { NowPlaying(state, nowLiked, wantPlay, buffering, onToggle, onLike, onClean) }
            historyItems(state, likedIds, onLike, 20.dp)
        }
    }
}

@Composable
private fun NowPlaying(
    state: RadioState,
    liked: Boolean,
    wantPlay: Boolean,
    buffering: Boolean,
    onToggle: () -> Unit,
    onLike: (Track) -> Unit,
    onClean: () -> Unit,
    coverSize: Dp? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Cover(
            state.now?.art,
            if (coverSize != null) Modifier.size(coverSize) else Modifier.fillMaxWidth(0.82f).aspectRatio(1f),
        )
        NowPlayingControls(state, liked, wantPlay, buffering, onToggle, onLike, onClean)
    }
}

@Composable
private fun Cover(art: String?, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(44.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = art, label = "cover") { url ->
            if (url.isNullOrBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(64.dp))
                }
            } else {
                AsyncImage(
                    model = url,
                    contentDescription = "Обложка",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** Title, artist, progress, playback buttons and the "next" line (everything except the cover). */
@Composable
private fun NowPlayingControls(
    state: RadioState,
    liked: Boolean,
    wantPlay: Boolean,
    buffering: Boolean,
    onToggle: () -> Unit,
    onLike: (Track) -> Unit,
    onClean: () -> Unit,
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(500)
        }
    }
    val progress = if (state.duration > 0) {
        ((state.elapsed + (nowMs - state.fetchedAt) / 1000f) / state.duration).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AnimatedContent(
            targetState = state.now,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "meta",
        ) { t ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    t?.title ?: if (state.error) "Нет связи с радио" else "Подключаемся…",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    t?.artist ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        WavyProgress(
            progress = progress,
            playing = wantPlay,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            FilledTonalIconButton(onClick = onClean, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.Fullscreen, contentDescription = "Чистый экран")
            }
            val corner by animateDpAsState(
                if (wantPlay) 28.dp else 44.dp,
                spring(),
                label = "playShape",
            )
            FilledIconButton(
                onClick = onToggle,
                modifier = Modifier.size(88.dp),
                shape = RoundedCornerShape(corner),
            ) {
                if (wantPlay && buffering) {
                    CircularProgressIndicator(Modifier.size(40.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(
                        if (wantPlay) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (wantPlay) "Пауза" else "Слушать",
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
            FilledTonalIconToggleButton(
                checked = liked,
                onCheckedChange = { state.now?.let(onLike) },
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (liked) "Убрать из понравившихся" else "Лайк",
                    tint = if (liked) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                )
            }
        }

        state.next?.let {
            Text(
                "Дальше: ${it.artist} — ${it.title}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    localCover: String?,
    liked: Boolean,
    extra: @Composable () -> Unit = {},
    onLike: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = localCover?.let { File(it) } ?: track.art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            extra()
            IconToggleButton(checked = liked, onCheckedChange = { onLike() }) {
                Icon(
                    if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (liked) "Убрать из понравившихся" else "Лайк",
                    tint = if (liked) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                )
            }
        }
    }
}

@Composable
private fun LikesScreen(likes: List<LikedTrack>, onBack: () -> Unit, onUnlike: (String) -> Unit) {
    val sys = WindowInsets.systemBars.asPaddingValues()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxHeight(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = sys.calculateTopPadding() + 8.dp,
            bottom = sys.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                }
                Text(
                    "Понравившиеся",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        if (likes.isEmpty()) {
            item {
                Text(
                    "Здесь пока пусто. Нажми сердечко у трека, чтобы он сохранился.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                )
            }
        }
        items(likes, key = { it.track.id }) { l ->
            TrackRow(l.track, l.coverPath, liked = true, extra = { SearchMenu(l.track) }) { onUnlike(l.track.id) }
        }
    }
    }
}

private class FlyingHeart(
    val id: Long,
    val x: Float,
    val y: Float,
    val dx: Float,
    val size: Dp,
    val delayMs: Int,
)

@Composable
private fun FlyingHeartView(h: FlyingHeart, onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(h.delayMs.toLong())
        started = true
        progress.animateTo(1f, tween(1400, easing = LinearOutSlowInEasing))
        onDone()
    }
    Icon(
        Icons.Rounded.Favorite,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier
            .offset {
                val p = progress.value
                IntOffset(
                    (h.x + h.dx * p - h.size.toPx() / 2f).roundToInt(),
                    (h.y - 240.dp.toPx() * p - h.size.toPx() / 2f).roundToInt(),
                )
            }
            .size(h.size)
            .graphicsLayer {
                val p = progress.value
                alpha = if (started) (1f - p * p).coerceIn(0f, 1f) else 0f
                val sc = (0.5f + p * 4f).coerceAtMost(1f)
                scaleX = sc
                scaleY = sc
            },
    )
}

/**
 * Clean screen: video background, cover, title and artist.
 *  - tap the cover: a small play/pause button appears for a few seconds;
 *  - double tap the cover: like the track (with flying hearts);
 *  - lock button on the right (5 s): fixes the mode so taps on the background don't close it.
 */
@Composable
private fun CleanScreen(
    state: RadioState,
    liked: Boolean,
    wantPlay: Boolean,
    buffering: Boolean,
    locked: Boolean,
    onToggle: () -> Unit,
    onLike: () -> Unit,
    onToggleLock: () -> Unit,
    onExit: () -> Unit,
) {
    val likedNow by rememberUpdatedState(liked)
    val onLikeNow by rememberUpdatedState(onLike)

    // Lock button: visible for 5 s after entering and after every lock/unlock.
    // While locked, a tap on the background (or Back) shows it again.
    var lockTick by remember { mutableIntStateOf(0) }
    var lockVisible by remember { mutableStateOf(true) }
    val lockAlpha by animateFloatAsState(if (lockVisible) 1f else 0f, tween(250), label = "lock")
    LaunchedEffect(lockTick, locked) {
        lockVisible = true
        delay(5_000)
        lockVisible = false
    }

    // Play/pause button on the cover: visible for 3 s after a tap.
    var controlsTick by remember { mutableIntStateOf(0) }
    var controlsVisible by remember { mutableStateOf(false) }
    val controlsAlpha by animateFloatAsState(if (controlsVisible) 1f else 0f, tween(200), label = "controls")
    LaunchedEffect(controlsTick) {
        if (controlsTick > 0) {
            controlsVisible = true
            delay(3_000)
            controlsVisible = false
        }
    }

    BackHandler(enabled = locked) {
        AppLog.d("UI", "Back pressed while the clean screen is locked")
        lockTick++
    }

    val hearts = remember { mutableStateListOf<FlyingHeart>() }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) { if (locked) lockTick++ else onExit() },
    ) {
        val side = minOf(maxWidth * 0.82f, maxHeight * 0.6f)
        val textMaxWidth = maxWidth * 0.9f

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(Modifier.size(side)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(44.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { controlsTick++ },
                                onDoubleTap = { off ->
                                    AppLog.d("UI", "Clean screen: double tap (like)")
                                    if (!likedNow) onLikeNow()
                                    repeat(7) {
                                        hearts.add(
                                            FlyingHeart(
                                                id = Random.nextLong(),
                                                x = off.x,
                                                y = off.y,
                                                dx = (Random.nextFloat() - 0.5f) * 140.dp.toPx(),
                                                size = (18 + Random.nextInt(18)).dp,
                                                delayMs = Random.nextInt(0, 260),
                                            ),
                                        )
                                    }
                                },
                            )
                        },
                ) {
                    AsyncImage(
                        model = state.now?.art,
                        contentDescription = "Обложка",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (controlsAlpha > 0.01f) {
                        FilledIconButton(
                            onClick = {
                                AppLog.d("UI", "Clean screen: play/pause button")
                                onToggle()
                                controlsTick++
                            },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(60.dp)
                                .graphicsLayer { alpha = controlsAlpha },
                        ) {
                            if (wantPlay && buffering) {
                                CircularProgressIndicator(
                                    Modifier.size(30.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Icon(
                                    if (wantPlay) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (wantPlay) "Пауза" else "Слушать",
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                    }
                    if (lockAlpha > 0.01f) {
                        FilledTonalIconButton(
                            onClick = onToggleLock,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = 62.dp)
                                .graphicsLayer { alpha = lockAlpha },
                        ) {
                            Icon(
                                if (locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                contentDescription = if (locked) "Разблокировать чистый экран" else "Зафиксировать чистый экран",
                            )
                        }
                    }
                }
                for (h in hearts.toList()) {
                    key(h.id) { FlyingHeartView(h) { hearts.remove(h) } }
                }
            }

            AnimatedContent(
                targetState = state.now,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "cleanMeta",
            ) { t ->
                Column(
                    modifier = Modifier.widthIn(max = textMaxWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        t?.title ?: "",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        t?.artist ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

    }
}

private val streamingServices = listOf(
    "Spotify" to "https://open.spotify.com/search/",
    "YouTube Music" to "https://music.youtube.com/search?q=",
    "Яндекс Музыка" to "https://music.yandex.ru/search?text=",
    "VK Музыка" to "https://vk.com/audio?q=",
    "Apple Music" to "https://music.apple.com/search?term=",
    "SoundCloud" to "https://soundcloud.com/search?q=",
)

@Composable
private fun SearchMenu(track: Track) {
    var open by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val query = Uri.encode("${track.artist} ${track.title}")
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.Search, contentDescription = "Найти в стриминговых сервисах")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            streamingServices.forEach { (name, url) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        open = false
                        AppLog.d("UI", "Search '${track.artist} - ${track.title}' in $name")
                        runCatching { uriHandler.openUri(url + query) }
                            .onFailure { AppLog.e("UI", "Failed to open $name", it) }
                    },
                )
            }
        }
    }
}

@Composable
private fun MoreSheet(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var taps by remember { mutableIntStateOf(0) }
    var lastTap by remember { mutableLongStateOf(0L) }

    fun openLink(url: String, what: String) {
        AppLog.d("UI", "Open link: $what")
        runCatching { uriHandler.openUri(url) }
            .onFailure { AppLog.e("UI", "Failed to open $url", it) }
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF2B2B30),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MenuRow(
                text = "Официальный ТГ-канал DATAMOSH",
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_telegram),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp),
                    )
                },
            ) { openLink("https://t.me/datam0sh", "telegram channel") }

            MenuRow(
                text = "Обратная связь",
                icon = { Text("?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            ) { openLink("https://t.me/datafmsupport_bot", "feedback bot") }

            val ctx = LocalContext.current
            MenuRow(
                text = "Живые обои с фоном DATAMOSH",
                icon = { Icon(Icons.Rounded.Wallpaper, contentDescription = null, modifier = Modifier.size(22.dp)) },
            ) {
                openLiveWallpaperPicker(ctx)
                onDismiss()
            }

            // Hidden switch: tap 3 times, then long-press once -> toggles debug logging.
            Text(
                "Версия приложения ${AppLog.versionName()} ($BUILD_DATE)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                val now = SystemClock.elapsedRealtime()
                                taps = if (now - lastTap > 3_000) 1 else taps + 1
                                lastTap = now
                            },
                            onLongPress = {
                                val now = SystemClock.elapsedRealtime()
                                if (taps == 3 && now - lastTap <= 4_000) AppLog.toggleDebug()
                                taps = 0
                            },
                        )
                    },
            )
        }
    }
}

/**
 * Opens the system preview of our live wallpaper, where the user picks home / lock screen.
 * Some firmwares don't support the direct preview; then fall back to the live wallpaper list.
 */
private fun openLiveWallpaperPicker(ctx: Context) {
    AppLog.d("UI", "Open live wallpaper picker")
    val preview = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        .putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(ctx, LiveWallpaperService::class.java),
        )
    val chooser = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
    for (intent in listOf(preview, chooser)) {
        try {
            ctx.startActivity(intent)
            return
        } catch (e: ActivityNotFoundException) {
            AppLog.e("UI", "No activity for ${intent.action}", e)
        }
    }
    Toast.makeText(ctx, "Телефон не поддерживает живые обои", Toast.LENGTH_LONG).show()
}

@Composable
private fun MenuRow(text: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { icon() }
            Spacer(Modifier.width(14.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
    }
}
