package com.example.madcamp_week1

data class VideoData (
    val id: String,
    val title: String,
    val author: String,
    val views: Long,
    val likes: Int,
    val url: String,
    val imageFile: String,
    val category: String? = null
)