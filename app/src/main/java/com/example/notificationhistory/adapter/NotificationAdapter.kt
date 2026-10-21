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

    /** 配置变化时刷新已显示的条目（不重建列表，保持滚动位置） */
    fun submitOptions(newOptions: DisplayOptions) {
        if (options == newOptions) return
        options = newOptions
        if (itemCount > 0) notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position), options)
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
            tvTime.text = when (options.timeFormat) {
                AppPreferences.TimeFormat.ABSOLUTE -> TimeUtil.formatAbsolute(item.timestamp)
                AppPreferences.TimeFormat.RELATIVE -> TimeUtil.formatRelative(item.timestamp)
            }

            // 标题与正文：标题为空时不占位，正文为空时给出占位文案保持卡片结构完整
            val title = item.title.trim()
            val content = item.content.trim()
            tvTitle.isVisible = title.isNotEmpty()
            tvTitle.text = title
            tvContent.isVisible = content.isNotEmpty() || title.isEmpty()
            tvContent.text = content.ifEmpty { context.getString(R.string.content_placeholder) }

            // 状态标签
            bindTag(item)

            // 应用图标：包被卸载 / 无图标时回退系统默认图标
            val icon = runCatching { context.packageManager.getApplicationIcon(item.packageName) }
                .getOrNull()
            if (icon != null) {
                ivIcon.setImageDrawable(icon)
            } else {
                ivIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
        }

        private fun bindTag(item: NotificationEntity) {
            val (labelRes, containerAttr, onContainerAttr) = when {
                item.isDeleted -> Triple(
                    R.string.tag_removed,
                    com.google.android.material.R.attr.colorSurfaceContainerHighest,
                    com.google.android.material.R.attr.colorOnSurfaceVariant
                )

                item.isOngoing -> Triple(
                    R.string.tag_ongoing,
                    com.google.android.material.R.attr.colorTertiaryContainer,
                    com.google.android.material.R.attr.colorOnTertiaryContainer
                )

                else -> Triple(
                    R.string.tag_normal,
                    com.google.android.material.R.attr.colorSecondaryContainer,
                    com.google.android.material.R.attr.colorOnSecondaryContainer
                )
            }

            chipTag.setText(labelRes)
            chipTag.setTextColor(themeColor(itemView, onContainerAttr, R.color.md_theme_on_secondary_container))
            chipTag.background = roundedBackground(
                themeColor(itemView, containerAttr, R.color.md_theme_secondary_container)
            )
        }

        private fun roundedBackground(color: Int): GradientDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = itemView.resources.getDimension(R.dimen.radius_sm)
        }
    }

    private fun themeColor(view: View, @AttrRes attr: Int, @ColorRes fallback: Int): Int =
        runCatching { MaterialColors.getColor(view, attr) }
            .getOrElse { ContextCompat.getColor(view.context, fallback) }

    object DiffCallback : DiffUtil.ItemCallback<NotificationEntity>() {
        override fun areItemsTheSame(oldItem: NotificationEntity, newItem: NotificationEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: NotificationEntity, newItem: NotificationEntity) =
            oldItem == newItem
    }
}
