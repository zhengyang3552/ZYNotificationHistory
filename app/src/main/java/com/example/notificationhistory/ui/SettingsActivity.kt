package com.example.notificationhistory.ui

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.notificationhistory.R
import com.example.notificationhistory.data.NotificationRepository
import com.example.notificationhistory.databinding.ActivitySettingsBinding
import com.example.notificationhistory.util.AppPreferences
import com.example.notificationhistory.util.NotificationAccess
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * 设置页。
 *
 * 改造要点：
 * - 移除硬编码的深棕色配色，改用与其他页面一致的 M3 语义色；
 * - 移除底部导航栏：返回走顶栏返回键，通知使用权入口就在本页卡片内；
 * - 主题切换不再手动 recreate（原实现 recreate + onResume 再次 applySavedTheme，
 *   容易出现闪烁与重复重建），改为一次全局 setDefaultNightMode 由框架统一重建；
 * - 补齐「显示包名」「保留上限」两个此前只能改代码/根本改不了的配置项；
 * - 新增记录数量展示与清空入口，形成「存储」闭环。
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val repository by lazy { NotificationRepository(this) }

    private var recordCount = 0
    private var suppressThemeCallback = false
    private var suppressTimeCallback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // 动态取色需在视图创建前应用（Android 12+ 跟随壁纸，关闭时回退统一品牌色）
        DynamicColor.apply(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupThemeSection()
        setupDynamicColorSwitch()
        setupTimeFormatSection()
        setupShowPackageSwitch()
        setupRetentionRow()
        setupPermissionSection()
        setupStorageSection()
        setupAboutSection()
        observeRecordCount()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
    }

    // ---------------------------------------------------------------- 初始化

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupThemeSection() {
        val current = AppPreferences.themeMode(this)
        suppressThemeCallback = true
        binding.toggleTheme.check(
            when (current) {
                AppPreferences.ThemeMode.LIGHT -> R.id.btn_theme_light
                AppPreferences.ThemeMode.DARK -> R.id.btn_theme_dark
                AppPreferences.ThemeMode.SYSTEM -> R.id.btn_theme_system
            }
        )
        suppressThemeCallback = false

        binding.toggleTheme.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || suppressThemeCallback) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                R.id.btn_theme_light -> AppPreferences.ThemeMode.LIGHT
                R.id.btn_theme_dark -> AppPreferences.ThemeMode.DARK
                else -> AppPreferences.ThemeMode.SYSTEM
            }
            // setDefaultNightMode 会由 AppCompat 统一重建界面，无需手动 recreate
            AppPreferences.setThemeMode(this, mode)
        }
    }

    private fun setupDynamicColorSwitch() {
        binding.switchDynamicColor.isChecked = DynamicColor.enabled(this)
        binding.switchDynamicColor.setOnCheckedChangeListener { _, checked ->
            DynamicColor.setEnabled(this, checked)
            // DynamicColor.apply 在 onCreate 中执行，重建后立即生效
            recreate()
        }
    }

    private fun setupTimeFormatSection() {
        val current = AppPreferences.timeFormat(this)
        suppressTimeCallback = true
        binding.toggleTimeFormat.check(
            if (current == AppPreferences.TimeFormat.ABSOLUTE) {
                R.id.btn_time_absolute
            } else {
                R.id.btn_time_relative
            }
        )
        suppressTimeCallback = false

        binding.toggleTimeFormat.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || suppressTimeCallback) return@addOnButtonCheckedListener
            val format = if (checkedId == R.id.btn_time_absolute) {
                AppPreferences.TimeFormat.ABSOLUTE
            } else {
                AppPreferences.TimeFormat.RELATIVE
            }
            AppPreferences.setTimeFormat(this, format)
        }
    }

    private fun setupShowPackageSwitch() {
        binding.switchShowPackage.isChecked = AppPreferences.showPackageName(this)
        binding.switchShowPackage.setOnCheckedChangeListener { _, checked ->
            AppPreferences.setShowPackageName(this, checked)
        }
    }

    private fun setupRetentionRow() {
        updateRetentionLabel()
        binding.rowRetention.setOnClickListener { showRetentionDialog() }
    }

    private fun updateRetentionLabel() {
        val limit = AppPreferences.retentionLimit(this)
        binding.tvRetentionValue.text = if (limit <= 0) {
            getString(R.string.settings_retention_unlimited)
        } else {
            getString(R.string.settings_retention_unit, limit)
        }
    }

    private fun showRetentionDialog() {
        val options = AppPreferences.RETENTION_OPTIONS
        val labels = options.map { limit ->
            if (limit <= 0) {
                getString(R.string.settings_retention_unlimited)
            } else {
                getString(R.string.settings_retention_unit, limit)
            }
        }.toTypedArray()

        val current = AppPreferences.retentionLimit(this)
        val checkedIndex = options.indexOf(current).let { if (it < 0) options.lastIndex else it }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_retention_dialog_title)
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                val limit = options[which]
                AppPreferences.setRetentionLimit(this, limit)
                updateRetentionLabel()
                lifecycleScope.launch {
                    runCatching { repository.applyRetention(limit) }
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun setupPermissionSection() {
        binding.btnOpenSettings.setOnClickListener {
            if (!NotificationAccess.openSettings(this)) {
                Snackbar.make(
                    binding.root,
                    R.string.permission_settings_unavailable,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupStorageSection() {
        binding.btnClearAll.setOnClickListener {
            if (recordCount <= 0) {
                Snackbar.make(binding.root, R.string.empty_title, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_clear_title)
                .setMessage(getString(R.string.dialog_clear_message, recordCount))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_clear) { _, _ ->
                    lifecycleScope.launch {
                        val deleted = runCatching { repository.clearAll() }.getOrDefault(0)
                        if (isFinishing || isDestroyed) return@launch
                        Snackbar.make(
                            binding.root,
                            getString(R.string.msg_cleared, deleted),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
                .show()
        }
    }

    private fun setupAboutSection() {
        @Suppress("DEPRECATION")
        val versionName = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull()

        binding.tvVersion.text = if (versionName.isNullOrBlank()) {
            getString(R.string.settings_version_unknown)
        } else {
            getString(R.string.settings_version, versionName)
        }
    }

    // ---------------------------------------------------------------- 状态

    private fun refreshPermissionState() {
        val granted = NotificationAccess.isGranted(this)
        binding.tvPermissionState.setText(
            if (granted) R.string.settings_permission_granted else R.string.settings_permission_denied
        )
        binding.ivPermissionState.setImageResource(
            if (granted) R.drawable.ic_check_circle_24 else R.drawable.ic_error_24
        )
        binding.ivPermissionState.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                this,
                if (granted) R.color.md_theme_primary else R.color.md_theme_error
            )
        )
    }

    private fun observeRecordCount() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeCount().collect { count ->
                    recordCount = count
                    binding.tvRecordCount.text = getString(R.string.settings_record_count, count)
                }
            }
        }
    }
}
