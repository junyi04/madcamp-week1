package com.example.madcamp_week1

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.madcamp_week1.ui.theme.PressStart

val RetroDark = Color(0xFF1A1A2E)
val RetroPink = Color(0xFFFF10F0)
val RetroPurple = Color(0xFFB300FF)
val RetroCyan = Color(0xFF00FFFF)
val RetroYellow = Color(0xFFFFFF00)
val RetroOrange = Color(0xFFFF6600)

data class OnboardingStep(
    val title: String,
    val subtitle: String,
    val icon: String,
    val description: String,
    val colors: List<Color>,
    val borderColor: Color
)

@Composable
fun OnboardingModal(
    isOpen: Boolean,
    onComplete: () -> Unit
) {
    if (!isOpen) return

    var step by remember { mutableStateOf(0) }
    var isVisible by remember { mutableStateOf(false) }

    val steps = listOf(
        OnboardingStep("영포티 밈 저장소", "난 아직 젊다!", "🎬", "20대와 함께 걸어가기 위해 준비했어요", listOf(RetroPink, RetroPurple), RetroPink),
        OnboardingStep("매일 Top10 업데이트", "놓치지 마세요!", "🔥", "가장 핫한 밈들을 매일 확인해보세요", listOf(RetroYellow, RetroOrange), RetroYellow),
        OnboardingStep("매일 방문해서 보상받기", "출석체크 시스템", "🎁", "연속 출석으로 특별한 보상을 얻어요", listOf(RetroCyan, Color.Green), RetroCyan)
    )

    val currentStep = steps[step]

    // 등장 애니메이션 효과
    LaunchedEffect(isOpen, step) {
        isVisible = true
    }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // 모달 카드
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(initialScale = 0.95f) + fadeIn(),
                exit = scaleOut(targetScale = 0.95f) + fadeOut()
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RetroDark, RoundedCornerShape(8.dp))
                        .border(4.dp, currentStep.borderColor, RoundedCornerShape(8.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. 진행 표시기 (Progress Bar)
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        steps.forEachIndexed { index, _ ->
                            Box(
                                modifier = Modifier
                                    .height(4.dp)
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .background(
                                        when {
                                            index == step -> Brush.horizontalGradient(currentStep.colors)
                                            index < step -> Brush.horizontalGradient(listOf(RetroCyan, RetroCyan))
                                            else -> Brush.horizontalGradient(listOf(Color(0xFF333333), Color(0xFF333333)))
                                        },
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }

                    // 2. 아이콘
                    Text(
                        text = currentStep.icon,
                        fontSize = 48.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 3. 제목 & 부제목
                    Text(
                        text = currentStep.title,
                        color = RetroCyan,
                        fontSize = 18.sp,
                        fontFamily = PressStart,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = currentStep.subtitle,
                        style = TextStyle(
                            brush = Brush.horizontalGradient(currentStep.colors),
                            fontSize = 14.sp,
                            fontFamily = PressStart,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = currentStep.description,
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp,
                        fontFamily = PressStart,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // 4. 특징 항목
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black, RoundedCornerShape(8.dp))
                            .border(2.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when(step) {
                            0 -> {
                                FeatureItem(Icons.Default.Star, "틱톡 크롤링 기반 Top10", RetroCyan)
                                FeatureItem(Icons.Default.Favorite, "매일 자정 최신 밈 업데이트", RetroYellow)
                            }
                            1 -> {
                                FeatureItem(Icons.Default.KeyboardArrowRight, "춤, 챌린지, 음식, TTS 등", RetroPink)
                                FeatureItem(Icons.Default.KeyboardArrowRight, "카테고리별 유행 밈", RetroCyan)
                            }
                            2 -> {
                                FeatureItem(Icons.Default.Favorite, "매일 특별한 보상", RetroYellow)
                                FeatureItem(Icons.Default.Favorite, "연속 출석 달력", RetroPink)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 5. 버튼 레이어
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (step > 0) {
                            Button(
                                onClick = {
                                    isVisible = false
                                    // 애니메이션 후 상태 변경을 위해 약간의 딜레이
                                    step--
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("이전", color = Color.White)
                            }
                        }

                        Button(
                            onClick = {
                                if (step < 2) {
                                    step++
                                } else {
                                    onComplete()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = currentStep.borderColor),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(if (step == 2) "시작하기" else "다음", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 6. 건너뛰기
                    TextButton(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
                        Text("건너뛰기", color = Color(0xFF666666), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = color, fontSize = 11.sp)
    }
}