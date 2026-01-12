package com.example.madcamp_week1

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.madcamp_week1.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : NavActivity() {

    override val currentNavItem: NavItem = NavItem.MAIN
    private lateinit var binding: ActivityMainBinding
    private lateinit var attendanceManager: AttendanceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // ===== 자동 출석 체크 (가장 먼저!) =====
        attendanceManager = AttendanceManager(this)
        checkAttendanceAutomatically()
        // ====================================

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
    /**
     * 자동 출석 체크
     */
    private fun checkAttendanceAutomatically() {

        Log.d("MainActivity", "출석 체크 시작")
        val filePath = attendanceManager.getFilePath()
        Log.d("MainActivity", "JSON 파일 위치: $filePath")
        val success = attendanceManager.checkTodayAttendance()

        if (success) {
            Toast.makeText(this, "✅ 오늘 출석 완료!", Toast.LENGTH_LONG).show()
            Log.d("MainActivity", "출석 성공!")
        } else {
            Log.d("MainActivity", "이미 출석한 상태")
            // 이미 출석했어도 확인 메시지
            Toast.makeText(this, "오늘은 이미 출석했습니다", Toast.LENGTH_SHORT).show()
            val total = attendanceManager.getTotalAttendanceDays()
            Log.d("MainActivity", "총 출석일: $total")
            Log.d("MainActivity", "========================================")
        }
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


