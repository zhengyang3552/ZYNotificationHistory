package com.example.notificationhistory.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.notificationhistory.data.NotificationEntity
import com.example.notificationhistory.data.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 主界面状态。
 * [loaded] 用于区分「首帧还没读到数据库」与「确实没有记录」，避免空状态误闪。
 */
data class MainUiState(
    val records: List<NotificationEntity> = emptyList(),
    val displayItems: List<DisplayItem> = emptyList(),
    val totalCount: Int = 0,
    val ongoingCount: Int = 0,
    val query: String = "",
    val loaded: Boolean = false
) {
    val hasRecords: Boolean get() = records.isNotEmpty()
    val isSearching: Boolean get() = query.isNotEmpty()
}

/**
 * 主界面 ViewModel：把数据库 Flow、搜索过滤与增删操作从 Activity 中剥离，
 * 保证屏幕旋转 / 主题切换（Activity 重建）时数据与搜索词不丢失。
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NotificationRepository(application)

    private val allRecords = MutableStateFlow<List<NotificationEntity>>(emptyList())
    private val queryFlow = MutableStateFlow("")
    private val loadedFlow = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            repository.observeAll()
                .catch {
                    // 读取失败时结束 loading，避免界面永远停在空态判断之前
                    loadedFlow.value = true
                }
                .collect { records ->
                    allRecords.value = records
                    loadedFlow.value = true
                }
        }
    }

    val uiState: StateFlow<MainUiState> =
        combine(allRecords, queryFlow, loadedFlow) { records, query, loaded ->
            val filtered = filter(records, query)
            MainUiState(
                records = filtered,
                displayItems = DisplayItem.from(filtered),
                totalCount = records.size,
                ongoingCount = records.count { it.isOngoing && !it.isDeleted },
                query = query,
                loaded = loaded
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainUiState()
        )

    fun onQueryChange(query: String) {
        queryFlow.value = query.trim()
    }

    fun clearQuery() {
        queryFlow.value = ""
    }

    fun delete(record: NotificationEntity) {
        viewModelScope.launch {
            runCatching { repository.delete(record.id) }
        }
    }

    /** 堆叠卡片侧滑：删除整组记录 */
    fun delete(records: List<NotificationEntity>) {
        viewModelScope.launch {
            runCatching { records.forEach { repository.delete(it.id) } }
        }
    }

    fun restore(record: NotificationEntity) {
        viewModelScope.launch {
            runCatching { repository.restore(record) }
        }
    }

    /** 撤销堆叠卡片删除：整组恢复 */
    fun restore(records: List<NotificationEntity>) {
        viewModelScope.launch {
            runCatching { records.forEach { repository.restore(it) } }
        }
    }

    fun clearAll(onFinished: (Int) -> Unit) {
        viewModelScope.launch {
            val deleted = runCatching { repository.clearAll() }.getOrDefault(0)
            onFinished(deleted)
        }
    }

    private fun filter(
        records: List<NotificationEntity>,
        query: String
    ): List<NotificationEntity> {
        if (query.isBlank()) return records
        return records.filter { record ->
            record.appName.contains(query, ignoreCase = true) ||
                record.title.contains(query, ignoreCase = true) ||
                record.content.contains(query, ignoreCase = true) ||
                record.packageName.contains(query, ignoreCase = true)
        }
    }
}
