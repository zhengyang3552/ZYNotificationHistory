package com.example.notificationhistory.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import com.example.notificationhistory.util.AppIconCache
import com.example.notificationhistory.util.AppPreferences
import com.example.notificationhistory.util.TimeUtil
import com.google.android.material.color.MaterialColors

/**
 * 通知列表适配器。
 *
 * 相比改造前：
 * - 不再在每次 bind 时读取 SharedPreferences（原实现每个条目每次滚动都会读一次磁盘配置），
 *   改为由页面统一下发 [DisplayOptions]；
 * - 状态标签使用语义色角色（常驻 = tertiaryContainer、临时 = secondaryContainer、
 *   已移除 = surfaceContainerHighest），避免三种状态都用高饱和容器色抢注意力；
 * - 所有 ViewHolder 复用时状态被完整重置，不会出现「上一个条目的已移除样式残留」。
 */
class NotificationAdapter(
    private val onItemClick: (NotificationEntity) -> Unit,
    private val onItemLongClick: (NotificationEntity) -> Unit
) : ListAdapter<NotificationEntity, NotificationAdapter.NotificationViewHolder>(DiffCallback) {

    data class DisplayOptions(
        val showPackageName: Boolean = true,
        val timeFormat: AppPreferences.TimeFormat = AppPreferences.TimeFormat.RELATIVE
    )

    private var options = DisplayOptions()

    /** 三种状态的标签样式缓存：null 表示尚未按当前主题初始化 */
    private var tagStyles: List<TagStyle>? = null

    /** 配置变化时刷新已显示的条目（不重建列表，保持滚动位置） */
    fun submitOptions(newOptions: DisplayOptions) {
        if (options == newOptions) return
        options = newOptions
        if (itemCount > 0) notifyItemRangeChanged(0, itemCount)
    }

    /**
     * 周期性刷新可见条目的时间文案。
     *
     * 相对时间（如「3 分钟前」）是绑定那一刻计算的，数据库没变化就不会重新绑定，
     * 界面上就会一直停留在旧值。这里带 [PAYLOAD_TIME] 做局部刷新：
     * 只重写时间 TextView，不触发布局与动画，也不影响滚动位置。
     */
    fun refreshTimes() {
        if (options.timeFormat == AppPreferences.TimeFormat.ABSOLUTE) return
        if (itemCount > 0) notifyItemRangeChanged(0, itemCount, PAYLOAD_TIME)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position), options)
    }

    override fun onBindViewHolder(
        holder: NotificationViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        if (payloads.contains(PAYLOAD_TIME)) {
            holder.bindTime(getItem(position), options)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tv_app_name)
        private val tvPackage: TextView = itemView.findViewById(R.id.tv_package)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvContent: TextView = itemView.findViewById(R.id.tv_content)
        private val chipTag: TextView = itemView.findViewById(R.id.chip_tag)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)

        fun bind(item: NotificationEntity, options: DisplayOptions) {
            val context = itemView.context

            // 来源信息
            tvAppName.text = item.appName.ifBlank { context.getString(R.string.unknown_app) }
            tvPackage.isVisible = options.showPackageName && item.packageName.isNotBlank()
            tvPackage.text = item.packageName

            // 时间
            bindTime(item, options)

            // 标题与正文：标题为空时不占位，正文为空时给出占位文案保持卡片结构完整
            val title = item.title.trim()
            val content = item.content.trim()
            tvTitle.isVisible = title.isNotEmpty()
            tvTitle.text = title
            tvContent.isVisible = content.isNotEmpty() || title.isEmpty()
            tvContent.text = content.ifEmpty { context.getString(R.string.content_placeholder) }

            // 状态标签
            bindTag(item)

            // 应用图标：先取内存缓存，未命中先展示默认图标占位，再异步加载回填。
            // getApplicationIcon 是 binder IPC，放在绑定期同步执行会在滚动/重绑时掉帧
            ivIcon.tag = item.packageName
            val cachedIcon = AppIconCache.get(item.packageName)
            if (cachedIcon != null) {
                ivIcon.setImageDrawable(cachedIcon)
            } else {
                ivIcon.setImageResource(android.R.drawable.sym_def_app_icon)
                AppIconCache.loadAsync(context, item.packageName) { drawable ->
                    // 行可能已被复用给其他包名，校验后再填充
                    if (ivIcon.tag == item.packageName) {
                        ivIcon.setImageDrawable(drawable)
                    }
                }
            }

            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
        }

        /** 时间文案单独成方法，供定时局部刷新复用；每次都以当前时刻重新计算 */
        fun bindTime(item: NotificationEntity, options: DisplayOptions) {
            tvTime.text = when (options.timeFormat) {
                AppPreferences.TimeFormat.ABSOLUTE -> TimeUtil.formatAbsolute(item.timestamp)
                AppPreferences.TimeFormat.RELATIVE -> TimeUtil.formatRelative(item.timestamp)
            }
        }

        private fun bindTag(item: NotificationEntity) {
            val style = tagStyleFor(item)
            chipTag.setText(style.labelRes)
            chipTag.setTextColor(style.onContainerColor)
            chipTag.background = style.background
        }

        /**
         * 三种状态（已移除/常驻/临时）的标签样式按 Adapter 缓存：
         * 主题属性解析与 GradientDrawable 分配只发生一次，
         * 主题切换会重建 Activity，缓存随 Adapter 一起失效，无需手动清理。
         */
        private fun tagStyleFor(item: NotificationEntity): TagStyle {
            val styles = tagStyles ?: createTagStyles().also { tagStyles = it }
            return when {
                item.isDeleted -> styles[0]
                item.isOngoing -> styles[1]
                else -> styles[2]
            }
        }

        private fun createTagStyles(): List<TagStyle> = listOf(
            TagStyle(
                R.string.tag_removed,
                roundedBackground(
                    themeColor(itemView, com.google.android.material.R.attr.colorSurfaceContainerHighest,
                        R.color.md_theme_surface_container_highest)
                ),
                themeColor(itemView, com.google.android.material.R.attr.colorOnSurfaceVariant,
                    R.color.md_theme_on_surface_variant)
            ),
            TagStyle(
                R.string.tag_ongoing,
                roundedBackground(
                    themeColor(itemView, com.google.android.material.R.attr.colorTertiaryContainer,
                        R.color.md_theme_tertiary_container)
                ),
                themeColor(itemView, com.google.android.material.R.attr.colorOnTertiaryContainer,
                    R.color.md_theme_on_tertiary_container)
            ),
            TagStyle(
                R.string.tag_normal,
                roundedBackground(
                    themeColor(itemView, com.google.android.material.R.attr.colorSecondaryContainer,
                        R.color.md_theme_secondary_container)
                ),
                themeColor(itemView, com.google.android.material.R.attr.colorOnSecondaryContainer,
                    R.color.md_theme_on_secondary_container)
            ),
        )

        private fun roundedBackground(color: Int): GradientDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = itemView.resources.getDimension(R.dimen.radius_sm)
        }
    }

    private data class TagStyle(
        val labelRes: Int,
        val background: GradientDrawable,
        val onContainerColor: Int
    )

    private fun themeColor(view: View, @AttrRes attr: Int, @ColorRes fallback: Int): Int =
        runCatching { MaterialColors.getColor(view, attr) }
            .getOrElse { ContextCompat.getColor(view.context, fallback) }

    companion object {
        /** 局部刷新标记：只更新时间文案，不重绘整行 */
        const val PAYLOAD_TIME = "payload_time"
    }

    object DiffCallback : DiffUtil.ItemCallback<NotificationEntity>() {
        override fun areItemsTheSame(oldItem: NotificationEntity, newItem: NotificationEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: NotificationEntity, newItem: NotificationEntity) =
            oldItem == newItem
    }
}
