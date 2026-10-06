package com.whatsschedule.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.whatsschedule.app.util.LogStore

/**
 * Re-arms all active (non-fired) alarms after a device reboot.
 * AlarmManager alarms are lost on reboot, so they must be rescheduled.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ScheduleAlarmManager.rearmAll(context)
            LogStore.add(context, "Device rebooted – alarms rescheduled", "info")
        }
    }
}
