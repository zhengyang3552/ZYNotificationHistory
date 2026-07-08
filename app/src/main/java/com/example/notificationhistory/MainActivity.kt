package com.example.notificationhistory.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.notificationhistory.databinding.ActivityMainBinding
import com.example.notificationhistory.fragment.HomeFragment
import com.example.notificationhistory.fragment.SettingsFragment
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Fragment列表
    private val fragments = listOf(
        HomeFragment(),
        SettingsFragment()
    )

    // Tab标题
    private val tabTitles = arrayOf("首页", "设置")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
    }

    private fun setupViewPager() {
        // 设置ViewPager2适配器
        binding.viewPager.adapter = object : androidx.fragment.app.FragmentActivity.FragmentStateAdapter(this) {
            override fun getItemCount(): Int = fragments.size
            override fun createFragment(position: Int) = fragments[position]
        }

        // 连接TabLayout和ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }
}
