package com.example.madcamp_week1

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.madcamp_week1.databinding.ActivityAttendanceCheckBinding
import com.google.gson.GsonBuilder
import java.text.SimpleDateFormat
import java.util.*

class AttendanceCheckActivity : NavActivity() {

    override val currentNavItem: NavItem = NavItem.ATTENDANCECHECK

    private lateinit var binding: ActivityAttendanceCheckBinding
    private lateinit var attendanceManager: AttendanceManager

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)

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

        attendanceManager = AttendanceManager(this)

        // 파일 위치 로그
        Log.d("AttendanceCheck", "JSON 파일 위치: ${attendanceManager.getFilePath()}")

        setupCalendar()
        showTodayStatus()
    }

    private fun setupCalendar() {
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            val selectedDate = dateFormat.format(calendar.time)

            binding.selectedDateText.text = "선택: $selectedDate"

// JSON에서 해당 날짜 데이터 확인
            val attendanceData = attendanceManager.getAttendanceByDate(selectedDate)

            if (attendanceData != null) {
                // 출석 데이터가 있는 경우만 표시
                binding.statusText.text = "출석 완료 😆"
                binding.statusText.setTextColor(Color.GREEN)
                binding.statusText.visibility = android.view.View.VISIBLE

                // 출석 시간 표시
                val time = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
                    .format(Date(attendanceData.timestamp))
                binding.timeText.text = "출석 시간: $time"
                binding.timeText.visibility = android.view.View.VISIBLE

            } else {
                // 출석 데이터가 없는 경우 - 아무것도 표시 안 함
                binding.statusText.text = ""
                binding.statusText.visibility = android.view.View.GONE
                binding.timeText.text = ""
                binding.timeText.visibility = android.view.View.GONE
                Log.d("AttendanceCheck", "$selectedDate: 데이터 없음")
            }
        }
    }

    private fun showTodayStatus() {
        val today = dateFormat.format(Date())
        val attendanceData = attendanceManager.getAttendanceByDate(today)

        binding.selectedDateText.text = "오늘: $today"

        if (attendanceData != null) {
            binding.statusText.text = "오늘 출석 완료 😆"
            binding.statusText.setTextColor(Color.GREEN)
            binding.statusText.visibility = android.view.View.VISIBLE

            val time = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
                .format(Date(attendanceData.timestamp))
            binding.timeText.text = "출석 시간: $time"
            binding.timeText.visibility = android.view.View.VISIBLE
        } else {
            binding.statusText.text = "오늘 아직 출석 안 함"
            binding.statusText.setTextColor(Color.parseColor("#FF9800")) // 주황색
            binding.statusText.visibility = android.view.View.VISIBLE
            binding.timeText.visibility = android.view.View.GONE
        }

        // 총 출석일 표시
        val totalDays = attendanceManager.getTotalAttendanceDays()
        binding.statsText.text = "총 출석일: ${totalDays}일"

        // 모든 출석 날짜 로그
        val allAttendances = attendanceManager.getAllAttendances()
        Log.d("AttendanceCheck", "전체 출석 기록: ${allAttendances.size}개")
        allAttendances.forEach {
            Log.d("AttendanceCheck", "  - ${it.date}: 출석완료")
        }
    }

    /**
     * 출석 데이터를 다이얼로그로 표시하고 클립보드에 복사
     */
    private fun showAttendanceDataDialog() {
        val allAttendances = attendanceManager.getAllAttendances()

        // JSON을 보기 좋게 포맷팅
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonData = gson.toJson(allAttendances)

        // 다이얼로그 생성
        AlertDialog.Builder(this)
            .setTitle("📋 출석 데이터")
            .setMessage("총 ${allAttendances.size}개 출석 기록\n\n$jsonData")
            .setPositiveButton("📋 클립보드에 복사") { _, _ ->
                // 클립보드에 복사
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Attendance Data", jsonData)
                clipboard.setPrimaryClip(clip)

                Toast.makeText(this, "✅ 클립보드에 복사되었습니다!", Toast.LENGTH_SHORT).show()
                Log.d("AttendanceCheck", "클립보드에 복사됨:\n$jsonData")
            }
            .setNeutralButton("📂 파일 위치 보기") { _, _ ->
                val filePath = attendanceManager.getFilePath()
                AlertDialog.Builder(this)
                    .setTitle("📂 파일 위치")
                    .setMessage(filePath)
                    .setPositiveButton("복사") { _, _ ->
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("File Path", filePath)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this, "파일 경로 복사됨!", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("닫기", null)
                    .show()
            }
            .setNegativeButton("닫기", null)
            .show()
    }
}