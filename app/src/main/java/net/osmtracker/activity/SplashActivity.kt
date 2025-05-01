package net.osmtracker.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import net.osmtracker.R

class SplashActivity : AppCompatActivity() {

    companion object {
        // مدت زمان نمایش اسپلش (۳ ثانیه)
        private const val SPLASH_DELAY = 3000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // استفاده از Handler برای تأخیر
        Handler(Looper.getMainLooper()).postDelayed({
            // بستن SplashActivity برای بازگشت به TrackManager
            finish()
        }, SPLASH_DELAY)
    }
}