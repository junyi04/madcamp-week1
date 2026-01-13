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
import androidx.compose.runtime.*
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

    // Activity → Compose bridge
    private var showAttendanceFromActivity: (() -> Unit)? = null

    // 오늘 출석이 필요한지
    private var needAttendanceToday = false

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

            showAttendanceFromActivity = {
                setShowAttendanceModal(true)
            }

            // 앱 시작 시 오늘 출석 필요 여부 계산
            LaunchedEffect(Unit) {
                if (!isFirstLaunch()) {
                    needAttendanceToday = !attendanceManager.isAttendedToday()
                    if (needAttendanceToday && hasNotificationPermission()) {
                        attendanceManager.markToday()
                        showAttendanceFromActivity?.invoke()
                        AlarmScheduler.scheduleMidnightAlarm(this@MainActivity)
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {

                AndroidView(
                    factory = { binding.root },
                    modifier = Modifier.fillMaxSize()
                )

                if (showAttendanceModal) {
                    AttendanceModal(
                        totalDays = attendanceManager.getTotalAttendanceDays(),
                        attendances = attendanceManager.getAllAttendances(),
                        onClose = { setShowAttendanceModal(false) }
                    )
                }

                if (showOnboarding) {
                    OnboardingModal(
                        isOpen = showOnboarding,
                        onComplete = {
                            setOnboardingFinished()
                            setShowOnboarding(false)
                            needAttendanceToday = true
                            askNotificationPermission()
                        }
                    )
                }
            }
        }

        setupBottomNav(
            binding.includeBottomNav.mainBtn,
            binding.includeBottomNav.categoriesBtn,
            binding.includeBottomNav.attendanceCheckBtn,
            binding.includeBottomNav.comicBtn
        )

        val initialData = loadVideoData()
        mainAdapter = VideoAdapter(initialData)
        binding.rvVideoList.apply {
            adapter = mainAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        fetchVideoDataFromServer()
    }

    // 권한 결과 → 출석 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 101) {
            if (needAttendanceToday) {
                attendanceManager.markToday()
                showAttendanceFromActivity?.invoke()
            }
            AlarmScheduler.scheduleMidnightAlarm(this)
        }
    }

    // 알림 권한 요청
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            } else {
                if (needAttendanceToday) {
                    attendanceManager.markToday()
                    showAttendanceFromActivity?.invoke()
                }
                AlarmScheduler.scheduleMidnightAlarm(this)
            }
        } else {
            if (needAttendanceToday) {
                attendanceManager.markToday()
                showAttendanceFromActivity?.invoke()
            }
            AlarmScheduler.scheduleMidnightAlarm(this)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    // ---------------------- 서버 & 기타 ----------------------

    private fun fetchVideoDataFromServer() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://young-forty.ngrok.app/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java).getVideoData()
            .enqueue(object : Callback<List<VideoData>> {
                override fun onResponse(call: Call<List<VideoData>>, response: Response<List<VideoData>>) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            runOnUiThread { mainAdapter.updateCategoryData(it, "Top 10") }
                        }
                    }
                }
                override fun onFailure(call: Call<List<VideoData>>, t: Throwable) {
                    Log.e("NETWORK", t.message ?: "")
                }
            })
    }

    private fun getJsonFromAssets(context: Context, fileName: String): String? {
        return try { context.assets.open(fileName).bufferedReader().use { it.readText() } }
        catch (e: Exception) { null }
    }

    private fun loadVideoData(): List<VideoData> {
        val json = getJsonFromAssets(this, "video_data.json")
        return if (json != null) Gson().fromJson(json, object : TypeToken<List<VideoData>>() {}.type)
        else emptyList()
    }

    private fun isFirstLaunch(): Boolean {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return prefs.getBoolean("first_launch", true)
    }

    private fun setOnboardingFinished() {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit().putBoolean("first_launch", false).apply()
    }
}