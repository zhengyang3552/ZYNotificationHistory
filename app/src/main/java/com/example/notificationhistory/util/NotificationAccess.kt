package com.example.notificationhistory.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.notificationhistory.service.NotificationService

/**
 * 通知使用权（NotificationListenerService 授权）相关工具。
 *
 * 改造前通过读取 Secure 设置里的 enabled_notification_listeners 字符串并做
 * ComponentName.flattenToString() 全等比较，遇到系统写入短类名（pkg/.Service）
 * 时会误判为未授权；这里改用官方 API 判断，并提供多级跳转兜底。
 */
object NotificationAccess {

    private const val TAG = "NotificationAccess"

    fun isGranted(context: Context): Boolean = runCatching {
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }.onFailure { Log.w(TAG, "读取通知使用权状态失败", it) }
        .getOrDefault(false)

    /**
     * 依次尝试：本应用的授权详情页 -> 系统通知使用权列表 -> 应用详情页。
     * @return 是否成功跳转
     */
    fun openSettings(activity: Activity): Boolean {
        val listeners = listOfNotNull(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                    putExtra(
                        Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        ComponentName(activity, NotificationService::class.java).flattenToString()
                    )
                }
            } else {
                null
            },
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", activity.packageName, null)
            )
        )

        for (intent in listeners) {
            try {
                activity.startActivity(intent)
                return true
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "系统设置页不可用: ${intent.action}")
            } catch (e: SecurityException) {
                Log.w(TAG, "无权打开系统设置页: ${intent.action}")
            }
        }
        return false
    }
}
