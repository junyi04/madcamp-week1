package com.example.madcamp_week1

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.madcamp_week1.alarm.AlarmScheduler
import com.example.madcamp_week1.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : NavActivity() {

    override val currentNavItem: NavItem = NavItem.MAIN
    private lateinit var binding: ActivityMainBinding
    private lateinit var attendanceManager: AttendanceManager
    private lateinit var mainAdapter: VideoAdapter

    // 서버 IP 주소
    private val serverIp = "10.249.86.17"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AlarmScheduler.scheduleMidnightAlarm(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // ===== 자동 출석 체크 (가장 먼저!) =====
        attendanceManager = AttendanceManager(this)
        checkAttendanceAutomatically()
        // ====================================

        askNotificationPermission()

        setupBottomNav(
            binding.includeBottomNav.mainBtn,
            binding.includeBottomNav.categoriesBtn,
            binding.includeBottomNav.attendanceCheckBtn,
            binding.includeBottomNav.alarmBtn
        )

        val initialData = loadVideoData()
        mainAdapter = VideoAdapter(initialData)
        binding.rvVideoList.apply {
            adapter = mainAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        fetchVideoDataFromServer()

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

    // FastAPI 서버에서 데이터를 가져오기
    private fun fetchVideoDataFromServer() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://$serverIp:8001/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        apiService.getVideoData().enqueue(object : Callback<List<VideoData>> {
            override fun onResponse(
                call: Call<List<VideoData>?>,
                response: Response<List<VideoData>?>
            ) {
                if (response.isSuccessful) {
                    val videoList = response.body()
                    if (videoList != null) {
                        runOnUiThread {
                            mainAdapter.updateData(videoList)
                            Log.d("API_SUCCESS", "데이터 ${videoList.size}개로 화면을 갱신했습니다.")
                        }
                    }
                } else {
                    Log.e("API_ERROR", "서버 응답 에러: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<VideoData>?>, t: Throwable) {
                Log.e("NETWORK_ERROR", "서버 연결에 실패했씁니다: ${t.message}")
            }
        })
    }

    // 사용자에게 알림 수신 권한을 요청하는 팝업
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    // 파일 이름을 String 형식으로 읽어오는 유틸리티 함수
    private fun getJsonFromAssets(context: Context, fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // video_data.json 읽어서 객체 리스트로 변환
    private fun loadVideoData(): List<VideoData> {
        val jsonString = getJsonFromAssets(this, "video_data.json")
        return if (jsonString != null) {
            val listType = object : TypeToken<List<VideoData>>() {}.type
            Gson().fromJson(jsonString, listType)
        } else {
            emptyList()
        }
    }
}