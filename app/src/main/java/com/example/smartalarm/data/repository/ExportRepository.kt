package com.example.smartalarm.data.repository

import android.content.Context
import com.example.smartalarm.data.model.SleepSession
import com.example.smartalarm.domain.session.SessionReplayMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportRepository {

    //Formateador de hora para exportar
    val formatter = SimpleDateFormat("dd:MM:HH:mm:ss a", Locale.getDefault())



    fun exportCsv(context: Context, sessions: List<SleepSession>) {

        val file = File(context.getExternalFilesDir(null), "sleep_sessions.csv")

        val builder = StringBuilder()

        builder.append("start,end,replayMode,bpmMin,bpmMax,bpmAvg\n")

        for (s in sessions) {

            builder.append("${formatter.format(Date(s.startTime))},${formatter.format(Date(s.endTime))},${SessionReplayMode.displayNameFromStoredValue(s.replayMode)},${s.bpmMin},${s.bpmMax},${s.bpmAvg}\n")
        }

        file.writeText(builder.toString())
    }

    fun exportJson(context: Context, sessions: List<SleepSession>) {

        val file = File(context.getExternalFilesDir(null), "sleep_sessions.json")

        val array = JSONArray()

        for (s in sessions) {

            val obj = JSONObject()

            obj.put("start", formatter.format(Date(s.startTime)))
            obj.put("end", formatter.format(Date(s.endTime)))
            obj.put("replayMode", SessionReplayMode.displayNameFromStoredValue(s.replayMode))
            obj.put("bpmMin", s.bpmMin)
            obj.put("bpmMax", s.bpmMax)
            obj.put("bpmAvg", s.bpmAvg)

            array.put(obj)
        }

        file.writeText(array.toString())
    }
}
