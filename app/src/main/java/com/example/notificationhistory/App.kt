package com.example.notificationhistory

import android.app.Application
import android.util.Log
import com.example.notificationhistory.data.AppDatabase
import com.example.notificationhistory.util.AppPreferences

/**
 * 应用入口。
 *
 * 主题必须在任何 Activity 创建前应用：改造前只有 SettingsActivity 会读取主题偏好，
 * 冷启动进入主界面时约有一帧仍是系统主题，深色用户会看到明显的白底闪烁。
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        AppPreferences.applyTheme(this)

        runCatching { AppDatabase.getInstance(this) }
            .onFailure { Log.e(TAG, "数据库预热失败，将在首次访问时重试", it) }
    }

    private companion object {
        const val TAG = "App"
    }
}
