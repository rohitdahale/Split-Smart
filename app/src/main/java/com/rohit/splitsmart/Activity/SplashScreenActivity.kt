package com.rohit.splitsmart.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import com.rohit.splitsmart.databinding.ActivitySplashScreenBinding

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        binding.logoImageView.alpha = 0f
        binding.appNameText.alpha = 0f
        binding.taglineText.alpha = 0f

        binding.logoImageView.animate().alpha(1f).setDuration(1000).start()
        binding.appNameText.animate().alpha(1f).setStartDelay(500).setDuration(1000).start()
        binding.taglineText.animate().alpha(1f).setStartDelay(700).setDuration(1000).start()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        },2500)

        setContentView(binding.root)
    }
}