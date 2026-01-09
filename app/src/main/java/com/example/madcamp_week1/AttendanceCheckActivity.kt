package com.example.madcamp_week1

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.madcamp_week1.databinding.ActivityAttendanceCheckBinding

class AttendanceCheckActivity : NavActivity() {
    override val currentNavItem: NavItem = NavItem.ATTENDANCECHECK
    lateinit var binding: ActivityAttendanceCheckBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAttendanceCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNav(
            binding.includeBottomNav.mainBtn,
            binding.includeBottomNav.categoriesBtn,
            binding.includeBottomNav.attendanceCheckBtn,
            binding.includeBottomNav.alarmBtn
        )



    }
}