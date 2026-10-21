package com.example.notificationhistory.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.notificationhistory.R
import com.example.notificationhistory.data.NotificationEntity
import com.example.notificationhistory.data.NotificationRepository
import com.example.notificationhistory.util.AppPreferences
import com.example.notificationhistory.util.NotificationContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 通知监听服务。
 *
 * 相比改造前补齐的边界处理：
 * - 回调参数可能为空（系统在服务销毁边缘回调时会出现），全部做空值防御；
 * - 应用信息读取失败（应用被卸载、包名不可见）时回退为包名，不再整条记录丢失；
 * - 同一 key 的重复上报、常驻通知换 key 等场景交由仓库层去重，写入不再抛约束异常；
 * - postTime 异常（未来时间/0）时回退为当前时间，避免时间线错乱；
 * - 无标题且无正文的通知（系统占位、纯图标通知）不入库，减少噪音；
 * - 写入失败只记录日志，不影响监听服务本身继续工作。
 */
class NotificationService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val repository: NotificationRepository by lazy { NotificationRepository(this) }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val posted = sbn ?: return
        // 记录自己产生的通知没有意义
        if (posted.packageName == packageName) return

        val text = NotificationContent.extract(posted)
        if (text.title.isEmpty() && text.content.isEmpty()) return

        val entity = buildEntity(posted, text)
        val limit = AppPreferences.retentionLimit(this)

        scope.launch {
            runCatching { repository.savePosted(entity, limit) }
                .onFailure { Log.w(TAG, "保存通知失败: ${entity.key}", it) }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val removed = sbn ?: return
        val pkg = removed.packageName ?: return
        val key = removed.key ?: return

        scope.launch {
            runCatching { repository.markRemoved(key, pkg) }
                .onFailure { Log.w(TAG, "标记通知移除失败: $key", it) }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "通知监听已连接")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "通知监听已断开")
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildEntity(
        sbn: StatusBarNotification,
        text: NotificationContent.Text
    ): NotificationEntity {
        val now = System.currentTimeMillis()
        val timestamp = sbn.postTime.takeIf { it in 1..now } ?: now
        val packageName = sbn.packageName.orEmpty()
        // key 为空时用「包名 + 通知 id + 时间」兜底，避免唯一索引把所有空 key 合并成一条
        val key = sbn.key?.takeIf { it.isNotBlank() }
            ?: "$packageName#${sbn.id}#$timestamp"

        return NotificationEntity(
            key = key,
            packageName = packageName,
            appName = resolveAppName(packageName),
            title = text.title,
            content = text.content,
            isOngoing = sbn.isOngoing,
            timestamp = timestamp
        )
    }

    @Suppress("DEPRECATION")
    private fun resolveAppName(packageName: String): String {
        if (packageName.isEmpty()) return getString(R.string.unknown_app)
        return runCatching {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrNull()?.takeIf { it.isNotBlank() }
            // 应用可能已被卸载或当前用户不可见，退化为包名，至少不丢记录
            ?: packageName
    }

    private companion object {
        const val TAG = "NotificationService"
    }
}
