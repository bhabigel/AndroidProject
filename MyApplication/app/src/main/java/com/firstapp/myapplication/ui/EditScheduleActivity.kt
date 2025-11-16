package com.firstapp.myapplication.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firstapp.myapplication.R
import com.firstapp.myapplication.models.ScheduleResponseDto
import com.firstapp.myapplication.models.UpdateScheduleDto
import com.firstapp.myapplication.network.Network
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class EditScheduleActivity : AppCompatActivity() {
    companion object { const val EXTRA_SCHEDULE_ID = "extra_schedule_id" }

    private var scheduleId: Int = -1
    private var currentSchedule: ScheduleResponseDto? = null
    private var startTimeHour: Int = 13
    private var startTimeMinute: Int = 30
    private var endTimeHour: Int = 16
    private var endTimeMinute: Int = 30
    private var participantIds: MutableList<Int> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_schedule)

        scheduleId = intent?.getIntExtra(EXTRA_SCHEDULE_ID, -1) ?: -1

        if (scheduleId == -1) {
            Toast.makeText(this, "No schedule id", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        loadSchedule()
    }

    private fun setupViews() {
        // Back button
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Save button
        findViewById<TextView>(R.id.btnSave).setOnClickListener {
            saveSchedule()
        }

        // Time pickers
        findViewById<TextView>(R.id.btnStartTime).setOnClickListener {
            showTimePicker(true)
        }

        findViewById<TextView>(R.id.btnEndTime).setOnClickListener {
            showTimePicker(false)
        }

        // Add partner button
        findViewById<TextView>(R.id.btnAddPartner).setOnClickListener {
            showAddPartnerDialog()
        }

        // Delete button
        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            deleteSchedule()
        }
    }

    private fun loadSchedule() {
        lifecycleScope.launch {
            try {
                val response = Network.api.getScheduleById(scheduleId)
                if (response.isSuccessful) {
                    val schedule = response.body()
                    if (schedule != null) {
                        currentSchedule = schedule
                        populateFields(schedule)
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

    private fun populateFields(schedule: ScheduleResponseDto) {
        // Schedule name
        val habitName = schedule.habit?.name ?: schedule.notes ?: "Schedule #${schedule.id}"
        findViewById<TextView>(R.id.tvScheduleName).text = habitName

        // Parse and set start time
        parseTime(schedule.start_time, true)
        updateTimeDisplay(true)

        // Parse and set end time
        schedule.end_time?.let {
            parseTime(it, false)
            updateTimeDisplay(false)
        }

        // Duration
        val duration = schedule.duration_minutes ?: calculateDurationFromTimes()
        findViewById<EditText>(R.id.etDuration).setText(duration.toString())

        // Status
        val status = schedule.status ?: "Planned"
        val radioGroup = findViewById<RadioGroup>(R.id.rgStatus)
        when (status.lowercase()) {
            "completed" -> radioGroup.check(R.id.rbCompleted)
            "skipped" -> radioGroup.check(R.id.rbSkipped)
            else -> radioGroup.check(R.id.rbPlanned)
        }

        // Partners
        participantIds = schedule.participantIds?.toMutableList() ?: mutableListOf()
        updatePartnersDisplay()

        // Notes
        findViewById<EditText>(R.id.etNotes).setText(schedule.notes ?: "")
    }

    private fun parseTime(timeStr: String?, isStartTime: Boolean) {
        try {
            timeStr?.let { time ->
                if (time.contains('T')) {
                    val timePart = time.substringAfter('T').substringBefore('+').substringBefore('Z')
                    val parts = timePart.split(':')
                    if (parts.size >= 2) {
                        val hour = parts[0].toInt()
                        val minute = parts[1].toInt()
                        if (isStartTime) {
                            startTimeHour = hour
                            startTimeMinute = minute
                        } else {
                            endTimeHour = hour
                            endTimeMinute = minute
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Use default times if parsing fails
        }
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val hour = if (isStartTime) startTimeHour else endTimeHour
        val minute = if (isStartTime) startTimeMinute else endTimeMinute

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            if (isStartTime) {
                startTimeHour = selectedHour
                startTimeMinute = selectedMinute
            } else {
                endTimeHour = selectedHour
                endTimeMinute = selectedMinute
            }
            updateTimeDisplay(isStartTime)
            updateDurationFromTimes()
        }, hour, minute, false).show()
    }

    private fun updateTimeDisplay(isStartTime: Boolean) {
        val hour = if (isStartTime) startTimeHour else endTimeHour
        val minute = if (isStartTime) startTimeMinute else endTimeMinute
        
        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val amPm = if (hour >= 12) "PM" else "AM"
        val timeStr = String.format("%d:%02d %s", displayHour, minute, amPm)
        
        if (isStartTime) {
            findViewById<TextView>(R.id.btnStartTime).text = timeStr
        } else {
            findViewById<TextView>(R.id.btnEndTime).text = timeStr
        }
    }

    private fun updateDurationFromTimes() {
        val duration = calculateDurationFromTimes()
        findViewById<EditText>(R.id.etDuration).setText(duration.toString())
    }

    private fun calculateDurationFromTimes(): Int {
        val startMinutes = startTimeHour * 60 + startTimeMinute
        val endMinutes = endTimeHour * 60 + endTimeMinute
        return if (endMinutes > startMinutes) endMinutes - startMinutes else 0
    }

    private fun showAddPartnerDialog() {
        val editText = EditText(this).apply {
            hint = "Partner ID"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Add Partner")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val partnerIdStr = editText.text.toString().trim()
                if (partnerIdStr.isNotEmpty()) {
                    try {
                        val partnerId = partnerIdStr.toInt()
                        if (!participantIds.contains(partnerId)) {
                            participantIds.add(partnerId)
                            updatePartnersDisplay()
                        }
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this, "Invalid partner ID", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePartnersDisplay() {
        val container = findViewById<LinearLayout>(R.id.llPartners)
        container.removeAllViews()

        if (participantIds.isEmpty()) {
            val noPartnersView = TextView(this).apply {
                text = "No partners added"
                textSize = 14f
                setTextColor(resources.getColor(R.color.text_muted, null))
                setPadding(0, dpToPx(8), 0, dpToPx(8))
            }
            container.addView(noPartnersView)
            return
        }

        participantIds.forEach { partnerId ->
            addPartnerItem(container, partnerId)
        }
    }

    private fun addPartnerItem(container: LinearLayout, partnerId: Int) {
        val partnerView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dpToPx(8), 0, dpToPx(8))
        }

        val partnerText = TextView(this).apply {
            text = "Partner #$partnerId"
            textSize = 14f
            setTextColor(resources.getColor(R.color.text_primary, null))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val removeButton = TextView(this).apply {
            text = "Remove"
            textSize = 12f
            setTextColor(resources.getColor(R.color.text_muted, null))
            background = resources.getDrawable(R.drawable.bg_input, null)
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            setOnClickListener {
                participantIds.remove(partnerId)
                updatePartnersDisplay()
            }
        }

        partnerView.addView(partnerText)
        partnerView.addView(removeButton)
        container.addView(partnerView)
    }

    private fun saveSchedule() {
        try {
            val durationStr = findViewById<EditText>(R.id.etDuration).text.toString().trim()
            val duration = if (durationStr.isNotEmpty()) durationStr.toInt() else calculateDurationFromTimes()

            val radioGroup = findViewById<RadioGroup>(R.id.rgStatus)
            val status = when (radioGroup.checkedRadioButtonId) {
                R.id.rbCompleted -> "Completed"
                R.id.rbSkipped -> "Skipped"
                else -> "Planned"
            }

            val notes = findViewById<EditText>(R.id.etNotes).text.toString().trim()

            // Create ISO datetime strings
            val startTime = formatTimeToISO(startTimeHour, startTimeMinute)
            val endTime = formatTimeToISO(endTimeHour, endTimeMinute)

            val updateDto = UpdateScheduleDto(
                start_time = startTime,
                end_time = endTime,
                duration_minutes = duration,
                status = status,
                participantIds = if (participantIds.isNotEmpty()) participantIds else null,
                notes = if (notes.isNotEmpty()) notes else null
            )

            lifecycleScope.launch {
                try {
                    val response = Network.api.updateSchedule(scheduleId, updateDto)
                    if (response.isSuccessful) {
                        Toast.makeText(this@EditScheduleActivity, "Schedule updated successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(this@EditScheduleActivity, "Failed to update: ${response.code()}\n$errorBody", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@EditScheduleActivity, "Error updating schedule: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error preparing update: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun formatTimeToISO(hour: Int, minute: Int): String {
        val today = LocalDateTime.now().toLocalDate()
        val dateTime = today.atTime(hour, minute)
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    private fun deleteSchedule() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Schedule")
            .setMessage("Are you sure you want to delete this schedule? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = Network.api.deleteSchedule(scheduleId)
                        if (response.isSuccessful) {
                            Toast.makeText(this@EditScheduleActivity, "Schedule deleted", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@EditScheduleActivity, "Failed to delete: ${response.code()}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@EditScheduleActivity, "Error deleting: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}