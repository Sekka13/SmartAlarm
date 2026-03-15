package com.example.smartalarm.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartalarm.R
import com.example.smartalarm.data.model.SleepSession
import java.text.SimpleDateFormat
import java.util.*

class SessionAdapter(
    private var sessions: List<SleepSession>
) : RecyclerView.Adapter<SessionAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val textStart: TextView = view.findViewById(R.id.text_start_time)
        val textEnd: TextView = view.findViewById(R.id.text_end_time)
        val textMin: TextView = view.findViewById(R.id.text_bpm_min)
        val textMax: TextView = view.findViewById(R.id.text_bpm_max)
        val textAvg: TextView = view.findViewById(R.id.text_bpm_avg)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val session = sessions[position]

        // Convertimos timestamp a formato 12h AM/PM
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val startTimeStr = sdf.format(Date(session.startTime))
        val endTimeStr = sdf.format(Date(session.endTime))

        holder.textStart.text = "Start: $startTimeStr"
        holder.textEnd.text = "End: $endTimeStr"
        holder.textMin.text = "Min: ${session.bpmMin}"
        holder.textMax.text = "Max: ${session.bpmMax}"
        holder.textAvg.text = "Avg: ${session.bpmAvg}"
    }

    override fun getItemCount(): Int = sessions.size

    fun setSessions(newSessions: List<SleepSession>) {
        sessions = newSessions
        notifyDataSetChanged()
    }
}