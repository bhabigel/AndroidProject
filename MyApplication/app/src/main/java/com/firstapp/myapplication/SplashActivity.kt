package com.firstapp.myapplication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
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
    }
    override fun onStart()
    {
        super.onStart()
        Log.d(TAG, "onStart")
    }
    override fun onPause()
    {
        super.onPause()
        Log.d(TAG, "onPause")
    }
    override fun onDestroy()
    {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }
}