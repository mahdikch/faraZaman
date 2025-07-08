package net.osmtracker.activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import net.osmtracker.OSMTracker
import net.osmtracker.R
import net.osmtracker.presentation.login.LoginActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.imageView)
        val slogan = findViewById<TextView>(R.id.slogan_text)

        val logoAnim = AnimationUtils.loadAnimation(this, R.anim.splash_logo_fade_in)
        slogan.startAnimation(AnimationUtils.loadAnimation(this, R.anim.splash_text_slide_up))
        logo.startAnimation(logoAnim)

        logoAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                navigateToNextScreen()
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }

    private fun navigateToNextScreen() {
        // SharedPreferences را برای بررسی وضعیت لاگین و نمایش Intro چک می‌کنیم
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val token = prefs.getString("ACCESS_TOKEN", null)

        val intent: Intent

        if (!token.isNullOrEmpty()) {
            // اگر کاربر لاگین کرده است، مستقیم به TrackManager برو
            intent = Intent(this, TrackManager::class.java)
        } else {
            // اگر کاربر لاگین نکرده است، به LoginActivity برو
            intent = Intent(this, LoginActivity::class.java)
        }

        startActivity(intent)
        finish() // این اکتیویتی را از حافظه پاک کن
    }
}