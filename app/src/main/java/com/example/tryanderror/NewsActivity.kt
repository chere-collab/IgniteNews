package com.example.tryanderror

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.widget.SearchView
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import androidx.cardview.widget.CardView
import android.widget.ImageView
import android.widget.TextView
import android.view.ViewGroup
import android.view.LayoutInflater
import com.bumptech.glide.Glide

class NewsActivity : AppCompatActivity() {

    private lateinit var newsAdapter: NewsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var rvCategories: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var searchView: SearchView
    private lateinit var shimmerLayout: com.facebook.shimmer.ShimmerFrameLayout
    private lateinit var heroViewPager: ViewPager2
    private lateinit var carouselCard: CardView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    // Auto-scroll
    private val autoScrollHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var autoScrollRunnable: Runnable? = null

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val bookmarkedUrls = mutableSetOf<String>()
    
    private val gson = Gson()
    private val PREFS_NAME = "news_prefs"
    private val KEY_CACHE = "news_cache"
    private val KEY_CATEGORY = "default_category"
    private val KEY_LANGUAGE = "default_language"

    private var currentCategory: String? = "technology"
    private var currentLanguage = "en"
    private var currentSortBy = "publishedAt"
    private var currentArticles: List<NewsArticle> = emptyList()
    private var lastFetchTime = 0L // Feature 31: Throttling
    
    // API KEY (Unified)
    private val API_KEY = RetrofitInstance.API_KEY

    private lateinit var storyAdapter: StoryAdapter
    private lateinit var rvStories: RecyclerView
    private lateinit var suggestionCard: CardView
    private lateinit var suggestionAdapter: SuggestionAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)

        // Feature 1: The Stories Strip
        rvStories = findViewById(R.id.rvStories)
        rvStories.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        storyAdapter = StoryAdapter(emptyList()) { article -> handleStoryClick(article) }
        rvStories.adapter = storyAdapter

        // Feature 3: Check for search intent from Bookmarks
        val searchIntentQuery = intent.getStringExtra("search_query")
        
        // Enable Firestore Persistence for Offline Bookmarks (Permanent saving even with low signal)
        val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        db.firestoreSettings = settings

        recyclerView = findViewById(R.id.newsRecyclerView)
        progressBar = findViewById(R.id.newsProgressBar)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        rvCategories = findViewById(R.id.rvCategories)
        searchView = findViewById(R.id.newsSearchView)
        shimmerLayout = findViewById(R.id.shimmerLayout)
        heroViewPager = findViewById<ViewPager2>(R.id.heroViewPager)
        carouselCard = findViewById<CardView>(R.id.carouselCard)

        setupNavigationDrawer()

        // Feature 80: Setup Predictive Search
        suggestionCard = findViewById(R.id.suggestionCard)
        val rvSuggestions = findViewById<RecyclerView>(R.id.rvSuggestions)
        rvSuggestions.layoutManager = LinearLayoutManager(this)
        suggestionAdapter = SuggestionAdapter(emptyList()) { suggestion ->
            searchView.setQuery(suggestion, true)
            suggestionCard.visibility = View.GONE
        }
        rvSuggestions.adapter = suggestionAdapter

        val trendingTopics = listOf(
            "AI Innovation", "SpaceX", "Climate Change", "World Economy", 
            "Tech Trends", "Health Science", "Blockchain", "Renewable Energy",
            "Future of Work", "Global Politics", "Mars Mission", "Quantum Computing"
        )

        // Redesigned Header Elements
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.newsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        findViewById<View>(R.id.btnSortArrows).setOnClickListener {
            showSortMenu()
        }
        // Force Search Text Visibility (Theme-Aware)
        val searchEditText = searchView.findViewById<android.widget.EditText>(androidx.appcompat.R.id.search_src_text)
        val primaryColor = androidx.core.content.ContextCompat.getColor(this, R.color.text_primary)
        searchEditText?.setTextColor(primaryColor)
        searchEditText?.setHintTextColor(android.graphics.Color.GRAY)

        // Make three dots Orange (REMOVED: menu consolidated into drawer)
        // toolbar.overflowIcon?.setTint(orangeColor)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Setup Premium Category Bubbles
        val categories = listOf(
            Category("Following",  "following",  "⭐️", "#FFD700"),
            Category("Technology", "technology", "💻", "#FF5722"),
            Category("Politics",   "politics_search", "⚖️", "#607D8B"),
            Category("Business",   "business",   "💼", "#3F51B5"),
            Category("Sports",     "sports",     "⚽", "#4CAF50"),
            Category("Entertainment","entertainment","🎬","#9C27B0"),
            Category("Science",    "science",    "🔬", "#00BCD4"),
            Category("Health",     "health",     "❤️", "#E91E63")
        )

        categoryAdapter = CategoryAdapter(categories) { category ->
            currentCategory = category.key
            // Feature 12: Save Category Choice Permanently
            val prefsEditor = getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).edit()
            prefsEditor.putString(KEY_CATEGORY, category.key).apply()

            searchView.setQuery("", false)
            searchView.clearFocus()
            
            // Feature 64: Deep Channel Switching
            when (category.key) {
                "following" -> {
                    val userId = auth.currentUser?.uid ?: return@CategoryAdapter
                    val prefs = getSharedPreferences("PublisherPrefs_$userId", android.content.Context.MODE_PRIVATE)
                    val followed = prefs.getStringSet("followed_sources", emptySet()) ?: emptySet()
                    
                    if (followed.isEmpty()) {
                        currentArticles = emptyList()
                        updateAdapter()
                        Toast.makeText(this, "Follow some sources first!", Toast.LENGTH_LONG).show()
                    } else {
                        // Fetch from followed sources
                        fetchNews("following", showCarousel = false)
                    }
                }
                "politics_search" -> fetchSearchNews("world politics", showCarousel = false)
                else -> fetchNews(category.key, showCarousel = false)
            }
        }
        rvCategories.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false
        )
        rvCategories.adapter = categoryAdapter
        categoryAdapter.selectCategory(currentCategory ?: "technology")

        // Load Preferences
        val prefs = getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        currentCategory = prefs.getString(KEY_CATEGORY, "technology") ?: "technology"
        currentLanguage = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        categoryAdapter.selectCategory(currentCategory ?: "technology")
        
        // Feature 3: Apply the search intent if coming from bookmarks
        searchIntentQuery?.let {
            searchView.post {
                searchView.setQuery(it, true)
            }
        }

        // Load Bookmarks from Firestore
        loadBookmarks()

        // Feature 81: Handle Search with Predictive Suggestions
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    val fullQuery = getCurrentEffectiveQuery(query)
                    fetchSearchNews(fullQuery, showCarousel = false, sortBy = "relevancy")
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    suggestionCard.visibility = View.GONE
                } else {
                    val filtered = trendingTopics.filter { it.contains(newText, ignoreCase = true) }
                    if (filtered.isNotEmpty()) {
                        suggestionAdapter.updateSuggestions(filtered)
                        suggestionCard.visibility = View.VISIBLE
                    } else {
                        suggestionCard.visibility = View.GONE
                    }
                }
                return true
            }
        })

        // Feature 2: Scroll-to-Top Logic
        val fabScroll = findViewById<View>(R.id.fabScrollToTop)
        fabScroll.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                // Show FAB only after scrolling down a bit
                val offset = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                if (offset > 2) {
                    if (fabScroll.visibility == View.GONE) {
                        fabScroll.visibility = View.VISIBLE
                        fabScroll.alpha = 0f
                        fabScroll.animate().alpha(1f).setDuration(300).start()
                    }
                } else {
                    if (fabScroll.visibility == View.VISIBLE) {
                        fabScroll.animate().alpha(0f).setDuration(300).withEndAction {
                            fabScroll.visibility = View.GONE
                        }.start()
                    }
                }
            }
        })

        val orangeColor = androidx.core.content.ContextCompat.getColor(this, R.color.ignite_orange)
        swipeRefreshLayout.setColorSchemeColors(orangeColor)
        swipeRefreshLayout.setOnRefreshListener {
            // Feature 8: Haptic Feedback on Refresh
            swipeRefreshLayout.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            
            val query = searchView.query.toString()
            if (query.isNotBlank()) {
                val fullQuery = getCurrentEffectiveQuery(query)
                fetchSearchNews(fullQuery, true)
            } else {
                fetchNews(currentCategory ?: "technology", true)
            }
        }

        // Load from Cache First
        loadFromCache()
        
        // Subscribe to High-Priority News Alerts
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("technology")
        
        // Then fetch from network
        fetchNews(currentCategory!!)

        // Setup Bottom Navigation
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true // Already here
                R.id.nav_bookmarks -> {
                    startActivity(Intent(this, BookmarksActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    bottomNav.selectedItemId = R.id.nav_home
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    bottomNav.selectedItemId = R.id.nav_home
                    true
                }
                else -> false
            }
        }
        
        updateProfileIcon()
    }

    private fun setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        val btnMenu: android.widget.ImageButton = findViewById(R.id.btnMenu)

        // 🍔 Open drawer on hamburger click
        btnMenu.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
                drawerLayout.openDrawer(androidx.core.view.GravityCompat.START)
            }
        }

        // 🔗 Handle Header Info
        val headerView = navView.getHeaderView(0)
        val tvUserEmail = headerView.findViewById<TextView>(R.id.tvNavUserEmail)
        val ivProfile = headerView.findViewById<ImageView>(R.id.ivNavProfile)
        
        val user = auth.currentUser
        tvUserEmail.text = user?.email ?: "Guest User"
        
        // Show actual profile image if they have one!
        val prefs = getSharedPreferences("UserProfilePrefs", android.content.Context.MODE_PRIVATE)
        val base64String = prefs.getString("profile_${user?.uid}", null)
        if (base64String != null) {
            val bytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ivProfile.setImageBitmap(bitmap)
        } else if (user?.photoUrl != null) {
            Glide.with(this).load(user.photoUrl).circleCrop().into(ivProfile)
        }

        // 🧭 Navigation Item Selection
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    searchView.setQuery("", false)
                    searchView.clearFocus()
                    fetchNews(currentCategory ?: "technology")
                    recyclerView.smoothScrollToPosition(0)
                }
                R.id.nav_bookmarks -> {
                    startActivity(Intent(this, BookmarksActivity::class.java))
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                R.id.nav_share -> {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out IgniteNews! The elite way to stay informed. 🔥\nhttps://play.google.com/store/apps/details?id=com.example.tryanderror")
                    startActivity(Intent.createChooser(shareIntent, "Share with Colleagues"))
                }
                R.id.nav_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                }
                R.id.nav_privacy -> {
                    startActivity(Intent(this, LegalActivity::class.java))
                }
            }
            drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val newCat = prefs.getString(KEY_CATEGORY, "technology") ?: "technology"
        val newLang = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        
        if (newCat != currentCategory || newLang != currentLanguage) {
            currentCategory = newCat
            currentLanguage = newLang
            categoryAdapter.selectCategory(currentCategory ?: "technology")
            fetchNews(currentCategory!!)
        }
        
        // Refresh the profile icon in case they changed it in Settings!
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
                // Keep default tool icon
            }
        } else if (user.photoUrl != null) {
            Glide.with(this).asBitmap().load(user.photoUrl).circleCrop().into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                override fun onResourceReady(resource: android.graphics.Bitmap, transition: com.bumptech.glide.request.transition.Transition<in android.graphics.Bitmap>?) {
                    val drawable = androidx.core.graphics.drawable.RoundedBitmapDrawableFactory.create(resources, resource)
                    drawable.isCircular = true
                    profileItem.icon = drawable
                    profileItem.iconTintList = null // Stop the orange tint from affecting your face!
                }
                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
            })
        }
    }

    override fun onPause() {
        super.onPause()
        autoScrollRunnable?.let { autoScrollHandler.removeCallbacks(it) }
    }

    private fun saveToCache(articles: List<NewsArticle>) {
        val prefs = getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val json = gson.toJson(articles)
        prefs.edit().putString(KEY_CACHE, json).apply()
    }

    private fun loadFromCache() {
        val prefs = getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CACHE, null)
        if (json != null) {
            val type = object : TypeToken<List<NewsArticle>>() {}.type
            currentArticles = gson.fromJson(json, type)
            updateAdapter()
        }
    }

    private fun loadBookmarks() {
        // Switch to Local Storage to bypass Firestore permission issues!
        val userId = auth.currentUser?.uid ?: return
        val prefs = getSharedPreferences("BookmarkPrefs_$userId", android.content.Context.MODE_PRIVATE)
        val json = prefs.getString("saved_articles", "[]")
        val type = object : com.google.gson.reflect.TypeToken<MutableList<NewsArticle>>() {}.type
        val saved: MutableList<NewsArticle> = gson.fromJson(json, type) ?: mutableListOf()
        
        bookmarkedUrls.clear()
        saved.forEach { it.url?.let { url -> bookmarkedUrls.add(url) } }
        updateAdapter()
    }

    private fun handleBookmarkClick(article: NewsArticle, isChecked: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val prefs = getSharedPreferences("BookmarkPrefs_$userId", android.content.Context.MODE_PRIVATE)
        val json = prefs.getString("saved_articles", "[]")
        val type = object : com.google.gson.reflect.TypeToken<MutableList<NewsArticle>>() {}.type
        val saved: MutableList<NewsArticle> = gson.fromJson(json, type) ?: mutableListOf()

        if (isChecked) {
            if (!saved.any { it.url == article.url }) {
                saved.add(article)
                bookmarkedUrls.add(article.url!!)
                android.widget.Toast.makeText(this, "Saved to Bookmarks!", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            saved.removeAll { it.url == article.url }
            bookmarkedUrls.remove(article.url)
            android.widget.Toast.makeText(this, "Removed from Bookmarks", android.widget.Toast.LENGTH_SHORT).show()
        }

        // Save back to Local Storage
        prefs.edit().putString("saved_articles", gson.toJson(saved)).apply()
        newsAdapter.updateBookmarks(bookmarkedUrls.toSet())
        
        // Tactile vibration
        findViewById<View>(android.R.id.content).performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun fetchNews(category: String, wasRefreshing: Boolean = false, showCarousel: Boolean = true) {
        // Feature 66: Special 'Following' Logic
        if (category == "following") {
            val userId = auth.currentUser?.uid ?: return
            val prefs = getSharedPreferences("PublisherPrefs_$userId", android.content.Context.MODE_PRIVATE)
            val followed = prefs.getStringSet("followed_sources", emptySet()) ?: emptySet()
            
            if (followed.isEmpty()) {
                currentArticles = emptyList()
                updateAdapter()
                Toast.makeText(this, "Your personal feed is empty. Follow some sources!", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Perform a high-precision multi-source search
            val query = followed.joinToString(" OR ") { "\"$it\"" } // Exact source name matching
            fetchSearchNews(query, wasRefreshing, showCarousel = false, sortBy = "publishedAt")
            return
        }

        // Feature 52: Smart Language Throttling
        val now = System.currentTimeMillis()
        if (!wasRefreshing && now - lastFetchTime < 30_000 && category == currentCategory && currentLanguage == lastFetchLanguage) {
            loadFromCache()
            return
        }

        setLoading(true)
        lastFetchLanguage = currentLanguage // Track which language we just fetched
        carouselCard.visibility = if (showCarousel) View.VISIBLE else View.GONE
        
        RetrofitInstance.api.getTopHeadlines(category = category, language = currentLanguage, apiKey = API_KEY)
            .enqueue(createNewsCallback(showCarousel, wasRefreshing, category))
    }

    private var lastFetchLanguage: String? = null

    private fun fetchSearchNews(query: String, wasRefreshing: Boolean = false, showCarousel: Boolean = false, sortBy: String = currentSortBy) {
        setLoading(true)
        lastFetchQuery = query
        carouselCard.visibility = if (showCarousel) View.VISIBLE else View.GONE
        RetrofitInstance.api.searchNews(query = query, language = currentLanguage, sortBy = sortBy, apiKey = API_KEY)
            .enqueue(createNewsCallback(false, wasRefreshing, query))
    }

    private var lastFetchQuery: String? = null

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Menu items moved to side drawer
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    private fun showSortMenu() {
        // Anchor to the new arrow button
        val popup = androidx.appcompat.widget.PopupMenu(this, findViewById(R.id.btnSortArrows))
        val query = searchView.query.toString()

        popup.menu.add("Newest First").setOnMenuItemClickListener {
            if (query.isNotBlank()) {
                currentSortBy = "publishedAt"
                val fullQuery = getCurrentEffectiveQuery(query)
                fetchSearchNews(fullQuery)
            } else {
                // Manual Sort Local List (A-Z or Date)
                currentArticles = currentArticles.sortedByDescending { it.publishedAt }
                updateAdapter()
            }
            true
        }

        popup.menu.add("Popularity").setOnMenuItemClickListener {
            if (query.isNotBlank()) {
                currentSortBy = "popularity"
                val fullQuery = getCurrentEffectiveQuery(query)
                fetchSearchNews(fullQuery)
            } else {
                Toast.makeText(this, "Popularity sort is only available in Search", Toast.LENGTH_SHORT).show()
            }
            true
        }

        popup.menu.add("Alphabetical (A-Z)").setOnMenuItemClickListener {
            // Manual sort the existing list alphabetically by title
            currentArticles = currentArticles.sortedBy { it.title }
            updateAdapter()
            true
        }

        popup.show()
    }


    private fun getCurrentEffectiveQuery(userQuery: String): String {
        val context = when (currentCategory) {
            "politics_search" -> "politics"
            "following" -> ""
            null -> ""
            else -> currentCategory
        }
        
        return if (context.isNullOrBlank()) {
            userQuery
        } else {
            "+($userQuery) +$context"
        }
    }

    private fun refreshCurrent() {
        val query = searchView.query.toString()
        if (query.isNotBlank()) {
            val fullQuery = getCurrentEffectiveQuery(query)
            fetchSearchNews(fullQuery)
        } else {
            fetchNews(currentCategory ?: "technology")
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            if (!swipeRefreshLayout.isRefreshing) {
                shimmerLayout.visibility = View.VISIBLE
                shimmerLayout.startShimmer()
                recyclerView.visibility = View.GONE
            }
        } else {
            shimmerLayout.stopShimmer()
            shimmerLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            
            if (swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
                vibrate()
            }
        }
    }

    private fun vibrate() {
        val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(50)
        }
    }

    private fun createNewsCallback(isHome: Boolean, wasRefreshingOnStart: Boolean = false, contextLabel: String = "") = object : Callback<NewsResponse> {
        override fun onResponse(call: Call<NewsResponse>, response: Response<NewsResponse>) {
            val isUserRefresh = wasRefreshingOnStart || swipeRefreshLayout.isRefreshing
            setLoading(false)
            if (response.isSuccessful && response.body() != null) {
                lastFetchTime = System.currentTimeMillis()
                var newArticles = response.body()!!.articles
                
                // Feature 66: Strict Source Filtering for 'Following' Feed
                if (currentCategory == "following") {
                    val userId = auth.currentUser?.uid ?: return
                    val prefs = getSharedPreferences("PublisherPrefs_$userId", android.content.Context.MODE_PRIVATE)
                    val followed = prefs.getStringSet("followed_sources", emptySet()) ?: emptySet()
                    
                    newArticles = newArticles.filter { article ->
                        followed.contains(article.source?.name)
                    }
                }

                if (newArticles.isEmpty()) {
                    if (currentCategory == "following") {
                        Toast.makeText(this@NewsActivity, "No recent stories from these specific sources.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@NewsActivity, "No results for [$contextLabel] in [$currentLanguage]", Toast.LENGTH_LONG).show()
                    }
                } else {
                    saveToCache(newArticles)
                    SmartCache.saveOfflineArticles(this@NewsActivity, newArticles)
                    
                    if (isHome) {
                        val heroList = newArticles.take(5)
                        val feedList = newArticles.drop(5)
                        setupHeroCarousel(heroList)
                        currentArticles = feedList
                    } else {
                        carouselCard.visibility = View.GONE
                        currentArticles = newArticles
                    }
                    if (isUserRefresh) {
                        Toast.makeText(this@NewsActivity, "You're up to date!", Toast.LENGTH_SHORT).show()
                    }
                }
                updateAdapter()
            } else {
                if (response.code() == 429) {
                    Toast.makeText(this@NewsActivity, "Searching fast! Take a 30s breather...", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@NewsActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
                loadFromCache()
            }
        }

        override fun onFailure(call: Call<NewsResponse>, t: Throwable) {
            setLoading(false)
            loadFromCache()
        }
    }

    private fun setupHeroCarousel(articles: List<NewsArticle>) {
        if (articles.isEmpty()) {
            carouselCard.visibility = View.GONE
            return
        }
        carouselCard.visibility = View.VISIBLE
        val adapter = HeroAdapter(
            articles, 
            onClick = { article, imageView ->
                recordView()
                val intent = Intent(this@NewsActivity, NewsDetailActivity::class.java)
                intent.putExtra("url", article.url)
                intent.putExtra("title", article.title)
                intent.putExtra("description", article.description)
                intent.putExtra("urlToImage", article.urlToImage)
                intent.putExtra("source", article.source?.name)
                
                val options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this@NewsActivity,
                    imageView,
                    "shared_article_image"
                )
                startActivity(intent, options.toBundle())
            },
            onSourceClick = { sourceName ->
                if (!sourceName.isNullOrBlank()) {
                    searchView.setQuery(sourceName, true)
                }
            }
        )
        heroViewPager.adapter = adapter

        // Auto-scroll every 3.5 seconds
        autoScrollRunnable?.let { autoScrollHandler.removeCallbacks(it) }
        autoScrollRunnable = object : Runnable {
            override fun run() {
                val count = adapter.itemCount
                if (count > 1) {
                    val next = (heroViewPager.currentItem + 1) % count
                    heroViewPager.setCurrentItem(next, true)
                }
                autoScrollHandler.postDelayed(this, 3500)
            }
        }
        autoScrollHandler.postDelayed(autoScrollRunnable!!, 3500)
    }

    private fun updateAdapter() {
        if (!::newsAdapter.isInitialized) {
            newsAdapter = NewsAdapter(
                articles = currentArticles,
                bookmarkedUrls = bookmarkedUrls,
                onBookmarkClick = { article, isChecked -> handleBookmarkClick(article, isChecked) },
                onItemClick = { article, imageView ->
                    // Record View Statistic
                    recordView()
                    
                    val intent = Intent(this@NewsActivity, NewsDetailActivity::class.java)
                    intent.putExtra("url", article.url)
                    intent.putExtra("title", article.title)
                    intent.putExtra("description", article.description)
                    intent.putExtra("urlToImage", article.urlToImage)
                    intent.putExtra("source", article.source?.name)
                    
                    val options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this@NewsActivity,
                        imageView,
                        "shared_article_image"
                    )
                    startActivity(intent, options.toBundle())
                },
                onSourceClick = { sourceName ->
                    if (!sourceName.isNullOrBlank()) {
                        searchView.setQuery(sourceName, true) // Triggers the search query listener
                    }
                }
            )
            recyclerView.adapter = newsAdapter
        } else {
            newsAdapter.updateArticles(currentArticles)
            newsAdapter.updateBookmarks(bookmarkedUrls.toSet())
        }
        
        // Feature 59: Update the visual Stories strip
        storyAdapter.updateStories(currentArticles.take(8))
    }

    private fun handleStoryClick(article: NewsArticle) {
        val intent = Intent(this@NewsActivity, NewsDetailActivity::class.java)
        intent.putExtra("url", article.url)
        intent.putExtra("title", article.title)
        intent.putExtra("urlToImage", article.urlToImage)
        intent.putExtra("source", article.source?.name)
        startActivity(intent)
    }

    private fun recordView() {
        val userId = auth.currentUser?.uid ?: return
        val view = hashMapOf(
            "timestamp" to com.google.firebase.Timestamp.now()
        )
        db.collection("users").document(userId).collection("views").add(view)
    }

    inner class HeroAdapter(
        private val articles: List<NewsArticle>, 
        private val onClick: (NewsArticle, ImageView) -> Unit,
        private val onSourceClick: (String?) -> Unit
    ) : RecyclerView.Adapter<HeroAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.heroImage)
            val title: TextView = view.findViewById(R.id.heroTitle)
            val source: TextView = view.findViewById(R.id.heroSource)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hero_news, parent, false)
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val article = articles[position]
            holder.title.text = article.title
            holder.source.text = article.source?.name ?: "Ignite News"
            holder.source.setTextColor(androidx.core.content.ContextCompat.getColor(this@NewsActivity, R.color.ignite_orange))
            holder.source.setOnClickListener { onSourceClick(article.source?.name) }
            
            holder.image.transitionName = "shared_article_image"
            Glide.with(holder.itemView.context)
                .load(article.urlToImage)
                .error(android.R.drawable.ic_menu_gallery)
                .into(holder.image)
            holder.itemView.setOnClickListener { onClick(article, holder.image) }
        }
        override fun getItemCount() = articles.size
    }
}
