package com.firstapp.myapplication.ui

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firstapp.myapplication.R
import com.firstapp.myapplication.network.Network
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CreateProgressActivity : AppCompatActivity() {
    companion object { const val EXTRA_SCHEDULE_ID = "extra_schedule_id" }

    private var scheduleId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_progress)

        scheduleId = intent?.getIntExtra(EXTRA_SCHEDULE_ID, -1) ?: -1
        val etDate = findViewById<EditText>(R.id.etDate)
        val etLogged = findViewById<EditText>(R.id.etLoggedTime)
        val etNotes = findViewById<EditText>(R.id.etNotes)
        val cbCompleted = findViewById<CheckBox>(R.id.cbCompleted)
        val btn = findViewById<Button>(R.id.btnSubmitProgress)

        // prefill date to today
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        etDate.setText(sdf.format(Date()))

        btn.setOnClickListener {
            if (scheduleId == -1) {
                Toast.makeText(this, "No schedule id provided", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dateText = etDate.text.toString().trim()
            if (dateText.isEmpty()) {
                Toast.makeText(this, "Date is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val logged = etLogged.text.toString().trim()
            val loggedInt = logged.toIntOrNull()
            val notes = etNotes.text.toString().trim()

            lifecycleScope.launch {
                try {
                    val body = mutableMapOf<String, Any>(
                        "scheduleId" to scheduleId,
                        "date" to dateText
                    )
                    if (loggedInt != null) body["logged_time"] = loggedInt
                    if (notes.isNotEmpty()) body["notes"] = notes
                    body["is_completed"] = true

                    val resp = Network.api.createProgress(body)
                    if (resp.isSuccessful) {
                        Toast.makeText(this@CreateProgressActivity, "Progress created", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@CreateProgressActivity, "Failed: ${resp.code()}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@CreateProgressActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
