package com.example.notificationhistory.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

/**
 * 通知记录仓库：把“上报/移除/去重/清理”这类涉及多步写入的逻辑从 Service 与 UI 中抽离。
 *
 * 关键规则：
 * 1. 同一 [NotificationEntity.key] 再次上报视为同一条通知的更新（进度条、聊天串、常驻通知
 *    都会复用同一个 key），只刷新文案与状态；内容有变化且非常驻时把时间刷新为当前时间
 *    （微信等连续消息的通知 postTime 停留在第一条，需记为最后一条的接收时间），
 *    常驻通知保留首次记录时间，避免列表在后台更新时来回跳动；
 * 2. 常驻通知在部分应用上每次刷新都会换 key，因此再按包名兜底匹配，避免同一常驻通知刷出多条；
 * 3. 所有写操作放在事务里，配合 IGNORE 插入策略，重复上报不会抛约束异常。
 */
class NotificationRepository(context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val dao = database.notificationDao()

    fun observeAll(): Flow<List<NotificationEntity>> = dao.observeAll()

    fun observeCount(): Flow<Int> = dao.observeCount()

    suspend fun savePosted(entity: NotificationEntity, retentionLimit: Int) {
        database.withTransaction {
            val byKey = dao.findByKey(entity.key)
            when {
                byKey != null -> {
                    val changed = byKey.title != entity.title || byKey.content != entity.content
                    dao.update(
                        byKey.copy(
                            title = entity.title,
                            content = entity.content,
                            isOngoing = entity.isOngoing,
                            isDeleted = false,
                            // 聊天类通知（如微信连续消息）原地更新时，通知自带的 postTime
                            // 仍停留在第一条消息的时间；内容有变化说明来了新消息，
                            // 改用当前时间，保证记录的是最后一条消息的接收时间。
                            // 常驻通知（下载进度等）保持首次记录时间，避免列表来回跳动。
                            timestamp = if (changed && !byKey.isOngoing && !entity.isOngoing) {
                                System.currentTimeMillis()
                            } else {
                                byKey.timestamp
                            }
                        )
                    )
                }

                entity.isOngoing -> {
                    val ongoing = dao.findActiveOngoingByPackage(entity.packageName)
                    if (ongoing != null) {
                        dao.update(
                            ongoing.copy(
                                key = entity.key,
                                title = entity.title,
                                content = entity.content,
                                isOngoing = true,
                                isDeleted = false
                            )
                        )
                    } else {
                        dao.insertIgnore(entity)
                    }
                }

                else -> dao.insertIgnore(entity)
            }

            // 先判断是否真的超额，避免每条通知都执行一次 DELETE ... NOT IN (子查询)
            if (retentionLimit > 0 && dao.count() > retentionLimit) {
                dao.trimTo(retentionLimit)
            }
        }
    }

    /** 通知从通知栏消失：常驻记录降级为「已移除」，其余记录保留为历史 */
    suspend fun markRemoved(key: String, packageName: String) {
        database.withTransaction {
            val byKey = dao.findByKey(key)
            if (byKey != null && byKey.isOngoing && !byKey.isDeleted) {
                dao.markAsRemoved(byKey.id)
                return@withTransaction
            }
            dao.findActiveOngoingByPackage(packageName)?.let { dao.markAsRemoved(it.id) }
        }
    }

    suspend fun delete(id: Int): Int = dao.deleteById(id)

    suspend fun restore(entity: NotificationEntity) {
        dao.insertOrReplace(entity)
    }

    suspend fun clearAll(): Int = dao.deleteAll()

    /** 用户下调保留上限后立即收敛数据规模 */
    suspend fun applyRetention(limit: Int) {
        if (limit > 0) {
            database.withTransaction { dao.trimTo(limit) }
        }
    }
}
