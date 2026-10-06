package com.whatsschedule.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.whatsschedule.app.MainActivity
import com.whatsschedule.app.R
import com.whatsschedule.app.data.ScheduleRepository
import com.whatsschedule.app.model.Schedule
import com.whatsschedule.app.util.LogStore

/**
 * Fires when a schedule's time arrives. Posts a notification whose tap action
 * opens WhatsApp (via the wa.me deep link) with the message pre-filled, then
 * marks the schedule as fired (or re-arms it for the next day if repeating).
 *
 * Modern Android forbids launching an Activity directly from the background,
 * so we surface a high-priority notification the user taps to send — this is
 * the reliable native equivalent of the web app's window.open().
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ScheduleAlarmManager.EXTRA_ID, -1L)
        if (id == -1L) return
        val schedule = ScheduleRepository.find(context, id) ?: return
        if (!schedule.active) return

        ensureChannel(context)
        postNotification(context, schedule)

        LogStore.add(
            context,
            "Triggered autoreply to ${schedule.name} (${schedule.phone})",
            "success"
        )

        if (schedule.repeatDaily) {
            // advance 24h and re-arm
            schedule.time += 24L * 60 * 60 * 1000
            schedule.fired = false
            ScheduleRepository.update(context, schedule)
            ScheduleAlarmManager.arm(context, schedule)
        } else {
            schedule.fired = true
            schedule.active = false
            ScheduleRepository.update(context, schedule)
        }
    }

    private fun postNotification(context: Context, schedule: Schedule) {
        val waIntent = whatsappIntent(schedule)
        val contentPi = PendingIntent.getActivity(
            context, schedule.id.toInt(), waIntent, piFlags()
        )

        val openAppPi = PendingIntent.getActivity(
            context, (schedule.id + 1).toInt(),
            Intent(context, MainActivity::class.java),
            piFlags()
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_send)
            .setContentTitle("Autoreply ready: ${schedule.name}")
            .setContentText(schedule.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(schedule.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .addAction(R.drawable.ic_send, "Send on WhatsApp", contentPi)
            .addAction(R.drawable.ic_app, "Open app", openAppPi)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(schedule.id.toInt(), notif)
    }

    private fun whatsappIntent(schedule: Schedule): Intent {
        val url = "https://wa.me/${schedule.cleanPhone()}?text=" +
            Uri.encode(schedule.message)
        return Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun piFlags(): Int {
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        return flags
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Autoreply triggers",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Fires when a scheduled WhatsApp message is due" }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "autoreply_triggers"
    }
}
