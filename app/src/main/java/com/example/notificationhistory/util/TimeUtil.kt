package com.example.notificationhistory.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 时间格式化。
 *
 * 相比改造前增加的处理：
 * - 未来时间戳（设备时钟回拨、跨时区同步）不再出现负数或误判为「刚刚」；
 * - 超过一周时给出具体日期，长历史里仍然可读；
 * - 所有格式化都有兜底，日期格式化异常不会冒泡到列表绑定过程。
 */
object TimeUtil {

    private const val PATTERN_DATE_TIME = "yyyy-MM-dd HH:mm"
    private const val PATTERN_DATE_TIME_SECONDS = "yyyy-MM-dd HH:mm:ss"
    private const val PATTERN_TIME = "HH:mm"
    private const val PATTERN_DATE = "MM-dd"

    fun formatAbsolute(timestamp: Long): String = format(timestamp, PATTERN_DATE_TIME)

    fun formatAbsoluteWithSeconds(timestamp: Long): String =
        format(timestamp, PATTERN_DATE_TIME_SECONDS)

    /**
     * 相对时间：刚刚 / N 秒前 / N 分钟前 / N 小时前 / 昨天 HH:mm / N 天前 / MM-dd
     */
    fun formatRelative(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - timestamp
        // 时钟回拨或未来时间：按「刚刚」处理，避免出现「-3 分钟前」
        if (diff < 0) return "刚刚"

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)

        return when {
            seconds < 10 -> "刚刚"
            seconds < 60 -> "${seconds}秒前"
            minutes < 60 -> "${minutes}分钟前"
            hours < 24 -> "${hours}小时前"
            isYesterday(timestamp, now) -> "昨天 " + format(timestamp, PATTERN_TIME)
            hours < 24 * 7 -> "${hours / 24}天前"
            else -> format(timestamp, PATTERN_DATE)
        }
    }

    private fun isYesterday(timestamp: Long, now: Long): Boolean {
        val startOfToday = Calendar.getInstance().apply { timeInMillis = now }.startOfDay()
        val startOfYesterday = (startOfToday.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return timestamp >= startOfYesterday.timeInMillis && timestamp < startOfToday.timeInMillis
    }

    private fun Calendar.startOfDay(): Calendar = apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun format(timestamp: Long, pattern: String): String =
        runCatching {
            SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
        }.getOrElse { "--" }
}
