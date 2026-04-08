package com.example.tryanderror

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SmartCache {
    private const val PREFS_NAME = "OfflineCache"
    private const val KEY_ARTICLES = "cached_articles"

    fun saveOfflineArticles(context: Context, articles: List<NewsArticle>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val topArticles = articles.take(5) // Only keep top 5 for fast performance
        val json = Gson().toJson(topArticles)
        prefs.edit().putString(KEY_ARTICLES, json).apply()
    }

    fun getOfflineArticles(context: Context): List<NewsArticle> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ARTICLES, "[]")
        val type = object : TypeToken<List<NewsArticle>>() {}.type
        return Gson().fromJson(json, type) ?: emptyList()
    }
    
    fun getCachedByUrl(context: Context, url: String): NewsArticle? {
        val articles = getOfflineArticles(context)
        return articles.find { it.url == url }
    }
}
