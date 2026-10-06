package com.whatsschedule.app.ui

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.whatsschedule.app.alarm.ScheduleAlarmManager
import com.whatsschedule.app.data.ScheduleRepository
import com.whatsschedule.app.databinding.ActivityAddScheduleBinding
import com.whatsschedule.app.model.Schedule
import com.whatsschedule.app.util.LogStore
import java.util.Calendar

class AddScheduleActivity : AppCompatActivity() {

    private lateinit var b: ActivityAddScheduleBinding
    private var selectedTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAddScheduleBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.toolbar.setNavigationOnClickListener { finish() }

        b.edtDateTime.setOnClickListener { pickDateTime() }

        b.btnSchedule.setOnClickListener { schedule() }
    }

    private fun pickDateTime() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            TimePickerDialog(this, { _, h, min ->
                val picked = Calendar.getInstance().apply {
                    set(y, m, d, h, min, 0)
                }
                selectedTime = picked.timeInMillis
                val fmt = java.text.SimpleDateFormat(
                    "MMM d, yyyy 'at' h:mm a", java.util.Locale.getDefault()
                )
                b.edtDateTime.setText(fmt.format(picked.time))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun schedule() {
        val name = b.edtName.text?.toString()?.trim()
        val phone = b.edtPhone.text?.toString()?.trim()
        val message = b.edtMessage.text?.toString()?.trim()
        val repeat = b.switchRepeat.isChecked

        if (name.isNullOrEmpty() || phone.isNullOrEmpty() || message.isNullOrEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedTime == 0L || selectedTime <= System.currentTimeMillis()) {
            Toast.makeText(this, "Please pick a future date & time", Toast.LENGTH_SHORT).show()
            return
        }

        // On Android 12+ check if exact alarms are allowed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                Toast.makeText(this, "Exact alarms not allowed. Opening settings…", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                return
            }
        }

        val id = ScheduleRepository.nextId()
        val s = Schedule(
            id = id,
            name = name,
            phone = phone,
            time = selectedTime,
            message = message,
            active = true,
            fired = false,
            repeatDaily = repeat
        )

        ScheduleRepository.add(this, s)
        ScheduleAlarmManager.arm(this, s)
        LogStore.add(this, "Scheduled autoreply added for $name ($phone)")
        Toast.makeText(this, "Message scheduled!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
