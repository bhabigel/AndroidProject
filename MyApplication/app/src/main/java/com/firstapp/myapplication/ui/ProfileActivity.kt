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

        val tvName = findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnAddNewHabit = findViewById<Button>(R.id.btnAddNewHabit)
        val llHabitsList = findViewById<LinearLayout>(R.id.llHabitsList)

        // Bottom navigation
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navAssistant).setOnClickListener {
            // Placeholder
        }

        // Load profile
        lifecycleScope.launch {
            try {
                val resp = Network.api.getProfile()
                if (resp.isSuccessful) {
                    val body = resp.body()
                    tvName.text = body?.get("username")?.toString() ?: "User"
                    tvEmail.text = body?.get("email")?.toString() ?: "email@example.com"
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

        // Load habits
        loadHabits(llHabitsList)

        btnAddNewHabit.setOnClickListener {
            startActivity(Intent(this, AddHabitActivity::class.java))
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

    private fun loadHabits(container: LinearLayout) {
        lifecycleScope.launch {
            try {
                val resp = Network.api.listHabits()
                if (resp.isSuccessful) {
                    val habits = resp.body() ?: emptyList()
                    container.removeAllViews()
                    
                    if (habits.isEmpty()) {
                        // Add sample habits for demo
                        addHabitItem(container, "Drink rum", 84, "#6366F1")
                        addHabitItem(container, "Follow treasure map", 100, "#10B981")
                        addHabitItem(container, "Run from Kraken", 0, "#8B5CF6")
                        addHabitItem(container, "Walk the plank", 0, "#8B5CF6")
                        addHabitItem(container, "Journal adventures", 0, "#8B5CF6")
                    } else {
                        for (habit in habits) {
                            val name = habit.name
                            val progress = 50 // Calculate from actual data
                            addHabitItem(container, name, progress, "#6366F1")
                        }
                    }
                } else {
                    // Add sample habits
                    container.removeAllViews()
                    addHabitItem(container, "Drink rum", 84, "#6366F1")
                    addHabitItem(container, "Follow treasure map", 100, "#10B981")
                    addHabitItem(container, "Run from Kraken", 0, "#8B5CF6")
                    addHabitItem(container, "Walk the plank", 0, "#8B5CF6")
                    addHabitItem(container, "Journal adventures", 0, "#8B5CF6")
                }
            } catch (e: Exception) {
                Log.e(TAG, "habits fetch", e)
                // Add sample habits
                container.removeAllViews()
                addHabitItem(container, "Drink rum", 84, "#6366F1")
                addHabitItem(container, "Follow treasure map", 100, "#10B981")
                addHabitItem(container, "Run from Kraken", 0, "#8B5CF6")
                addHabitItem(container, "Walk the plank", 0, "#8B5CF6")
                addHabitItem(container, "Journal adventures", 0, "#8B5CF6")
            }
        }
    }

    private fun addHabitItem(container: LinearLayout, name: String, progress: Int, iconColor: String) {
        // Main habit row
        val habitRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(12)
            }
            gravity = Gravity.CENTER_VERTICAL
        }

        // Icon (colored square)
        val icon = android.view.View(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor(iconColor))
            layoutParams = LinearLayout.LayoutParams(dpToPx(6), dpToPx(32)).apply {
                rightMargin = dpToPx(12)
            }
        }
        habitRow.addView(icon)

        // Text + Progress container
        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val habitName = TextView(this).apply {
            text = name
            textSize = 15f
            setTextColor(resources.getColor(R.color.text_primary, null))
        }
        textContainer.addView(habitName)

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(6)
            ).apply {
                topMargin = dpToPx(8)
            }
            max = 100
            this.progress = progress
            progressDrawable = resources.getDrawable(R.drawable.bg_progress_bar, null)
        }
        textContainer.addView(progressBar)

        habitRow.addView(textContainer)

        // Percentage text
        val percentText = TextView(this).apply {
            text = "$progress%"
            textSize = 14f
            val color = if (progress == 100) R.color.accent_green else R.color.accent_cyan
            setTextColor(resources.getColor(color, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(12)
            }
        }
        habitRow.addView(percentText)

        container.addView(habitRow)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
