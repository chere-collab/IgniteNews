package com.example.tryanderror

data class NewsArticle(
    val title: String?,
    val description: String?,
    val url: String?,
    val urlToImage: String?,
    val publishedAt: String?,
    val source: Source?,
    val content: String?
)

data class Source(
    val name: String?
)

data class NewsResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<NewsArticle>
)
