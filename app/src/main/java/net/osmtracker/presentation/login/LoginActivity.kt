package net.osmtracker.presentation.login

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import net.osmtracker.OSMTracker
import net.osmtracker.activity.Intro
import net.osmtracker.databinding.ActivityLoginBinding

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.loginButton.setOnClickListener {
            val username = binding.usernameInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()
            
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "لطفاً اطلاعات را کامل وارد کنید", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            viewModel.login(username, password)
        }
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.loginButton.isEnabled = false
                }
                is LoginState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.loginButton.isEnabled = true
                    
                    // Save tokens
                    PreferenceManager.getDefaultSharedPreferences(applicationContext)
                        .edit()
                        .putString("ACCESS_TOKEN", state.response.access_token)
                        .putString("REFRESH_TOKEN", state.response.refresh_token)
                        .apply()
                    
                    PreferenceManager.getDefaultSharedPreferences(getBaseContext())
                        .edit()
                        .putBoolean(OSMTracker.Preferences.KEY_DISPLAY_APP_Login, false)
                        .apply()

                    Toast.makeText(this, "ورود موفقیت‌آمیز", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, Intro::class.java))
                    finish()
                }
                is LoginState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.loginButton.isEnabled = true
                    Toast.makeText(this, "خطا در ورود: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}