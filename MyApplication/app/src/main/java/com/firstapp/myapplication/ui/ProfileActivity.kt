package com.firstapp.myapplication.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firstapp.myapplication.R
import com.firstapp.myapplication.auth.AuthManager
import com.firstapp.myapplication.network.Network
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {
    companion object { private const val TAG = "ProfileActivity" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        AuthManager.init(applicationContext)

        setupUI()
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
        loadHabits(findViewById(R.id.llHabitsList))
    }

    private fun setupUI() {
        val tvName = findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnAddNewHabit = findViewById<Button>(R.id.btnAddNewHabit)
        val btnEditProfile = findViewById<Button>(R.id.btnEditProfile)
        val llHabitsList = findViewById<LinearLayout>(R.id.llHabitsList)

        // Bottom navigation
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navAssistant).setOnClickListener {
            // Placeholder
        }

        btnAddNewHabit.setOnClickListener {
            startActivity(Intent(this, AddHabitActivity::class.java))
        }

        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        btnLogout.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val resp = Network.api.logout()
                    AuthManager.clear()
                    startActivity(Intent(this@ProfileActivity, LoginActivity::class.java))
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@ProfileActivity, "Logout error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadProfile() {
        val tvName = findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = findViewById<TextView>(R.id.tvProfileEmail)
        
        lifecycleScope.launch {
            try {
                val resp = Network.api.getProfile()
                if (resp.isSuccessful) {
                    val profile = resp.body()
                    tvName.text = profile?.username ?: "User"
                    tvEmail.text = profile?.email ?: "email@example.com"
                } else {
                    tvName.text = "User"
                    tvEmail.text = "email@example.com"
                }
            } catch (e: Exception) {
                Log.e(TAG, "profile fetch", e)
                tvName.text = "User"
                tvEmail.text = "email@example.com"
            }
        }
    }

    private fun loadHabits(container: LinearLayout) {
        lifecycleScope.launch {
            try {
                val resp = Network.api.listHabits()
                if (resp.isSuccessful) {
                    val habits = resp.body() ?: emptyList()
                    container.removeAllViews()
                    
                    if (habits.isEmpty()) {
                        // Show sample habits matching the screenshot design exactly
                        addHabitItem(container, "Drink rum", 84, "#6366F1")
                        addHabitItem(container, "Follow treasure map", 100, "#06B6D4")
                        addHabitItem(container, "Run from Kraken", 0, "#8B5CF6")
                        addHabitItem(container, "Walk the plank", 0, "#8B5CF6")
                        addHabitItem(container, "Journal adventures", 0, "#06B6D4")
                    } else {
                        // First, get all schedules for today
                        val schedulesResp = Network.api.getSchedulesByDay()
                        val allSchedules = if (schedulesResp.isSuccessful) {
                            schedulesResp.body() ?: emptyList()
                        } else {
                            emptyList()
                        }
                        
                        // Display real user habits with calculated progress based on actual schedules
                        for (habit in habits) {
                            val name = habit.name
                            // Calculate real progress using the same logic as ScheduleDetailActivity
                            val progress = calculateProgressForHabit(habit.id, allSchedules)
                            val color = getProgressColor(progress)
                            addHabitItem(container, name, progress, color)
                        }
                    }
                } else {
                    // Show sample habits if API fails
                    container.removeAllViews()
                    addHabitItem(container, "Drink rum", 84, "#6366F1")
                    addHabitItem(container, "Follow treasure map", 100, "#06B6D4")
                    addHabitItem(container, "Run from Kraken", 0, "#8B5CF6")
                    addHabitItem(container, "Walk the plank", 0, "#8B5CF6")
                    addHabitItem(container, "Journal adventures", 0, "#8B5CF6")
                }
            } catch (e: Exception) {
                Log.e(TAG, "habits fetch", e)
                // Show sample habits for demo purposes
                container.removeAllViews()
                addHabitItem(container, "Drink rum", 84, "#6366F1")
                addHabitItem(container, "Follow treasure map", 100, "#06B6D4")
                addHabitItem(container, "Run from Kraken", 0, "#8B5CF6")
                addHabitItem(container, "Walk the plank", 0, "#8B5CF6")
                addHabitItem(container, "Journal adventures", 0, "#8B5CF6")
            }
        }
    }

    private fun calculateProgressForHabit(habitId: Int, allSchedules: List<com.firstapp.myapplication.models.ScheduleResponseDto>): Int {
        try {
            // Filter schedules for this specific habit
            val habitSchedules = allSchedules.filter { it.habit?.id == habitId }
            
            if (habitSchedules.isEmpty()) {
                return 0 // No schedules = 0% progress
            }
            
            var totalProgress = 0
            var scheduleCount = 0
            
            for (schedule in habitSchedules) {
                scheduleCount++
                
                // Use EXACT same logic as ScheduleDetailActivity.calculateAndDisplayProgress
                val progressPercentage = when {
                    schedule.progress?.isNotEmpty() == true -> {
                        // Calculate based on historical progress data
                        val totalActivities = schedule.progress.size
                        val completedActivities = schedule.progress.count { it.is_completed }
                        if (totalActivities > 0) {
                            (completedActivities * 100) / totalActivities
                        } else {
                            0
                        }
                    }
                    schedule.status != null -> {
                        // Fallback to schedule status if no progress data
                        when (schedule.status.lowercase()) {
                            "completed" -> 100
                            "skipped" -> 0
                            "planned" -> 25 // Assuming planned means some progress but not complete
                            else -> 0
                        }
                    }
                    else -> 0
                }
                
                totalProgress += progressPercentage
            }
            
            // Return average progress across all schedules for this habit
            return if (scheduleCount > 0) {
                totalProgress / scheduleCount
            } else {
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating progress for habit $habitId: ${e.localizedMessage}")
            return 0
        }
    }

    private fun getProgressColor(progress: Int): String {
        return when {
            progress == 100 -> "#06B6D4" // Cyan for 100%
            progress >= 80 -> "#6366F1"  // Blue for high progress
            progress >= 40 -> "#8B5CF6"  // Purple for medium progress
            progress > 0 -> "#A855F7"    // Light purple for some progress
            else -> "#6B7280"            // Gray for no progress
        }
    }

    private fun addHabitItem(container: LinearLayout, name: String, progress: Int, iconColor: String) {
        // Main habit row with better styling
        val habitRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(16)
                bottomMargin = dpToPx(4)
            }
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(4), dpToPx(8), dpToPx(4), dpToPx(8))
        }

        // Icon (colored square with rounded corners)
        val icon = android.view.View(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor(iconColor))
            layoutParams = LinearLayout.LayoutParams(dpToPx(4), dpToPx(36)).apply {
                rightMargin = dpToPx(16)
            }
        }
        habitRow.addView(icon)

        // Text + Progress container
        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        // Habit name with better styling
        val habitName = TextView(this).apply {
            text = name
            textSize = 16f
            setTextColor(resources.getColor(R.color.text_primary, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(8)
            }
        }
        textContainer.addView(habitName)

        // Progress bar with custom styling
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(6)
            )
            max = 100
            this.progress = progress
            progressDrawable = resources.getDrawable(R.drawable.bg_progress_bar, null)
        }
        textContainer.addView(progressBar)

        habitRow.addView(textContainer)

        // Percentage text with color based on progress
        val percentText = TextView(this).apply {
            text = "$progress%"
            textSize = 14f
            val color = when {
                progress == 100 -> R.color.accent_cyan
                progress >= 80 -> R.color.accent_cyan  
                progress >= 40 -> R.color.accent_blue
                progress > 0 -> R.color.text_secondary
                else -> R.color.text_muted
            }
            setTextColor(resources.getColor(color, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(16)
            }
            gravity = Gravity.CENTER
            minWidth = dpToPx(45) // Consistent width for alignment
        }
        habitRow.addView(percentText)

        container.addView(habitRow)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
