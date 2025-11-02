package com.firstapp.myapplication.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firstapp.myapplication.R
import com.firstapp.myapplication.auth.AuthManager
import com.firstapp.myapplication.network.Network
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class RegisterActivity : AppCompatActivity() {
    companion object { private const val TAG = "RegisterActivity" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val nameEt = findViewById<EditText>(R.id.etName)
        val emailEt = findViewById<EditText>(R.id.etEmail)
        val passEt = findViewById<EditText>(R.id.etPassword)
        val confirmEt = findViewById<EditText>(R.id.etPasswordConfirm)
        val registerBtn = findViewById<Button>(R.id.btnRegisterSubmit)

        registerBtn.setOnClickListener {
            val name = nameEt.text.toString().trim()
            val email = emailEt.text.toString().trim()
            val pass = passEt.text.toString()
            val conf = confirmEt.text.toString()
            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass != conf) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // call backend signup
            lifecycleScope.launch {
                try {
                    val mt = "text/plain".toMediaTypeOrNull()
                    val usernameBody = name.toRequestBody(mt)
                    val emailBody = email.toRequestBody(mt)
                    val passBody = pass.toRequestBody(mt)

                    val resp = Network.api.signup(usernameBody, emailBody, passBody, null)
                    if (resp.isSuccessful) {
                        val body = resp.body()
                        val tokens = body?.tokens
                        AuthManager.init(applicationContext)
                        AuthManager.save(tokens?.accessToken, tokens?.refreshToken)
                        Toast.makeText(this@RegisterActivity, "Registered and logged in", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@RegisterActivity, com.firstapp.myapplication.MainActivity::class.java))
                        finish()
                    } else {
                        val err = resp.errorBody()?.string()
                        Log.w(TAG, "signup failed: ${resp.code()} $err")
                        Toast.makeText(this@RegisterActivity, "Signup failed: ${resp.code()}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "signup error", e)
                    Toast.makeText(this@RegisterActivity, "Network error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
