package com.example.madcamp_week1

import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
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

    // 서버 IP 주소
    private val serverIp = "10.249.86.17"

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
        categoryAdapter = VideoAdapter(emptyList(), isGridMode = true)
        binding.rvCategoryList.apply {
            adapter = categoryAdapter
            layoutManager = GridLayoutManager(this@CategoriesActivity, 2)
        }

        fetchCategoryDataFromServer("dance")
    }

    private fun setupTabs() {
        val categories = listOf("춤" to "dance", "챌린지" to "challenge", "음식" to "food", "TTS" to "tts")
        categories.forEach { (displayName, _) ->
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(displayName))
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val selectedName = when (tab?.text.toString()) {
                    "춤" -> "dance"
                    "챌린지" -> "challenge"
                    "음식" -> "food"
                    "TTS" -> "tts"
                    else -> "dance"
                }
                fetchCategoryDataFromServer(selectedName)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // 서버에서 카테고리 데이터 가져오기
    private fun fetchCategoryDataFromServer(categoryName: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://$serverIp:8001/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        apiService.getCategoryData(categoryName).enqueue(object : Callback<List<VideoData>> {
            override fun onResponse(call: Call<List<VideoData>>, response: Response<List<VideoData>>) {
                if (response.isSuccessful) {
                    val videoList = response.body() ?: emptyList()
                    // UI 갱신
                    runOnUiThread {
                        categoryAdapter.updateData(videoList)
                        Log.d("CATEGORY_SUCCESS", "$categoryName 데이터 ${videoList.size}개 로드 완료")
                    }
                }
            }

            override fun onFailure(call: Call<List<VideoData>>, t: Throwable) {
                Log.e("CATEGORY_ERROR", "$categoryName 로드 실패: ${t.message}")
            }
        })
    }
}