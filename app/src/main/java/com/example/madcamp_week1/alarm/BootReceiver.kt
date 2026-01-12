package com.example.madcamp_week1.alarm

import android.content.*

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmScheduler.scheduleMidnightAlarm(context)
        }
    }
}