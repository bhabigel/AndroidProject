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

class ResetPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        val emailEt = findViewById<EditText>(R.id.etResetEmail)
        val sendBtn = findViewById<Button>(R.id.btnSendReset)

        sendBtn.setOnClickListener {
            val email = emailEt.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                try {
                    val resp = Network.api.resetPasswordViaEmail(mapOf("email" to email))
                    if (resp.isSuccessful) {
                        Toast.makeText(this@ResetPasswordActivity, "If the email exists, a new password was sent.", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@ResetPasswordActivity, "Failed: ${resp.code()}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@ResetPasswordActivity, "Network error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
