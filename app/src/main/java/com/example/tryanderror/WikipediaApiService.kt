package com.example.tryanderror

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface WikipediaApiService {
    @GET("api/rest_v1/page/summary/{title}")
    fun getPageSummary(
        @Path("title") title: String
    ): Call<WikipediaResponse>

    @GET("w/api.php?action=opensearch&format=json&limit=1")
    fun searchArticles(
        @retrofit2.http.Query("search") search: String
    ): Call<List<Any>>
}

data class WikipediaResponse(
    val title: String?,
    val extract: String?,
    val thumbnail: WikipediaThumbnail?,
    val content_urls: WikipediaContentUrls?
)

data class WikipediaThumbnail(
    val source: String?
)

data class WikipediaContentUrls(
    val mobile: WikipediaMobileUrls?
)

data class WikipediaMobileUrls(
    val page: String?
)
