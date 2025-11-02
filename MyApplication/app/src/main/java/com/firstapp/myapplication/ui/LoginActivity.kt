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
import com.firstapp.myapplication.network.SignInRequest
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    companion object { private const val TAG = "LoginActivity" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        AuthManager.init(applicationContext)

        val emailEt = findViewById<EditText>(R.id.etEmail)
        val passwordEt = findViewById<EditText>(R.id.etPassword)
        val signInBtn = findViewById<Button>(R.id.btnSignIn)
        val registerBtn = findViewById<Button>(R.id.btnRegister)

        registerBtn.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
        }

        signInBtn.setOnClickListener {
            val email = emailEt.text.toString().trim()
            val pass = passwordEt.text.toString().trim()
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Fill email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                try {
                    val resp = Network.api.signin(SignInRequest(email, pass))
                    if (resp.isSuccessful) {
                        val body = resp.body()
                        val tokens = body?.tokens
                        AuthManager.save(tokens?.accessToken, tokens?.refreshToken)
                            startActivity(Intent(this@LoginActivity, com.firstapp.myapplication.MainActivity::class.java))
                        finish()
                    } else {
                        val err = resp.errorBody()?.string()
                        Log.w(TAG, "signin failed: ${resp.code()} $err")
                        Toast.makeText(this@LoginActivity, "Signin failed: ${resp.code()}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "signin error", e)
                    Toast.makeText(this@LoginActivity, "Network error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
