package com.example.madcamp_week1

import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.madcamp_week1.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : NavActivity() {

    override val currentNavItem: NavItem = NavItem.MAIN
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNav(
            binding.includeBottomNav.mainBtn,
            binding.includeBottomNav.categoriesBtn,
            binding.includeBottomNav.attendanceCheckBtn,
            binding.includeBottomNav.alarmBtn
        )

        val allData = loadVideoData()
        val mainAdapter = VideoAdapter(allData)
        binding.rvVideoList.apply {
            adapter = mainAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        binding.rvVideoList.layoutManager = LinearLayoutManager(this)
        binding.rvVideoList.adapter = VideoAdapter(allData)
    }

    private fun getJsonFromAssets(context: Context, fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadVideoData(): List<VideoData> {
        val gson = Gson()
        val jsonString = getJsonFromAssets(this, "video_data.json") // 파일명 확인하세요!

        return if (jsonString != null) {
            val listType = object : TypeToken<List<VideoData>>() {}.type
            gson.fromJson(jsonString, listType)
        } else {
            emptyList()
        }
    }
}


