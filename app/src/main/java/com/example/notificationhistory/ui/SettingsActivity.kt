package com.example.notificationhistory.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import com.example.notificationhistory.R
import com.example.notificationhistory.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREFS_NAME = "notification_prefs"
        const val KEY_THEME = "theme_mode"
        const val KEY_TIME_FORMAT = "default_time_format"

        // Theme mode values
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize prefs FIRST before any method that uses it
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        // Apply theme BEFORE super.onCreate to avoid flicker
        applySavedTheme()

        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupPreferences()
        setupBottomNavigation()
    }

    private fun applySavedTheme() {
        val themeMode = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        when (themeMode) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupPreferences() {
        // Initialize RadioGroups
        val themeGroup = binding.radioGroupTheme
        val timeGroup = binding.radioGroupTimeFormat

        // Load theme preference
        val currentTheme = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        when (currentTheme) {
            THEME_LIGHT -> binding.radioThemeLight.isChecked = true
            THEME_DARK -> binding.radioThemeDark.isChecked = true
            else -> binding.radioThemeSystem.isChecked = true
        }

        // Theme RadioGroup listener
        themeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_theme_system -> {
                    prefs.edit().putString(KEY_THEME, THEME_SYSTEM).apply()
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    Toast.makeText(this, "已切换到跟随系统", Toast.LENGTH_SHORT).show()
                }
                R.id.radio_theme_light -> {
                    prefs.edit().putString(KEY_THEME, THEME_LIGHT).apply()
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    Toast.makeText(this, "已切换到浅色主题", Toast.LENGTH_SHORT).show()
                }
                R.id.radio_theme_dark -> {
                    prefs.edit().putString(KEY_THEME, THEME_DARK).apply()
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    Toast.makeText(this, "已切换到黑色主题", Toast.LENGTH_SHORT).show()
                }
            }
            // Restart activity to apply theme change
            recreate()
        }

        // Load time format preference
        val currentTimeFormat = prefs.getString(KEY_TIME_FORMAT, "relative") ?: "relative"
        if (currentTimeFormat == "relative") {
            binding.radioTimeRelative.isChecked = true
        } else {
            binding.radioTimeAbsolute.isChecked = true
        }

        // Time format RadioGroup listener
        timeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_time_relative -> {
                    prefs.edit().putString(KEY_TIME_FORMAT, "relative").apply()
                    Toast.makeText(this, "已切换到相对时间", Toast.LENGTH_SHORT).show()
                }
                R.id.radio_time_absolute -> {
                    prefs.edit().putString(KEY_TIME_FORMAT, "absolute").apply()
                    Toast.makeText(this, "已切换到精确时间", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupBottomNavigation() {
        // Main button - navigate to MainActivity
        binding.btnMain.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Service button - navigate to notification service settings
        binding.btnService.setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        // Settings button - already on settings, just show toast
        binding.btnSettings.setOnClickListener {
            Toast.makeText(this, "当前已在设置页面", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        // Re-apply theme to ensure consistency
        applySavedTheme()
    }
}
