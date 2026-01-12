package com.example.madcamp_week1

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.madcamp_week1.alarm.AlarmScheduler
import com.example.madcamp_week1.databinding.ActivityMainBinding
import com.example.madcamp_week1.ui.attendence.AttendanceModal
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

    private val serverIp = "172.20.62.68"

    // 알림 권한 후 출석을 띄울지 여부
    private var pendingAttendanceAfterPermission = false

    // Compose 상태를 Activity에서 건드리기 위한 콜백
    private var showAttendanceFromActivity: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        attendanceManager = AttendanceManager(this)

        setContent {

            val (showOnboarding, setShowOnboarding) = remember {
                mutableStateOf(isFirstLaunch())
            }

            val (showAttendanceModal, setShowAttendanceModal) = remember {
                mutableStateOf(false)
            }

            // Activity → Compose 연결
            showAttendanceFromActivity = {
                setShowAttendanceModal(true)
            }

            Box(modifier = Modifier.fillMaxSize()) {

                // XML 기반 메인 UI
                AndroidView(
                    factory = { binding.root },
                    modifier = Modifier.fillMaxSize()
                )

                // 🎁 출석 모달
                if (showAttendanceModal) {
                    AttendanceModal(
                        totalDays = attendanceManager.getTotalAttendanceDays(),
                        onClose = { setShowAttendanceModal(false) }
                    )
                }

                // 🚀 온보딩
                if (showOnboarding) {
                    OnboardingModal(
                        isOpen = showOnboarding,
                        onComplete = {
                            setOnboardingFinished()
                            setShowOnboarding(false)

                            // 알림 권한 후 출석 모달을 띄울 예정
                            pendingAttendanceAfterPermission = true

                            askNotificationPermission()
                            AlarmScheduler.scheduleMidnightAlarm(this@MainActivity)
                        }
                    )
                }
            }
        }

        // 하단 네비게이션
        setupBottomNav(
            binding.includeBottomNav.mainBtn,
            binding.includeBottomNav.categoriesBtn,
            binding.includeBottomNav.attendanceCheckBtn,
            binding.includeBottomNav.alarmBtn
        )

        // RecyclerView
        val initialData = loadVideoData()
        mainAdapter = VideoAdapter(initialData)
        binding.rvVideoList.apply {
            adapter = mainAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        // 서버에서 데이터 가져오기
        fetchVideoDataFromServer()
    }

    // 🔔 알림 권한 응답 → 출석 체크
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 101 && pendingAttendanceAfterPermission) {
            pendingAttendanceAfterPermission = false

            val success = attendanceManager.checkTodayAttendance()
            if (success) {
                runOnUiThread {
                    showAttendanceFromActivity?.invoke()
                }
            }
        }
    }

    // ============================
    // 서버 통신
    // ============================
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
                            mainAdapter.updateCategoryData(videoList, "Top 10")
                        }
                    }
                } else {
                    Log.e("API_ERROR", "서버 응답 에러: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<VideoData>?>, t: Throwable) {
                Log.e("NETWORK_ERROR", "서버 연결 실패: ${t.message}")
            }
        })
    }

    // ============================
    // 알림 권한
    // ============================
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    // ============================
    // JSON 로드
    // ============================
    private fun getJsonFromAssets(context: Context, fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }

    private fun loadVideoData(): List<VideoData> {
        val jsonString = getJsonFromAssets(this, "video_data.json")
        return if (jsonString != null) {
            val listType = object : TypeToken<List<VideoData>>() {}.type
            Gson().fromJson(jsonString, listType)
        } else emptyList()
    }

    // ============================
    // 온보딩 상태
    // ============================
    private fun isFirstLaunch(): Boolean {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return prefs.getBoolean("first_launch", true)
    }

    private fun setOnboardingFinished() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("first_launch", false).apply()
    }
}