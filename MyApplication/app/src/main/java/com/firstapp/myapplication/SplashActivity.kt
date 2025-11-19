package com.firstapp.myapplication

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.firstapp.myapplication.network.Network
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
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

    private lateinit var logoImageView: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var subtitleTextView: TextView
    private var isAnimationComplete = false
    private var isAuthCheckComplete = false
    private var nextIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hide the action bar for full screen experience
        supportActionBar?.hide()
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Log.d(TAG, "onCreate")

        initViews()
        startAnimations()
        
        // initialize auth storage
        AuthManager.init(applicationContext)
        performAuthCheck()
    }

    private fun initViews() {
        logoImageView = findViewById(R.id.logoImageView)
        titleTextView = findViewById(R.id.titleTextView)
        subtitleTextView = findViewById(R.id.subtitleTextView)

        // Initially hide all views for animation
        logoImageView.alpha = 0f
        titleTextView.alpha = 0f
        subtitleTextView.alpha = 0f
        
        // Scale down for entry animation
        logoImageView.scaleX = 0.3f
        logoImageView.scaleY = 0.3f
    }

    private fun startAnimations() {
        // Logo animation - Scale up with bounce and fade in
        val logoScaleX = ObjectAnimator.ofFloat(logoImageView, "scaleX", 0.3f, 1.2f, 1.0f)
        val logoScaleY = ObjectAnimator.ofFloat(logoImageView, "scaleY", 0.3f, 1.2f, 1.0f)
        val logoAlpha = ObjectAnimator.ofFloat(logoImageView, "alpha", 0f, 1f)
        
        logoScaleX.interpolator = BounceInterpolator()
        logoScaleY.interpolator = BounceInterpolator()
        logoAlpha.interpolator = AccelerateDecelerateInterpolator()
        
        val logoAnimatorSet = AnimatorSet()
        logoAnimatorSet.playTogether(logoScaleX, logoScaleY, logoAlpha)
        logoAnimatorSet.duration = 1500

        // Title animation - Slide in from left and fade in
        val titleTranslationX = ObjectAnimator.ofFloat(titleTextView, "translationX", -300f, 0f)
        val titleAlpha = ObjectAnimator.ofFloat(titleTextView, "alpha", 0f, 1f)
        
        titleTranslationX.interpolator = AccelerateDecelerateInterpolator()
        titleAlpha.interpolator = AccelerateDecelerateInterpolator()
        
        val titleAnimatorSet = AnimatorSet()
        titleAnimatorSet.playTogether(titleTranslationX, titleAlpha)
        titleAnimatorSet.duration = 800
        titleAnimatorSet.startDelay = 500

        // Subtitle animation - Slide in from right and fade in
        val subtitleTranslationX = ObjectAnimator.ofFloat(subtitleTextView, "translationX", 300f, 0f)
        val subtitleAlpha = ObjectAnimator.ofFloat(subtitleTextView, "alpha", 0f, 1f)
        
        subtitleTranslationX.interpolator = AccelerateDecelerateInterpolator()
        subtitleAlpha.interpolator = AccelerateDecelerateInterpolator()
        
        val subtitleAnimatorSet = AnimatorSet()
        subtitleAnimatorSet.playTogether(subtitleTranslationX, subtitleAlpha)
        subtitleAnimatorSet.duration = 800
        subtitleAnimatorSet.startDelay = 800

        // Start all animations
        logoAnimatorSet.start()
        titleAnimatorSet.start()
        subtitleAnimatorSet.start()

        // Add a final pulse animation to the logo
        subtitleAnimatorSet.doOnEnd {
            val pulseAnimation = ObjectAnimator.ofFloat(logoImageView, "scaleX", 1.0f, 1.1f, 1.0f)
            val pulseAnimationY = ObjectAnimator.ofFloat(logoImageView, "scaleY", 1.0f, 1.1f, 1.0f)
            
            val pulseSet = AnimatorSet()
            pulseSet.playTogether(pulseAnimation, pulseAnimationY)
            pulseSet.duration = 600
            pulseSet.interpolator = AccelerateDecelerateInterpolator()
            pulseSet.start()
            
            pulseSet.doOnEnd {
                isAnimationComplete = true
                checkAndNavigate()
            }
        }
    }

    private fun performAuthCheck() {
        // small delay to allow animations to start, then auto-redirect based on token presence or refresh
        Handler(Looper.getMainLooper()).postDelayed({
            lifecycleScope.launch {
                val access = AuthManager.accessToken
                val refresh = AuthManager.refreshToken

                if (!access.isNullOrEmpty()) {
                    // we have an access token, proceed to home
                    nextIntent = Intent(this@SplashActivity, com.firstapp.myapplication.MainActivity::class.java)
                } else if (!refresh.isNullOrEmpty()) {
                    // attempt to refresh tokens using refresh token (blocking network call off main thread)
                    val pair = withContext(Dispatchers.IO) { Network.refreshWithRefreshTokenBlocking(refresh) }
                    if (pair != null) {
                        AuthManager.save(pair.first, pair.second)
                        nextIntent = Intent(this@SplashActivity, HomeActivity::class.java)
                    } else {
                        nextIntent = Intent(this@SplashActivity, LoginActivity::class.java)
                    }
                } else {
                    // otherwise go to login
                    nextIntent = Intent(this@SplashActivity, LoginActivity::class.java)
                }
                
                isAuthCheckComplete = true
                checkAndNavigate()
            }
        }, 1000)
    }

    private fun checkAndNavigate() {
        if (isAnimationComplete && isAuthCheckComplete && nextIntent != null) {
            startActivity(nextIntent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}