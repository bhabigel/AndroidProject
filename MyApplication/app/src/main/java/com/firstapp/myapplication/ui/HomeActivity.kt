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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HomeActivity : AppCompatActivity() {
    companion object { private const val TAG = "HomeActivity" }
    
    private var currentDate: LocalDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        AuthManager.init(applicationContext)

        // Setup date navigation
        setupDateNavigation()
        
        // Update date display
        updateDateDisplay()

        // Wire up FAB and navigation
        val btnFAB = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnAddFAB)
        btnFAB.setOnClickListener {
            showAddMenu()
        }

        // Bottom navigation
        findViewById<LinearLayout>(R.id.navAssistant).setOnClickListener {
            // Navigate to assistant screen (placeholder)
        }
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        fetchSchedules()
    }

    override fun onResume() {
        super.onResume()
        // refresh list when returning from create screens
        fetchSchedules()
    }

    private fun setupDateNavigation() {
        val btnPrevDay = findViewById<TextView>(R.id.btnPrevDay)
        val btnNextDay = findViewById<TextView>(R.id.btnNextDay)
        
        btnPrevDay.setOnClickListener {
            currentDate = currentDate.minusDays(1)
            updateDateDisplay()
            fetchSchedules()
        }
        
        btnNextDay.setOnClickListener {
            currentDate = currentDate.plusDays(1)
            updateDateDisplay()
            fetchSchedules()
        }
    }

    private fun updateDateDisplay() {
        val dateText = findViewById<TextView>(R.id.tvDate)
        val titleText = findViewById<TextView>(R.id.tvTodayTitle)
        
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
        dateText.text = currentDate.format(formatter)
        
        // Update title based on selected date
        val today = LocalDate.now()
        titleText.text = when {
            currentDate.isEqual(today) -> "Progress"
            currentDate.isEqual(today.minusDays(1)) -> "Yesterday" 
            currentDate.isEqual(today.plusDays(1)) -> "Tomorrow"
            currentDate.isBefore(today) -> "Past day"
            else -> "Future day"
        }
    }

    private fun fetchSchedules() {
        val container = findViewById<LinearLayout>(R.id.llSchedules)
        lifecycleScope.launch {
            try {
                // Format date for API call (YYYY-MM-DD)
                val dateStr = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val resp = Network.api.getSchedulesByDay(dateStr)
                
                if (resp.isSuccessful) {
                    val list = resp.body() ?: emptyList()
                    container.removeAllViews()
                    
                    if (list.isEmpty()) {
                        // Show empty state
                        val emptyView = TextView(this@HomeActivity).apply {
                            text = "No schedules for this day"
                            textSize = 16f
                            setTextColor(resources.getColor(R.color.text_secondary, null))
                            gravity = android.view.Gravity.CENTER
                            setPadding(0, dpToPx(32), 0, dpToPx(32))
                        }
                        container.addView(emptyView)
                        return@launch
                    }
                    
                    // Group by time of day with better hour detection
                    val morning = list.filter { 
                        val hour = extractHour(it.start_time)
                        hour in 0..11
                    }
                    val afternoon = list.filter { 
                        val hour = extractHour(it.start_time)
                        hour in 12..17
                    }
                    val night = list.filter { 
                        val hour = extractHour(it.start_time)
                        hour in 18..23
                    }
                    
                    if (morning.isNotEmpty()) {
                        addSectionHeader(container, "🌞 Morning")
                        morning.forEach { addScheduleCard(container, it) }
                    }
                    if (afternoon.isNotEmpty()) {
                        addSectionHeader(container, "☀️ Afternoon")
                        afternoon.forEach { addScheduleCard(container, it) }
                    }
                    if (night.isNotEmpty()) {
                        addSectionHeader(container, "🌙 Night")
                        night.forEach { addScheduleCard(container, it) }
                    }
                } else {
                    Log.w(TAG, "Failed to fetch schedules: ${resp.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetch schedules", e)
            }
        }
    }

    private fun extractHour(timeStr: String?): Int {
        return try {
            if (timeStr?.contains('T') == true) {
                val timePart = timeStr.substringAfter('T').substringBefore('+').substringBefore('Z')
                timePart.split(':')[0].toInt()
            } else if (timeStr?.contains(':') == true) {
                timeStr.split(':')[0].toInt()
            } else {
                12 // Default to noon if parsing fails
            }
        } catch (e: Exception) {
            12
        }
    }
    
    private fun addSectionHeader(container: LinearLayout, title: String) {
        val tv = TextView(this).apply {
            text = title
            textSize = 13f
            setTextColor(resources.getColor(R.color.text_secondary, null))
            setPadding(0, dpToPx(18), 0, dpToPx(8))
        }
        container.addView(tv)
    }
    
    private fun addScheduleCard(container: LinearLayout, schedule: Any) {
        // Create card layout programmatically (simplified version)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(72)
            ).apply {
                topMargin = dpToPx(8)
            }
        }
        
        // Add icon placeholder
        val icon = android.view.View(this).apply {
            setBackgroundColor(resources.getColor(R.color.input_stroke, null))
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
        }
        card.addView(icon)
        
        // Add text content
        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                leftMargin = dpToPx(12)
            }
        }
        
        val titleTv = TextView(this).apply {
            text = "Schedule item"
            textSize = 14f
            setTextColor(resources.getColor(R.color.text_primary, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val timeTv = TextView(this).apply {
            text = "08:00"
            textSize = 12f
            setTextColor(resources.getColor(R.color.text_secondary, null))
            setPadding(0, dpToPx(6), 0, 0)
        }
        textContainer.addView(titleTv)
        textContainer.addView(timeTv)
        card.addView(textContainer)
        
        // Add check circle
        val check = android.view.View(this).apply {
            setBackgroundResource(R.drawable.bg_unchecked_circle)
            layoutParams = LinearLayout.LayoutParams(dpToPx(28), dpToPx(28))
        }
        card.addView(check)
        
        container.addView(card)
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
    
    private fun showAddMenu() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_menu, null)
        
        view.findViewById<Button>(R.id.btnAddSchedule).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, CreateScheduleActivity::class.java))
        }
        
        view.findViewById<Button>(R.id.btnAddHabit).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, AddHabitActivity::class.java))
        }
        
        dialog.setContentView(view)
        dialog.show()
    }
}
