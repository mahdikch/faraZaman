package net.osmtracker.presentation.login

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import net.osmtracker.OSMTracker
import net.osmtracker.R
import net.osmtracker.activity.Intro
import net.osmtracker.activity.TrackManager
import net.osmtracker.databinding.ActivityLoginBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var prefs: SharedPreferences
    private lateinit var provinceList: List<ProvinceInstance>
    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private var provinceSpinnerInitialized = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        // Disable login button until province is selected
        binding.loginButton.isEnabled = false

        // Fetch and populate province spinner
        fetchProvincesAndPopulateSpinner()
        setLoginButtonEnabled(false)
        startEntryAnimations()
        setupListeners() // این متد حالا پیاده‌سازی صحیح دارد
        observeViewModel()
        setupKeyboardListener()
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
                    prefs.edit()
                        .putString("ACCESS_TOKEN", state.response.access_token)
                        .putString("REFRESH_TOKEN", state.response.refresh_token)
                        .apply()
                    Toast.makeText(this, "ورود موفقیت‌آمیز", Toast.LENGTH_SHORT).show()
                    decideNextActivity()
                }
                is LoginState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.loginButton.isEnabled = true
                    Toast.makeText(this, "خطا در ورود: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun decideNextActivity() {
        val showIntro = prefs.getBoolean(OSMTracker.Preferences.KEY_DISPLAY_APP_INTRO, OSMTracker.Preferences.VAL_DISPLAY_APP_INTRO)
        val intent = if (showIntro) {
            Intent(this, Intro::class.java)
        } else {
            Intent(this, TrackManager::class.java)
        }
        startActivity(intent)
        finish()
    }

    // *** این متد اصلاح شده است ***
    private fun setupListeners() {
        binding.loginButton.setOnClickListener {
            val username = binding.usernameInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "لطفاً نام کاربری و رمز عبور را وارد کنید", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // فراخوانی ViewModel برای شروع فرآیند لاگین
            viewModel.login(username, password)
        }
    }

    // ... سایر متدها بدون تغییر ...
    private fun startEntryAnimations() {
        val anim = AnimationUtils.loadAnimation(this, R.anim.slide_from_bottom)
        binding.usernameInputLayout.startAnimation(anim)
        anim.startOffset = 100
        binding.passwordInputLayout.startAnimation(anim)
        anim.startOffset = 200
        binding.loginButton.startAnimation(anim)
    }

    private fun setupKeyboardListener() {
        val rootView = binding.root
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            rootView.getWindowVisibleDisplayFrame(r)
            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - r.bottom

            if (keypadHeight > screenHeight * 0.15) {
                binding.footer.visibility = View.GONE
            } else {
                binding.footer.visibility = View.VISIBLE
            }
        }
    }
    private fun fetchProvincesAndPopulateSpinner() {
        binding.provinceDropdown.setOnClickListener {
            binding.provinceDropdown.showDropDown()
        }
        binding.provinceDropdown.setOnItemClickListener { parent, view, position, id ->
            if (position >= 0) {
                val selected = provinceList[position]
                prefs.edit().putString("BASE_URL", selected.baseAddress).apply()
                // Reset Retrofit so next API call uses the new base URL
                try {
                    val clazz = Class.forName("net.osmtracker.di.RetrofitProvider")
                    val method = clazz.getDeclaredMethod("reset")
                    method.invoke(null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                setLoginButtonEnabled(true)
            }
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.provinceDropdown.isEnabled = false
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://app.tfs.co.ir/api/Application/Instances")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE

                    Toast.makeText(this@LoginActivity, "خطا در دریافت استان‌ها", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body != null) {
                    val provinces = JSONArray(body)
                    provinceList = mutableListOf()
                    val names = mutableListOf<String>()
                    for (i in 0 until provinces.length()) {
                        val obj = provinces.getJSONObject(i)
                        val province = ProvinceInstance(
                            obj.getLong("uid"),
                            obj.getString("title"),
                            obj.getString("baseAddress"),
                            obj.getBoolean("enabled"),
                            obj.getBoolean("current")
                        )
                        (provinceList as MutableList).add(province)
                        names.add(province.title)
                    }
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        val adapter = ArrayAdapter(this@LoginActivity, R.layout.dropdown_item, names)
                        binding.provinceDropdown.setAdapter(adapter)
                        binding.provinceDropdown.isEnabled = true

                    }
                }
            }
        })
    }

    private fun showProvinceDialog(provinces: List<ProvinceInstance>) {
        val names = provinces.map { it.title }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("انتخاب استان")
            .setItems(names) { _, which ->
                val selected = provinces[which]
                prefs.edit().putString("BASE_URL", selected.baseAddress).apply()
                Toast.makeText(this, "استان انتخاب شد: ${selected.title}", Toast.LENGTH_SHORT).show()
                recreateRetrofitWithNewBaseUrl(selected.baseAddress)
                binding.loginButton.isEnabled = true
            }
            .setCancelable(false)
            .show()
    }

    private fun recreateRetrofitWithNewBaseUrl(newBaseUrl: String) {
        try {
            val clazz = Class.forName("net.osmtracker.di.RetrofitProvider")
            val method = clazz.getDeclaredMethod("reset")
            method.invoke(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    data class ProvinceInstance(
        val uid: Long,
        val title: String,
        val baseAddress: String,
        val enabled: Boolean,
        val current: Boolean
    )
    private fun setLoginButtonEnabled(enabled: Boolean) {
        binding.loginButton.isEnabled = enabled
        val color = if (enabled) ContextCompat.getColor(this, R.color.brand_green)
        else ContextCompat.getColor(this, R.color.gray_disabled)
        binding.loginButton.setBackgroundColor(color)
    }
}