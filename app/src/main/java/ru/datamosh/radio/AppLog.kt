package ru.datamosh.radio

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Release date of this build in compact form (MMDDYY). */
const val BUILD_DATE = "112326"

/**
 * Logging to the public Download folder.
 *  - Crash reports are ALWAYS written (one txt file per crash).
 *  - A full action log is written only while "debug" is switched on; one file per app launch.
 */
object AppLog {
    private const val TAG = "DatamoshFM"

    private lateinit var app: Application
    private val lock = Any()
    private var out: OutputStream? = null
    private var sessionActive = false

    @Volatile
    var debugEnabled = false
        private set

    private fun time() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
    private fun fileStamp() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    private fun prefs() = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun init(application: Application) {
        app = application
        debugEnabled = prefs().getBoolean("debug", false)

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { writeCrash(thread, error) }
            runCatching {
                synchronized(lock) {
                    if (out != null) {
                        writeLine("E", "CRASH", "Uncaught exception on thread ${thread.name}\n" + Log.getStackTraceString(error))
                        closeStream()
                    }
                }
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun versionName(): String =
        runCatching { app.packageManager.getPackageInfo(app.packageName, 0).versionName }.getOrNull() ?: "?"

    // ---- session (one per app launch) -------------------------------------------------------

    fun startSession() {
        synchronized(lock) {
            sessionActive = true
            if (debugEnabled && out == null) openSessionFile()
        }
    }

    fun endSession() {
        synchronized(lock) {
            sessionActive = false
            if (out != null) {
                writeLine("I", "APP", "Session ended")
                closeStream()
            }
        }
    }

    /** Called by the hidden gesture on the version label. */
    fun toggleDebug() {
        val on = !debugEnabled
        synchronized(lock) {
            if (on) {
                debugEnabled = true
                prefs().edit().putBoolean("debug", true).apply()
                if (sessionActive && out == null) openSessionFile()
            } else {
                writeLine("I", "APP", "Debug disabled by user")
                closeStream()
                debugEnabled = false
                prefs().edit().putBoolean("debug", false).apply()
            }
        }
        Toast.makeText(app, if (on) "Отладка включена" else "Отладка отключена", Toast.LENGTH_SHORT).show()
    }

    // ---- logging ----------------------------------------------------------------------------

    fun d(tag: String, msg: String) = log("D", tag, msg, null)

    fun e(tag: String, msg: String, t: Throwable? = null) = log("E", tag, msg, t)

    private fun log(level: String, tag: String, msg: String, t: Throwable?) {
        if (!debugEnabled) return
        Log.d(TAG, "$tag: $msg")
        synchronized(lock) {
            writeLine(level, tag, msg + (t?.let { "\n" + Log.getStackTraceString(it) } ?: ""))
        }
    }

    private fun openSessionFile() {
        out = openStream("DATAMOSH_FM_log_${fileStamp()}.txt")
        writeLine("I", "APP", "=== DATAMOSH FM debug log ===")
        writeLine("I", "APP", "Version ${versionName()} ($BUILD_DATE), ${deviceInfo()}")
    }

    private fun writeLine(level: String, tag: String, msg: String) {
        val o = out ?: return
        runCatching {
            o.write("${time()} $level/$tag: $msg\n".toByteArray())
            o.flush()
        }
    }

    private fun closeStream() {
        runCatching { out?.close() }
        out = null
    }

    private fun deviceInfo() =
        "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), ${Build.MANUFACTURER} ${Build.MODEL}"

    // ---- crash report -----------------------------------------------------------------------

    private fun writeCrash(thread: Thread, error: Throwable) {
        val text = buildString {
            appendLine("DATAMOSH FM crash report")
            appendLine("Time: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
            appendLine("Version: ${versionName()} ($BUILD_DATE)")
            appendLine("Device: ${deviceInfo()}")
            appendLine("Thread: ${thread.name}")
            appendLine()
            append(Log.getStackTraceString(error))
        }
        openStream("DATAMOSH_FM_crash_${fileStamp()}.txt")?.use { it.write(text.toByteArray()) }
    }

    // ---- Download folder access -------------------------------------------------------------

    private fun openStream(name: String): OutputStream? = try {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = app.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let { app.contentResolver.openOutputStream(it) }
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            FileOutputStream(File(dir, name))
        }
    } catch (e: Exception) {
        // Last resort: the app's own external folder.
        runCatching { app.getExternalFilesDir(null)?.let { FileOutputStream(File(it, name)) } }.getOrNull()
    }
}
