package com.example.madcamp_week1.notification

import android.app.*
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.madcamp_week1.R

object NotificationUtil {

    private const val CHANNEL_ID = "daily_midnight"

    fun showDailyNotification(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Alarm",
                NotificationManager.IMPORTANCE_HIGH
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("DailyCue")
            .setContentText("새로운 하루가 시작되었음")
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(1001, notification)
    }
}