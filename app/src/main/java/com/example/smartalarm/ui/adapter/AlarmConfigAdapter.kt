package com.example.smartalarm.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartalarm.R
import com.example.smartalarm.data.model.AlarmConfig
import java.util.Locale

class AlarmConfigAdapter(
    private var items: List<AlarmConfig>,
    private val onAlarmClicked: (AlarmConfig) -> Unit,
    private val onAlarmEnabledChanged: (AlarmConfig, Boolean) -> Unit,
    private val onAlarmDeleteClicked: (AlarmConfig) -> Unit
) : RecyclerView.Adapter<AlarmConfigAdapter.AlarmViewHolder>() {

    inner class AlarmViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textTime: TextView = view.findViewById(R.id.text_alarm_item_time)
        val textDetails: TextView = view.findViewById(R.id.text_alarm_item_details)
        val switchEnabled: Switch = view.findViewById(R.id.switch_alarm_enabled)
        val buttonDelete: ImageButton = view.findViewById(R.id.button_delete_alarm)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm_config, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val item = items[position]

        holder.textTime.text = formatHour(item.hour24, item.minute)
        holder.textDetails.text = buildDetails(item)

        holder.switchEnabled.setOnCheckedChangeListener(null)
        holder.switchEnabled.isChecked = item.isEnabled

        holder.itemView.setOnClickListener {
            onAlarmClicked(item)
        }

        holder.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            onAlarmEnabledChanged(item, isChecked)
        }

        holder.buttonDelete.setOnClickListener {
            onAlarmDeleteClicked(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<AlarmConfig>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun formatHour(hour24: Int, minute: Int): String {
        val amPm = if (hour24 >= 12) "PM" else "AM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, amPm)
    }

    private fun buildDetails(item: AlarmConfig): String {
        val days = if (item.repeatDays.isBlank()) "One time" else item.repeatDays
        val state = if (item.isEnabled) "On" else "Off"
        return "Window ${item.smartWindowMinutes} min • $days • $state"
    }
}