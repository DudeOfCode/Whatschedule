package com.whatsschedule.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.whatsschedule.app.data.ScheduleRepository
import com.whatsschedule.app.model.Schedule

/**
 * Thin wrapper over AlarmManager that arms / cancels an exact alarm for each
 * active schedule. The request code is the (truncated) schedule id so each
 * alarm is uniquely addressable.
 */
object ScheduleAlarmManager {

    const val EXTRA_ID = "schedule_id"

    fun arm(ctx: Context, schedule: Schedule) {
        if (!schedule.active) return
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(ctx, schedule.id)

        // On Android 12+ exact alarms require permission; fall back to inexact.
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            am.canScheduleExactAlarms() else true

        if (canExact) {
            am.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, schedule.time, pi
            )
        } else {
            am.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, schedule.time, pi
            )
        }
    }

    fun cancel(ctx: Context, id: Long) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(ctx, id))
    }

    /** Re-arm all active, not-yet-fired schedules (used after boot). */
    fun rearmAll(ctx: Context) {
        ScheduleRepository.all(ctx)
            .filter { it.active && !it.fired }
            .forEach { arm(ctx, it) }
    }

    private fun pendingIntent(ctx: Context, id: Long): PendingIntent {
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ID, id)
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(ctx, id.toInt(), intent, flags)
    }
}
