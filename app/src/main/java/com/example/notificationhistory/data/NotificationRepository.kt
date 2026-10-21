package com.example.notificationhistory.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

/**
 * 通知记录仓库：把“上报/移除/去重/清理”这类涉及多步写入的逻辑从 Service 与 UI 中抽离。
 *
 * 关键规则：
 * 1. 同一 [NotificationEntity.key] 再次上报视为同一条通知的更新（进度条、聊天串、常驻通知
 *    都会复用同一个 key）。常驻通知只刷新文案与状态、保留首次记录时间；
 *    聊天类连续消息（微信/QQ 等）内容有变化时追加为新记录（派生唯一 key），
 *    已显示的消息内容与时间保持不变，由列表层按应用堆叠展示；
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
                byKey != null -> when {
                    // 常驻通知（下载进度等）原地刷新：只更新文案与状态，
                    // 保持首次记录时间，避免列表在后台更新时来回跳动
                    byKey.isOngoing || entity.isOngoing -> {
                        dao.update(
                            byKey.copy(
                                title = entity.title,
                                content = entity.content,
                                isOngoing = entity.isOngoing,
                                isDeleted = false
                                // 保持首次记录时间，列表不跳动
                            )
                        )
                    }

                    // 内容与该应用该会话最新一条相同：系统重连重放等重复上报，直接忽略
                    // （与最新一条而非首条比对，避免误吞连续两条内容相同的消息）
                    dao.findLatestByPackageAndTitle(entity.packageName, entity.title)
                        ?.let { it.content == entity.content } == true -> Unit

                    // 聊天类连续消息（微信/QQ 等原地更新同一条通知）：
                    // 追加为一条新记录，已显示的消息内容和时间保持不变，
                    // 由列表按应用堆叠展示、默认露出最新一条
                    else -> dao.insertIgnore(
                        entity.copy(
                            id = 0,
                            key = "${entity.key}#${System.currentTimeMillis()}",
                            timestamp = System.currentTimeMillis()
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
