package com.example.tryanderror;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitInstance {
    private static final String BASE_URL = "https://newsapi.org/";
    public static final String API_KEY = BuildConfig.NEWS_API_KEY;

    private static NewsApiService apiService = null;

    private RetrofitInstance() {}

    public static NewsApiService getApi() {
        if (apiService == null) {
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
                Request request = chain.request().newBuilder()
                    .addHeader("User-Agent", "TryAndErrorApp")
                    .build();
                return chain.proceed(request);
            }).build();

            Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

            apiService = retrofit.create(NewsApiService.class);
        }
        return apiService;
    }
}
