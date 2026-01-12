package com.example.madcamp_week1.ui.attendance

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.madcamp_week1.AttendanceManager
import com.example.madcamp_week1.ui.theme.PressStart
import com.example.madcamp_week1.ui.theme.Fredoka
import java.util.*

@Composable
fun AttendanceScreen() {
    val context = LocalContext.current
    val attendanceManager = remember { AttendanceManager(context) }

    val calendar = remember { Calendar.getInstance() }

    var year by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }

    val today = remember {
        val c = Calendar.getInstance()
        String.format("%04d-%02d-%02d",
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH)
        )
    }

    val isTodayAttended = attendanceManager.isAttended(today)

    val attendedDays = remember(year, month, isTodayAttended) {
        attendanceManager.getAllAttendances()
            .filter { it.date.startsWith(String.format("%04d-%02d", year, month + 1)) }
            .map { it.date.substring(8, 10).toInt() }
            .toSet()
    }

    val streak = attendanceManager.getTotalAttendanceDays()

    val daysInMonth = Calendar.getInstance().apply {
        set(year, month, 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)

    val firstDay = Calendar.getInstance().apply {
        set(year, month, 1)
    }.get(Calendar.DAY_OF_WEEK) - 1

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(16.dp)
    ) {

        // 오늘 출석 상태
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .border(2.dp, if (isTodayAttended) Color(0xFF00FFFF) else Color(0xFFFF9800))
                .background(Color(0xFF1A1A2E))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                if (isTodayAttended) "오늘 출석 완료" else "오늘 아직 출석 안됨",
                color = if (isTodayAttended) Color(0xFF00FFFF) else Color(0xFFFF9800),
                fontFamily = PressStart,
                fontSize = 10.sp
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            NeonCard("연속 출석", streak.toString(), Color(0xFFFF10F0))
            NeonCard("누적 보상", (streak * 10).toString(), Color.Cyan)
        }

        Spacer(Modifier.height(16.dp))

        Column(
            Modifier
                .border(3.dp, Color(0xFFB300FF))
                .padding(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("◀", Modifier.clickable {
                    if (month == 0) { month = 11; year-- } else month--
                }, color = Color.White)

                Text(
                    "${year}년 ${month + 1}월",
                    color = Color.White,
                    fontFamily = PressStart,
                    fontSize = 10.sp
                )

                Text("▶", Modifier.clickable {
                    if (month == 11) { month = 0; year++ } else month++
                }, color = Color.White)
            }

            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(columns = GridCells.Fixed(7)) {
                items(firstDay) { Box(Modifier.size(40.dp)) }

                items(daysInMonth) { i ->
                    val day = i + 1
                    val checked = attendedDays.contains(day)

                    Box(
                        Modifier
                            .size(40.dp)
                            .border(2.dp, if (checked) Color.Cyan else Color.DarkGray)
                            .background(if (checked) Color(0xFFFF10F0) else Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            day.toString(),
                            color = Color.White,
                            fontFamily = Fredoka,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NeonCard(title: String, value: String, borderColor: Color) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .border(3.dp, borderColor)
            .background(Color(0xFF1A1A2E))
            .padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = borderColor, fontFamily = PressStart, fontSize = 10.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = Color.White, fontFamily = Fredoka, fontSize = 22.sp)
    }
}