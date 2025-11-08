package com.firstapp.myapplication.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firstapp.myapplication.R
import com.firstapp.myapplication.models.HabitResponseDto
import com.firstapp.myapplication.models.ScheduleRequestDto
import com.firstapp.myapplication.network.Network
import com.google.android.material.chip.Chip
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
                    val adapter = ArrayAdapter(this@CreateScheduleActivity, 
                        android.R.layout.simple_spinner_item, habitNames)
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
                val today = LocalDateTime.now()
                val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
                val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
                val dateStr = today.format(dateFormatter)
                val startTimeStr = "$dateStr ${startTime}:00"

                val repeatPattern = when (repeatPatternId) {
                    R.id.rbNone -> "none"
                    R.id.rbDaily -> "daily"
                    R.id.rbWeekdays -> "weekdays"
                    R.id.rbWeekends -> "weekends"
                    R.id.rbCustom -> "custom"
                    else -> "none"
                }

                val request = ScheduleRequestDto(
                    habitId = habitId,
                    start_time = startTimeStr,
                    duration_minutes = if (duration.isNotEmpty()) duration.toInt() else null,
                    repeatPattern = repeatPattern,
                    notes = if (notes.isNotEmpty()) notes else null
                )

                val response = when (repeatPattern) {
                    "custom" -> {
                        if (selectedDaysOfWeek.isEmpty()) {
                            Toast.makeText(this@CreateScheduleActivity, 
                                "Please select at least one day", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val weeks = if (numberOfWeeks.isNotEmpty()) numberOfWeeks.toInt() else 4
                        withContext(Dispatchers.IO) {
                            Network.api.createWeekdayRecurringSchedule(request.copy(
                                daysOfWeek = selectedDaysOfWeek.toList().sorted(),
                                numberOfWeeks = weeks
                            ))
                        }
                    }
                    "none" -> withContext(Dispatchers.IO) {
                        Network.api.createCustomSchedule(request)
                    }
                    else -> withContext(Dispatchers.IO) {
                        Network.api.createRecurringSchedule(request)
                    }
                }

                if (response.isSuccessful) {
                    Toast.makeText(this@CreateScheduleActivity, 
                        "Schedule created successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
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
