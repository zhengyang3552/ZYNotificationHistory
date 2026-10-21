package com.example.notificationhistory.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

/**
 * 应用偏好设置的唯一入口。
 *
 * 改造前主题/时间格式的 key 散落在 Activity、Adapter 中（且存在“有人读、无人写”的
 * 孤儿配置 show_package_name），这里统一收敛：
 * - key 常量化，避免字符串拼写漂移；
 * - 读取时带默认值，配置缺失或非法值不会导致崩溃；
 * - 主题变更同时落盘并立即生效。
 */
object AppPreferences {

    const val FILE_NAME = "notification_prefs"

    // 保留原有 key，保证老版本升级后配置不丢失
    private const val KEY_THEME = "theme_mode"
    private const val KEY_TIME_FORMAT = "default_time_format"
    private const val KEY_SHOW_PACKAGE = "show_package_name"
    private const val KEY_RETENTION = "retention_limit"

    enum class ThemeMode(val value: String) {
        SYSTEM("system"),
        LIGHT("light"),
        DARK("dark");

        companion object {
            fun from(value: String?): ThemeMode =
                ThemeMode.entries.firstOrNull { it.value == value } ?: SYSTEM
        }
    }

    enum class TimeFormat(val value: String) {
        RELATIVE("relative"),
        ABSOLUTE("absolute");

        companion object {
            fun from(value: String?): TimeFormat =
                TimeFormat.entries.firstOrNull { it.value == value } ?: RELATIVE
        }
    }

    /** 保留上限候选值，0 表示不限制 */
    val RETENTION_OPTIONS = intArrayOf(500, 1_000, 5_000, 0)

    const val DEFAULT_RETENTION = 5_000

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun themeMode(context: Context): ThemeMode {
        val raw = runCatching { prefs(context).getString(KEY_THEME, null) }.getOrNull()
        return ThemeMode.from(raw)
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        runCatching { prefs(context).edit().putString(KEY_THEME, mode.value).apply() }
        applyNightMode(mode)
    }

    /** 冷启动时调用：把已保存的模式同步给 AppCompatDelegate */
    fun applyTheme(context: Context) = applyNightMode(themeMode(context))

    private fun applyNightMode(mode: ThemeMode) {
        val nightMode = when (mode) {
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }

    fun timeFormat(context: Context): TimeFormat {
        val raw = runCatching { prefs(context).getString(KEY_TIME_FORMAT, null) }.getOrNull()
        return TimeFormat.from(raw)
    }

    fun setTimeFormat(context: Context, format: TimeFormat) {
        runCatching { prefs(context).edit().putString(KEY_TIME_FORMAT, format.value).apply() }
    }

    fun showPackageName(context: Context): Boolean =
        runCatching { prefs(context).getBoolean(KEY_SHOW_PACKAGE, true) }.getOrDefault(true)

    fun setShowPackageName(context: Context, show: Boolean) {
        runCatching { prefs(context).edit().putBoolean(KEY_SHOW_PACKAGE, show).apply() }
    }

    /** 返回 >0 的上限；0 表示不限制 */
    fun retentionLimit(context: Context): Int {
        val value = runCatching { prefs(context).getInt(KEY_RETENTION, DEFAULT_RETENTION) }
            .getOrDefault(DEFAULT_RETENTION)
        return if (value < 0) DEFAULT_RETENTION else value
    }

    fun setRetentionLimit(context: Context, limit: Int) {
        runCatching { prefs(context).edit().putInt(KEY_RETENTION, limit.coerceAtLeast(0)).apply() }
    }
}
