package com.example.notificationhistory.ui

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.Toast
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
    private lateinit var notificationReceiver: BroadcastReceiver

    // Action constants
    private companion object {
        const val ACTION_DELETE_NOTIFICATION = "com.example.notificationhistory.ACTION_DELETE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupPermissionCard()
        setupClearAllFab()
        setupBackPressedCallback()
        setupDeleteReceiver()  // 设置广播接收器

        checkNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        // 注册广播接收器，接收删除通知的请求
        ContextCompat.registerReceiver(
            this,
            notificationReceiver,
            IntentFilter(ACTION_DELETE_NOTIFICATION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        // 取消注册广播接收器
        unregisterReceiver(notificationReceiver)
    }

    /**
     * 设置删除通知的广播接收器
     * 当从适配器接收到删除请求时，直接调用 Service 删除
     */
    private fun setupDeleteReceiver() {
        notificationReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_DELETE_NOTIFICATION) {
                    val key = intent.getStringExtra("key")
                    if (!key.isNullOrBlank()) {
                        deleteNotificationByKey(key)
                    }
                }
            }
        }
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
                // 通过广播发送删除请求
                sendDeleteBroadcast(entity.key)
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

    /**
     * 发送删除通知的广播
     */
    private fun sendDeleteBroadcast(key: String) {
        val intent = Intent(ACTION_DELETE_NOTIFICATION).apply {
            putExtra("key", key)
        }
        sendBroadcast(intent)
    }

    /**
     * 通过 key 直接删除通知栏中的通知
     */
    private fun deleteNotificationByKey(key: String) {
        try {
            val service = getNotificationListenerService()
            if (service != null) {
                // 调用 NotificationService 的 deleteByKey 方法
                val deleteMethod = service.javaClass.getMethod("deleteByKey", String::class.java)
                deleteMethod.invoke(service, key)
                Snackbar.make(
                    binding.root,
                    "已从通知栏移除",
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                Snackbar.make(
                    binding.root,
                    "无法获取通知服务",
                    Snackbar.LENGTH_SHORT
                ).show()
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
     * Helper function to call NotificationListenerService.getService() via reflection.
     * getService() is a hidden API, so we must use reflection.
     */
    private fun getNotificationListenerService(): Any? {
        return try {
            val getServiceMethod = android.service.notification.NotificationListenerService::class.java.getDeclaredMethod(
                "getService",
                Context::class.java,
                android.content.ComponentName::class.java
            )
            getServiceMethod.isAccessible = true
            getServiceMethod.invoke(null, this, android.content.ComponentName(this, NotificationService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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
