package com.example.notificationhistory.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    /** 常驻通知优先、其余按时间倒序（与原行为一致） */
    @Query(
        """
        SELECT * FROM notifications
        ORDER BY CASE WHEN isOngoing = 1 AND isDeleted = 0 THEN 1 ELSE 0 END DESC,
                 timestamp DESC
        """
    )
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun count(): Int

    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    suspend fun findById(id: Int): NotificationEntity?

    @Query("SELECT * FROM notifications WHERE `key` = :key LIMIT 1")
    suspend fun findByKey(key: String): NotificationEntity?

    @Query(
        """
        SELECT * FROM notifications
        WHERE packageName = :packageName AND isOngoing = 1 AND isDeleted = 0
        LIMIT 1
        """
    )
    suspend fun findActiveOngoingByPackage(packageName: String): NotificationEntity?

    /** 同应用同名会话的最新一条普通记录，用于重复上报（重连重放）去重 */
    @Query(
        """
        SELECT * FROM notifications
        WHERE packageName = :packageName AND title = :title
          AND isOngoing = 0 AND isDeleted = 0
        ORDER BY timestamp DESC
        LIMIT 1
        """
    )
    suspend fun findLatestByPackageAndTitle(packageName: String, title: String): NotificationEntity?

    /**
     * IGNORE：同一 key 重复上报时不再抛 SQLiteConstraintException
     *（改造前使用纯 @Insert + key 唯一索引，通知更新会直接抛异常导致整条记录丢失）。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: NotificationEntity): Long

    /** REPLACE：用于撤销删除时按原 id 回填 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: NotificationEntity): Long

    @Update
    suspend fun update(entity: NotificationEntity)

    @Query("UPDATE notifications SET isDeleted = 1, isOngoing = 0 WHERE id = :id")
    suspend fun markAsRemoved(id: Int)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Int): Int

    @Query("DELETE FROM notifications")
    suspend fun deleteAll(): Int

    /** 只保留最新 [keep] 条，其余删除 */
    @Query(
        """
        DELETE FROM notifications
        WHERE id NOT IN (
            SELECT id FROM notifications ORDER BY timestamp DESC LIMIT :keep
        )
        """
    )
    suspend fun trimTo(keep: Int): Int
}
