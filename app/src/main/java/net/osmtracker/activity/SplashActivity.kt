package net.osmtracker.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import net.osmtracker.OSMTracker
import net.osmtracker.R

class SplashActivity : AppCompatActivity() {
    companion object {
        // مدت زمان نمایش اسپلش (۳ ثانیه)
        private const val SPLASH_DELAY = 3000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // بررسی اینکه آیا Intro باید نمایش داده بشه
        val showAppIntro = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(OSMTracker.Preferences.KEY_DISPLAY_APP_INTRO, OSMTracker.Preferences.VAL_DISPLAY_APP_INTRO)

        // استفاده از Handler برای تأخیر
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = if (showAppIntro) {
                // نمایش Intro اگه اولین باره
                Intent(this, Intro::class.java)
            } else {
                // انتقال به TrackManager
                Intent(this, TrackManager::class.java)
            }
            startActivity(intent)
            // بستن SplashActivity
            finish()
        }, SPLASH_DELAY)
    }
}