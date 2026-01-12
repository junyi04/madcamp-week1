package com.example.madcamp_week1

import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.madcamp_week1.databinding.ActivityCategoriesBinding
import com.google.android.material.tabs.TabLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CategoriesActivity : NavActivity() {
    override val currentNavItem: NavItem = NavItem.CATEGORIES
    lateinit var binding: ActivityCategoriesBinding

    private lateinit var categoryAdapter: VideoAdapter

    private val serverIp = "172.20.62.68"

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

        setupRecyclerView()
        setupTabs()
    }

    private fun setupRecyclerView() {
        categoryAdapter = VideoAdapter(emptyList(), isCategoryMode = true)

        binding.rvCategoryList.apply {
            adapter = categoryAdapter
            layoutManager = LinearLayoutManager(this@CategoriesActivity)
        }

        fetchCategoryDataFromServer("dance", "Dance")
    }

    private fun setupTabs() {
        // 기존 탭 로직 유지
        val categories = listOf("춤" to "dance", "챌린지" to "challenge", "음식" to "food", "TTS" to "tts")

        // 중복 방지를 위해 탭 초기화 (안전장치)
        binding.tabLayout.removeAllTabs()

        categories.forEach { (displayName, _) ->
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(displayName))
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val displayName = tab?.text.toString()
                val serverName = when (displayName) {
                    "춤" -> "dance"
                    "챌린지" -> "challenge"
                    "음식" -> "food"
                    "TTS" -> "tts"
                    else -> "dance"
                }

                val titleForUI = when (serverName) {
                    "dance" -> "Dance"
                    "challenge" -> "Challenge"
                    "food" -> "Food"
                    "tts" -> "TTS"
                    else -> "Category"
                }

                fetchCategoryDataFromServer(serverName, titleForUI)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun fetchCategoryDataFromServer(categoryName: String, uiTitle: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://$serverIp:8001/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        apiService.getCategoryData(categoryName).enqueue(object : Callback<List<VideoData>> {
            override fun onResponse(call: Call<List<VideoData>>, response: Response<List<VideoData>>) {
                if (response.isSuccessful) {
                    val videoList = response.body() ?: emptyList()
                    runOnUiThread {
                        // 데이터와 타이틀을 한 번에 업데이트
                        categoryAdapter.updateCategoryData(videoList, uiTitle)
                    }
                }
            }

            override fun onFailure(call: Call<List<VideoData>>, t: Throwable) {
                Log.e("CATEGORY_ERROR", "$categoryName 로드 실패: ${t.message}")
            }
        })
    }
}