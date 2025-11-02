package com.firstapp.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.firstapp.myapplication.network.Network
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import com.firstapp.myapplication.auth.AuthManager
import com.firstapp.myapplication.ui.HomeActivity
import com.firstapp.myapplication.ui.LoginActivity

class SplashActivity : AppCompatActivity() {
    companion object{
        const val TAG = "SplashActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Log.d(TAG, "onCreate")

        // initialize auth storage
        AuthManager.init(applicationContext)

        // small delay to show splash, then auto-redirect based on token presence or refresh
        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            lifecycleScope.launch {
                val access = AuthManager.accessToken
                val refresh = AuthManager.refreshToken

                if (!access.isNullOrEmpty()) {
                    // we have an access token, proceed to home
                    startActivity(Intent(this@SplashActivity, com.firstapp.myapplication.MainActivity::class.java))
                    finish()
                    return@launch
                }

                if (!refresh.isNullOrEmpty()) {
                    // attempt to refresh tokens using refresh token (blocking network call off main thread)
                    val pair = withContext(Dispatchers.IO) { Network.refreshWithRefreshTokenBlocking(refresh) }
                    if (pair != null) {
                        AuthManager.save(pair.first, pair.second)
                        startActivity(Intent(this@SplashActivity, HomeActivity::class.java))
                        finish()
                        return@launch
                    }
                }

                // otherwise go to login
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                finish()
            }
        }, 800)
    }
}