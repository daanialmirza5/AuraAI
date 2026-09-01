package com.aura.ai.core.crash

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The "crash-reporting hook" Milestone 10 asked for, without a third-party SDK: wiring one in
 * (Crashlytics, Sentry, ...) needs an account/project only the app's publisher can create — a
 * genuine human-decision blocker, not an engineering one, per this project's own stated exception
 * for exactly this class of thing. What's real and shippable without anyone's credentials is a
 * local uncaught-exception handler: every crash is written to a bounded, on-device log
 * ([CRASH_LOG_DIR] under the app's private files, capped at [MAX_LOGS]) before the previous
 * handler (Android's own, which shows the OS "app has stopped" dialog and terminates the process)
 * still runs — this never swallows a crash, only records it first.
 *
 * A future pass can point a real crash-reporting SDK's own handler at this same install point
 * without anything else in the app needing to change.
 */
object LocalCrashLogger {
    private const val TAG = "LocalCrashLogger"
    private const val CRASH_LOG_DIR = "crash-logs"
    private const val MAX_LOGS = 20

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            @Suppress("TooGenericExceptionCaught") // Deliberate: this already-crashing path must
            // never itself throw and prevent previousHandler from running, regardless of what
            // specifically goes wrong writing the log (IOException, SecurityException, or
            // anything else file I/O on an arbitrary device/OS version might raise).
            try {
                writeCrashLog(appContext, thread, throwable)
            } catch (writeFailure: Exception) {
                Log.e(TAG, "Failed to write local crash log", writeFailure)
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrashLog(
        context: Context,
        thread: Thread,
        throwable: Throwable,
    ) {
        val dir = File(context.filesDir, CRASH_LOG_DIR).apply { mkdirs() }
        pruneOldLogs(dir)

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val stackTrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()

        File(dir, "crash_$timestamp.txt").writeText(
            "Thread: ${thread.name}\nTime: $timestamp\n\n$stackTrace",
        )
    }

    private fun pruneOldLogs(dir: File) {
        val logs = dir.listFiles()?.sortedBy { it.lastModified() } ?: return
        val excess = logs.size - (MAX_LOGS - 1)
        if (excess > 0) logs.take(excess).forEach { it.delete() }
    }
}
