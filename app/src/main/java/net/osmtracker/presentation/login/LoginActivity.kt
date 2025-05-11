package net.osmtracker.presentation.login

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import net.osmtracker.OSMTracker
import net.osmtracker.activity.Intro
import net.osmtracker.databinding.ActivityLoginBinding

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
    private fun validateLogin() {
        val username = binding.usernameInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()
        if (username == "admin" && password == "1234") {
            PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().putBoolean(OSMTracker.Preferences.KEY_DISPLAY_APP_Login, false).apply()
            Toast.makeText(this, "ورود موفقیت‌آمیز بود", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Intro::class.java))
            finish()
        } else {
            Toast.makeText(this, "نام کاربری یا رمز عبور اشتباه است", Toast.LENGTH_SHORT).show()
        }
    }
//    private fun setupListeners() {
//        binding.loginButton.setOnClickListener {
//            val username = binding.usernameInput.text.toString()
//            val password = binding.passwordInput.text.toString()
//            if (username.isNotEmpty() && password.isNotEmpty()) {
//                viewModel.login(username, password)
//            } else {
//                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private fun setupObservers() {
//        viewModel.loginState.observe(this) { state ->
//            when (state) {
//                is LoginState.Loading -> {
//                    binding.progressBar.isVisible = true
//                    binding.loginButton.isEnabled = false
//                }
//                is LoginState.Success -> {
//                    binding.progressBar.isVisible = false
//                    binding.loginButton.isEnabled = true
//                    // Navigate to MainActivity
//                    PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().putBoolean(OSMTracker.Preferences.KEY_DISPLAY_APP_Login, false).apply()
//                    Toast.makeText(this, "ورود موفقیت‌آمیز بود", Toast.LENGTH_SHORT).show()
//                    startActivity(Intent(this, Intro::class.java))
//                    finish()
//                }
//                is LoginState.Error -> {
//                    binding.progressBar.isVisible = false
//                    binding.loginButton.isEnabled = true
//                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//    }
}