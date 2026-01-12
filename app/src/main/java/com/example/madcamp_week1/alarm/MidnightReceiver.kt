package com.example.madcamp_week1.alarm

import android.content.*
import com.example.madcamp_week1.notification.NotificationUtil

class MidnightReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // 알림 띄우기
        NotificationUtil.showDailyNotification(context)

        // 다음날 자정 다시 예약
        AlarmScheduler.scheduleMidnightAlarm(context)
    }
}
