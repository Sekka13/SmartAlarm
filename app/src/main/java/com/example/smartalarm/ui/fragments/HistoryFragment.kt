package com.example.smartalarm.ui.fragments

import android.os.Bundle
import android.view.*
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartalarm.R
import com.example.smartalarm.data.db.AppDatabase
import com.example.smartalarm.data.repository.ExportRepository
import com.example.smartalarm.data.repository.SleepSessionRepository
import com.example.smartalarm.ui.adapter.SessionAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryFragment : Fragment() {

    private lateinit var sleepRepo: SleepSessionRepository
    private lateinit var exportRepo: ExportRepository

    private lateinit var recycler: RecyclerView
    private lateinit var buttonExportCsv: Button
    private lateinit var buttonExportJson: Button
    private lateinit var adapter: SessionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_history, container, false)
        val db = AppDatabase.getDatabase(requireContext())
        sleepRepo = SleepSessionRepository(db.sleepSessionDao())
        exportRepo = ExportRepository()
        recycler = view.findViewById(R.id.recycler_sessions)
        buttonExportCsv = view.findViewById(R.id.button_export_csv)
        buttonExportJson = view.findViewById(R.id.button_export_json)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = SessionAdapter(emptyList())
        recycler.adapter = adapter

        loadSessions()

        buttonExportCsv.setOnClickListener {
            exportCsv()
        }

        buttonExportJson.setOnClickListener {
            exportJson()
        }

        return view
    }

    private fun loadSessions() {
        lifecycleScope.launch {
            val sessions = withContext(Dispatchers.IO) {
                sleepRepo.getAll()
            }
            adapter.setSessions(sessions)
        }
    }

    private fun exportCsv() {
        lifecycleScope.launch {
            val sessions = withContext(Dispatchers.IO) { sleepRepo.getAll() }
            withContext(Dispatchers.IO) { exportRepo.exportCsv(requireContext(), sessions) }
        }
    }

    private fun exportJson() {
        lifecycleScope.launch {
            val sessions = withContext(Dispatchers.IO) { sleepRepo.getAll() }
            withContext(Dispatchers.IO) { exportRepo.exportJson(requireContext(), sessions) }
        }
    }
}