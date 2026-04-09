package com.example.tryanderror;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SmartCache {
    private static final String PREFS_NAME = "OfflineCache";
    private static final String KEY_ARTICLES = "cached_articles";

    public static void saveOfflineArticles(Context context, List<NewsArticle> articles) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Only keep top 5 for fast performance
        List<NewsArticle> topArticles = articles.size() > 5 ? articles.subList(0, 5) : articles;
        String json = new Gson().toJson(topArticles);
        prefs.edit().putString(KEY_ARTICLES, json).apply();
    }

    public static List<NewsArticle> getOfflineArticles(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_ARTICLES, "[]");
        Type type = new TypeToken<List<NewsArticle>>() {}.getType();
        List<NewsArticle> articles = new Gson().fromJson(json, type);
        return articles != null ? articles : new ArrayList<>();
    }

    public static NewsArticle getCachedByUrl(Context context, String url) {
        List<NewsArticle> articles = getOfflineArticles(context);
        for (NewsArticle article : articles) {
            if (article.getUrl() != null && article.getUrl().equals(url)) {
                return article;
            }
        }
        return null;
    }
}
