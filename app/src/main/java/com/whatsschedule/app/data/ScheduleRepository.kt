package com.whatsschedule.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.whatsschedule.app.model.Schedule

/**
 * Persists the list of scheduled autoreplies in SharedPreferences as JSON,
 * mirroring the web app's localStorage('wa_schedules') behaviour.
 */
object ScheduleRepository {
    private const val PREFS = "wa_store"
    private const val KEY = "wa_schedules"
    private val gson = Gson()
    private val type = object : TypeToken<MutableList<Schedule>>() {}.type

    private var cache: MutableList<Schedule>? = null

    fun all(ctx: Context): MutableList<Schedule> {
        cache?.let { return it }
        val raw = prefs(ctx).getString(KEY, null)
        val list = if (raw == null) mutableListOf()
        else runCatching { gson.fromJson<MutableList<Schedule>>(raw, type) }
            .getOrNull() ?: mutableListOf()
        cache = list
        return list
    }

    fun save(ctx: Context) {
        val list = cache ?: return
        prefs(ctx).edit().putString(KEY, gson.toJson(list)).apply()
    }

    fun add(ctx: Context, schedule: Schedule) {
        all(ctx).add(schedule)
        save(ctx)
    }

    fun update(ctx: Context, schedule: Schedule) {
        val list = all(ctx)
        val i = list.indexOfFirst { it.id == schedule.id }
        if (i >= 0) list[i] = schedule else list.add(schedule)
        save(ctx)
    }

    fun remove(ctx: Context, id: Long) {
        all(ctx).removeAll { it.id == id }
        save(ctx)
    }

    fun find(ctx: Context, id: Long): Schedule? = all(ctx).firstOrNull { it.id == id }

    fun nextId(): Long = System.currentTimeMillis()

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
