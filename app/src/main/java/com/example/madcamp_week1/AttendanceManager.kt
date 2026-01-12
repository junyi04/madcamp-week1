package com.example.madcamp_week1

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AttendanceManager(private val context: Context) {

    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)

    // 앱 내부 저장소의 storage 폴더
    private val storageDir = File(context.filesDir, "storage")
    private val attendanceFile = File(storageDir, "attendance.json")

    companion object {
        private const val TAG = "AttendanceManager"
        private const val ASSETS_PATH = "app/src/main/assets/storage/attendance.json"
    }

    init {
        Log.d(TAG, "========== AttendanceManager 초기화 ==========")

        // 1. storage 폴더 생성
        if (!storageDir.exists()) {
            val created = storageDir.mkdirs()
            Log.d(TAG, "storage 폴더 생성: $created")
            Log.d(TAG, "폴더 위치: ${storageDir.absolutePath}")
        }

        // 2. JSON 파일 없으면 assets에서 복사
        if (!attendanceFile.exists()) {
            Log.d(TAG, "JSON 파일 없음 - assets에서 복사 시작")
            copyFromAssets()
        } else {
            Log.d(TAG, "JSON 파일 이미 존재")
        }

        // 3. 최종 확인
        Log.d(TAG, "==============================================")
        Log.d(TAG, "JSON 파일 위치: ${attendanceFile.absolutePath}")
        Log.d(TAG, "파일 존재: ${attendanceFile.exists()}")

        if (attendanceFile.exists()) {
            Log.d(TAG, "파일 크기: ${attendanceFile.length()} bytes")
            val content = attendanceFile.readText()
            Log.d(TAG, "파일 내용: $content")
        }

        Log.d(TAG, "==============================================")
    }

    /**
     * assets 폴더에서 초기 JSON 파일 복사
     */
    private fun copyFromAssets() {
        try {
            context.assets.open(ASSETS_PATH).use { inputStream ->
                FileOutputStream(attendanceFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d(TAG, "✅ assets 복사 성공: $ASSETS_PATH → ${attendanceFile.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ assets 복사 실패: ${e.message}", e)
            // 복사 실패하면 수동으로 빈 배열 생성
            attendanceFile.writeText("[]")
            Log.d(TAG, "빈 JSON 배열 생성")
        }
    }

    /**
     * 오늘 출석 체크
     */
    fun checkTodayAttendance(): Boolean {
        val today = getTodayDate()

        Log.d(TAG, "=== 출석 체크 시작 ===")
        Log.d(TAG, "오늘 날짜: $today")

        // 이미 출석했는지 확인
        if (isAttended(today)) {
            Log.d(TAG, "이미 출석함")
            return false
        }

        // 출석 기록 추가
        val attendances = getAllAttendances().toMutableList()
        val newAttendance = AttendanceData(
            date = today,
            timestamp = System.currentTimeMillis(),
            attended = true
        )

        attendances.add(newAttendance)
        saveAttendances(attendances)

        Log.d(TAG, "✅ 출석 완료! 저장: $newAttendance")

        return true
    }

    /**
     * 특정 날짜 출석 여부 확인
     */
    fun isAttended(date: String): Boolean {
        return getAllAttendances().any { it.date == date && it.attended }
    }

    /**
     * 모든 출석 기록 가져오기
     */
    fun getAllAttendances(): List<AttendanceData> {
        if (!attendanceFile.exists()) {
            Log.d(TAG, "파일 없음 - 빈 리스트 반환")
            return emptyList()
        }

        return try {
            val json = attendanceFile.readText()

            if (json.isBlank() || json.trim() == "[]") {
                Log.d(TAG, "파일 비어있음")
                return emptyList()
            }

            val type = object : TypeToken<List<AttendanceData>>() {}.type
            val list: List<AttendanceData> = gson.fromJson(json, type)
            Log.d(TAG, "출석 기록 로드: ${list.size}개")
            list

        } catch (e: Exception) {
            Log.e(TAG, "파일 읽기 에러: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 출석 기록 저장
     */
    private fun saveAttendances(attendances: List<AttendanceData>) {
        try {
            val json = gson.toJson(attendances)
            attendanceFile.writeText(json)

            Log.d(TAG, "=== 저장 완료 ===")
            Log.d(TAG, "파일 위치: ${attendanceFile.absolutePath}")
            Log.d(TAG, "파일 크기: ${attendanceFile.length()} bytes")
            Log.d(TAG, "저장된 내용:\n$json")

        } catch (e: Exception) {
            Log.e(TAG, "파일 저장 에러: ${e.message}", e)
        }
    }

    /**
     * 오늘 날짜 가져오기
     */
    private fun getTodayDate(): String {
        return dateFormat.format(Date())
    }

    /**
     * 출석 통계
     */
    fun getTotalAttendanceDays(): Int {
        return getAllAttendances().count { it.attended }
    }

    /**
     * JSON 파일 경로 반환
     */
    fun getFilePath(): String {
        return attendanceFile.absolutePath
    }

    /**
     * 특정 날짜의 출석 데이터 가져오기
     */
    fun getAttendanceByDate(date: String): AttendanceData? {
        return getAllAttendances().find { it.date == date && it.attended }
    }

    /**
     * 출석 데이터 전체 삭제 (테스트용)
     */
    fun clearAllAttendances() {
        attendanceFile.writeText("[]")
        Log.d(TAG, "출석 기록 초기화")
    }
}