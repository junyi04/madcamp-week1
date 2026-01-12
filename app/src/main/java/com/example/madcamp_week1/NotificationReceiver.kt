package com.example.madcamp_week1

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ALARM_DEBUG", "알람 신호 수신 성공!")

        val channelId = "TOP10_CHANNEL"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. 알림 채널 생성 (오레오 이상 필수)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Top 10 업데이트 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "일일 업데이트 알림입니다."
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 2. 알림 빌드
        // 주의: R.drawable.ic_notification이 없다면 안드로이드 기본 아이콘인 android.R.drawable.ic_dialog_info 로 변경해서 테스트하세요.
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // 테스트를 위해 시스템 아이콘 사용
            .setContentTitle("알람 성공!")
            .setContentText("10초가 지나 알람이 울립니다.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // 3. 알림 발송
        notificationManager.notify(999, builder.build())
        Log.d("ALARM_DEBUG", "알림 발송 명령 완료")
    }
}