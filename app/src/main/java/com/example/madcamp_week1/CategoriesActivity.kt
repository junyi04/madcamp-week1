package com.example.madcamp_week1

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.madcamp_week1.databinding.ActivityCategoriesBinding
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CategoriesActivity : NavActivity() {
    override val currentNavItem: NavItem = NavItem.CATEGORIES
    lateinit var binding: ActivityCategoriesBinding

    private lateinit var allVideoData: List<VideoData>
    private lateinit var categoryAdapter: VideoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNav(
            binding.includeBottomNav.mainBtn,
            binding.includeBottomNav.categoriesBtn,
            binding.includeBottomNav.attendanceCheckBtn,
            binding.includeBottomNav.alarmBtn
        )

        allVideoData = loadVideoData()
        setupRecyclerView()
        setupTabs()
    }

    private fun setupRecyclerView() {
        categoryAdapter = VideoAdapter(emptyList(), isGridMode = true)
        binding.rvCategoryList.apply {
            adapter = categoryAdapter
            layoutManager = GridLayoutManager(this@CategoriesActivity, 2)
        }

        // 디폴트 카테고리
        filterAndDisplay("춤")
    }

    private fun setupTabs() {
        val categories = listOf("춤", "챌린지", "음식", "TTS")
        categories.forEach { name ->
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(name))
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val selectedCategory = tab?.text.toString()
                filterAndDisplay(selectedCategory)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // JSON의 category 변수를 사용한 필터링 로직
    private fun filterAndDisplay(categoryName: String) {
        val filteredList = allVideoData.filter { data ->
            data.category == categoryName
        }
        categoryAdapter.updateData(filteredList)
    }

    private fun loadVideoData(): List<VideoData> {
        return try {
            val jsonString = assets.open("video_data.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<VideoData>>() {}.type
            Gson().fromJson(jsonString, listType)
        } catch (e: Exception) {
            emptyList()
        }
    }
}