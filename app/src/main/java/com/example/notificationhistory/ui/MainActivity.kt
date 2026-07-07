package com.example.notificationhistory.ui

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import java.lang.reflect.Method
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
import com.example.notificationhistory.service.NotificationService
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
        adapter = NotificationAdapter(
            onRemoveNotification = { entity ->
                removeNotificationFromBar(entity)
            },
            onViewNotification = { entity ->
                viewNotification(entity)
            }
        )
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
                tvStatus.text = "通知权限未开启，请点击开启"
                tvStatus.setTextColor(getColor(R.color.md_theme_onSecondaryContainer))
                ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                ivStatusIcon.setImageTintList(
                    getColorStateList(R.color.md_theme_onSecondaryContainer)
                )
                btnPermission.text = "去开启"
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

    /**
     * Helper function to call NotificationListenerService.getService() via reflection.
     * getService() is a hidden API, so we must use reflection.
     */
    private fun getNotificationListenerService(): Any? {
        return try {
            val getServiceMethod = NotificationListenerService::class.java.getDeclaredMethod(
                "getService",
                Context::class.java,
                ComponentName::class.java
            )
            getServiceMethod.isAccessible = true
            getServiceMethod.invoke(null, this, ComponentName(this, NotificationService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Remove notification from notification bar using NotificationListenerService
     */
    private fun removeNotificationFromBar(entity: NotificationEntity) {
        try {
            val service = getNotificationListenerService()
            if (service != null) {
                val sbn = findStatusBarNotification(entity.packageName, entity.title, entity.content)
                if (sbn != null) {
                    val cancelNotificationMethod = service.javaClass.getMethod(
                        "cancelNotification",
                        String::class.java,
                        String::class.java,
                        Int::class.java
                    )
                    cancelNotificationMethod.invoke(service, sbn.packageName, sbn.tag, sbn.id)
                    Snackbar.make(
                        binding.root,
                        "已从通知栏移除",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    Snackbar.make(
                        binding.root,
                        "通知可能已不存在于通知栏",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            Snackbar.make(
                binding.root,
                "移除失败: ${e.message}",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Find StatusBarNotification by package, title and content
     */
    private fun findStatusBarNotification(
        packageName: String,
        title: String,
        content: String
    ): android.service.notification.StatusBarNotification? {
        try {
            val service = getNotificationListenerService()
            if (service != null) {
                val activeNotificationsField = service.javaClass.getField("activeNotifications")
                @Suppress("UNCHECKED_CAST")
                val activeNotifications = activeNotificationsField.get(service) as List<android.service.notification.StatusBarNotification>
                for (sbn in activeNotifications) {
                    if (sbn.packageName == packageName) {
                        val sbnTitle = sbn.notification.extras.getString("android.title") ?: ""
                        val sbnContent = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: ""
                        if (sbnTitle == title || sbnContent == content) {
                            return sbn
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * View notification - simulate clicking on notification by launching the app
     * If the app doesn't have a launcher intent, show app details page
     */
    private fun viewNotification(entity: NotificationEntity) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(entity.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                Snackbar.make(
                    binding.root,
                    "正在打开 ${entity.appName}",
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                // If no launcher intent (e.g., system service), show app details page
                showAppDetailsPage(entity.packageName)
            }
        } catch (e: Exception) {
            Snackbar.make(
                binding.root,
                "查看失败: ${e.message}",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Show app details page for the given package
     */
    private fun showAppDetailsPage(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            Snackbar.make(
                binding.root,
                "正在打开应用详情: ${getAppNameFromPackage(packageName)}",
                Snackbar.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Snackbar.make(
                binding.root,
                "无法打开应用详情: ${getAppNameFromPackage(packageName)}",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Get app name from package name
     */
    private fun getAppNameFromPackage(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
