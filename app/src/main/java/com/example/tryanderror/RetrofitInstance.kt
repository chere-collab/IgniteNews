package com.example.tryanderror

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val BASE_URL = "https://newsapi.org/"
    val API_KEY = com.example.tryanderror.BuildConfig.NEWS_API_KEY

    private val client = OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("User-Agent", "TryAndErrorApp")
            .build()
        chain.proceed(request)
    }.build()

    val api: NewsApiService
        get() = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NewsApiService::class.java)
}
