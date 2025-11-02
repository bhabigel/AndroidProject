package com.firstapp.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.firstapp.myapplication.ui.CreateScheduleActivity
import com.firstapp.myapplication.ui.HomeFragment
import com.firstapp.myapplication.ui.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    companion object { private const val TAG = "MainActivity" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // default fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()

        val bottom = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container, HomeFragment()).commit()
                    true
                }
                R.id.nav_profile -> {
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container, ProfileFragment()).commit()
                    true
                }
                else -> false
            }
        }

        val fab = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add)
        fab.setOnClickListener {
            try {
                startActivity(Intent(this, CreateScheduleActivity::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "open create schedule", e)
            }
        }
    }
}

