package com.example.madcamp_week1

data class AttendanceData(
    val date: String,
    val timestamp: Long,
    val attended: Boolean = true
)