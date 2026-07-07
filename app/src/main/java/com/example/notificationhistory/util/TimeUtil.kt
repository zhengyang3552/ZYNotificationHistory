package com.example.notificationhistory.util

import android.text.format.DateFormat
import java.util.concurrent.TimeUnit

object TimeUtil {
    fun formatAbsolute(timestamp: Long): String {
        return DateFormat.format("yyyy-MM-dd HH:mm:ss", timestamp).toString()
    }

    fun formatRelative(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            seconds < 10 -> "刚刚"
            seconds < 60 -> "${seconds}秒前"
            minutes < 60 -> "${minutes}分钟前"
            hours < 24 -> "${hours}小时前"
            days < 30 -> "${days}天前"
            else -> formatAbsolute(timestamp)
        }
    }
}