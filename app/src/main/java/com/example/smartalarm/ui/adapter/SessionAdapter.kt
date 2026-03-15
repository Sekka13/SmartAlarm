package com.example.smartalarm.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartalarm.R
import com.example.smartalarm.data.model.SleepSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class SessionAdapter(private var sessions: List<SleepSession>) :
    RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

    class SessionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textStart: TextView = view.findViewById(R.id.text_start_time)
        val textEnd: TextView = view.findViewById(R.id.text_end_time)
        val textBpmMin: TextView = view.findViewById(R.id.text_bpm_min)
        val textBpmMax: TextView = view.findViewById(R.id.text_bpm_max)
        val textBpmAvg: TextView = view.findViewById(R.id.text_bpm_avg)

    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun getItemCount(): Int = sessions.size

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]



        // Convertimos timestamp a HH:mm

        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        //sdf.timeZone = TimeZone.getDefault() // zona local del dispositivo

        val startTimeStr = sdf.format(Date(session.startTime))
        val endTimeStr = sdf.format(Date(session.endTime))

        holder.textStart.text = "Start: $startTimeStr"
        holder.textEnd.text = "End: $endTimeStr"
        holder.textBpmMin.text = "Min BPM: ${session.bpmMin}"
        holder.textBpmMax.text = "Max BPM: ${session.bpmMax}"
        holder.textBpmAvg.text = "Avg BPM: ${session.bpmAvg}"
    }

    fun setSessions(newSessions: List<SleepSession>) {
        sessions = newSessions
        notifyDataSetChanged()
    }


}