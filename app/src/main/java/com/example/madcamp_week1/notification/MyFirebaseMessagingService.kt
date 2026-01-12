package com.example.madcamp_week1.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.madcamp_week1.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MyFirebaseMessagingService : FirebaseMessagingService() {

    // 1. 새 토큰이 생성될 때 호출
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("MyFirebaseMessagingService", "Refreshed token: $token")
    }

    // 2. 서버에서 알림 메세지 보낼 때 호출
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 메세지에 테이터나 알림 내용이 포함되어 있는지 확인
        remoteMessage.notification?.let {
            showNotification(it.title, it.body)
        }
    }

    // 3. 휴대폰 상단바에 알림을 만드는 함수
    private fun showNotification(title: String?, message: String?) {
        val channelId = "TOP10_CHANNEL"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Top 10 업데이트 알림",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 알림 디자인 설정
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // 클릭 시 알림 삭제

        notificationManager.notify(101, builder.build())
    }
}