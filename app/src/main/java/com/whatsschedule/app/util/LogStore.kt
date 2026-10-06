package com.whatsschedule.app.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A single execution-log line. */
data class LogEntry(val time: Long, val text: String, val type: String)

/**
 * Lightweight persistent ring-buffer of execution log lines, mirroring the
 * "Execution Log" panel from the original web app. Backed by SharedPreferences.
 */
object LogStore {
    private const val PREFS = "wa_logs"
    private const val KEY = "entries"
    private const val MAX = 200
    private val gson = Gson()
    private val type = object : TypeToken<MutableList<LogEntry>>() {}.type

    fun all(ctx: Context): MutableList<LogEntry> {
        val raw = prefs(ctx).getString(KEY, null) ?: return mutableListOf()
        return runCatching { gson.fromJson<MutableList<LogEntry>>(raw, type) }
            .getOrNull() ?: mutableListOf()
    }

    fun add(ctx: Context, text: String, type: String = "info") {
        val list = all(ctx)
        list.add(LogEntry(System.currentTimeMillis(), text, type))
        while (list.size > MAX) list.removeAt(0)
        prefs(ctx).edit().putString(KEY, gson.toJson(list)).apply()
    }

    fun clear(ctx: Context) {
        prefs(ctx).edit().remove(KEY).apply()
    }

    fun formatTime(millis: Long): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(millis))

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
