package com.firstapp.myapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firstapp.myapplication.R
import com.firstapp.myapplication.models.ProgressResponseDto
import com.firstapp.myapplication.models.ScheduleResponseDto
import com.firstapp.myapplication.network.Network
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ScheduleDetailActivity : AppCompatActivity() {
    companion object { const val EXTRA_SCHEDULE_ID = "extra_schedule_id" }

    private var scheduleId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_detail)

        scheduleId = intent?.getIntExtra(EXTRA_SCHEDULE_ID, -1) ?: -1

        if (scheduleId == -1) {
            Toast.makeText(this, "No schedule id", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        loadScheduleDetails()
    }

    private fun setupViews() {
        // Back button
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Menu button (placeholder)
        findViewById<TextView>(R.id.btnMenu).setOnClickListener {
            // TODO: Implement menu functionality
        }

        // Add Progress Button
        findViewById<Button>(R.id.fabAddProgress).setOnClickListener {
            openCreateProgress()
        }

        // Edit Notes button
        findViewById<TextView>(R.id.btnEditNotes).setOnClickListener {
            // TODO: Implement notes editing
            Toast.makeText(this, "Edit notes functionality coming soon", Toast.LENGTH_SHORT).show()
        }

        // Compatibility with old button
        findViewById<Button>(R.id.btnAddProgress).setOnClickListener {
            openCreateProgress()
        }
    }

    private fun openCreateProgress() {
        try {
            val intent = android.content.Intent(this, CreateProgressActivity::class.java)
            intent.putExtra(CreateProgressActivity.EXTRA_SCHEDULE_ID, scheduleId)
            startActivity(intent)
        } catch (ex: Exception) {
            Toast.makeText(this, "Error opening progress: ${ex.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh schedule details when returning from progress creation
        loadScheduleDetails()
    }

    private fun loadScheduleDetails() {
        lifecycleScope.launch {
            try {
                val response = Network.api.getScheduleById(scheduleId)
                if (response.isSuccessful) {
                    val schedule = response.body()
                    if (schedule != null) {
                        populateScheduleDetails(schedule)
                    } else {
                        showError("Schedule not found")
                    }
                } else {
                    showError("Failed to load schedule: ${response.code()}")
                }
            } catch (e: Exception) {
                showError("Error loading schedule: ${e.localizedMessage}")
            }
        }
    }

    private fun populateScheduleDetails(schedule: ScheduleResponseDto) {
        // Habit name and icon
        val habitName = schedule.habit?.name ?: schedule.notes ?: "Schedule #${schedule.id}"
        findViewById<TextView>(R.id.tvHabitName).text = habitName

        // Schedule time
        val timeText = formatScheduleTime(schedule.start_time, schedule.end_time)
        findViewById<TextView>(R.id.tvScheduleTime).text = timeText

        // Status
        val status = schedule.status?.replaceFirstChar { it.uppercase() } ?: "Planned"
        findViewById<TextView>(R.id.tvStatus).text = status

        // Duration
        val duration = if (schedule.duration_minutes != null && schedule.duration_minutes > 0) {
            "${schedule.duration_minutes}m"
        } else {
            calculateDuration(schedule.start_time, schedule.end_time)
        }
        findViewById<TextView>(R.id.tvDuration).text = duration

        // Partner info (placeholder for now)
        val partnerName = findViewById<TextView>(R.id.tvPartnerName)
        partnerName.text = if (schedule.participantIds?.isNotEmpty() == true) {
            "Partner #${schedule.participantIds.first()}"
        } else {
            "No partners"
        }

        // Repeat type
        val repeatText = if (schedule.is_custom) "Custom" else "Recurring"
        findViewById<TextView>(R.id.tvRepeat).text = repeatText

        // Notes
        val notesText = schedule.notes ?: "No notes available"
        findViewById<TextView>(R.id.tvNotes).text = notesText

        // Recent activities
        populateRecentActivities(schedule.progress ?: emptyList())

        // Compatibility fields
        findViewById<TextView>(R.id.tvScheduleId).text = "Schedule #$scheduleId"
        findViewById<TextView>(R.id.tvStartTime).text = "Start: ${schedule.start_time}"
        findViewById<EditText>(R.id.etNotes).setText(schedule.notes ?: "")
    }

    private fun formatScheduleTime(startTime: String?, endTime: String?): String {
        return try {
            val start = startTime?.let { 
                if (it.contains('T')) {
                    val timePart = it.substringAfter('T').substringBefore('+').substringBefore('Z')
                    val parts = timePart.split(':')
                    if (parts.size >= 2) {
                        val hour = parts[0].toInt()
                        val minute = parts[1]
                        val amPm = if (hour >= 12) "PM" else "AM"
                        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                        "$displayHour:$minute $amPm"
                    } else timePart
                } else it
            }
            
            val end = endTime?.let { 
                if (it.contains('T')) {
                    val timePart = it.substringAfter('T').substringBefore('+').substringBefore('Z')
                    val parts = timePart.split(':')
                    if (parts.size >= 2) {
                        val hour = parts[0].toInt()
                        val minute = parts[1]
                        val amPm = if (hour >= 12) "PM" else "AM"
                        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                        "$displayHour:$minute $amPm"
                    } else timePart
                } else it
            }
            
            if (start != null && end != null) {
                "$start - $end"
            } else {
                start ?: end ?: "No time specified"
            }
        } catch (e: Exception) {
            startTime ?: "No time"
        }
    }

    private fun calculateDuration(startTime: String?, endTime: String?): String {
        return try {
            if (startTime != null && endTime != null) {
                // Simple duration calculation - you may want to implement proper time parsing
                "30m" // Default for now
            } else {
                "N/A"
            }
        } catch (e: Exception) {
            "N/A"
        }
    }

    private fun populateRecentActivities(progressList: List<ProgressResponseDto>) {
        val activitiesContainer = findViewById<LinearLayout>(R.id.llRecentActivities)
        activitiesContainer.removeAllViews()

        if (progressList.isEmpty()) {
            val noActivitiesView = TextView(this).apply {
                text = "No recent activities"
                textSize = 14f
                setTextColor(resources.getColor(R.color.text_secondary, null))
                setPadding(0, dpToPx(8), 0, dpToPx(8))
            }
            activitiesContainer.addView(noActivitiesView)
            return
        }

        progressList.take(5).forEach { progress ->
            addProgressItem(activitiesContainer, progress)
        }
    }

    private fun addProgressItem(container: LinearLayout, progress: ProgressResponseDto) {
        val itemView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, container, false)
        
        val titleText = if (progress.is_completed) "✅ Completed" else "❌ Missed"
        val subtitleText = formatProgressDate(progress.date)
        
        itemView.findViewById<TextView>(android.R.id.text1).apply {
            text = titleText
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 14f
        }
        
        itemView.findViewById<TextView>(android.R.id.text2).apply {
            text = subtitleText
            setTextColor(resources.getColor(R.color.text_secondary, null))
            textSize = 12f
        }

        container.addView(itemView)
    }

    private fun formatProgressDate(dateStr: String): String {
        return try {
            // Simple date formatting - adjust as needed
            dateStr.substringBefore('T')
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}
