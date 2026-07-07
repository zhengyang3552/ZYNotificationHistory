package com.example.notificationhistory.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY CASE WHEN isOngoing = 1 AND isDeleted = 0 THEN 1 ELSE 0 END DESC, timestamp DESC")
    fun getAll(): Flow<List<NotificationEntity>>

    @Insert
    suspend fun insert(notification: NotificationEntity)

    @Update
    suspend fun update(notification: NotificationEntity)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()

    @Query("DELETE FROM notifications WHERE packageName = :packageName AND isOngoing = 1 AND isDeleted = 0")
    suspend fun deleteOngoingByPackage(packageName: String)

    @Query("SELECT * FROM notifications WHERE packageName = :packageName AND isOngoing = 1 AND isDeleted = 0 LIMIT 1")
    suspend fun findActiveOngoingByPackage(packageName: String): NotificationEntity?

    @Query("UPDATE notifications SET isDeleted = 1, isOngoing = 0 WHERE id = :id")
    suspend fun markAsDeletedAndDowngrade(id: Int)
}
