package com.firstapp.myapplication.ui

import android.os.Bundle
import android.util.Log
import android.content.Intent
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firstapp.myapplication.R
import com.firstapp.myapplication.auth.AuthManager
import com.firstapp.myapplication.network.Network
import android.widget.Button
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    companion object { private const val TAG = "HomeActivity" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        AuthManager.init(applicationContext)

        val btnAddHabit = findViewById<Button>(R.id.btnAddHabit)
        val btnCreateSchedule = findViewById<Button>(R.id.btnCreateSchedule)

        btnAddHabit.setOnClickListener {
            startActivity(Intent(this, AddHabitActivity::class.java))
        }

        btnCreateSchedule.setOnClickListener {
            startActivity(Intent(this, CreateScheduleActivity::class.java))
        }

        fetchSchedules()
    }

    override fun onResume() {
        super.onResume()
        // refresh list when returning from create screens
        fetchSchedules()
    }

    private fun fetchSchedules() {
        val container = findViewById<LinearLayout>(R.id.llSchedules)
        val statusTv = findViewById<TextView>(R.id.tvStatus)
        statusTv.text = "Loading..."
        lifecycleScope.launch {
            try {
                val resp = Network.api.getSchedulesByDay(null)
                if (resp.isSuccessful) {
                    val list = resp.body() ?: emptyList()
                    statusTv.text = "${list.size} schedules"
                    container.removeAllViews()
                    for (s in list) {
                        val tv = TextView(this@HomeActivity).apply {
                            text = "#${s.id} — ${s.start_time ?: "?"} — ${s.status ?: "?"} — ${s.notes ?: ""}"
                            textSize = 16f
                            setPadding(8,8,8,8)
                            setOnClickListener {
                                try {
                                    val idInt = s.id.toInt()
                                    val intent = Intent(this@HomeActivity, ScheduleDetailActivity::class.java)
                                    intent.putExtra(ScheduleDetailActivity.EXTRA_SCHEDULE_ID, idInt)
                                    startActivity(intent)
                                } catch (ex: Exception) {
                                    Log.e(TAG, "Invalid schedule id", ex)
                                }
                            }
                        }
                        container.addView(tv)
                    }
                } else {
                    statusTv.text = "Failed: ${resp.code()}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetch schedules", e)
                statusTv.text = "Error: ${e.localizedMessage}"
            }
        }
    }
}
