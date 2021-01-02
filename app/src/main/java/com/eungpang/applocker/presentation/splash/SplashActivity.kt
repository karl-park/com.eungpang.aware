package com.eungpang.applocker.presentation.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.eungpang.applocker.presentation.main.MainActivity
import com.eungpang.snstimechecker.R

private const val INTERVAL_SPLASH = 700L
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler().postDelayed({
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }, INTERVAL_SPLASH)
    }


}