package com.example.madcamp_week1

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    // top10 화면
    @GET("top10")
    fun getVideoData(): Call<List<VideoData>>

    // 카테고리 화면
    @GET("category/{name}")
    fun getCategoryData(
        @Path("name") categoryName: String
    ): Call<List<VideoData>>
}