package com.example.smartalarm.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.smartalarm.R

class AlarmContainerFragment : Fragment(), AlarmSetupFragment.Listener {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_alarm_container, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (childFragmentManager.findFragmentById(R.id.alarm_container_child) == null) {
            showDashboard()
        }
    }

    fun showDashboard() {
        val current = childFragmentManager.findFragmentById(R.id.alarm_container_child)
        if (current is AlarmDashboardFragment) return

        childFragmentManager.beginTransaction()
            .replace(R.id.alarm_container_child, AlarmDashboardFragment(), "ALARM_DASHBOARD")
            .commit()
    }

    fun showSetup(alarmId: Long? = null) {
        val fragment = AlarmSetupFragment.newInstance(alarmId)

        childFragmentManager.beginTransaction()
            .replace(R.id.alarm_container_child, fragment, "ALARM_SETUP")
            .commit()
    }

    override fun onAlarmSetupCancelled() {
        showDashboard()
    }

    override fun onAlarmSaved() {
        showDashboard()
    }
}