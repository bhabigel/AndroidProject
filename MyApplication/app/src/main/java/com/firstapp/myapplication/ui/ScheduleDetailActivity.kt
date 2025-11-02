package com.firstapp.myapplication.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firstapp.myapplication.R
import com.firstapp.myapplication.network.Network
import kotlinx.coroutines.launch

class ScheduleDetailActivity : AppCompatActivity() {
    companion object { const val EXTRA_SCHEDULE_ID = "extra_schedule_id" }

    private var scheduleId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_detail)

        scheduleId = intent?.getIntExtra(EXTRA_SCHEDULE_ID, -1) ?: -1
    val tvId = findViewById<TextView>(R.id.tvScheduleId)
    val tvStart = findViewById<TextView>(R.id.tvStartTime)
    val etNotes = findViewById<EditText>(R.id.etNotes)
    val btnAddProgress = findViewById<Button>(R.id.btnAddProgress)
    val btnSave = findViewById<Button>(R.id.btnSave)
    val btnDelete = findViewById<Button>(R.id.btnDelete)

        if (scheduleId == -1) {
            Toast.makeText(this, "No schedule id", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvId.text = "Schedule #$scheduleId"

        loadSchedule(tvStart, etNotes)

        btnAddProgress.setOnClickListener {
            // open CreateProgressActivity with this schedule id
            val intent = android.content.Intent(this, CreateProgressActivity::class.java)
            intent.putExtra(CreateProgressActivity.EXTRA_SCHEDULE_ID, scheduleId)
            startActivity(intent)
        }

        btnSave.setOnClickListener {
            val notes = etNotes.text.toString().trim()
            lifecycleScope.launch {
                try {
                    val body = mutableMapOf<String, Any>()
                    body["notes"] = notes
                    val resp = Network.api.updateSchedule(scheduleId, body)
                    if (resp.isSuccessful) {
                        Toast.makeText(this@ScheduleDetailActivity, "Saved", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@ScheduleDetailActivity, "Failed: ${resp.code()}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@ScheduleDetailActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }

        btnDelete.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val resp = Network.api.deleteSchedule(scheduleId)
                    if (resp.isSuccessful) {
                        Toast.makeText(this@ScheduleDetailActivity, "Deleted", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@ScheduleDetailActivity, "Failed: ${resp.code()}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@ScheduleDetailActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // refresh schedule in case progress was added
        val tvStart = findViewById<TextView>(R.id.tvStartTime)
        val etNotes = findViewById<EditText>(R.id.etNotes)
        loadSchedule(tvStart, etNotes)
    }

    private fun loadSchedule(tvStart: TextView, etNotes: EditText) {
        lifecycleScope.launch {
            try {
                val resp = Network.api.getScheduleById(scheduleId)
                if (resp.isSuccessful) {
                    val s = resp.body()
                    tvStart.text = "Start: ${s?.start_time ?: "?"}"
                    etNotes.setText(s?.notes ?: "")
                } else {
                    Toast.makeText(this@ScheduleDetailActivity, "Failed to load: ${resp.code()}", Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ScheduleDetailActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}
