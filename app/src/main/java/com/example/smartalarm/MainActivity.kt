package com.example.smartalarm

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smartalarm.ui.fragments.AlarmContainerFragment
import com.example.smartalarm.ui.fragments.HistoryFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var alarmContainerFragment: AlarmContainerFragment
    private lateinit var historyFragment: HistoryFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            alarmContainerFragment = AlarmContainerFragment()
            historyFragment = HistoryFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, alarmContainerFragment, "ALARM")
                .add(R.id.fragment_container, historyFragment, "HISTORY")
                .hide(historyFragment)
                .commit()
        } else {
            alarmContainerFragment =
                supportFragmentManager.findFragmentByTag("ALARM") as AlarmContainerFragment
            historyFragment =
                supportFragmentManager.findFragmentByTag("HISTORY") as HistoryFragment
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_alarm -> {
                    supportFragmentManager.beginTransaction()
                        .show(alarmContainerFragment)
                        .hide(historyFragment)
                        .commit()
                    true
                }

                R.id.nav_history -> {
                    supportFragmentManager.beginTransaction()
                        .show(historyFragment)
                        .hide(alarmContainerFragment)
                        .commit()
                    true
                }

                else -> false
            }
        }
    }
}