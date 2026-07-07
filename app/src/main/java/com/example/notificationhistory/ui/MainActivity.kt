package com.example.notificationhistory.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.notificationhistory.R
import com.example.notificationhistory.adapter.NotificationAdapter
import com.example.notificationhistory.data.AppDatabase
import com.example.notificationhistory.data.NotificationEntity
import com.example.notificationhistory.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupPermissionCard()
        setupClearAllFab()
        setupBackPressedCallback()

        checkNotificationPermission()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
        }
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            itemAnimator = null
        }

        lifecycleScope.launch {
            AppDatabase.getInstance(this@MainActivity)
                .notificationDao()
                .getAll()
                .collectLatest { list ->
                    adapter.submitList(list)
                    updateEmptyState(list)
                }
        }
    }

    private fun setupPermissionCard() {
        binding.apply {
            tvStatus.setTextColor(getColor(R.color.md_theme_onSecondaryContainer))
            btnPermission.setOnClickListener {
                // 跳转到通知监听设置页面
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
    }

    private fun setupClearAllFab() {
        binding.fabClearAll.setOnClickListener {
            showClearAllDialog()
        }
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isNotificationServiceEnabled()) {
                        finish()
                    } else {
                        Snackbar.make(
                            binding.root,
                            "请先开启通知权限",
                            Snackbar.LENGTH_LONG
                        )
                            .setAction("去设置") {
                                startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                )
                            }
                            .show()
                    }
                }
            }
        )
    }

    private fun checkNotificationPermission() {
        if (isNotificationServiceEnabled()) {
            updatePermissionStatus(true)
        } else {
            updatePermissionStatus(false)
        }
    }

    private fun updatePermissionStatus(enabled: Boolean) {
        with(binding) {
            if (enabled) {
                cardStatus.visibility = View.GONE
            } else {
                cardStatus.visibility = View.VISIBLE
                tvStatus.text = "需要开启「通知使用权」以读取通知历史"
                tvStatus.setTextColor(getColor(R.color.md_theme_onSecondaryContainer))
                ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                ivStatusIcon.setImageTintList(
                    getColorStateList(R.color.md_theme_onSecondaryContainer)
                )
                btnPermission.text = "前往开启"
                btnPermission.isEnabled = true
                btnPermission.isClickable = true
            }
        }
    }

    private fun showClearAllDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("清空通知历史")
            .setMessage("确定要清空所有通知记录吗？此操作不可撤销。")
            .setPositiveButton("清空") { _, _ ->
                clearAllNotifications()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun clearAllNotifications() {
        lifecycleScope.launch {
            AppDatabase.getInstance(this@MainActivity).notificationDao().deleteAll()
            Snackbar.make(
                binding.root,
                "已清空所有通知历史",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateEmptyState(list: List<NotificationEntity>) {
        val hasItems = list.isNotEmpty()
        binding.emptyState.visibility = if (hasItems) View.GONE else View.VISIBLE
        binding.recyclerView.visibility = if (hasItems) View.VISIBLE else View.GONE
        binding.fabClearAll.visibility = if (hasItems) View.VISIBLE else View.GONE
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val cn =
            ComponentName(
                this,
                "com.example.notificationhistory.service.NotificationService"
            )
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && TextUtils.split(flat, ":").any { it == cn.flattenToString() }
    }
}
