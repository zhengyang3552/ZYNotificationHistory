package com.example.notificationhistory.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notificationhistory.R
import com.example.notificationhistory.data.NotificationEntity
import com.example.notificationhistory.ui.DisplayItem
import com.example.notificationhistory.util.AppIconCache
import com.example.notificationhistory.util.AppPreferences
import com.example.notificationhistory.util.TimeUtil
import com.google.android.material.color.MaterialColors

/**
 * 通知列表适配器。
 *
 * 条目分两类：
 * - [DisplayItem.Single]：独立通知卡片（含常置顶的常驻通知）；
 * - [DisplayItem.Stack]：同一应用的堆叠卡片，正面展示最新一条，
 *   点击展开后逐条列出历史消息，视觉与应用图标「消息流堆叠」一致。
 *
 * 其余约定：
 * - 显示配置由页面统一下发 [DisplayOptions]，条目不再读 SharedPreferences；
 * - 状态标签使用语义色角色，样式按 Adapter 缓存；
 * - 展开状态记录在 [expandedKeys] 中，跨 submitList 保留，
 *   数据刷新不会把用户展开的堆叠重新折叠。
 */
class NotificationAdapter(
    private val onRecordClick: (NotificationEntity) -> Unit,
    private val onRecordLongClick: (NotificationEntity) -> Unit
) : ListAdapter<DisplayItem, RecyclerView.ViewHolder>(DiffCallback) {

    data class DisplayOptions(
        val showPackageName: Boolean = true,
        val timeFormat: AppPreferences.TimeFormat = AppPreferences.TimeFormat.RELATIVE
    )

    private var options = DisplayOptions()

    /** 三种状态的标签样式缓存：null 表示尚未按当前主题初始化 */
    private var tagStyles: List<TagStyle>? = null

    /** 处于展开状态的堆叠 key（DisplayItem.Stack.key） */
    private val expandedKeys = mutableSetOf<String>()

    /** 配置变化时刷新已显示的条目（不重建列表，保持滚动位置） */
    fun submitOptions(newOptions: DisplayOptions) {
        if (options == newOptions) return
        options = newOptions
        if (itemCount > 0) notifyItemRangeChanged(0, itemCount)
    }

    /**
     * 周期性刷新可见条目的时间文案（相对时间实时性）。
     * 带 [PAYLOAD_TIME] 做局部刷新：只重写时间 TextView，不触发布局与动画。
     */
    fun refreshTimes() {
        if (options.timeFormat == AppPreferences.TimeFormat.ABSOLUTE) return
        if (itemCount > 0) notifyItemRangeChanged(0, itemCount, PAYLOAD_TIME)
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is DisplayItem.Stack -> TYPE_STACK
        else -> TYPE_SINGLE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_STACK) {
            StackViewHolder(inflater.inflate(R.layout.item_notification_stack, parent, false))
        } else {
            SingleViewHolder(inflater.inflate(R.layout.item_notification, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DisplayItem.Stack -> (holder as StackViewHolder).bind(item, options)
            is DisplayItem.Single -> (holder as SingleViewHolder).bind(item.record, options)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        if (payloads.contains(PAYLOAD_TIME)) {
            when (val item = getItem(position)) {
                is DisplayItem.Stack -> (holder as StackViewHolder).bindTime(item, options)
                is DisplayItem.Single -> (holder as SingleViewHolder).bindTime(item.record, options)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    // ---------------------------------------------------------------- 单条卡片

    inner class SingleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tv_app_name)
        private val tvPackage: TextView = itemView.findViewById(R.id.tv_package)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvContent: TextView = itemView.findViewById(R.id.tv_content)
        private val chipTag: TextView = itemView.findViewById(R.id.chip_tag)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)

        fun bind(item: NotificationEntity, options: DisplayOptions) {
            val context = itemView.context

            tvAppName.text = item.appName.ifBlank { context.getString(R.string.unknown_app) }
            tvPackage.isVisible = options.showPackageName && item.packageName.isNotBlank()
            tvPackage.text = item.packageName

            bindTime(item, options)

            bindTitleAndContent(item)

            bindTag(item)
            bindIcon(item)

            itemView.setOnClickListener { onRecordClick(item) }
            itemView.setOnLongClickListener {
                onRecordLongClick(item)
                true
            }
        }

        fun bindTime(item: NotificationEntity, options: DisplayOptions) {
            tvTime.text = formatTime(item, options)
        }

        fun bindTitleAndContent(item: NotificationEntity) {
            val context = itemView.context
            val title = item.title.trim()
            val content = item.content.trim()
            tvTitle.isVisible = title.isNotEmpty()
            tvTitle.text = title
            tvContent.isVisible = content.isNotEmpty() || title.isEmpty()
            tvContent.text = content.ifEmpty { context.getString(R.string.content_placeholder) }
        }

        fun bindTag(item: NotificationEntity) {
            val style = tagStyleFor(item, itemView)
            chipTag.setText(style.labelRes)
            chipTag.setTextColor(style.onContainerColor)
            chipTag.background = style.background
        }

        fun bindIcon(item: NotificationEntity) {
            ivIcon.tag = item.packageName
            val cachedIcon = AppIconCache.get(item.packageName)
            if (cachedIcon != null) {
                ivIcon.setImageDrawable(cachedIcon)
            } else {
                ivIcon.setImageResource(android.R.drawable.sym_def_app_icon)
                AppIconCache.loadAsync(itemView.context, item.packageName) { drawable ->
                    // 行可能已被复用给其他包名，校验后再填充
                    if (ivIcon.tag == item.packageName) {
                        ivIcon.setImageDrawable(drawable)
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- 堆叠卡片

    inner class StackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val historyContainer: LinearLayout =
            itemView.findViewById(R.id.ll_history)
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tv_app_name)
        private val tvPackage: TextView = itemView.findViewById(R.id.tv_package)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvContent: TextView = itemView.findViewById(R.id.tv_content)
        private val chipTag: TextView = itemView.findViewById(R.id.chip_tag)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val tvCount: TextView = itemView.findViewById(R.id.tv_count)
        private val ivChevron: ImageView = itemView.findViewById(R.id.iv_chevron)
        private val frontCard: View = itemView.findViewById(R.id.stack_front)

        /** 展开行的时间视图引用，供相对时间局部刷新复用 */
        private val entryTimeViews = mutableListOf<Pair<NotificationEntity, TextView>>()

        fun bind(item: DisplayItem.Stack, options: DisplayOptions) {
            val context = itemView.context
            val latest = item.latest

            // 正面卡片：与单条卡片同构，展示最新一条
            tvAppName.text = latest.appName.ifBlank { context.getString(R.string.unknown_app) }
            tvPackage.isVisible = options.showPackageName && latest.packageName.isNotBlank()
            tvPackage.text = latest.packageName
            bindTime(item, options)

            val title = latest.title.trim()
            val content = latest.content.trim()
            tvTitle.isVisible = title.isNotEmpty()
            tvTitle.text = title
            tvContent.isVisible = content.isNotEmpty() || title.isEmpty()
            tvContent.text = content.ifEmpty { context.getString(R.string.content_placeholder) }

            bindTag(latest)
            bindIcon(latest)

            tvCount.text = context.getString(R.string.stack_count, item.records.size)

            // 正面卡片（最新一条消息）= 打开详情，与单条卡片行为一致；
            // 卡片其余区域（计数/展开箭头行、顶部外露边缘）= 展开/收起堆叠。
            // 注意：正面卡片是可点击子视图，会消费自己区域内的点击，
            // 不会与根视图的展开/收起冲突。
            frontCard.setOnClickListener { onRecordClick(latest) }
            frontCard.setOnLongClickListener {
                onRecordLongClick(latest)
                true
            }
            itemView.setOnClickListener { toggleExpand(item) }
            itemView.setOnLongClickListener {
                onRecordLongClick(latest)
                true
            }

            // 展开状态跨 submitList 记忆：绑定即恢复
            renderHistory(item, options, expanded = item.key in expandedKeys)
        }

        fun bindTime(item: DisplayItem.Stack, options: DisplayOptions) {
            tvTime.text = formatTime(item.latest, options)
            for ((record, view) in entryTimeViews) {
                view.text = formatTime(record, options)
            }
        }

        private fun toggleExpand(item: DisplayItem.Stack) {
            val expanded = item.key in expandedKeys
            if (expanded) {
                expandedKeys -= item.key
            } else {
                expandedKeys += item.key
            }
            renderHistory(item, options, !expanded)
        }

        /**
         * 展开/收起渲染。
         * 收起时清空行视图释放引用；展开时按时间倒序重建历史行
         * （正面卡片已是最新一条，这里从第二条开始）。
         */
        private fun renderHistory(item: DisplayItem.Stack, options: DisplayOptions, expanded: Boolean) {
            val context = itemView.context
            ivChevron.animate()
                .rotation(if (expanded) 90f else 0f)
                .setDuration(150)
                .start()
            ivChevron.contentDescription = context.getString(
                if (expanded) R.string.stack_collapse else R.string.stack_expand
            )

            if (!expanded) {
                historyContainer.isVisible = false
                historyContainer.removeAllViews()
                entryTimeViews.clear()
                return
            }

            historyContainer.removeAllViews()
            entryTimeViews.clear()
            val inflater = LayoutInflater.from(context)
            for (record in item.history) {
                val row = inflater.inflate(R.layout.view_notification_entry, historyContainer, false)
                val tvTitle = row.findViewById<TextView>(R.id.entry_title)
                val tvContent = row.findViewById<TextView>(R.id.entry_content)
                val tvTime = row.findViewById<TextView>(R.id.entry_time)

                tvTitle.text = record.title.trim().ifEmpty {
                    record.appName.ifBlank { context.getString(R.string.unknown_app) }
                }
                tvContent.text = record.content.trim()
                tvTime.text = formatTime(record, options)
                entryTimeViews += record to tvTime

                row.setOnClickListener { onRecordClick(record) }
                row.setOnLongClickListener {
                    onRecordLongClick(record)
                    true
                }
                historyContainer.addView(row)
            }
            historyContainer.isVisible = true
        }

        private fun bindIcon(item: NotificationEntity) {
            ivIcon.tag = item.packageName
            val cachedIcon = AppIconCache.get(item.packageName)
            if (cachedIcon != null) {
                ivIcon.setImageDrawable(cachedIcon)
            } else {
                ivIcon.setImageResource(android.R.drawable.sym_def_app_icon)
                AppIconCache.loadAsync(itemView.context, item.packageName) { drawable ->
                    if (ivIcon.tag == item.packageName) {
                        ivIcon.setImageDrawable(drawable)
                    }
                }
            }
        }

        private fun bindTag(item: NotificationEntity) {
            val style = tagStyleFor(item, itemView)
            chipTag.setText(style.labelRes)
            chipTag.setTextColor(style.onContainerColor)
            chipTag.background = style.background
        }
    }

    // ---------------------------------------------------------------- 公共样式

    private data class TagStyle(
        val labelRes: Int,
        val background: GradientDrawable,
        val onContainerColor: Int
    )

    private fun formatTime(
        item: NotificationEntity,
        options: DisplayOptions
    ): String = when (options.timeFormat) {
        AppPreferences.TimeFormat.ABSOLUTE -> TimeUtil.formatAbsolute(item.timestamp)
        AppPreferences.TimeFormat.RELATIVE -> TimeUtil.formatRelative(item.timestamp)
    }

    private fun tagStyleFor(item: NotificationEntity, view: View): TagStyle {
        val styles = tagStyles ?: createTagStyles(view).also { tagStyles = it }
        return when {
            item.isDeleted -> styles[0]
            item.isOngoing -> styles[1]
            else -> styles[2]
        }
    }

    /**
     * 三种状态（已移除/常驻/临时）的标签样式按 Adapter 缓存：
     * 主题属性解析与 GradientDrawable 分配只发生一次，
     * 主题切换会重建 Activity，缓存随 Adapter 一起失效，无需手动清理。
     */
    private fun createTagStyles(view: View): List<TagStyle> = listOf(
        TagStyle(
            R.string.tag_removed,
            roundedBackground(
                view,
                themeColor(view, com.google.android.material.R.attr.colorSurfaceContainerHighest,
                    R.color.md_theme_surface_container_highest)
            ),
            themeColor(view, com.google.android.material.R.attr.colorOnSurfaceVariant,
                R.color.md_theme_on_surface_variant)
        ),
        TagStyle(
            R.string.tag_ongoing,
            roundedBackground(
                view,
                themeColor(view, com.google.android.material.R.attr.colorTertiaryContainer,
                    R.color.md_theme_tertiary_container)
            ),
            themeColor(view, com.google.android.material.R.attr.colorOnTertiaryContainer,
                R.color.md_theme_on_tertiary_container)
        ),
        TagStyle(
            R.string.tag_normal,
            roundedBackground(
                view,
                themeColor(view, com.google.android.material.R.attr.colorSecondaryContainer,
                    R.color.md_theme_secondary_container)
            ),
            themeColor(view, com.google.android.material.R.attr.colorOnSecondaryContainer,
                R.color.md_theme_on_secondary_container)
        ),
    )

    private fun roundedBackground(view: View, color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = view.resources.getDimension(R.dimen.radius_sm)
    }

    private fun themeColor(view: View, @AttrRes attr: Int, @ColorRes fallback: Int): Int =
        runCatching { MaterialColors.getColor(view, attr) }
            .getOrElse { ContextCompat.getColor(view.context, fallback) }

    companion object {
        /** 局部刷新标记：只更新时间文案，不重绘整行 */
        const val PAYLOAD_TIME = "payload_time"

        private const val TYPE_SINGLE = 0
        private const val TYPE_STACK = 1
    }

    object DiffCallback : DiffUtil.ItemCallback<DisplayItem>() {
        override fun areItemsTheSame(oldItem: DisplayItem, newItem: DisplayItem) =
            oldItem.key == newItem.key

        override fun areContentsTheSame(oldItem: DisplayItem, newItem: DisplayItem) =
            oldItem == newItem
    }
}
