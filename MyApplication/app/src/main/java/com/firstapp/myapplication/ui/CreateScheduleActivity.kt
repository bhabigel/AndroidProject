package com.firstapp.myapplication.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firstapp.myapplication.R
import com.firstapp.myapplication.network.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class CreateScheduleActivity : AppCompatActivity() {
    private var selectedHabitId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_schedule)

        val tvSelectedHabit = findViewById<TextView>(R.id.tvSelectedHabit)
        val btnPickHabit = findViewById<Button>(R.id.btnPickHabit)
        val etDate = findViewById<EditText>(R.id.etDate)
        val etStart = findViewById<EditText>(R.id.etStartTime)
        val etNotes = findViewById<EditText>(R.id.etNotes)
        val btn = findViewById<Button>(R.id.btnCreateSchedule)

        var habits: List<Map<String, Any>> = emptyList()

        // load habits
        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { Network.api.listHabits() }
                if (resp.isSuccessful) {
                    habits = resp.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // ignore; list will be empty
            }
        }

        btnPickHabit.setOnClickListener {
            if (habits.isEmpty()) {
                Toast.makeText(this, "No habits available. Create one first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val names = habits.map { it["name"]?.toString() ?: "(no name)" }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Pick habit")
                .setItems(names) { _, which ->
                    val h = habits[which]
                    selectedHabitId = (h["id"] as? Double)?.toInt() ?: (h["id"] as? Int)
                    tvSelectedHabit.text = names[which] + " (id:${selectedHabitId})"
                }
                .show()
        }

        // date picker
        etDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                val mm = (m + 1).toString().padStart(2, '0')
                val dd = d.toString().padStart(2, '0')
                etDate.setText("$y-$mm-$dd")
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // time picker
        etStart.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                val hh = hour.toString().padStart(2, '0')
                val mm = minute.toString().padStart(2, '0')
                etStart.setText("$hh:$mm")
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        btn.setOnClickListener {
            val habitId = selectedHabitId
            val dateText = etDate.text.toString().trim()
            val startText = etStart.text.toString().trim()
            val notes = etNotes.text.toString().trim()

            if (habitId == null || dateText.isEmpty() || startText.isEmpty()) {
                Toast.makeText(this, "Please pick a habit and select date/time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val startIso = "${dateText}T${startText}:00.000Z"
                    val body = mutableMapOf<String, Any>(
                        "habitId" to habitId,
                        "date" to dateText,
                        "start_time" to startIso
                    )
                    if (notes.isNotEmpty()) body["notes"] = notes

                    val resp = withContext(Dispatchers.IO) { Network.api.createCustomSchedule(body) }
                    if (resp.isSuccessful) {
                        Toast.makeText(this@CreateScheduleActivity, "Schedule created", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@CreateScheduleActivity, "Failed: ${resp.code()}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@CreateScheduleActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
