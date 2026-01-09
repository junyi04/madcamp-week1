package com.example.madcamp_week1

import com.google.gson.annotations.SerializedName

data class VideoData (
    val id: Long,
    val title: String,
    val author: String,
    val views: String,
    val likes: String,
    val url: String,
    val category: String,
    @SerializedName("image_file")
    val imageFile: String
)