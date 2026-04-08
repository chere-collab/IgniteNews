package com.example.tryanderror

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("v2/top-headlines")
    fun getTopHeadlines(
        @Query("country") country: String? = null,
        @Query("category") category: String? = null,
        @Query("sources") sources: String? = null,
        @Query("language") language: String? = "en",
        @Query("apiKey") apiKey: String
    ): Call<NewsResponse>

    @GET("v2/everything")
    fun searchNews(
        @Query("q") query: String,
        @Query("language") language: String = "en",
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("apiKey") apiKey: String
    ): Call<NewsResponse>
}
