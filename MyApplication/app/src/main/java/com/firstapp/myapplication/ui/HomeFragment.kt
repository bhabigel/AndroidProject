package com.firstapp.myapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.firstapp.myapplication.R
import com.firstapp.myapplication.auth.AuthManager
import com.firstapp.myapplication.network.Network
import kotlinx.coroutines.launch
import com.firstapp.myapplication.models.ScheduleResponseDto
import com.firstapp.myapplication.ui.CreateProgressActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HomeFragment : Fragment() {
    companion object { private const val TAG = "HomeFragment" }
    
    private var currentDate: LocalDate = LocalDate.now()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AuthManager.init(requireContext())

        // Set current date
        updateDateDisplay(view)
        
        // Setup date navigation
        setupDateNavigation(view)
        
        // Load schedules
        fetchSchedules(view)
    }

    private fun setupDateNavigation(view: View) {
        val btnPrevDay = view.findViewById<TextView>(R.id.btnPrevDay)
        val btnNextDay = view.findViewById<TextView>(R.id.btnNextDay)
        
        btnPrevDay.setOnClickListener {
            currentDate = currentDate.minusDays(1)
            updateDateDisplay(view)
            fetchSchedules(view)
        }
        
        btnNextDay.setOnClickListener {
            currentDate = currentDate.plusDays(1)
            updateDateDisplay(view)
            fetchSchedules(view)
        }
    }

    private fun updateDateDisplay(view: View) {
        val dateText = view.findViewById<TextView>(R.id.tvDate)
        val titleText = view.findViewById<TextView>(R.id.tvTodayTitle)
        
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
        dateText.text = currentDate.format(formatter)
        
        // Update title based on selected date
        val today = LocalDate.now()
        titleText.text = when {
            currentDate.isEqual(today) -> "Today's plan"
            currentDate.isEqual(today.minusDays(1)) -> "Yesterday's plan" 
            currentDate.isEqual(today.plusDays(1)) -> "Tomorrow's plan"
            currentDate.isBefore(today) -> "Past day"
            else -> "Future plan"
        }
    }

    private fun fetchSchedules(root: View) {
        val container = root.findViewById<LinearLayout>(R.id.llSchedules)
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
                        val emptyView = TextView(requireContext()).apply {
                            text = "No schedules for this day"
                            textSize = 16f
                            setTextColor(resources.getColor(R.color.text_secondary, null))
                            gravity = android.view.Gravity.CENTER
                            setPadding(0, dpToPx(32), 0, dpToPx(32))
                        }
                        container.addView(emptyView)
                        return@launch
                    }
                    
                    // Group schedules by time of day
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
        TextView(requireContext()).apply {
            text = title
            textSize = 13f
            setTextColor(resources.getColor(R.color.text_secondary, null))
            setPadding(0, dpToPx(18), 0, dpToPx(8))
            container.addView(this)
        }
    }

    private fun addScheduleCard(container: LinearLayout, schedule: ScheduleResponseDto) {
        // Create card layout matching the expected design
        val layoutInflater = LayoutInflater.from(requireContext())
        val cardView = layoutInflater.inflate(R.layout.item_schedule, container, false)
        
        // Find views in the inflated layout
        val tvHabitName = cardView.findViewById<TextView>(R.id.tvHabitName)
        val tvTime = cardView.findViewById<TextView>(R.id.tvTime)
        val ivHabitIcon = cardView.findViewById<View>(R.id.ivHabitIcon)
        val ivStatus = cardView.findViewById<View>(R.id.ivStatus)
        
        // Set habit name (fallback to notes or generic name)
        tvHabitName.text = schedule.notes ?: "Habit #${schedule.id}"
        
        // Format and set time
        val timeStr = schedule.start_time?.let { time ->
            // Extract time portion from ISO datetime or use as-is
            if (time.contains('T')) {
                val timePart = time.substringAfter('T').substringBefore('+').substringBefore('Z')
                val parts = timePart.split(':')
                if (parts.size >= 2) "${parts[0]}:${parts[1]}" else timePart
            } else time
        } ?: "--:--"
        tvTime.text = timeStr
        
        // Set habit icon (placeholder colored square)
        ivHabitIcon.setBackgroundColor(resources.getColor(R.color.accent_purple_start, null))
        
        // Set status icon (check circle or empty circle based on completion)
        val isCompleted = schedule.status == "completed" // Adjust based on your status values
        ivStatus.setBackgroundResource(
            if (isCompleted) R.drawable.bg_checked_circle 
            else R.drawable.bg_unchecked_circle
        )
        
        // Set click listener to open ScheduleDetailActivity
        cardView.setOnClickListener {
            try {
                val intent = android.content.Intent(requireContext(), ScheduleDetailActivity::class.java)
                intent.putExtra(ScheduleDetailActivity.EXTRA_SCHEDULE_ID, schedule.id)
                startActivity(intent)
            } catch (ex: Exception) {
                Log.e(TAG, "Open schedule details", ex)
            }
        }
        
        container.addView(cardView)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
