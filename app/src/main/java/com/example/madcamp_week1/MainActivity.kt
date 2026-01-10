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

class MainActivity : NavActivity() {

    override val currentNavItem: NavItem = NavItem.MAIN
    private lateinit var binding: ActivityMainBinding
    private lateinit var mainAdapter: VideoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 알림 권한 요청
        askNotificationPermission()

        setupBottomNav(
            binding.includeBottomNav.mainBtn,
            binding.includeBottomNav.categoriesBtn,
            binding.includeBottomNav.attendanceCheckBtn,
            binding.includeBottomNav.alarmBtn
        )

        val allData = loadVideoData()
        mainAdapter = VideoAdapter(allData)
        binding.rvVideoList.apply {
            adapter = VideoAdapter(allData)
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        // Firebase 토큰 가져오기
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            Log.d("FCMTOKEN", "My Token: ${task.result}")
        }

        // Firestore 실시간 리스너
        val db = Firebase.firestore
        db.collection("top10")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("FIRESTORE", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val videoList = mutableListOf<VideoData>()
                for (doc in snapshots!!) {
                    // Firestore에서 데이터를 객체로 변환
                    val video = doc.toObject(VideoData::class.java)
                    videoList.add(video)
                }

                // 4. 이제 mainAdapter를 찾을 수 있습니다
                mainAdapter.updateData(videoList)
            }

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