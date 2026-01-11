package com.example.madcamp_week1

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.madcamp_week1.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

// 매일 자정 업데이트 알람
fun setDailyNotification(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, NotificationReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 시간 설정: 매일 자정
    val calendar = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, 21)
        set(Calendar.MINUTE, 18)
        set(Calendar.SECOND, 0)

        // 자정이 지났다면, 내일 자정으로 설정
        if (before(Calendar.getInstance())) {
            add(Calendar.DATE, 1)
        }
    }

    // 매일 반복 설정
    alarmManager.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        AlarmManager.INTERVAL_DAY,
        pendingIntent
    )
}

class MainActivity : NavActivity() {

    override val currentNavItem: NavItem = NavItem.MAIN
    private lateinit var binding: ActivityMainBinding
    private lateinit var mainAdapter: VideoAdapter

    // 서버 IP 주소
    private val serverIp = "10.249.86.17"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setDailyNotification(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

    private fun sendLocalTestNotification(title: String, message: String) {
        val channelId = "TOP10_CHANNEL"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 채널 생성
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Top 10 업데이트 알림",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 알림 빌드
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_dialog_info) // 우리가 만든 벡터 아이콘
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // 알림 실행
        notificationManager.notify(999, builder.build())
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