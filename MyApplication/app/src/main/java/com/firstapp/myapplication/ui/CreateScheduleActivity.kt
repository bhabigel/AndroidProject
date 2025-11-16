package com.firstapp.myapplication.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firstapp.myapplication.R
import com.firstapp.myapplication.models.HabitResponseDto
import com.firstapp.myapplication.models.ScheduleRequestDto
import com.firstapp.myapplication.network.Network
import com.google.android.material.chip.Chip
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class CreateScheduleActivity : AppCompatActivity() {
    private var selectedHabitId: Int? = null
    private var habits: List<HabitResponseDto> = emptyList()
    private var selectedDaysOfWeek: MutableSet<Int> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_schedule)

        setupViews()
        loadHabits()
    }

    private fun setupViews() {
        // Find views
        val spHabitName = findViewById<Spinner>(R.id.spHabitName)
        val btnCreateNewHabit = findViewById<Button>(R.id.btnCreateNewHabit)
        val etStartTime = findViewById<EditText>(R.id.etStartTime)
        val etDuration = findViewById<EditText>(R.id.etDuration)
        val rgRepeatPattern = findViewById<RadioGroup>(R.id.rgRepeatPattern)
        val layoutCustomDays = findViewById<View>(R.id.layoutCustomDays)
        val chipGroupDays = findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupDays)
        val etNumberOfWeeks = findViewById<EditText>(R.id.etNumberOfWeeks)
        val btnAddPartner = findViewById<Button>(R.id.btnAddPartner)
        val chipGroupPartners = findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupPartners)
        val etNotes = findViewById<EditText>(R.id.etNotes)
        val btnCancel = findViewById<Button>(R.id.btnCancel)
        val btnCreate = findViewById<Button>(R.id.btnCreate)

        // Setup time picker
        etStartTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                val time = String.format("%02d:%02d", hour, minute)
                etStartTime.setText(time)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

    // Ensure input text is visible on dark background
    val textColor = ContextCompat.getColor(this, R.color.text_primary)
    etStartTime.setTextColor(textColor)
    etDuration.setTextColor(textColor)
    etNumberOfWeeks.setTextColor(textColor)
    etNotes.setTextColor(textColor)

        // Setup repeat pattern selection
        rgRepeatPattern.setOnCheckedChangeListener { _, checkedId ->
            layoutCustomDays.visibility = if (checkedId == R.id.rbCustom) View.VISIBLE else View.GONE
        }

        // Setup weekday chips
        val dayChips = listOf(
            findViewById<Chip>(R.id.chipMonday),
            findViewById<Chip>(R.id.chipTuesday),
            findViewById<Chip>(R.id.chipWednesday),
            findViewById<Chip>(R.id.chipThursday),
            findViewById<Chip>(R.id.chipFriday),
            findViewById<Chip>(R.id.chipSaturday),
            findViewById<Chip>(R.id.chipSunday)
        )

        dayChips.forEachIndexed { index, chip ->
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedDaysOfWeek.add(index + 1)
                } else {
                    selectedDaysOfWeek.remove(index + 1)
                }
            }
        }

        // Create new habit button
        btnCreateNewHabit.setOnClickListener {
            // TODO: Navigate to Create Habit screen
        }

        // Add partner button
        btnAddPartner.setOnClickListener {
            // TODO: Show partner selection dialog
        }

        // Cancel button
        btnCancel.setOnClickListener {
            finish()
        }

        // Create button
        btnCreate.setOnClickListener {
            createSchedule(
                selectedHabitId,
                etStartTime.text.toString(),
                etDuration.text.toString(),
                rgRepeatPattern.checkedRadioButtonId,
                etNumberOfWeeks.text.toString(),
                etNotes.text.toString()
            )
        }
    }

    private fun loadHabits() {
        val spHabitName = findViewById<Spinner>(R.id.spHabitName)

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { Network.api.listHabits() }
                if (response.isSuccessful) {
                    habits = response.body() ?: emptyList()
                    val habitNames = listOf("Select a habit") + habits.map { it.name }
                        val adapter = object : ArrayAdapter<String>(
                            this@CreateScheduleActivity,
                            android.R.layout.simple_spinner_item,
                            habitNames
                        ) {
                            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                                val v = super.getView(position, convertView, parent) as TextView
                                v.setTextColor(ContextCompat.getColor(this@CreateScheduleActivity, R.color.text_primary))
                                return v
                            }

                            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                                val v = super.getDropDownView(position, convertView, parent) as TextView
                                v.setTextColor(ContextCompat.getColor(this@CreateScheduleActivity, R.color.text_primary))
                                return v
                            }
                        }
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spHabitName.adapter = adapter
                    
                    spHabitName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            selectedHabitId = if (position > 0) habits[position - 1].id else null
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) { }
                    }
                } else {
                    Toast.makeText(this@CreateScheduleActivity, 
                        "Failed to load habits: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CreateScheduleActivity, 
                    "Error loading habits: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createSchedule(
        habitId: Int?,
        startTime: String,
        duration: String,
        repeatPatternId: Int,
        numberOfWeeks: String,
        notes: String
    ) {
        if (habitId == null) {
            Toast.makeText(this, "Please select a habit", Toast.LENGTH_SHORT).show()
            return
        }

        if (startTime.isEmpty()) {
            Toast.makeText(this, "Please select a start time", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // Build offset-aware ISO 8601 datetimes (include timezone offset)
                val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
                var startTimeStr: String
                var dateTimeForDateField: String
                try {
                    val parts = startTime.split(":")
                    val hour = parts[0].toInt()
                    val minute = parts[1].toInt()
                    val todayDate = java.time.LocalDate.now()
                    val zone = java.time.ZoneId.systemDefault()
                    val zdt = java.time.ZonedDateTime.of(
                        todayDate.year,
                        todayDate.monthValue,
                        todayDate.dayOfMonth,
                        hour,
                        minute,
                        0,
                        0,
                        zone
                    )
                    startTimeStr = zdt.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    // date field as start of day with offset
                    val dayStart = zdt.toLocalDate().atStartOfDay(zone)
                    dateTimeForDateField = dayStart.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                } catch (e: Exception) {
                    // fallback to previous local format if parsing fails
                    val today = LocalDateTime.now()
                    val dateStr = today.format(dateFormatter)
                    startTimeStr = "$dateStr" + "T" + "$startTime:00"
                    dateTimeForDateField = "$dateStr" + "T00:00:00"
                }

                val repeatPattern = when (repeatPatternId) {
                    R.id.rbNone -> "none"
                    R.id.rbDaily -> "daily"
                    R.id.rbWeekdays -> "weekdays"
                    R.id.rbWeekends -> "weekends"
                    R.id.rbCustom -> "custom"
                    else -> "none"
                }

                // Build and send request per-type (custom vs recurring) to match backend expectations
                val gson = Gson()

                val response = when (repeatPattern) {
                    "custom" -> {
                        if (selectedDaysOfWeek.isEmpty()) {
                            Toast.makeText(this@CreateScheduleActivity, 
                                "Please select at least one day", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val weeks = if (numberOfWeeks.isNotEmpty()) numberOfWeeks.toInt() else 4
                        val weekdayRequest = ScheduleRequestDto(
                            habitId = habitId,
                            start_time = startTimeStr,
                            duration_minutes = if (duration.isNotEmpty()) duration.toInt() else null,
                            is_custom = true,
                            daysOfWeek = selectedDaysOfWeek.toList().sorted(),
                            numberOfWeeks = weeks,
                            notes = if (notes.isNotEmpty()) notes else null
                        )

                        // log exact JSON we will send
                        try { Log.d("CreateSchedule", "Outgoing JSON: ${gson.toJson(weekdayRequest)}") } catch (_: Exception) {}

                        withContext(Dispatchers.IO) {
                            Network.api.createWeekdayRecurringSchedule(weekdayRequest)
                        }
                    }
                    "none" -> {
                        // Custom single schedule (manual time) — do not include repeatPattern field
                        val customRequest = ScheduleRequestDto(
                            habitId = habitId,
                            date = dateTimeForDateField,
                            start_time = startTimeStr,
                            duration_minutes = if (duration.isNotEmpty()) duration.toInt() else null,
                            is_custom = true,
                            notes = if (notes.isNotEmpty()) notes else null
                        )
                        try { Log.d("CreateSchedule", "Outgoing JSON: ${gson.toJson(customRequest)}") } catch (_: Exception) {}
                        withContext(Dispatchers.IO) {
                            Network.api.createCustomSchedule(customRequest)
                        }
                    }
                    else -> {
                        val recurringRequest = ScheduleRequestDto(
                            habitId = habitId,
                            start_time = startTimeStr,
                            duration_minutes = if (duration.isNotEmpty()) duration.toInt() else null,
                            is_custom = true,
                            repeatPattern = repeatPattern,
                            notes = if (notes.isNotEmpty()) notes else null
                        )
                        try { Log.d("CreateSchedule", "Outgoing JSON: ${gson.toJson(recurringRequest)}") } catch (_: Exception) {}
                        withContext(Dispatchers.IO) {
                            Network.api.createRecurringSchedule(recurringRequest)
                        }
                    }
                }

                if (response.isSuccessful) {
                    Toast.makeText(this@CreateScheduleActivity,
                        "Schedule created successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    // try to read error body for better debugging
                    val err = try { response.errorBody()?.string() } catch (e: Exception) { null }
                    Log.e("CreateSchedule", "Create failed code=${response.code()} body=$err")
                    Toast.makeText(this@CreateScheduleActivity,
                        "Failed to create schedule: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CreateScheduleActivity, 
                    "Error creating schedule: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
