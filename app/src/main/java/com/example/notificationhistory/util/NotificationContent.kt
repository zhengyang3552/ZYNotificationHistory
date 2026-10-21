package com.example.notificationhistory.util

import android.app.Notification
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * 从 StatusBarNotification 中提取可读文本。
 *
 * 改造前的实现只读取 android.title / android.text，遇到以下场景会得到空内容：
 * - 聊天类通知使用 MessagingStyle，正文在 android.messages 里；
 * - 长文本使用 BigTextStyle，正文在 android.bigText 里；
 * - 摘要类通知使用 InboxStyle，正文在 android.textLines 里；
 * - android.title 在部分系统上是 CharSequence 子类（SpannableString），直接 getString 会拿到 null。
 * 这里按优先级逐个回退，并对最终文本做长度截断与空白折叠。
 */
object NotificationContent {

    private const val TAG = "NotificationContent"

    /** 单条记录正文最多保留的字符数，避免超大文本撑爆数据库与列表 */
    private const val MAX_TEXT_LENGTH = 2_000
    private const val MAX_TITLE_LENGTH = 200

    data class Text(val title: String, val content: String)

    fun extract(sbn: StatusBarNotification?): Text {
        val notification = sbn?.notification ?: return Text("", "")
        val extras = notification.extras ?: return Text("", "")

        val title = sequenceOf(
            Notification.EXTRA_TITLE,
            Notification.EXTRA_TITLE_BIG
        ).firstNotNullOfOrNull { readCharSequence(extras, it) }

        val content = sequenceOf(
            Notification.EXTRA_TEXT,
            Notification.EXTRA_BIG_TEXT,
            Notification.EXTRA_SUMMARY_TEXT
        ).firstNotNullOfOrNull { readCharSequence(extras, it) }
            ?: readLastMessage(extras)
            ?: readTextLines(extras)

        return Text(
            title = (title ?: "").take(MAX_TITLE_LENGTH),
            content = (content ?: "").take(MAX_TEXT_LENGTH)
        )
    }

    private fun readCharSequence(extras: Bundle, key: String): String? = runCatching {
        extras.getCharSequence(key)?.toString()
    }.onFailure { Log.w(TAG, "读取 $key 失败", it) }
        .getOrNull()
        ?.let { normalize(it) }
        ?.takeIf { it.isNotEmpty() }

    /** MessagingStyle：取最后一条消息的正文 */
    private fun readLastMessage(extras: Bundle): String? = runCatching {
        val messages = readParcelableArray(extras, Notification.EXTRA_MESSAGES)
            ?: return@runCatching null
        for (index in messages.indices.reversed()) {
            val bundle = messages[index] as? Bundle ?: continue
            val text = bundle.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?.let { normalize(it) }
            if (!text.isNullOrEmpty()) return@runCatching text
        }
        null
    }.onFailure { Log.w(TAG, "读取 MessagingStyle 失败", it) }
        .getOrNull()

    /** InboxStyle：把多行摘要合并展示 */
    private fun readTextLines(extras: Bundle): String? = runCatching {
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?: return@runCatching null
        lines.asSequence()
            .mapNotNull { it?.toString()?.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" · ")
            .takeIf { it.isNotEmpty() }
    }.onFailure { Log.w(TAG, "读取 InboxStyle 失败", it) }
        .getOrNull()

    /** API 33 起 Parcelable 数组读取需要显式指定类型，避免旧接口的废弃调用 */
    private fun readParcelableArray(extras: Bundle, key: String): Array<out Parcelable>? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelableArray(key, Parcelable::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelableArray(key)
        }

    private fun normalize(raw: String): String = raw
        .replace('\u0000', ' ')
        .replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}
