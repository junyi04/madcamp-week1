package com.example.madcamp_week1.ui.comic

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.madcamp_week1.R
import com.example.madcamp_week1.ui.theme.DungGeunMo
import com.example.madcamp_week1.ui.theme.PressStart

data class ComicPanel(
    val imageResId: Int,
    val dialog: String,
    val backgroundColor: Color = Color(0xFFFFFBE6)
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ComicTransitionPage(
    onComicFinished: () -> Unit
) {
    val comicPanels = remember {
        listOf(
            ComicPanel(
                imageResId = R.drawable.ic_young_img1, // 이미지 1
                dialog = "허거덩스!! 이런 갓-앱을\n이제야 알게 되다니...!",
                backgroundColor = Color(0xFFE8F5E9),
            ),
            ComicPanel(
                imageResId = R.drawable.ic_young_img2, // 이미지 2
                dialog = "좋아, 나도 이 앱을 통해 MZ 세대를 따라잡겠어!!",
                backgroundColor = Color(0xFFE3F2FD)
            ),
            ComicPanel(
                imageResId = R.drawable.ic_young_img3, // 이미지 3
                dialog = "밤티 인생 청산이다!! 영포티 가보자고~!!",
                backgroundColor = Color(0xFFFBE9E7)
            )
        )
    }

    var currentPanelIndex by remember { mutableStateOf(0) }
    val currentPanel = comicPanels[currentPanelIndex]

    // 페이지 전환 애니메이션
    AnimatedContent(
        targetState = currentPanelIndex,
        transitionSpec = {
            if (targetState > initialState) {
                slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
            } else {
                slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
            }.using(
                SizeTransform(clip = false)
            )
        },
        label = "comic_panel_transition"
    ) { targetPanelIndex ->
        val panel = comicPanels[targetPanelIndex]

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(panel.backgroundColor)
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 만화 이미지
            Image(
                painter = painterResource(id = panel.imageResId),
                contentDescription = null,
                contentScale = ContentScale.Crop, // 이미지가 잘리더라도 꽉 채움
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(16.dp)) // 만화 컷 테두리
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 말풍선 대사
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(500, delayMillis = 200)),
                exit = fadeOut(animationSpec = tween(200)),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.Black, RoundedCornerShape(12.dp))
                    .padding(vertical = 16.dp, horizontal = 20.dp)
            ) {
                Text(
                    text = panel.dialog,
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontFamily = DungGeunMo,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 다음 버튼
            Button(
                onClick = {
                    if (currentPanelIndex < comicPanels.size - 1) {
                        currentPanelIndex++ // 다음 컷으로 이동
                    } else {
                        onComicFinished() // 만화 끝
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = if (currentPanelIndex < comicPanels.size - 1) "▶  NEXT" else "▶  END",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PressStart
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewComicTransitionPage() {
    ComicTransitionPage(onComicFinished = {})
}