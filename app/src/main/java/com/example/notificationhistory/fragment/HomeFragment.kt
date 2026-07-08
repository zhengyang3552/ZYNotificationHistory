package com.example.notificationhistory.fragment

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notificationhistory.R
import com.example.notificationhistory.adapter.NotificationAdapter
import com.example.notificationhistory.data.AppDatabase
import com.example.notificationhistory.data.NotificationEntity
import com.example.notificationhistory.databinding.FragmentHomeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupPermissionCard()
        setupClearAllFab()
        checkNotificationPermission()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HomeFragment.adapter
            itemAnimator = null
        }

        viewLifecycleOwner.lifecycleScope.launch {
            AppDatabase.getInstance(requireContext())
                .notificationDao()
                .getAll()
                .collectLatest { list ->
                    adapter.submitList(list)
                    updateEmptyState(list)
                }
        }
    }

    private fun setupPermissionCard() {
        binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSecondaryContainer))
        binding.btnPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun setupClearAllFab() {
        binding.fabClearAll.setOnClickListener {
            showClearAllDialog()
        }
    }

    private fun showClearAllDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("清空通知历史")
            .setMessage("确定要清空所有通知记录吗？此操作不可撤销。")
            .setPositiveButton("清空") { _, _ ->
                clearAllNotifications()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun clearAllNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            AppDatabase.getInstance(requireContext()).notificationDao().deleteAll()
            Snackbar.make(
                binding.root,
                "已清空所有通知历史",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateEmptyState(list: List<NotificationEntity>) {
        val hasItems = list.isNotEmpty()
        binding.emptyState.visibility = if (hasItems) View.GONE else View.VISIBLE
        binding.recyclerView.visibility = if (hasItems) View.VISIBLE else View.GONE
        binding.fabClearAll.visibility = if (hasItems) View.VISIBLE else View.GONE
    }

    private fun checkNotificationPermission() {
        if (isNotificationServiceEnabled()) {
            updatePermissionStatus(true)
        } else {
            updatePermissionStatus(false)
        }
    }

    private fun updatePermissionStatus(enabled: Boolean) {
        if (enabled) {
            binding.cardStatus.visibility = View.GONE
        } else {
            binding.cardStatus.visibility = View.VISIBLE
            binding.tvStatus.text = "需要开启「通知使用权」以读取通知历史"
            binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSecondaryContainer))
            binding.ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_alert)
            binding.ivStatusIcon.setImageTintList(
                ContextCompat.getColorStateList(requireContext(), R.color.md_theme_onSecondaryContainer)
            )
            binding.btnPermission.text = "前往开启"
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val cn = ComponentName(
            requireContext(),
            "com.example.notificationhistory.service.NotificationService"
        )
        val flat = Settings.Secure.getString(requireContext().contentResolver, "enabled_notification_listeners")
        return flat != null && TextUtils.split(flat, ":").any { it == cn.flattenToString() }
    }

    override fun onResume() {
        super.onResume()
        checkNotificationPermission()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
