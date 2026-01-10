package com.example.madcamp_week1

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.madcamp_week1.databinding.ActivityMainBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

class MainActivity : NavActivity() {

    override val currentNavItem: NavItem = NavItem.MAIN
    private lateinit var binding: ActivityMainBinding
    private lateinit var mainAdapter: VideoAdapter

    // 서버 IP 주소
    private val serverIp = "10.249.86.18"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        /**
         * 테스트용 코드입니다.
         */
        binding.btnTestNotification.setOnClickListener {
            sendLocalTestNotification(
                "신규 Top 10 업데이트! 🔥",
                "지금 바로 틱톡 인기 영상을 확인하세요!"
            )
        }
    }

    /**
     * 테스트용 코드입니다.
     */
    private fun sendLocalTestNotification(title: String, message: String) {
        val channelId = "TOP10_CHANNEL"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. 채널 생성 (Android 8.0 이상 필수)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Top 10 업데이트 알림",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 2. 알림 빌드 (이전에 에러 났던 PRIORITY_HIGH 적용)
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // 우리가 만든 벡터 아이콘
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // 3. 알림 실행
        notificationManager.notify(999, builder.build())
    }

    // FastAPI 서버에서 데이터를 가져오기
    private fun fetchVideoDataFromServer() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://$serverIp:8000/")
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
                        sendLocalTestNotification("업데이트 완료", "새로운 영상을 불러왔습니다.")
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
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
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