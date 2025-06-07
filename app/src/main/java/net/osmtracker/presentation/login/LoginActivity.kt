package net.osmtracker.presentation.login

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.osmtracker.OSMTracker
import net.osmtracker.R
import net.osmtracker.activity.Intro
import net.osmtracker.databinding.ActivityLoginBinding
import net.osmtracker.service.remote.AuthService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
//    private val viewModel: LoginViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.loginButton.setOnClickListener {
            validateLogin()
        }
//        setupObservers()
//        setupListeners()
    }
//    private fun validateLogin() {
//        val username = binding.usernameInput.text.toString().trim()
//        val password = binding.passwordInput.text.toString().trim()
//        if (username == "admin" && password == "1234") {
//            PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().putBoolean(OSMTracker.Preferences.KEY_DISPLAY_APP_Login, false).apply()
//            Toast.makeText(this, "ورود موفقیت‌آمیز بود", Toast.LENGTH_SHORT).show()
//            startActivity(Intent(this, Intro::class.java))
//            finish()
//        } else {
//            Toast.makeText(this, "نام کاربری یا رمز عبور اشتباه است", Toast.LENGTH_SHORT).show()
//        }
//    }

    private fun validateLogin() {
        val username = binding.usernameInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()
        binding.progressBar.visibility= View.VISIBLE
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "لطفاً اطلاعات را کامل وارد کنید", Toast.LENGTH_SHORT).show()
            return
        }

        val clientId = "b1aa497a-8778-4351-8c91-9719e2b0362f"
        val clientSecret = "i4uCgAvIAw7BuAn7dDxDW0jHznYaqIhA"
        val authHeader = "Basic " + android.util.Base64.encodeToString(
            "$clientId:$clientSecret".toByteArray(), android.util.Base64.NO_WRAP
        )

        val retrofit = Retrofit.Builder()
            .baseUrl("https://demo.tfs.co.ir/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(AuthService::class.java)

        lifecycleScope.launch {
            try {
                val response = service.login(authHeader, username, password)
                val token = response.access_token
                val refresh_token = response.refresh_token

                PreferenceManager.getDefaultSharedPreferences(applicationContext)
                    .edit().putString("ACCESS_TOKEN", token).apply()
                PreferenceManager.getDefaultSharedPreferences(applicationContext)
                    .edit().putString("REFRESH_TOKEN", refresh_token).apply()
                PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().putBoolean(OSMTracker.Preferences.KEY_DISPLAY_APP_Login, false).apply()

                Toast.makeText(this@LoginActivity, "ورود موفقیت‌آمیز", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@LoginActivity, Intro::class.java))
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "خطا در ورود: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("LOGIN", "error", e)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }


}