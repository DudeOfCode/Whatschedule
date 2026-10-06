package com.whatsschedule.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.whatsschedule.app.R
import com.whatsschedule.app.alarm.ScheduleAlarmManager
import com.whatsschedule.app.data.ScheduleRepository
import com.whatsschedule.app.databinding.ActivityMainBinding
import com.whatsschedule.app.model.Schedule
import com.whatsschedule.app.util.LogStore

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var adapter: SchedulesAdapter

    private val notifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — we post anyway on older versions */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Re-arm any active alarms that may have been lost
        ScheduleAlarmManager.rearmAll(this)

        adapter = SchedulesAdapter(this, ::onToggle, ::onDelete)
        b.recyclerSchedules.layoutManager = LinearLayoutManager(this)
        b.recyclerSchedules.adapter = adapter

        b.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddScheduleActivity::class.java))
        }

        b.btnClearLogs.setOnClickListener {
            LogStore.clear(this)
            renderLog()
            LogStore.add(this, "Logs cleared.")
            renderLog()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val list = ScheduleRepository.all(this)
        adapter.submit(list)
        b.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

        // Update status badge
        val active = list.count { it.active && !it.fired }
        if (active > 0) {
            b.statusBadge.text = "$active Active"
        } else {
            b.statusBadge.text = "No Active"
        }

        renderLog()
    }

    private fun renderLog() {
        val entries = LogStore.all(this)
        if (entries.isEmpty()) {
            b.logText.text = "[System] Autoreply service initialized and listening…"
            return
        }
        val sb = StringBuilder()
        for (e in entries) {
            val color = when (e.type) {
                "success" -> "🟢"
                "error" -> "🔴"
                else -> "⚪"
            }
            sb.appendLine("$color [${LogStore.formatTime(e.time)}] ${e.text}")
        }
        b.logText.text = sb.toString()
        b.logScroll.post { b.logScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun onToggle(schedule: Schedule) {
        if (schedule.active) {
            // Pause
            schedule.active = false
            ScheduleAlarmManager.cancel(this, schedule.id)
            ScheduleRepository.update(this, schedule)
            LogStore.add(this, "Paused schedule for ${schedule.name}", "info")
        } else {
            // Resume (only if not already fired, or if repeating)
            if (schedule.fired && !schedule.repeatDaily) {
                LogStore.add(this, "Cannot resume – already fired", "error")
                return
            }
            schedule.active = true
            ScheduleAlarmManager.arm(this, schedule)
            ScheduleRepository.update(this, schedule)
            LogStore.add(this, "Resumed schedule for ${schedule.name}", "info")
        }
        refresh()
    }

    private fun onDelete(schedule: Schedule) {
        ScheduleAlarmManager.cancel(this, schedule.id)
        ScheduleRepository.remove(this, schedule.id)
        LogStore.add(this, "Removed schedule for ${schedule.name}", "info")
        refresh()
    }
}
