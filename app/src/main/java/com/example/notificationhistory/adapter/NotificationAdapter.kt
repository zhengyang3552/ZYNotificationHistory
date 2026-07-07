package com.example.notificationhistory.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notificationhistory.R
import com.example.notificationhistory.data.NotificationEntity
import com.example.notificationhistory.util.TimeUtil
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class NotificationAdapter(
    private val onRemoveNotification: (NotificationEntity) -> Unit,
    private val onViewNotification: (NotificationEntity) -> Unit
) :
    ListAdapter<NotificationEntity, NotificationAdapter.VH>(DiffCallback()) {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View = itemView.findViewById(R.id.notification_item_root)
        val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)
        val tvAppName: TextView = itemView.findViewById(R.id.tv_app_name)
        val tvPackage: TextView = itemView.findViewById(R.id.tv_package)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvContent: TextView = itemView.findViewById(R.id.tv_content)
        val chipTag: TextView = itemView.findViewById(R.id.chip_tag)
        val tvTime: TextView = itemView.findViewById(R.id.tv_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val context = holder.itemView.context

        holder.tvAppName.text = item.appName
        holder.tvTitle.text = item.title
        holder.tvContent.text = item.content

        // Package name visibility based on settings
        val prefs = context.getSharedPreferences("notification_prefs", 0)
        val showPackageName = prefs.getBoolean("show_package_name", true)
        holder.tvPackage.visibility = if (showPackageName) View.VISIBLE else View.GONE

        // Set tag text with proper contrast colors (keeping rounded corner background)
        when {
            item.isDeleted -> {
                holder.chipTag.text = "已移除"
                setTagBackground(holder.chipTag, R.color.md_theme_errorContainer, R.color.md_theme_onErrorContainer)
            }
            item.isOngoing -> {
                holder.chipTag.text = "常驻"
                setTagBackground(holder.chipTag, R.color.md_theme_secondaryContainer, R.color.md_theme_onSecondaryContainer)
            }
            else -> {
                holder.chipTag.text = "临时"
                setTagBackground(holder.chipTag, R.color.md_theme_primaryContainer, R.color.md_theme_onPrimaryContainer)
            }
        }

        // Time display based on global settings
        val defaultTimeFormat = prefs.getString("default_time_format", "relative")
        holder.tvTime.text = if (defaultTimeFormat == "relative") {
            TimeUtil.formatRelative(item.timestamp)
        } else {
            TimeUtil.formatAbsolute(item.timestamp)
        }

        // App icon
        try {
            val icon = context.packageManager.getApplicationIcon(item.packageName)
            holder.ivIcon.setImageDrawable(icon)
        } catch (e: Exception) {
            holder.ivIcon.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        // Long press menu
        holder.root.setOnLongClickListener {
            showContextMenu(context, item)
            true
        }
    }

    private fun showContextMenu(context: android.content.Context, item: NotificationEntity) {
        MaterialAlertDialogBuilder(context)
            .setTitle(item.appName)
            .setItems(arrayOf("从通知栏移除此通知", "查看此通知")) { _, which ->
                when (which) {
                    0 -> {
                        // 从通知栏移除
                        onRemoveNotification(item)
                    }
                    1 -> {
                        // 查看此通知（打开对应应用）
                        onViewNotification(item)
                    }
                }
            }
            .show()
    }

    /**
     * Set tag background with rounded corners and proper colors
     */
    private fun setTagBackground(tag: TextView, bgRes: Int, textRes: Int) {
        tag.setTextColor(ContextCompat.getColor(tag.context, textRes))
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(ContextCompat.getColor(tag.context, bgRes))
            cornerRadius = 20f  // 20dp rounded corners
        }
        tag.background = drawable
    }

    class DiffCallback : DiffUtil.ItemCallback<NotificationEntity>() {
        override fun areItemsTheSame(oldItem: NotificationEntity, newItem: NotificationEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: NotificationEntity, newItem: NotificationEntity) =
            oldItem == newItem
    }
}
