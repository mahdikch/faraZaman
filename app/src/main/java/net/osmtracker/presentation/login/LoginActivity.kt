package net.osmtracker.presentation.login

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import net.osmtracker.OSMTracker
import net.osmtracker.activity.Intro
import net.osmtracker.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.loginButton.setOnClickListener {
            validateLogin()
        }
    }
    private fun validateLogin() {
        val username = binding.usernameInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()

        if (username == "admin" && password == "1234") {
            PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().putBoolean(OSMTracker.Preferences.KEY_DISPLAY_APP_Login, false).apply()

            Toast.makeText(this, "ورود موفقیت‌آمیز بود", Toast.LENGTH_SHORT).show()
//            val showAppIntro = PreferenceManager.getDefaultSharedPreferences(this)
//                .getBoolean(OSMTracker.Preferences.KEY_DISPLAY_APP_INTRO, OSMTracker.Preferences.VAL_DISPLAY_APP_INTRO)
//            val intent = if (showAppIntro) {
                // نمایش Intro اگه اولین باره

//            } else {
                // انتقال به TrackManager
//                Intent(this, TrackManager::class.java)
//            }
            startActivity(Intent(this, Intro::class.java))

            finish()
        } else {
            Toast.makeText(this, "نام کاربری یا رمز عبور اشتباه است", Toast.LENGTH_SHORT).show()
        }
    }
}