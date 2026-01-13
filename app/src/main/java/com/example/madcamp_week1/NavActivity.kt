package com.example.madcamp_week1

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity

enum class NavItem { MAIN, CATEGORIES, ATTENDANCECHECK, COMIC }

abstract class NavActivity : AppCompatActivity() {

    abstract val currentNavItem: NavItem

    protected fun setupBottomNav(
        mainBtn: View,
        categoriesBtn: View,
        attendanceCheckBtn: View,
        comicBtn: View
    ) {
        mainBtn.setOnClickListener {
            if (currentNavItem != NavItem.MAIN) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        attendanceCheckBtn.setOnClickListener {
            if (currentNavItem != NavItem.ATTENDANCECHECK) {
                startActivity(Intent(this, AttendanceCheckActivity::class.java))
                finish()
            }
        }

        categoriesBtn.setOnClickListener {
            if (currentNavItem != NavItem.CATEGORIES) {
                startActivity(Intent(this, CategoriesActivity::class.java))
                finish()
            }
        }

        comicBtn.setOnClickListener {
            if (currentNavItem != NavItem.COMIC) {
                startActivity(Intent(this, ComicActivity::class.java))
                finish()
            }
        }
    }
}
