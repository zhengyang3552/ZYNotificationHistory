package com.example.notificationhistory.ui

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.notificationhistory.databinding.ActivitySettingsBinding
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("notification_prefs", MODE_PRIVATE)

        setupToolbar()
        setupPreferences()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupPreferences() {
        // Load current values
        val defaultTimeFormat = prefs.getString("default_time_format", "relative")
        val showPackageName = prefs.getBoolean("show_package_name", true)

        binding.switchRelativeTime.isChecked = (defaultTimeFormat == "relative")
        binding.switchShowPackage.isChecked = showPackageName

        // Time format switch
        binding.switchRelativeTime.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit()
                .putString("default_time_format", if (isChecked) "relative" else "absolute")
                .apply()
        }

        // Show package name switch
        binding.switchShowPackage.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit()
                .putBoolean("show_package_name", isChecked)
                .apply()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
