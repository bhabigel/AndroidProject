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

class HomeActivity : AppCompatActivity() {
    companion object { private const val TAG = "HomeActivity" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        AuthManager.init(applicationContext)

        // Wire up FAB and navigation
        val btnFAB = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnAddFAB)
        btnFAB.setOnClickListener {
            startActivity(Intent(this, AddHabitActivity::class.java))
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

    private fun fetchSchedules() {
        val container = findViewById<LinearLayout>(R.id.llSchedules)
        lifecycleScope.launch {
            try {
                val resp = Network.api.getSchedulesByDay(null)
                if (resp.isSuccessful) {
                    val list = resp.body() ?: emptyList()
                    container.removeAllViews()
                    
                    // Group by time of day
                    val morning = list.filter { it.start_time?.startsWith("0") == true || it.start_time?.startsWith("1") == true }
                    val afternoon = list.filter { it.start_time?.startsWith("1") == true || it.start_time?.startsWith("2") == true }
                    val night = list.filter { it.start_time?.startsWith("1") == true || it.start_time?.startsWith("2") == true }
                    
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
}
