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
import com.google.firebase.firestore.FirebaseFirestore

class BookmarksActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var newsAdapter: NewsAdapter
    private lateinit var emptyText: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val bookmarkedArticles = mutableListOf<NewsArticle>()
    private val bookmarkedUrls = mutableSetOf<String>()
    private val gson = com.google.gson.Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmarks)

        val toolbar = findViewById<Toolbar>(R.id.bookmarksToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        recyclerView = findViewById(R.id.bookmarksRecyclerView)
        emptyText = findViewById(R.id.emptyBookmarksText)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Setup Bottom Nav (Synchronized)
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_bookmarks
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, NewsActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                    true
                }
                R.id.nav_bookmarks -> true
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                    true
                }
                else -> false
            }
        }

        loadBookmarks()
        setupSwipeToDelete()
        updateProfileIcon()
    }

    override fun onResume() {
        super.onResume()
        updateProfileIcon()
    }

    private fun updateProfileIcon() {
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
        val profileItem = bottomNav.menu.findItem(R.id.nav_settings)
        val user = auth.currentUser ?: return
        
        val prefs = getSharedPreferences("UserProfilePrefs", android.content.Context.MODE_PRIVATE)
        val base64String = prefs.getString("profile_${user.uid}", null)
        
        if (base64String != null && base64String.isNotEmpty()) {
            try {
                val bytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                val roundedDrawable = androidx.core.graphics.drawable.RoundedBitmapDrawableFactory.create(resources, bitmap)
                roundedDrawable.isCircular = true
                profileItem.icon = roundedDrawable
                profileItem.iconTintList = null // Stop the orange tint from affecting your face!
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT or androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean = false
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val article = bookmarkedArticles[position]
                
                // Keep backup for UNDO
                val backupUrl = article.url
                
                // Snap-Remove from list
                bookmarkedArticles.removeAt(position)
                backupUrl?.let { bookmarkedUrls.remove(it) }
                newsAdapter.notifyItemRemoved(position)
                
                if (bookmarkedArticles.isEmpty()) emptyText.visibility = View.VISIBLE

                // Show Premium UNDO Logic
                val snackbar = com.google.android.material.snackbar.Snackbar.make(
                    recyclerView, "Bookmark Removed", com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                )
                snackbar.setAction("UNDO") {
                    bookmarkedArticles.add(position, article)
                    backupUrl?.let { bookmarkedUrls.add(it) }
                    newsAdapter.notifyItemInserted(position)
                    if (bookmarkedArticles.isNotEmpty()) emptyText.visibility = View.GONE
                    recyclerView.scrollToPosition(position)
                }
                
                // If the user doesn't hit Undo, delete permanently
                snackbar.addCallback(object : com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback<com.google.android.material.snackbar.Snackbar>() {
                    override fun onDismissed(transientBottomBar: com.google.android.material.snackbar.Snackbar?, event: Int) {
                        if (event != DISMISS_EVENT_ACTION) {
                            finalizeDelete(article)
                        }
                    }
                })
                snackbar.show()
            }
        }
        androidx.recyclerview.widget.ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }

    private fun finalizeDelete(article: NewsArticle) {
        val userId = auth.currentUser?.uid ?: return
        val prefs = getSharedPreferences("BookmarkPrefs_$userId", android.content.Context.MODE_PRIVATE)
        val json = prefs.getString("saved_articles", "[]")
        val type = object : com.google.gson.reflect.TypeToken<MutableList<NewsArticle>>() {}.type
        val saved: MutableList<NewsArticle> = gson.fromJson(json, type) ?: mutableListOf()

        saved.removeAll { it.url == article.url }
        prefs.edit().putString("saved_articles", gson.toJson(saved)).apply()
    }

    private fun loadBookmarks() {
        val userId = auth.currentUser?.uid ?: return
        val prefs = getSharedPreferences("BookmarkPrefs_$userId", android.content.Context.MODE_PRIVATE)
        val json = prefs.getString("saved_articles", "[]")
        val type = object : com.google.gson.reflect.TypeToken<MutableList<NewsArticle>>() {}.type
        val saved: MutableList<NewsArticle> = gson.fromJson(json, type) ?: mutableListOf()

        bookmarkedArticles.clear()
        bookmarkedUrls.clear()
        
        bookmarkedArticles.addAll(saved)
        saved.forEach { it.url?.let { url -> bookmarkedUrls.add(url) } }

        if (bookmarkedArticles.isEmpty()) {
            emptyText.visibility = View.VISIBLE
        } else {
            emptyText.visibility = View.GONE
        }

        updateAdapter()
    }

    private fun updateAdapter() {
        newsAdapter = NewsAdapter(
            articles = bookmarkedArticles,
            bookmarkedUrls = bookmarkedUrls,
            onBookmarkClick = { article, isChecked ->
                if (!isChecked) {
                    removeBookmark(article)
                }
            },
            onItemClick = { article, imageView ->
                val intent = Intent(this, NewsDetailActivity::class.java)
                intent.putExtra("url", article.url)
                intent.putExtra("title", article.title)
                intent.putExtra("description", article.description)
                intent.putExtra("urlToImage", article.urlToImage)
                intent.putExtra("source", article.source?.name)
                
                val options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this@BookmarksActivity,
                    imageView,
                    "shared_article_image"
                )
                startActivity(intent, options.toBundle())
            },
            onSourceClick = { sourceName ->
                // Return to main and search
                val intent = Intent(this@BookmarksActivity, NewsActivity::class.java)
                intent.putExtra("search_query", sourceName)
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
            }
        )
        recyclerView.adapter = newsAdapter
    }

    private fun removeBookmark(article: NewsArticle) {
        val prefs = getSharedPreferences("BookmarkPrefs", android.content.Context.MODE_PRIVATE)
        val json = prefs.getString("saved_articles", "[]")
        val type = object : com.google.gson.reflect.TypeToken<MutableList<NewsArticle>>() {}.type
        val saved: MutableList<NewsArticle> = gson.fromJson(json, type) ?: mutableListOf()

        saved.removeAll { it.url == article.url }
        
        // Save back
        prefs.edit().putString("saved_articles", gson.toJson(saved)).apply()
        
        // Reload UI
        loadBookmarks()
    }
}
