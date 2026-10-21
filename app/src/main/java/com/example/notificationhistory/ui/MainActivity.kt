package com.example.notificationhistory.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notificationhistory.R
import com.example.notificationhistory.adapter.NotificationAdapter
import com.example.notificationhistory.data.NotificationEntity
import com.example.notificationhistory.databinding.ActivityMainBinding
import com.example.notificationhistory.util.AppPreferences
import com.example.notificationhistory.util.NotificationAccess
import com.example.notificationhistory.util.TimeUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 主界面：通知历史列表。
 *
 * 改造要点：
 * - 数据通过 ViewModel 的 StateFlow 驱动，旋转屏幕/切换主题后列表与搜索词不丢；
 * - 列表改用 RecyclerView 独立滚动（改造前把 RecyclerView 放进 NestedScrollView 并设
 *   wrap_content，会一次性布局全部条目，条目多时明显卡顿且失去复用）;
 * - 支持搜索、单条删除（侧滑 + 长按菜单，均可撤销）、详情查看与复制；
 * - 返回键不再被权限状态劫持（原实现在未授权时拦截返回键，用户无法退出应用）。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NotificationAdapter
    private val viewModel: MainViewModel by viewModels()

    private var searchView: SearchView? = null
    private var latestState = MainUiState()
    private var permissionGranted = false

    private companion object {
        /** 相对时间刷新周期：足够「实时」，又不会造成无谓的重绘 */
        const val TIME_REFRESH_INTERVAL_MS = 15_000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupPermissionBanner()
        setupFab()
        setupBackHandling()
        observeState()
        startTimeTicker()

        refreshPermissionState()
    }

    override fun onResume() {
        super.onResume()
        // 从系统设置返回后权限状态可能已变化
        refreshPermissionState()
        adapter.submitOptions(currentDisplayOptions())
        render(latestState)
    }

    // ---------------------------------------------------------------- 初始化

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_settings -> {
                    openSettings()
                    true
                }

                else -> false
            }
        }

        val item = binding.toolbar.menu.findItem(R.id.action_search)
        val view = item?.actionView as? SearchView ?: return
        searchView = view
        view.queryHint = getString(R.string.search_hint)
        view.maxWidth = resources.displayMetrics.widthPixels
        view.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.onQueryChange(query.orEmpty())
                view.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.onQueryChange(newText.orEmpty())
                return true
            }
        })
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(
            onItemClick = { record -> showDetail(record) },
            onItemLongClick = { record -> showActions(record) }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            setHasFixedSize(false)
            itemAnimator = DefaultItemAnimator().apply {
                // 关闭「内容变化」动画，避免后台通知更新时整行闪烁
                supportsChangeAnimations = false
            }
        }

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val record = adapter.currentList.getOrNull(position) ?: return
                deleteWithUndo(record)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun setupPermissionBanner() {
        binding.btnPermission.setOnClickListener { openNotificationAccessSettings() }
        binding.btnEmptyAction.setOnClickListener { openNotificationAccessSettings() }
    }

    private fun setupFab() {
        binding.fabClearAll.setOnClickListener { showClearAllDialog() }
    }

    /**
     * 相对时间定时刷新。
     *
     * 相对时间只在条目绑定那一刻计算一次，数据库没有新事件时列表不会重新绑定，
     * 于是「13 秒前」会一直停在旧值。这里在界面可见期间每 15 秒发一次局部刷新，
     * 只重写时间 TextView（payload 复用），不触发布局、动画与滚动位置变化。
     */
    private fun startTimeTicker() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(TIME_REFRESH_INTERVAL_MS)
                    adapter.refreshTimes()
                }
            }
        }
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val view = searchView
                if (view != null && !view.isIconified) {
                    view.isIconified = true
                    viewModel.clearQuery()
                    return
                }
                // 交还给系统处理，保持正常的退出行为
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    // ---------------------------------------------------------------- 状态渲染

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    latestState = state
                    render(state)
                }
            }
        }
    }

    private fun render(state: MainUiState) {
        adapter.submitList(state.records)
        adapter.submitOptions(currentDisplayOptions())

        binding.recyclerView.isVisible = state.hasRecords
        binding.emptyState.isVisible = !state.hasRecords
        binding.fabClearAll.isVisible = state.totalCount > 0

        binding.toolbar.subtitle = if (permissionGranted) {
            getString(R.string.main_subtitle_recording, state.totalCount)
        } else {
            getString(R.string.main_subtitle_idle)
        }

        if (!state.hasRecords) {
            binding.tvEmptyTitle.setText(R.string.empty_title)
            if (state.isSearching) {
                binding.tvEmptyMessage.text =
                    getString(R.string.empty_message_filtered, state.query)
                binding.btnEmptyAction.isVisible = false
            } else {
                binding.tvEmptyMessage.setText(R.string.empty_message)
                binding.btnEmptyAction.isVisible = !permissionGranted
            }
        }
    }

    private fun refreshPermissionState() {
        permissionGranted = NotificationAccess.isGranted(this)
        binding.cardPermission.isVisible = !permissionGranted
    }

    private fun currentDisplayOptions() = NotificationAdapter.DisplayOptions(
        showPackageName = AppPreferences.showPackageName(this),
        timeFormat = AppPreferences.timeFormat(this)
    )

    // ---------------------------------------------------------------- 交互

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun openNotificationAccessSettings() {
        if (!NotificationAccess.openSettings(this)) {
            showMessage(getString(R.string.permission_settings_unavailable))
        }
    }

    private fun showClearAllDialog() {
        val total = latestState.totalCount
        if (total <= 0) {
            showMessage(getString(R.string.empty_title))
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_clear_title)
            .setMessage(getString(R.string.dialog_clear_message, total))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_clear) { _, _ ->
                viewModel.clearAll { deleted ->
                    if (isFinishing || isDestroyed) return@clearAll
                    showMessage(getString(R.string.msg_cleared, deleted))
                }
            }
            .show()
    }

    private fun deleteWithUndo(record: NotificationEntity) {
        viewModel.delete(record)
        Snackbar.make(binding.root, R.string.msg_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.action_undo) {
                viewModel.restore(record)
                showMessage(getString(R.string.msg_restored))
            }
            .show()
    }

    private fun showActions(record: NotificationEntity) {
        val actions = arrayOf(
            getString(R.string.action_detail),
            getString(R.string.action_copy),
            getString(R.string.action_delete)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(record.title.ifBlank { getString(R.string.detail_title) })
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> showDetail(record)
                    1 -> copyToClipboard(record)
                    else -> deleteWithUndo(record)
                }
            }
            .show()
    }

    private fun showDetail(record: NotificationEntity) {
        val message = buildString {
            append(getString(R.string.detail_app)).append("：").append(record.appName).append('\n')
            append(getString(R.string.detail_package)).append("：")
                .append(record.packageName).append('\n')
            append(getString(R.string.detail_time)).append("：")
                .append(TimeUtil.formatAbsoluteWithSeconds(record.timestamp)).append('\n')
            append(getString(R.string.detail_state)).append("：")
                .append(tagLabel(record)).append("\n\n")
            append(getString(R.string.detail_content)).append("：\n")
            append(record.content.ifBlank { getString(R.string.content_placeholder) })
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(record.title.ifBlank { getString(R.string.detail_title) })
            .setMessage(message)
            .setNegativeButton(R.string.action_cancel, null)
            .setNeutralButton(R.string.action_copy) { _, _ -> copyToClipboard(record) }
            .setPositiveButton(R.string.action_delete) { _, _ -> deleteWithUndo(record) }
            .show()
    }

    private fun tagLabel(record: NotificationEntity): String = getString(
        when {
            record.isDeleted -> R.string.tag_removed
            record.isOngoing -> R.string.tag_ongoing
            else -> R.string.tag_normal
        }
    )

    private fun copyToClipboard(record: NotificationEntity) {
        val text = record.content.ifBlank { record.title }
        if (text.isBlank()) {
            showMessage(getString(R.string.msg_empty_content))
            return
        }
        val clipboard = getSystemService(ClipboardManager::class.java)
        if (clipboard == null) {
            showMessage(getString(R.string.msg_empty_content))
            return
        }
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), text))
        showMessage(getString(R.string.msg_clipboard))
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}
