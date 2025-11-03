package com.firstapp.myapplication.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firstapp.myapplication.R
import com.firstapp.myapplication.network.Network
import kotlinx.coroutines.launch

class AddHabitActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_habit)

        val spHabitName = findViewById<android.widget.Spinner>(R.id.spHabitName)
        val etGoalAmount = findViewById<EditText>(R.id.etGoalAmount)
        val btnCreate = findViewById<Button>(R.id.btnCreate)
        val btnCancel = findViewById<Button>(R.id.btnCancel)

        btnCancel.setOnClickListener {
            finish()
        }

        btnCreate.setOnClickListener {
            val goal = etGoalAmount.text.toString().trim()

            if (goal.isEmpty()) {
                Toast.makeText(this, "Goal is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    // backend expects: name, description (optional), categoryId, goal
                    val body = mutableMapOf<String, Any>(
                        "name" to "New Habit",
                        "goal" to goal,
                        "categoryId" to 1
                    )

                    val resp = Network.api.createHabit(body)
                    if (resp.isSuccessful) {
                        Toast.makeText(this@AddHabitActivity, "Habit created", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@AddHabitActivity, "Failed: ${resp.code()}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@AddHabitActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
