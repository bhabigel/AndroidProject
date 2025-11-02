package com.firstapp.myapplication.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firstapp.myapplication.R
import com.firstapp.myapplication.auth.AuthManager
import com.firstapp.myapplication.network.Network
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {
    companion object { private const val TAG = "ProfileActivity" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        AuthManager.init(applicationContext)

        val tvName = findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        lifecycleScope.launch {
            try {
                val resp = Network.api.getProfile()
                if (resp.isSuccessful) {
                    val body = resp.body()
                    tvName.text = body?.get("username")?.toString() ?: "-"
                    tvEmail.text = body?.get("email")?.toString() ?: "-"
                } else {
                    Toast.makeText(this@ProfileActivity, "Failed to load profile: ${resp.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "profile fetch", e)
                Toast.makeText(this@ProfileActivity, "Network error", Toast.LENGTH_LONG).show()
            }
        }

        btnLogout.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val resp = Network.api.logout()
                    if (resp.isSuccessful) {
                        AuthManager.clear()
                        startActivity(Intent(this@ProfileActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@ProfileActivity, "Logout failed", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@ProfileActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
