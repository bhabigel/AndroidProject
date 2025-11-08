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

class HomeFragment : Fragment() {
    companion object { private const val TAG = "HomeFragment" }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AuthManager.init(requireContext())

        // Set current date
        updateDateDisplay(view)
        
        // Load schedules
        fetchSchedules(view)
    }

    private fun updateDateDisplay(view: View) {
        val dateText = view.findViewById<TextView>(R.id.tvDate)
        val today = java.time.LocalDate.now()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d")
        dateText.text = today.format(formatter)
    }

    private fun fetchSchedules(root: View) {
        val container = root.findViewById<LinearLayout>(R.id.llSchedules)
        lifecycleScope.launch {
            try {
                val resp = Network.api.getSchedulesByDay(null)
                if (resp.isSuccessful) {
                    val list = resp.body() ?: emptyList()
                    container.removeAllViews()
                    
                    // Group schedules by time of day
                    val morning = list.filter { it.start_time?.startsWith("0") == true || it.start_time?.startsWith("1") == true }
                    val afternoon = list.filter { it.start_time?.startsWith("1") == true }
                    val night = list.filter { it.start_time?.startsWith("2") == true }

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
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetch schedules", e)
            }
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

    private fun addScheduleCard(container: LinearLayout, schedule: Any) {
        // Create card layout
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(72)
            ).apply {
                topMargin = dpToPx(8)
            }

            // Add icon
            addView(View(context).apply {
                setBackgroundColor(resources.getColor(R.color.input_stroke, null))
                layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
            })

            // Add text content
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    leftMargin = dpToPx(12)
                }

                // Title
                addView(TextView(context).apply {
                    text = schedule.toString() // TODO: Use proper fields once model is available
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.text_primary, null))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                })

                // Time
                addView(TextView(context).apply {
                    text = "08:00" // TODO: Format time from schedule
                    textSize = 12f
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                    setPadding(0, dpToPx(6), 0, 0)
                })
            })

            // Add check circle
            addView(View(context).apply {
                setBackgroundResource(R.drawable.bg_unchecked_circle)
                layoutParams = LinearLayout.LayoutParams(dpToPx(28), dpToPx(28))
            })

            // Set click listener
            setOnClickListener {
                try {
                    val intent = android.content.Intent(requireContext(), ScheduleDetailActivity::class.java)
                    intent.putExtra(ScheduleDetailActivity.EXTRA_SCHEDULE_ID, schedule.toString()) // TODO: Use proper ID
                    startActivity(intent)
                } catch (ex: Exception) {
                    Log.e(TAG, "Open schedule details", ex)
                }
            }

            container.addView(this)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
