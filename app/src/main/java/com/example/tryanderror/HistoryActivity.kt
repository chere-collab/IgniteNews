package com.example.tryanderror

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: NewsAdapter
    private val auth = FirebaseAuth.getInstance()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val toolbar = findViewById<Toolbar>(R.id.historyToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recyclerView = findViewById(R.id.historyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        historyAdapter = NewsAdapter(
            articles = emptyList(),
            bookmarkedUrls = emptySet(),
            onBookmarkClick = { article, isBookmarked -> 
                // Toggle bookmarks from history
            },
            onItemClick = { article, imageView ->
                val intent = Intent(this, NewsDetailActivity::class.java)
                intent.putExtra("url", article.url)
                intent.putExtra("title", article.title)
                intent.putExtra("source", article.source?.name)
                intent.putExtra("urlToImage", article.urlToImage)
                startActivity(intent)
            },
            onSourceClick = { source ->
                val intent = Intent(this, NewsActivity::class.java)
                intent.putExtra("search_query", source)
                startActivity(intent)
            }
        )
        recyclerView.adapter = historyAdapter

        loadHistory()
    }

    private fun loadHistory() {
        val user = auth.currentUser ?: return
        val prefs = getSharedPreferences("HistoryPrefs_${user.uid}", android.content.Context.MODE_PRIVATE)
        val json = prefs.getString("saved_history", "[]")
        val type = object : TypeToken<MutableList<NewsArticle>>() {}.type
        val articles: MutableList<NewsArticle> = gson.fromJson(json, type) ?: mutableListOf()

        val emptyText = findViewById<TextView>(R.id.emptyHistoryText)
        if (articles.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            emptyText.text = "History is empty. Read some articles first! 📖"
        } else {
            emptyText.visibility = View.GONE
            historyAdapter.updateArticles(articles)
        }
    }
}
