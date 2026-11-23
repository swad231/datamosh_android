package ru.datamosh.radio

import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

const val STREAM_URL = "https://radio.datamosh.ru/listen/datamosh_radio/radio.mp3"
const val BACKGROUND_URL = "https://cdn.datamosh.ru/1114.webm"
const val API_URL = "https://radio.datamosh.ru/api/nowplaying/datamosh_radio"

data class Track(val id: String, val title: String, val artist: String, val art: String)

data class HistoryItem(val shId: Long, val playedAt: Long, val track: Track)

data class RadioState(
    val now: Track? = null,
    val next: Track? = null,
    val elapsed: Int = 0,
    val duration: Int = 0,
    val fetchedAt: Long = 0L,
    val history: List<HistoryItem> = emptyList(),
    val error: Boolean = false,
)

fun httpGet(url: String): ByteArray {
    val c = URL(url).openConnection() as HttpURLConnection
    c.connectTimeout = 10_000
    c.readTimeout = 15_000
    try {
        return c.inputStream.use { it.readBytes() }
    } finally {
        c.disconnect()
    }
}

private fun JSONObject.toTrack(): Track {
    val title = optString("title").ifBlank { optString("text") }
    return Track(optString("id"), title, optString("artist"), optString("art"))
}

/** One-off now-playing request, for the widget when nothing is polling. */
fun fetchNowPlayingOnce(): Track? = runCatching {
    JSONObject(String(httpGet(API_URL), Charsets.UTF_8))
        .getJSONObject("now_playing")
        .getJSONObject("song")
        .toTrack()
}.onFailure { AppLog.e("RADIO", "One-off now-playing request failed", it) }.getOrNull()

/** Polls the AzuraCast now-playing API while the UI or the playback service is alive. */
object RadioRepo {
    private val _state = MutableStateFlow(RadioState())
    val state: StateFlow<RadioState> = _state

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clients = AtomicInteger(0)
    private var job: Job? = null
    private var lastTrackId: String? = null
    private var failing = false

    @Synchronized
    fun acquire() {
        if (clients.getAndIncrement() == 0) job = scope.launch { loop() }
    }

    @Synchronized
    fun release() {
        if (clients.decrementAndGet() <= 0) {
            clients.set(0)
            job?.cancel()
            job = null
        }
    }

    private suspend fun loop() {
        while (currentCoroutineContext().isActive) {
            var wait = 8_000L
            try {
                val j = JSONObject(String(httpGet(API_URL), Charsets.UTF_8))
                val np = j.getJSONObject("now_playing")
                val now = HistoryItem(
                    np.getLong("sh_id"),
                    np.getLong("played_at"),
                    np.getJSONObject("song").toTrack(),
                )

                val arr = j.optJSONArray("song_history")
                val top = ArrayList<HistoryItem>()
                if (arr != null) {
                    for (i in 0 until minOf(arr.length(), 5)) {
                        val o = arr.getJSONObject(i)
                        top.add(
                            HistoryItem(
                                o.getLong("sh_id"),
                                o.getLong("played_at"),
                                o.getJSONObject("song").toTrack(),
                            ),
                        )
                    }
                }

                val next = j.optJSONObject("playing_next")?.optJSONObject("song")?.toTrack()
                val remaining = np.optInt("remaining", 30)

                if (failing) {
                    failing = false
                    AppLog.d("RADIO", "Now-playing API reachable again")
                }
                if (lastTrackId != now.track.id) {
                    lastTrackId = now.track.id
                    AppLog.d("RADIO", "Now playing: ${now.track.artist} - ${now.track.title} (${np.optInt("duration")}s)")
                }

                _state.value = RadioState(
                    now = now.track,
                    next = next,
                    elapsed = np.optInt("elapsed"),
                    duration = np.optInt("duration"),
                    fetchedAt = System.currentTimeMillis(),
                    history = top,
                    error = false,
                )
                wait = if (remaining <= 12) (remaining * 1000L + 800L).coerceAtLeast(1_500L) else 8_000L
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!failing) {
                    failing = true
                    AppLog.e("RADIO", "Now-playing request failed", e)
                }
                _state.update { it.copy(error = true) }
                wait = 5_000L
            }
            delay(wait)
        }
    }
}
