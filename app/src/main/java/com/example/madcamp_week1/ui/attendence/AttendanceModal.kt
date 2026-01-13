package com.example.madcamp_week1.ui.attendence

import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.madcamp_week1.AttendanceData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AttendanceModal(
    totalDays: Int,
    attendances: List<AttendanceData>,
    onClose: () -> Unit
) {
    // 오늘의 요일 가져오기
    val calendar = Calendar.getInstance()
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

    // 월요일 = 인덱스 0
    val currentDayIndex = (dayOfWeek + 5) % 7

    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .background(Color.Black, RoundedCornerShape(12.dp))
                .border(3.dp, Color.Cyan, RoundedCornerShape(12.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎁 오늘의 출석 보상!", color = Color.Cyan, fontSize = 20.sp)

                Spacer(modifier = Modifier.height(16.dp))

                val days = listOf("월", "화", "수", "목", "금", "토", "일")

                // 요일별 출석 여부 계산
                val checkedDays = BooleanArray(7)

                attendances.forEach { att ->
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
                    val date = sdf.parse(att.date) ?: return@forEach

                    val cal = Calendar.getInstance()
                    cal.time = date

                    val day = cal.get(Calendar.DAY_OF_WEEK)
                    val index = (day + 5) % 7   // 월=0

                    checkedDays[index] = true
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    days.forEachIndexed { index, day ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day, color = Color.White)
                            Spacer(modifier = Modifier.height(6.dp))

                            val isChecked = checkedDays[index]

                            Text(
                                if (isChecked) "✔" else "○",
                                color = if (isChecked) Color.Green else Color.Gray,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(onClick = onClose) {
                    Text("확인")
                }
            }
        }
    }
}
