package com.whatsschedule.app.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.whatsschedule.app.databinding.ItemScheduleBinding
import com.whatsschedule.app.model.Schedule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SchedulesAdapter(
    private val ctx: android.content.Context,
    private val onToggle: (Schedule) -> Unit,
    private val onDelete: (Schedule) -> Unit
) : RecyclerView.Adapter<SchedulesAdapter.VH>() {

    private val items = mutableListOf<Schedule>()
    private val fmt = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())

    fun submit(list: List<Schedule>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemScheduleBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = items[position]
        holder.b.txtName.text = s.name
        holder.b.txtPhone.text = s.cleanPhone()
        holder.b.txtMessage.text = "\"${s.message}\""
        holder.b.txtTime.text = fmt.format(Date(s.time))

        // Repeat chip visibility
        holder.b.chipRepeat.visibility = if (s.repeatDaily) android.view.View.VISIBLE else android.view.View.GONE

        // Dim fired / inactive items
        holder.b.root.alpha = if (s.active && !s.fired) 1f else 0.5f

        // Strikethrough for fired
        if (s.fired) {
            holder.b.txtName.paintFlags = holder.b.txtName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.b.txtName.paintFlags = holder.b.txtName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        // Toggle button
        holder.b.btnToggle.text = if (s.active) "Pause" else "Resume"
        if (s.active) {
            holder.b.btnToggle.setBackgroundColor(
                android.graphics.Color.parseColor("#FEF3C7")
            ) // amber100
            holder.b.btnToggle.setTextColor(
                android.graphics.Color.parseColor("#B45309")
            ) // amber700
        } else {
            holder.b.btnToggle.setBackgroundColor(
                android.graphics.Color.parseColor("#D1FAE5")
            ) // emerald100
            holder.b.btnToggle.setTextColor(
                android.graphics.Color.parseColor("#059669")
            ) // emerald600
        }
        holder.b.btnToggle.setOnClickListener { onToggle(s) }

        holder.b.btnDelete.setOnClickListener { onDelete(s) }
    }

    override fun getItemCount() = items.size
}
