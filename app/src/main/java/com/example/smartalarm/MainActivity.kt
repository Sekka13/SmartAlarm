package com.example.smartalarm

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smartalarm.ui.fragments.HomeFragment
import com.example.smartalarm.ui.fragments.HistoryFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var homeFragment: HomeFragment
    private lateinit var historyFragment: HistoryFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            homeFragment = HomeFragment()
            historyFragment = HistoryFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, homeFragment, "HOME")
                .add(R.id.fragment_container, historyFragment, "HISTORY")
                .hide(historyFragment)
                .commit()
        } else {
            homeFragment = supportFragmentManager.findFragmentByTag("HOME") as HomeFragment
            historyFragment = supportFragmentManager.findFragmentByTag("HISTORY") as HistoryFragment
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .show(homeFragment)
                        .hide(historyFragment)
                        .commit()
                    true
                }

                R.id.nav_history -> {
                    supportFragmentManager.beginTransaction()
                        .show(historyFragment)
                        .hide(homeFragment)
                        .commit()
                    true
                }

                else -> false
            }
        }
    }
}