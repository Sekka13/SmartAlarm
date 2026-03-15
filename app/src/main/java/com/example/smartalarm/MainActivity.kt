package com.example.smartalarm

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smartalarm.data.db.AppDatabase
import com.example.smartalarm.data.repository.ExportRepository
import com.example.smartalarm.data.repository.SleepSessionRepository
import com.example.smartalarm.ui.fragments.HomeFragment
import com.example.smartalarm.ui.fragments.HistoryFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var sleepRepo: SleepSessionRepository
    private lateinit var exportRepo: ExportRepository

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        database = AppDatabase.getDatabase(this)

        sleepRepo = SleepSessionRepository(
            database.sleepSessionDao()
        )

        exportRepo = ExportRepository()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (savedInstanceState == null) {

            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    HomeFragment(sleepRepo)
                )
                .commit()
        }

        bottomNav.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_home -> {

                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragment_container,
                            HomeFragment(sleepRepo)
                        )
                        .commit()

                    true
                }

                R.id.nav_history -> {

                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragment_container,
                            HistoryFragment(sleepRepo, exportRepo)
                        )
                        .commit()

                    true
                }

                else -> false
            }
        }
    }
}