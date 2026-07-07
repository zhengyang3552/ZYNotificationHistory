package com.example.notificationhistory.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [Index(value = ["key"], unique = true)]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val key: String = "",                    // StatusBarNotification.key，用于精确删除
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val isOngoing: Boolean = false,
    val isDeleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
