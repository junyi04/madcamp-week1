package com.example.madcamp_week1

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.madcamp_week1.databinding.ActivityComicBinding
import com.example.madcamp_week1.ui.comic.ComicTransitionPage

class ComicActivity : NavActivity() {

    override val currentNavItem: NavItem = NavItem.COMIC
    private lateinit var binding: ActivityComicBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityComicBinding.inflate(layoutInflater)

        setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                // 만화 컴포즈 페이지
                ComicTransitionPage(
                    onComicFinished = {
                        // 만화가 다 끝나면 메인으로 보내기
                         startActivity(Intent(this@ComicActivity, MainActivity::class.java))
                         finish()
                    }
                )

                AndroidView(
                    factory = {
                        val view = binding.includeBottomNav.root
                        (view.parent as? android.view.ViewGroup)?.removeView(view)
                        view
                    },
                )
            }
        }

        setupBottomNav(
            binding.includeBottomNav.mainBtn,
            binding.includeBottomNav.categoriesBtn,
            binding.includeBottomNav.attendanceCheckBtn,
            binding.includeBottomNav.comicBtn
        )
    }
}