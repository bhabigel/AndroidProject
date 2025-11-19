package com.firstapp.myapplication.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.firstapp.myapplication.R
import com.firstapp.myapplication.auth.AuthManager
import com.firstapp.myapplication.network.Network
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class ProfileFragment : Fragment() {
    companion object { private const val TAG = "ProfileFragment" }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadImage(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AuthManager.init(requireContext())
        
        val btnEditProfile = view.findViewById<Button>(R.id.btnEditProfile)
        val btnAddNewHabit = view.findViewById<Button>(R.id.btnAddNewHabit)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)
        
        btnEditProfile?.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }
        
        btnAddNewHabit?.setOnClickListener {
            startActivity(Intent(requireContext(), AddHabitActivity::class.java))
        }
        
        btnLogout?.setOnClickListener {
            lifecycleScope.launch {
                try {
                    Network.api.logout()
                    AuthManager.clear()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    activity?.finish()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Logout error", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        loadProfile(view)
    }

    private fun loadProfile(root: View) {
        val tvName = root.findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = root.findViewById<TextView>(R.id.tvProfileEmail)
        val llHabits = root.findViewById<LinearLayout>(R.id.llHabits)
        val ivAvatar = root.findViewById<ShapeableImageView>(R.id.ivAvatar)

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
        
        // Load habits
        loadHabits(llHabits)
    }

    private fun loadHabits(container: LinearLayout) {
        lifecycleScope.launch {
            try {
                val habitsResp = Network.api.listHabits()
                val todayDate = java.time.LocalDate.now().toString()
                val schedulesResp = Network.api.getSchedulesByDay(todayDate)
                
                if (habitsResp.isSuccessful && schedulesResp.isSuccessful) {
                    val habits = habitsResp.body() ?: emptyList()
                    val todaySchedules = schedulesResp.body() ?: emptyList()
                    container.removeAllViews()
                    
                    if (habits.isEmpty()) {
                        // Add sample habits for demo
                        addHabitItem(container, "Other", 50, "#6366F1", -1)
                        addHabitItem(container, "Reading", 50, "#10B981", -1) 
                        addHabitItem(container, "nothing", 50, "#8B5CF6", -1)
                        addHabitItem(container, "Study", 50, "#8B5CF6", -1)
                    } else {
                        for (habit in habits) {
                            // Calculate progress based on today's schedules for this habit
                            val habitSchedules = todaySchedules.filter { it.habit?.name == habit.name }
                            val progress = if (habitSchedules.isEmpty()) {
                                0
                            } else {
                                val completedCount = habitSchedules.count { it.status?.lowercase() == "completed" }
                                val totalCount = habitSchedules.size
                                (completedCount * 100) / totalCount
                            }
                            
                            val color = when {
                                progress == 100 -> "#10B981" // Green
                                progress >= 50 -> "#6366F1"  // Blue
                                else -> "#8B5CF6"            // Purple
                            }
                            
                            addHabitItem(container, habit.name, progress, color, habit.id)
                        }
                    }
                } else {
                    // Fallback to sample data
                    container.removeAllViews()
                    addHabitItem(container, "Other", 50, "#6366F1", -1)
                    addHabitItem(container, "Reading", 50, "#10B981", -1) 
                    addHabitItem(container, "nothing", 50, "#8B5CF6", -1)
                    addHabitItem(container, "Study", 50, "#8B5CF6", -1)
                }
            } catch (e: Exception) {
                Log.e(TAG, "habits/schedules fetch", e)
                // Fallback to sample data
                container.removeAllViews()
                addHabitItem(container, "Other", 50, "#6366F1", -1)
                addHabitItem(container, "Reading", 50, "#10B981", -1)
                addHabitItem(container, "nothing", 50, "#8B5CF6", -1)
                addHabitItem(container, "Study", 50, "#8B5CF6", -1)
            }
        }
    }

    private fun addHabitItem(container: LinearLayout, name: String, progress: Int, iconColor: String, habitId: Int) {
        val context = requireContext()
        
        // Main habit row
        val habitRow = LinearLayout(context).apply {
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
        val icon = android.view.View(context).apply {
            setBackgroundColor(android.graphics.Color.parseColor(iconColor))
            layoutParams = LinearLayout.LayoutParams(dpToPx(6), dpToPx(32)).apply {
                rightMargin = dpToPx(12)
            }
        }
        habitRow.addView(icon)

        // Text + Progress container
        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val habitName = TextView(context).apply {
            text = name
            textSize = 15f
            setTextColor(resources.getColor(R.color.text_primary, null))
        }
        textContainer.addView(habitName)

        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
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

        // Delete button (only for real habits, not sample data)
        if (habitId > 0) {
            val deleteButton = TextView(context).apply {
                text = "🗑️"
                textSize = 16f
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                setBackgroundResource(android.R.drawable.btn_default)
                setOnClickListener {
                    deleteHabit(habitId, container, habitRow)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    leftMargin = dpToPx(8)
                }
            }
            habitRow.addView(deleteButton)
        }

        // Percentage text
        val percentText = TextView(context).apply {
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

    private fun deleteHabit(habitId: Int, container: LinearLayout, habitRow: LinearLayout) {
        // Visual deletion only (no backend call)
        container.removeView(habitRow)
        Toast.makeText(requireContext(), "Habit removed from list", Toast.LENGTH_SHORT).show()
    }

    private fun uploadImage(uri: Uri) {
        val ivAvatar = view?.findViewById<ShapeableImageView>(R.id.ivAvatar)
        lifecycleScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes == null) {
                    Log.e(TAG, "unable to read image bytes")
                    return@launch
                }

                val requestFile: RequestBody = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", "avatar.jpg", requestFile)

                val resp = withContext(Dispatchers.IO) { Network.api.uploadProfileImage(part) }
                if (resp.isSuccessful) {
                    // show the selected image immediately
                    ivAvatar?.setImageURI(uri)
                } else {
                    Log.e(TAG, "upload failed: ${resp.code()} ${resp.errorBody()?.string()}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "uploadImage", e)
            }
        }
    }
}
