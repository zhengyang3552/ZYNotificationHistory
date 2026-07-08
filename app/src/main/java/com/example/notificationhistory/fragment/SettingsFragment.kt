package com.example.notificationhistory.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.notificationhistory.R
import com.example.notificationhistory.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val PREFS_NAME = "notification_prefs"
        const val KEY_THEME = "theme_mode"
        const val KEY_TIME_FORMAT = "default_time_format"

        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPreferences()
    }

    private fun setupPreferences() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, requireContext().MODE_PRIVATE)
        
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
                    Toast.makeText(context, "已切换到跟随系统", Toast.LENGTH_SHORT).show()
                }
                R.id.radio_theme_light -> {
                    prefs.edit().putString(KEY_THEME, THEME_LIGHT).apply()
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    Toast.makeText(context, "已切换到浅色主题", Toast.LENGTH_SHORT).show()
                }
                R.id.radio_theme_dark -> {
                    prefs.edit().putString(KEY_THEME, THEME_DARK).apply()
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    Toast.makeText(context, "已切换到黑色主题", Toast.LENGTH_SHORT).show()
                }
            }
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
                    Toast.makeText(context, "已切换到相对时间", Toast.LENGTH_SHORT).show()
                }
                R.id.radio_time_absolute -> {
                    prefs.edit().putString(KEY_TIME_FORMAT, "absolute").apply()
                    Toast.makeText(context, "已切换到精确时间", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
