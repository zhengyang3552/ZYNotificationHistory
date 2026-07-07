package com.example.notificationhistory.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.notificationhistory.data.AppDatabase
import com.example.notificationhistory.data.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationService : NotificationListenerService() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.IO)

    /**
     * 根据 key 删除通知栏中的通知（外部调用入口）
     */
    fun deleteByKey(key: String) {
        cancelNotification(key)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val packageName = sbn.packageName
        val isOngoing = sbn.isOngoing

        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val title = extras.getString("android.title") ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""

            // 保存 key（StatusBarNotification.key 是精确标识，用于后续删除）
            val entityKey = sbn.key

            val entity = NotificationEntity(
                key = entityKey,
                packageName = packageName,
                appName = appName,
                title = title,
                content = text,
                isOngoing = isOngoing,
                timestamp = sbn.postTime.coerceAtLeast(System.currentTimeMillis() - 10000)
            )

            scope.launch {
                val db = AppDatabase.getInstance(this@NotificationService)
                if (isOngoing) {
                    // Check if an ongoing notification for this package already exists
                    val existing = db.notificationDao().findActiveOngoingByPackage(packageName)
                    if (existing != null) {
                        // Update existing record without changing its original timestamp
                        val updated = existing.copy(
                            key = entityKey,
                            title = title,
                            content = text,
                            // keep isDeleted false and isOngoing true
                            isDeleted = false,
                            isOngoing = true
                        )
                        db.notificationDao().update(updated)
                    } else {
                        // Insert new ongoing notification
                        db.notificationDao().insert(entity)
                    }
                } else {
                    // For non-ongoing notifications, simply insert
                    db.notificationDao().insert(entity)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.isOngoing) {
            scope.launch {
                val db = AppDatabase.getInstance(this@NotificationService)
                val entity = db.notificationDao().findActiveOngoingByPackage(sbn.packageName)
                if (entity != null) {
                    db.notificationDao().markAsDeletedAndDowngrade(entity.id)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
