package com.example.notificationhistory.ui

import com.example.notificationhistory.data.NotificationEntity

/**
 * 主列表的展示项模型。
 *
 * 与数据库记录一一对应的[Single]之外，同一应用的普通通知会聚合为[Stack]：
 * 卡片默认只展示最新一条，其余作为「历史消息」折叠在堆叠中，
 * 视觉上复刻应用图标「消息流堆叠」的层次效果。
 * 常驻通知不参与堆叠，保持独立置顶卡片。
 */
sealed class DisplayItem {

    /** 稳定标识，供 DiffUtil 与展开状态记忆使用 */
    abstract val key: String

    /** 独立卡片：单条普通通知或常驻通知 */
    data class Single(val record: NotificationEntity) : DisplayItem() {
        override val key: String = "single-${record.id}"
    }

    /**
     * 堆叠卡片：同一应用的多条通知，[records] 按时间倒序，
     * 第一条为最新消息（堆叠正面），其余为展开后的历史消息。
     */
    data class Stack(
        val packageName: String,
        val records: List<NotificationEntity>
    ) : DisplayItem() {
        override val key: String = "stack-$packageName"
        val latest: NotificationEntity get() = records.first()
        val history: List<NotificationEntity> get() = records.drop(1)
    }

    companion object {

        /**
         * 把按时间倒序的记录流组装为展示项。
         * 常驻（未删除）记录保持独立置顶；其余按包名归组，
         * 组的先后顺序跟随组内最新一条的时间（即输入流顺序）。
         */
        fun from(records: List<NotificationEntity>): List<DisplayItem> {
            val items = mutableListOf<DisplayItem>()
            val groups = LinkedHashMap<String, MutableList<NotificationEntity>>()
            for (record in records) {
                if (record.isOngoing && !record.isDeleted) {
                    items += Single(record)
                    continue
                }
                groups.getOrPut(record.packageName) { mutableListOf() }.add(record)
            }
            for ((packageName, list) in groups) {
                items += if (list.size > 1) {
                    Stack(packageName, list)
                } else {
                    Single(list.first())
                }
            }
            return items
        }
    }
}
