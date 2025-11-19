package com.firstapp.myapplication.ui

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Spinner
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firstapp.myapplication.R
import com.firstapp.myapplication.network.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class AddHabitActivity : AppCompatActivity() {
    private var selectedCategoryId: Int? = null
    private var selectedTime: String? = null
    private var selectedRepeat: Any? = null // String or Map for custom
    private var selectedPartner: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_habit)

        val spHabitName = findViewById<Spinner>(R.id.spHabitName)
        val etHabitName = findViewById<EditText>(R.id.etHabitName)
        val etHabitDescription = findViewById<EditText>(R.id.etHabitDescription)
        val etGoalAmount = findViewById<EditText>(R.id.etGoalAmount)
        val spGoalUnit = findViewById<Spinner>(R.id.spGoalUnit)
        val tvTimePicker = findViewById<TextView>(R.id.tvTimePicker)
        val btnEveryDay = findViewById<TextView>(R.id.btnEveryDay)
        val btnWeekdays = findViewById<TextView>(R.id.btnWeekdays)
        val btnWeekends = findViewById<TextView>(R.id.btnWeekends)
        val btnCustom = findViewById<TextView>(R.id.btnCustom)
        val tvChoosePartner = findViewById<TextView>(R.id.tvChoosePartner)
        val switchReminders = findViewById<android.widget.Switch>(R.id.switchReminders)

        val btnCreate = findViewById<Button>(R.id.btnCreate)
        val btnCancel = findViewById<Button>(R.id.btnCancel)

        // load categories into spinner and populate emoji grid
        lifecycleScope.launch {
            val categories = try {
                val resp = withContext(Dispatchers.IO) { Network.api.listHabitCategories() }
                if (resp.isSuccessful) resp.body() ?: emptyList() else emptyList()
            } catch (e: Exception) {
                emptyList<com.firstapp.myapplication.models.HabitCategoryResponseDto>()
            }
            val items = mutableListOf("Select category")
            items.addAll(categories.map { it.name })
            val adapter = ArrayAdapter(this@AddHabitActivity, android.R.layout.simple_spinner_item, items)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spHabitName.adapter = adapter

            spHabitName.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedCategoryId = if (position > 0 && position - 1 < categories.size) categories[position - 1].id else null
                    // Always update habit name when category is selected (not just when empty)
                    if (position > 0 && position - 1 < categories.size) {
                        etHabitName.setText(categories[position - 1].name)
                    }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                    selectedCategoryId = null
                }
            })

            // populate emoji grid for categories (emojis that match category names)
            val gl = findViewById<GridLayout>(R.id.glCategoryIcons)
            gl.removeAllViews()
            
            categories.forEachIndexed { idx, cat ->
                // Use position-based emoji assignment to match backend order
                val emojis = listOf("🤸‍♀️", "📚", "💻", "💧", "✍️", "🏃‍♂️", "🏷️")
                val emoji = emojis.getOrNull(idx) ?: "🏷️"
                
                val tv = TextView(this@AddHabitActivity).apply {
                    text = emoji
                    textSize = 28f
                    setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                    setBackgroundResource(R.drawable.bg_card)
                    gravity = Gravity.CENTER
                    setOnClickListener {
                        // mark selection visually
                        for (i in 0 until gl.childCount) {
                            val child = gl.getChildAt(i) as TextView
                            child.setBackgroundResource(R.drawable.bg_card)
                        }
                        it.setBackgroundResource(R.drawable.bg_button_primary)
                        selectedCategoryId = cat.id
                        // Always update habit name when emoji is selected
                        etHabitName.setText(cat.name)
                    }
                }

                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = dpToPx(56)
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
                }
                tv.layoutParams = params
                gl.addView(tv)
            }
        }

        // goal unit spinner
        val units = listOf("times", "minutes")
        val unitAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spGoalUnit.adapter = unitAdapter

        // time picker
        tvTimePicker.setOnClickListener {
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            TimePickerDialog(this, { _, h, m ->
                val timeStr = String.format("%02d:%02d", h, m)
                selectedTime = timeStr
                tvTimePicker.text = timeStr
            }, hour, minute, true).show()
        }

        // repeat buttons
        val repeatButtons = listOf(btnEveryDay, btnWeekdays, btnWeekends, btnCustom)
        fun selectRepeat(btn: TextView, value: String) {
            selectedRepeat = value
            repeatButtons.forEach { it.setBackgroundResource(R.drawable.bg_card) }
            btn.setBackgroundResource(R.drawable.bg_button_primary)
            if (value == "CUSTOM") {
                showCustomRepeatDialog()
            }
        }

        btnEveryDay.setOnClickListener { selectRepeat(btnEveryDay, "EVERY_DAY") }
        btnWeekdays.setOnClickListener { selectRepeat(btnWeekdays, "WEEKDAYS") }
        btnWeekends.setOnClickListener { selectRepeat(btnWeekends, "WEEKENDS") }
        btnCustom.setOnClickListener { selectRepeat(btnCustom, "CUSTOM") }

        // partner picker -> simple input dialog
        tvChoosePartner.setOnClickListener { showPartnerDialog() }

        btnCancel.setOnClickListener { finish() }

    btnCreate.setOnClickListener {
            val amount = etGoalAmount.text.toString().trim()
            val unit = spGoalUnit.selectedItem?.toString() ?: "times"
            val nameInput = etHabitName.text.toString().trim()
            val descInput = etHabitDescription.text.toString().trim()
            val remindersOn = switchReminders.isChecked

            // Backend requires name (string) and categoryId (number) and goal (string).
            if (amount.isEmpty()) {
                Toast.makeText(this, "Goal amount is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (nameInput.isEmpty()) {
                Toast.makeText(this, "Please provide a habit name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedCategoryId == null) {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // assemble body (same as before) and show confirmation dialog with JSON so you can inspect
            // Build minimal body according to backend contract: name, categoryId, goal, optional description
            val body = mutableMapOf<String, Any>()
            body["name"] = nameInput
            body["categoryId"] = selectedCategoryId!!

            // goal formatting: make a readable string the backend expects, e.g. "10 times" or "30 minutes"
            val goalStr = when (unit.lowercase()) {
                "minutes" -> "${amount} minutes"
                else -> "${amount} ${unit}"
            }
            body["goal"] = goalStr
            if (descInput.isNotEmpty()) body["description"] = descInput

            // Send immediately when Create is pressed (no confirmation dialog)
            lifecycleScope.launch {
                try {
                    Log.d("AddHabitActivity", "Create body: $body")
                    val resp = withContext(Dispatchers.IO) { Network.api.createHabit(body) }
                    if (resp.isSuccessful) {
                        Toast.makeText(this@AddHabitActivity, "Habit created", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Log.e("AddHabitActivity", "Create failed code=${resp.code()} body=${resp.errorBody()?.string()}")
                        Toast.makeText(this@AddHabitActivity, "Failed: ${resp.code()}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("AddHabitActivity", "Create habit error", e)
                    Toast.makeText(this@AddHabitActivity, "Error: ${e}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showPartnerDialog() {
        val input = EditText(this)
        input.hint = "Partner name (optional)"
        AlertDialog.Builder(this)
            .setTitle("Choose partner")
            .setView(input)
            .setPositiveButton("OK") { _, _ -> selectedPartner = input.text.toString().trim().ifEmpty { null } }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCustomRepeatDialog() {
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(20, 20, 20, 20)

        val npInterval = NumberPicker(this)
        npInterval.minValue = 1
        npInterval.maxValue = 52
        npInterval.value = 1
        container.addView(npInterval)

        val spUnit = Spinner(this)
        val units = listOf("Day(s)", "Week(s)", "Month(s)", "Year(s)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spUnit.adapter = adapter
        container.addView(spUnit)

        // weekday checkboxes (visible if Week(s) selected)
        val weekdaysLayout = LinearLayout(this)
        weekdaysLayout.orientation = LinearLayout.HORIZONTAL
        val checkBoxes = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun").map { label ->
            CheckBox(this).apply { text = label; weekdaysLayout.addView(this) }
        }
        container.addView(weekdaysLayout)

        spUnit.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val sel = units[position]
                if (sel.startsWith("Week")) weekdaysLayout.visibility = View.VISIBLE else weekdaysLayout.visibility = View.GONE
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        AlertDialog.Builder(this)
            .setTitle("Custom repeat")
            .setView(container)
            .setPositiveButton("OK") { _, _ ->
                val interval = npInterval.value
                val unitRaw = spUnit.selectedItem?.toString() ?: "Day(s)"
                val unit = when {
                    unitRaw.startsWith("Week") -> "WEEK"
                    unitRaw.startsWith("Month") -> "MONTH"
                    unitRaw.startsWith("Year") -> "YEAR"
                    else -> "DAY"
                }
                val days = mutableListOf<String>()
                checkBoxes.forEachIndexed { i, cb -> if (cb.isChecked) days.add(listOf("MON","TUE","WED","THU","FRI","SAT","SUN")[i]) }
                val repeatBody = mutableMapOf<String, Any>("type" to "CUSTOM", "every" to interval, "unit" to unit)
                if (unit == "WEEK" && days.isNotEmpty()) repeatBody["days"] = days
                selectedRepeat = repeatBody
                Toast.makeText(this, "Custom repeat set", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
