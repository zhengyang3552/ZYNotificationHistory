package com.example.notificationhistory.ui

import android.app.Activity
import android.content.Context
import android.os.Build
import com.google.android.material.color.DynamicColors

/**
 * 动态取色（参考 rikkahub 的"壁纸取色 + 种子色回退"方案，View 体系实现）。
 *
 * Android 12+ 且用户开启时用 Material DynamicColors 以壁纸取色包装当前主题；
 * 其余情况（低版本或开关关闭）保持 values/ 下的统一品牌种子色方案。
 * 按需在 Activity onCreate 中调用 [apply]（须在 super.onCreate 之前），
 * 因此设置页切换开关后 recreate() 即可立即生效，无需重启应用。
 */
object DynamicColor {

    private const val PREFS_NAME = "notification_prefs"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color"

    fun enabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DYNAMIC_COLOR, false)

    fun setEnabled(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()
    }

    fun apply(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && enabled(activity)) {
            DynamicColors.applyToActivityIfAvailable(activity)
        }
    }
}
