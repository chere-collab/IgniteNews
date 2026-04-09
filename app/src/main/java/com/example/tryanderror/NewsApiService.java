package com.example.tryanderror;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NewsApiService {
    @GET("v2/top-headlines")
    Call<NewsResponse> getTopHeadlines(
        @Query("country") String country,
        @Query("category") String category,
        @Query("sources") String sources,
        @Query("language") String language,
        @Query("apiKey") String apiKey
    );

    @GET("v2/everything")
    Call<NewsResponse> searchNews(
        @Query("q") String query,
        @Query("language") String language,
        @Query("sortBy") String sortBy,
        @Query("apiKey") String apiKey
    );
}
