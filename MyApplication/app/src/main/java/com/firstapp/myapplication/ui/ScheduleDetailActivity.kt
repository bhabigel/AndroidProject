package com.firstapp.myapplication.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firstapp.myapplication.R
import com.firstapp.myapplication.models.ProgressResponseDto
import com.firstapp.myapplication.models.ScheduleResponseDto
import com.firstapp.myapplication.models.UpdateScheduleDto
import com.firstapp.myapplication.network.Network
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ScheduleDetailActivity : AppCompatActivity() {
    companion object { const val EXTRA_SCHEDULE_ID = "extra_schedule_id" }

    private var scheduleId: Int = -1
    private var isEditingNotes: Boolean = false

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

        // Three-dot menu button
        findViewById<TextView>(R.id.btnMenu).setOnClickListener {
            showOptionsMenu()
        }

        // Add Progress Button
        findViewById<Button>(R.id.fabAddProgress).setOnClickListener {
            openCreateProgress()
        }

        // Edit Notes button
        findViewById<TextView>(R.id.btnEditNotes).setOnClickListener {
            toggleNotesEditMode()
        }

        // Save Notes button
        findViewById<TextView>(R.id.btnSaveNotes).setOnClickListener {
            saveNotes()
        }

        // Cancel Edit button
        findViewById<TextView>(R.id.btnCancelEdit).setOnClickListener {
            cancelNotesEdit()
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

    private fun openEditSchedule() {
        try {
            val intent = android.content.Intent(this, EditScheduleActivity::class.java)
            intent.putExtra(EditScheduleActivity.EXTRA_SCHEDULE_ID, scheduleId)
            startActivity(intent)
        } catch (ex: Exception) {
            Toast.makeText(this, "Error opening edit: ${ex.localizedMessage}", Toast.LENGTH_LONG).show()
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
        val notesText = if (schedule.notes.isNullOrBlank()) "No notes available" else schedule.notes
        findViewById<TextView>(R.id.tvNotes).text = notesText

        // Recent activities
        populateRecentActivities(schedule.progress ?: emptyList())

        // Calculate and display progress
        calculateAndDisplayProgress(schedule.progress ?: emptyList(), schedule.status)

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

    private fun toggleNotesEditMode() {
        isEditingNotes = !isEditingNotes
        
        val displayLayout = findViewById<LinearLayout>(R.id.layoutNotesDisplay)
        val editLayout = findViewById<LinearLayout>(R.id.layoutNotesEdit)
        val editButton = findViewById<TextView>(R.id.btnEditNotes)
        val notesEditText = findViewById<EditText>(R.id.etNotesEdit)
        val notesTextView = findViewById<TextView>(R.id.tvNotes)
        
        if (isEditingNotes) {
            // Switch to edit mode
            displayLayout.visibility = View.GONE
            editLayout.visibility = View.VISIBLE
            editButton.text = "👁️ View"
            
            // Copy current notes to edit field
            notesEditText.setText(notesTextView.text.toString())
            notesEditText.requestFocus()
            
            // Show keyboard
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(notesEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        } else {
            // Switch to view mode
            displayLayout.visibility = View.VISIBLE
            editLayout.visibility = View.GONE
            editButton.text = "✏️ Edit"
            
            // Hide keyboard
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(notesEditText.windowToken, 0)
        }
    }

    private fun saveNotes() {
        val notesEditText = findViewById<EditText>(R.id.etNotesEdit)
        val newNotes = notesEditText.text.toString().trim()
        
        lifecycleScope.launch {
            try {
                // Show loading state
                findViewById<TextView>(R.id.btnSaveNotes).text = "Saving..."
                findViewById<TextView>(R.id.btnSaveNotes).isEnabled = false
                
                val updateDto = UpdateScheduleDto(notes = newNotes.ifEmpty { null })
                val response = Network.api.updateSchedule(scheduleId, updateDto)
                
                if (response.isSuccessful) {
                    // Update the display text
                    val notesTextView = findViewById<TextView>(R.id.tvNotes)
                    notesTextView.text = if (newNotes.isEmpty()) "No notes available" else newNotes
                    
                    // Exit edit mode
                    isEditingNotes = false
                    toggleNotesEditMode()
                    
                    Toast.makeText(this@ScheduleDetailActivity, "Notes saved successfully", Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@ScheduleDetailActivity, "Failed to save notes: ${response.code()}\n$errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ScheduleDetailActivity, "Error saving notes: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                // Restore button state
                findViewById<TextView>(R.id.btnSaveNotes).text = "Save"
                findViewById<TextView>(R.id.btnSaveNotes).isEnabled = true
            }
        }
    }

    private fun cancelNotesEdit() {
        // Simply exit edit mode without saving
        isEditingNotes = false
        toggleNotesEditMode()
    }

    private fun showOptionsMenu() {
        val menuView = LayoutInflater.from(this).inflate(R.layout.popup_schedule_options, null)
        val popupWindow = PopupWindow(menuView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        
        // Configure popup
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true
        popupWindow.setBackgroundDrawable(resources.getDrawable(android.R.color.transparent, null))
        
        // Set click listeners
        menuView.findViewById<TextView>(R.id.menuEditSchedule).setOnClickListener {
            popupWindow.dismiss()
            openEditSchedule()
        }
        
        menuView.findViewById<TextView>(R.id.menuDeleteSchedule).setOnClickListener {
            popupWindow.dismiss()
            showDeleteConfirmation()
        }
        
        // Show popup anchored to the menu button
        val anchor = findViewById<TextView>(R.id.btnMenu)
        popupWindow.showAsDropDown(anchor, -200, 0, Gravity.END)
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Schedule")
            .setMessage("Are you sure you want to delete this schedule? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteSchedule()
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(true)
            .show()
    }

    private fun deleteSchedule() {
        lifecycleScope.launch {
            try {
                val response = Network.api.deleteSchedule(scheduleId)
                if (response.isSuccessful) {
                    Toast.makeText(this@ScheduleDetailActivity, "Schedule deleted successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@ScheduleDetailActivity, "Failed to delete schedule: ${response.code()}\n$errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ScheduleDetailActivity, "Error deleting schedule: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun calculateAndDisplayProgress(progressList: List<ProgressResponseDto>, scheduleStatus: String?) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val progressPercentage = findViewById<TextView>(R.id.tvProgressPercentage)
        
        val percentage = when {
            progressList.isNotEmpty() -> {
                // Calculate based on historical progress data
                val totalActivities = progressList.size
                val completedActivities = progressList.count { it.is_completed }
                if (totalActivities > 0) {
                    (completedActivities * 100) / totalActivities
                } else {
                    0
                }
            }
            scheduleStatus != null -> {
                // Fallback to schedule status if no progress data
                when (scheduleStatus.lowercase()) {
                    "completed" -> 100
                    "skipped" -> 0
                    else -> 0
                }
            }
            else -> 0
        }
        
        // Update UI
        progressBar.progress = percentage
        progressPercentage.text = "$percentage%"
        
        // Update progress bar color based on percentage
        updateProgressBarColor(progressBar, percentage)
    }
    
    private fun updateProgressBarColor(progressBar: ProgressBar, percentage: Int) {
        try {
            val progressDrawable = progressBar.progressDrawable
            if (progressDrawable is android.graphics.drawable.LayerDrawable) {
                val progressLayer = progressDrawable.findDrawableByLayerId(android.R.id.progress)
                if (progressLayer is android.graphics.drawable.ClipDrawable) {
                    val shape = progressLayer.drawable as? android.graphics.drawable.GradientDrawable
                    shape?.setColor(getProgressColor(percentage))
                }
            }
        } catch (e: Exception) {
            // Fallback: just use the default color if drawable manipulation fails
        }
    }
    
    private fun getProgressColor(percentage: Int): Int {
        return when {
            percentage >= 80 -> resources.getColor(R.color.accent_green, null)
            percentage >= 50 -> resources.getColor(R.color.accent_cyan, null)
            percentage >= 25 -> resources.getColor(R.color.accent_purple_start, null)
            else -> resources.getColor(R.color.text_muted, null)
        }
    }
}
