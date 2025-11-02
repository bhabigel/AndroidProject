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

        val etName = findViewById<EditText>(R.id.etHabitName)
        val etDesc = findViewById<EditText>(R.id.etHabitDescription)
        val etGoal = findViewById<EditText>(R.id.etHabitGoal)
        val btn = findViewById<Button>(R.id.btnSubmitHabit)

        btn.setOnClickListener {
            val name = etName.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            val goal = etGoal.text.toString().trim()

            if (name.isEmpty() || goal.isEmpty()) {
                Toast.makeText(this, "Name and goal are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    // backend expects: name, description (optional), categoryId, goal
                    val body = mutableMapOf<String, Any>(
                        "name" to name,
                        "goal" to goal,
                        "categoryId" to 1
                    )
                    if (desc.isNotEmpty()) body["description"] = desc

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
