// compose 기반
package com.example.madcamp_week1

import android.os.Bundle
import com.example.madcamp_week1.databinding.ActivityAttendanceCheckBinding
import com.example.madcamp_week1.ui.attendance.AttendanceScreen

class AttendanceCheckActivity : NavActivity() {

    override val currentNavItem: NavItem = NavItem.ATTENDANCECHECK
    private lateinit var binding: ActivityAttendanceCheckBinding
    private lateinit var attendanceManager: AttendanceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNav(
            binding.includeBottomNav.mainBtn,
            binding.includeBottomNav.categoriesBtn,
            binding.includeBottomNav.attendanceCheckBtn,
            binding.includeBottomNav.comicBtn
        )

        // 앱에 들어오면 자동 출석
        attendanceManager = AttendanceManager(this)
        attendanceManager.checkTodayAttendance()

        // Compose UI 삽입
        binding.attendanceComposeView.setContent {
            AttendanceScreen()
        }
    }
}