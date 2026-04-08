package com.example.tryanderror

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient

object WikipediaInstance {
    private const val BASE_URL = "https://en.wikipedia.org/"

    private val client = OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("User-Agent", "IgniteNews_Android_App/1.0 (contact: info@example.com)") // REQUIRED by Wikipedia API
            .build()
        chain.proceed(request)
    }.build()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: WikipediaApiService by lazy {
        retrofit.create(WikipediaApiService::class.java)
    }
}
