package com.example.madcamp_week1.ui.attendance

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.madcamp_week1.AttendanceManager
import com.example.madcamp_week1.ui.theme.DungGeunMo
import com.example.madcamp_week1.ui.theme.PressStart
import com.example.madcamp_week1.ui.theme.Fredoka
import java.util.*

@Composable
fun AttendanceScreen() {
    val context = LocalContext.current
    val attendanceManager = remember { AttendanceManager(context) }

    // 모달 상태 변수
    var showRewardModal by remember { mutableStateOf(false) }
    var rewardMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
         val success = attendanceManager.checkTodayAttendance()

            if (success) {
                rewardMessage = "오늘의 출석 보상 10포인트가 지급되었습니다!"
                showRewardModal = true
            }
        /**
         * 테스트용
         */
//        val testMode = true
//        if (testMode) {
//            rewardMessage = "오늘의 출석 보상 10포인트가\n지급되었습니다!"
//            showRewardModal = true
//        }

    }

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
                .padding(16.dp)
        ) {

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                NeonCard(
                    "연속 출석",
                    streak.toString(),
                    Color(0xFFFF10F0),
                    icon = Icons.Default.LocalFireDepartment,
                    iconColor = Color(0xFFFF6600)
                )
                NeonCard(
                    "누적 보상",
                    (streak * 10).toString(),
                    Color.Cyan,
                    icon = Icons.Default.CardGiftcard,
                    iconColor = Color(0xFFFFFF00)
                )
            }

            Spacer(Modifier.height(16.dp))

            Column(
                Modifier
                    .border(3.dp, Color(0xFFB300FF))
                    .padding(12.dp)
            ) {

                // 월 네비게이션
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("◀", Modifier.clickable {
                        if (month == 0) {
                            month = 11; year--
                        } else month--
                    }, color = Color.White)

                    Text(
                        "${year}년 ${month + 1}월",
                        color = Color.White,
                        fontFamily = PressStart,
                        fontSize = 10.sp
                    )

                    Text("▶", Modifier.clickable {
                        if (month == 11) {
                            month = 0; year++
                        } else month++
                    }, color = Color.White)
                }

                Spacer(Modifier.height(20.dp))

                // 요일 헤더
                val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")
                Row(Modifier.fillMaxWidth()) {
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            color = if (day == "일") Color(0xFFFF10F0) else Color(0xFF00FFFF),
                            fontFamily = DungGeunMo,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(15.dp))

                // 날짜 그리드
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.height(240.dp)
                ) {
                    items(firstDay) { Box(Modifier.size(40.dp)) }

                    // 실제 날짜
                    items(daysInMonth) { i ->
                        val day = i + 1
                        val checked = attendedDays.contains(day)

                        Box(
                            Modifier
                                .padding(2.dp)
                                .aspectRatio(1f)
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

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(3.dp, Color(0xFFFFFF00))
                    .background(Color(0xFF1A1A2E))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎁", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "보상 안내",
                        color = Color(0xFFFFFF00),
                        fontFamily = DungGeunMo,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                // 보상 리스트 아이템
                RewardInfoRow("7일 연속", "특별 배지", Color(0xFF00FFFF))
                RewardInfoRow("30일 연속", "프리미엄 아이콘", Color(0xFF00FFFF))
                RewardInfoRow("100일 연속", "특수 효과", Color(0xFF00FFFF))
            }
        }

        // 모달
        if (showRewardModal) {
            AttendanceRewardModal(
                message = rewardMessage,
                onClose = { showRewardModal = false }
            )
        }
    }
}

@Composable
fun NeonCard(title: String, value: String, borderColor: Color, icon: ImageVector, iconColor: Color) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .border(3.dp, borderColor)
            .background(Color(0xFF1A1A2E))
            .padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = iconColor
        )
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            color = borderColor,
            fontFamily = DungGeunMo,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            color = Color.White,
            fontFamily = Fredoka,
            fontSize = 20.sp
        )
    }
}

@Composable
fun RewardInfoRow(condition: String, reward: String, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = condition,
            color = accentColor,
            fontFamily = Fredoka,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = reward,
            color = Color.White,
            fontFamily = DungGeunMo,
            fontSize = 13.sp
        )
    }
}

@Composable
fun AttendanceRewardModal(message: String, onClose: () -> Unit) {
    // 시스템 레벨에서 화면 중앙 자리 잡아줌
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        // 내부 UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable { /* 배경 클릭 시 닫히지 않게 빈 공간 유지 */ },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .border(4.dp, Color.Yellow)
                    .background(Color(0xFF1A1A2E))
                    .padding(24.dp)
                    .clickable(enabled = false) { }, // 콘텐츠 클릭 시 닫힘 방지
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉", fontSize = 40.sp)
                Spacer(Modifier.height(16.dp))

                Text(
                    "CONGRATS!",
                    color = Color.Yellow,
                    fontFamily = PressStart,
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = message,
                    color = Color.White,
                    fontFamily = Fredoka,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                // 확인 버튼
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB300FF)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                    modifier = Modifier.fillMaxWidth().border(2.dp, Color.Cyan)
                ) {
                    Text(
                        "확인",
                        color = Color.White,
                        fontFamily = DungGeunMo,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}