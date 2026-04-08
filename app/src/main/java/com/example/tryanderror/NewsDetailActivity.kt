package com.example.tryanderror

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.MaterialToolbar
import com.bumptech.glide.Glide
import com.google.android.material.appbar.CollapsingToolbarLayout
import java.util.Locale

class NewsDetailActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private var tts: TextToSpeech? = null
    
    private var currentTextZoom = 100
    private var isReaderMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_detail)

        val toolbar = findViewById<Toolbar>(R.id.detailToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        webView = findViewById(R.id.newsWebView)
        progressBar = findViewById(R.id.detailProgressBar)

        val headerImage = findViewById<ImageView>(R.id.detailHeaderImage)
        val collapsingToolbar = findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbar)
        
        // Extract data
        val url = intent.getStringExtra("url")
        val imageUrl = intent.getStringExtra("urlToImage")
        val title = intent.getStringExtra("title")
        val source = intent.getStringExtra("source") ?: "IgniteNews"

        collapsingToolbar.title = source
        
        // Load the beautiful parallax image
        if (!imageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(imageUrl)
                .into(headerImage)
        }

        // 📜 Track Reading History (Secure Cloud-First)
        addToHistory(url, title, source, imageUrl)

        // Feature 7: Reading Progress Logic
        val readingBar = findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.readingProgressBar)
        val nestedScroll = findViewById<androidx.core.widget.NestedScrollView>(R.id.detailNestedScroll) // Wait! I need to ensure ID is set.

        nestedScroll.setOnScrollChangeListener { v: androidx.core.widget.NestedScrollView, _, scrollY, _, _ ->
            val totalContentHeight = v.getChildAt(0).measuredHeight - v.measuredHeight
            if (totalContentHeight > 0) {
                val progress = (scrollY.toFloat() / totalContentHeight * 100).toInt()
                readingBar.progress = progress
            }
        }

        tts = TextToSpeech(this, this)

        val detailSourceText = findViewById<TextView>(R.id.tvDetailSource)
        val chipFollow = findViewById<com.google.android.material.chip.Chip>(R.id.chipFollowSource)
        
        detailSourceText.text = source
        
        // Feature 61: Deep Follow Logic (Hardened + Local-First)
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val publisherPrefs = getSharedPreferences("PublisherPrefs_$userId", android.content.Context.MODE_PRIVATE)
        val followedSources = publisherPrefs.getStringSet("followed_sources", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        
        if (!source.isNullOrEmpty()) {
            // Initial Check
            if (followedSources.contains(source)) {
                chipFollow.text = "Following"
                chipFollow.setChipBackgroundColorResource(R.color.ignite_orange)
                chipFollow.setTextColor(android.graphics.Color.WHITE)
            }

            chipFollow.setOnClickListener {
                val currentFollows = publisherPrefs.getStringSet("followed_sources", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                
                if (currentFollows.contains(source)) {
                    // Unfollow
                    currentFollows.remove(source)
                    publisherPrefs.edit().putStringSet("followed_sources", currentFollows).apply()
                    
                    chipFollow.text = "+ Follow"
                    chipFollow.setChipBackgroundColorResource(android.R.color.transparent)
                    chipFollow.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.ignite_orange))
                    Toast.makeText(this, "Stopped following $source", Toast.LENGTH_SHORT).show()
                } else {
                    // Follow
                    currentFollows.add(source!!)
                    publisherPrefs.edit().putStringSet("followed_sources", currentFollows).apply()
                    
                    chipFollow.text = "Following"
                    chipFollow.setChipBackgroundColorResource(R.color.ignite_orange)
                    chipFollow.setTextColor(android.graphics.Color.WHITE)
                    Toast.makeText(this, "Now following $source!", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Source is null or empty, hide the follow button as it has no target
            chipFollow.visibility = android.view.View.GONE
        }

        // Wikipedia Intelligence Logic REMOVED


        if (url != null) {
            trackArticleView(url)
            if (NetworkUtils.isNetworkAvailable(this)) {
                setupWebView(url)
                setupZoomGestures()
                fetchRelatedNews(title, source)
            } else {
                // Feature 72: High-Speed Vault Recovery
                val cached = SmartCache.getCachedByUrl(this, url!!)
                if (cached != null) {
                    val offlineHtml = """
                        <html>
                        <body style="padding: 24px; font-family: sans-serif; background-color: #FFFFFF; color: #333333; line-height: 1.6;">
                            <h1 style="color: #FF5722; font-size: 24px;">${cached.title}</h1>
                            <p style="color: #666666; font-size: 14px; font-weight: bold;">Source: ${cached.source?.name ?: "News Vault"}</p>
                            <hr style="border: 0; border-top: 1px solid #EEEEEE; margin: 24px 0;">
                            <p style="font-size: 18px;">${cached.description ?: "Full content is unavailable offline. However, we've saved this key summary for you."}</p>
                            <p style="font-size: 18px; margin-top: 16px;">${cached.content ?: ""}</p>
                            <div style="background: #FFF3E0; padding: 16px; border-radius: 8px; margin-top: 24px;">
                                <p style="margin: 0; font-size: 12px; color: #E64A19;">🚀 <b>Elite Reading Mode:</b> You are reading an offline version saved by your Smart Vault.</p>
                            </div>
                        </body>
                        </html>
                    """.trimIndent()
                    webView.loadDataWithBaseURL(null, offlineHtml, "text/html", "UTF-8", null)
                    Toast.makeText(this, "Reading from Vault (Offline)", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Network Error: This story isn't in your vault yet.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            finish()
        }
    }

    // Wikipedia Helper Methods REMOVED


    private fun trackArticleView(url: String) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        
        val viewData = hashMapOf(
            "url" to url,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        
        db.collection("users").document(user.uid).collection("views")
            .add(viewData)
            // Feature 90: Silent gamification tracking complete
    }

    private fun setupZoomGestures() {
        val zoomIndicator = findViewById<TextView>(R.id.tvZoomIndicator)
        val detector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            private var lastX = 0f
            private val THRESHOLD = 30f // Sensitivity

            override fun onScroll(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (e1 == null) return false
                
                // Only if mostly horizontal
                if (Math.abs(distanceX) > Math.abs(distanceY) * 2) {
                    val deltaX = e2.x - e1.x
                    
                    if (Math.abs(deltaX) > THRESHOLD) {
                        val zoomChange = (deltaX / 10).toInt()
                        currentTextZoom += zoomChange
                        
                        // Clamp
                        if (currentTextZoom < 50) currentTextZoom = 50
                        if (currentTextZoom > 280) currentTextZoom = 280
                        
                        // Apply to WebView
                        webView.settings.textZoom = currentTextZoom
                        
                        // Show Indicator
                        zoomIndicator.text = "Text Size: $currentTextZoom%"
                        zoomIndicator.visibility = View.VISIBLE
                        zoomIndicator.alpha = 1f
                        
                        // Fade Out
                        zoomIndicator.animate().alpha(0f).setStartDelay(1200).setDuration(500).withEndAction {
                            zoomIndicator.visibility = View.GONE
                        }.start()
                        
                        return true
                    }
                }
                return false
            }
        })

        webView.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            false // Continue for normal webview scrolling
        }
    }

    private fun fetchRelatedNews(title: String?, source: String?) {
        val query = (title?.split(" ")?.take(3)?.joinToString(" ") ?: source) ?: "news"
        val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.relatedRecyclerView)
        val shimmer = findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerRelated)
        
        rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
        shimmer.startShimmer()
        shimmer.visibility = View.VISIBLE
        rv.visibility = View.GONE
        // Mock bookmark Set for Now? No, load from local prefs!
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val prefs = getSharedPreferences("BookmarkPrefs_$userId", android.content.Context.MODE_PRIVATE)
        val json = prefs.getString("saved_articles", "[]")
        val type = object : com.google.gson.reflect.TypeToken<MutableList<NewsArticle>>() {}.type
        val saved: MutableList<NewsArticle> = com.google.gson.Gson().fromJson(json, type) ?: mutableListOf()
        val bookmarkedUrls = saved.mapNotNull { it.url }.toSet()

        RetrofitInstance.api.searchNews(query = query, apiKey = RetrofitInstance.API_KEY)
            .enqueue(object : retrofit2.Callback<NewsResponse> {
                override fun onResponse(call: retrofit2.Call<NewsResponse>, response: retrofit2.Response<NewsResponse>) {
                    shimmer.stopShimmer()
                    shimmer.visibility = View.GONE
                    
                    if (response.isSuccessful && response.body() != null) {
                        val articles = response.body()!!.articles
                            .filter { it.url != intent.getStringExtra("url") } // Exclude current article
                            .take(8)
                        
                        if (articles.isEmpty()) {
                            findViewById<View>(R.id.relatedSectionHeader).visibility = View.GONE
                            return
                        }

                        findViewById<View>(R.id.relatedSectionHeader).visibility = View.VISIBLE
                        rv.visibility = View.VISIBLE
                        rv.alpha = 0f
                        rv.animate().alpha(1f).setDuration(500).start()
                        
                        rv.adapter = NewsAdapter(
                            articles = articles,
                            bookmarkedUrls = bookmarkedUrls,
                            onBookmarkClick = { art, checked -> handleLocalBookmark(art, checked) },
                            onItemClick = { art, img ->
                                val intent = Intent(this@NewsDetailActivity, NewsDetailActivity::class.java)
                                intent.putExtra("url", art.url)
                                intent.putExtra("title", art.title)
                                intent.putExtra("urlToImage", art.urlToImage)
                                intent.putExtra("source", art.source?.name)
                                startActivity(intent)
                                finish() // Slide to next one smoothly
                            },
                            onSourceClick = { src ->
                                // Feature 3: Deep link back to home source search
                                val intent = Intent(this@NewsDetailActivity, NewsActivity::class.java)
                                intent.putExtra("search_query", src)
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                startActivity(intent)
                            }
                        )
                    }
                }
                override fun onFailure(call: retrofit2.Call<NewsResponse>, t: Throwable) {}
            })
    }

    private fun handleLocalBookmark(article: NewsArticle, isChecked: Boolean) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val prefs = getSharedPreferences("BookmarkPrefs_$userId", android.content.Context.MODE_PRIVATE)
        val json = prefs.getString("saved_articles", "[]")
        val type = object : com.google.gson.reflect.TypeToken<MutableList<NewsArticle>>() {}.type
        val saved: MutableList<NewsArticle> = com.google.gson.Gson().fromJson(json, type) ?: mutableListOf()

        if (isChecked) {
            if (!saved.any { it.url == article.url }) saved.add(article)
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        } else {
            saved.removeAll { it.url == article.url }
            Toast.makeText(this, "Removed", Toast.LENGTH_SHORT).show()
        }
        prefs.edit().putString("saved_articles", com.google.gson.Gson().toJson(saved)).apply()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        } else {
            Toast.makeText(this, "TTS Initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupWebView(url: String) {
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }
        }
        
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                }
            }
        }

        webView.loadUrl(url)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_detail, menu)
        
        // Feature 50: Setup Direct 'Aa' Text Action View
        val item = menu?.findItem(R.id.action_text_size)
        item?.actionView?.findViewById<View>(R.id.tvActionTextSize)?.setOnClickListener {
            showTextOptions()
        }

        // Feature 102: Force White Icons for Visibility (Elite Contrast)
        val toolbar = findViewById<MaterialToolbar>(R.id.detailToolbar)
        val white = androidx.core.content.ContextCompat.getColor(this, android.R.color.white)
        toolbar.navigationIcon?.setTint(white) // Back arrow
        
        for (i in 0 until (menu?.size() ?: 0)) {
            val menuItem = menu?.getItem(i)
            menuItem?.icon?.let {
                val icon = androidx.core.graphics.drawable.DrawableCompat.wrap(it).mutate()
                androidx.core.graphics.drawable.DrawableCompat.setTint(icon, white)
                menuItem.icon = icon
            }
        }
        
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_text_size -> {
                showTextOptions()
                return true
            }
            R.id.action_listen -> {
                speakNews()
                return true
            }
            R.id.action_share -> {
                shareNews()
                return true
            }
            R.id.action_download -> {
                downloadArticle()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showTextOptions() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_text_zoom, null)
        val slider = dialogView.findViewById<com.google.android.material.slider.Slider>(R.id.zoomSlider)
        val zoomLabel = dialogView.findViewById<TextView>(R.id.tvDialogZoomLabel)
        val switchReader = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchReaderMode)

        // Sync initial state
        slider.value = currentTextZoom.toFloat()
        zoomLabel.text = "Text Size: $currentTextZoom%"
        switchReader.isChecked = isReaderMode

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Done", null)
            .show()

        // 📏 Live Slider Logic
        slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                currentTextZoom = value.toInt()
                webView.settings.textZoom = currentTextZoom
                zoomLabel.text = "Text Size: $currentTextZoom%"
            }
        }

        // 📜 Reader Mode Toggle
        switchReader.setOnCheckedChangeListener { _, isChecked ->
            isReaderMode = isChecked
            toggleReaderMode()
        }
    }

    private fun toggleReaderMode() {
        isReaderMode = !isReaderMode
        if (isReaderMode) {
            val js = """
                javascript:(function() {
                    var selectors = 'header, footer, nav, aside, .ad, .advertisement, iframe, .sidebar, .comments';
                    var elements = document.querySelectorAll(selectors);
                    for (var i = 0; i < elements.length; i++) {
                        elements[i].style.display = 'none';
                    }
                    document.body.style.padding = '20px';
                    document.body.style.fontFamily = 'sans-serif';
                    document.body.style.fontSize = '18px';
                    document.body.style.lineHeight = '1.6';
                    document.body.style.backgroundColor = '#FFFFFF';
                    document.body.style.color = '#333333';
                })()
            """.trimIndent()
            webView.evaluateJavascript(js, null)
            Toast.makeText(this, "Reader Mode Enabled", Toast.LENGTH_SHORT).show()
        } else {
            webView.reload()
            Toast.makeText(this, "Reader Mode Disabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun speakNews() {
        val title = intent.getStringExtra("title") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val textToSpeak = "Title: $title. Description: $description"
        
        if (tts?.isSpeaking == true) {
            tts?.stop()
            Toast.makeText(this, "Speech stopped", Toast.LENGTH_SHORT).show()
        } else {
            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
            Toast.makeText(this, "Reading news...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadArticle() {
        val title = (intent.getStringExtra("title") ?: "Article").replace("[^a-zA-Z0-9]".toRegex(), "_")
        val manager = getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
        val jobName = "IgniteNews_$title"
        
        Toast.makeText(this, "Generating Research PDF...", Toast.LENGTH_LONG).show()
        
        try {
            // Feature 101: High-Fidelity PDF Export
            val adapter = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                webView.createPrintDocumentAdapter(jobName)
            } else {
                @Suppress("DEPRECATION")
                webView.createPrintDocumentAdapter()
            }
            
            manager.print(jobName, adapter, android.print.PrintAttributes.Builder().build())
            
            // Tactile success
            val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "PDF creation failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareNews() {
        val url = intent.getStringExtra("url") ?: return
        val title = intent.getStringExtra("title") ?: "Ignite News Insight"
        val source = intent.getStringExtra("source") ?: "IgniteNews"
        
        try {
            // Feature 87: Elite Share Card Generation
            val width = 1080
            val height = 1080
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            
            // 1. Draw Deep Matte Background
            canvas.drawColor(android.graphics.Color.parseColor("#121212"))
            
            // 2. Setup Typography Paint
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 72f
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            
            // 3. Draw Wrapped Headline
            val textPaint = android.text.TextPaint(paint)
            val staticLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.text.StaticLayout.Builder.obtain(title, 0, title.length, textPaint, width - 160)
                    .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.2f)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                android.text.StaticLayout(title, textPaint, width - 160, android.text.Layout.Alignment.ALIGN_NORMAL, 1.2f, 0f, false)
            }
                
            canvas.save()
            canvas.translate(80f, 200f)
            staticLayout.draw(canvas)
            canvas.restore()
            
            // 4. Draw Signature Accent (Ignite Orange Source)
            paint.color = android.graphics.Color.parseColor("#FF5722")
            paint.textSize = 48f
            canvas.drawText(source.uppercase(), 80f, height - 120f, paint)
            
            // 5. Draw Signature App Logo (The Infinite Fold)
            val logo = android.graphics.BitmapFactory.decodeResource(resources, R.drawable.app_logo)
            if (logo != null) {
                val scaledLogo = android.graphics.Bitmap.createScaledBitmap(logo, 100, 100, true)
                canvas.drawBitmap(scaledLogo, width - 180f, height - 160f, null)
            }

            // 6. Save to Secure Cache
            val cachePath = java.io.File(cacheDir, "images")
            cachePath.mkdirs()
            val file = java.io.File(cachePath, "share_card.png")
            val stream = java.io.FileOutputStream(file)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            
            // 7. Dispatch the Image Intent
            val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "image/png"
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Read the full story: $url\n\nShared via IgniteNews")
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            startActivity(Intent.createChooser(shareIntent, "Share Elite Card via"))
            
        } catch (e: Exception) {
            // Fallback to text if graphics fail
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this news!")
            shareIntent.putExtra(Intent.EXTRA_TEXT, "$title\n\nRead more at: $url")
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }
    }

    override fun onDestroy() {
        if (tts != null) {
            tts?.stop()
            tts?.shutdown()
        }
        super.onDestroy()
    }

    private fun addToHistory(url: String?, title: String?, source: String?, imageUrl: String?) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        if (url == null || title == null) return

        val article = NewsArticle(title, "", url, imageUrl, "Viewed recently", Source(source), "")
        val prefs = getSharedPreferences("HistoryPrefs_${user.uid}", android.content.Context.MODE_PRIVATE)
        val gson = com.google.gson.Gson()
        val json = prefs.getString("saved_history", "[]")
        val type = object : com.google.gson.reflect.TypeToken<MutableList<NewsArticle>>() {}.type
        val historyList: MutableList<NewsArticle> = gson.fromJson(json, type) ?: mutableListOf()

        // Remove if duplicate exists to bring it to top
        historyList.removeAll { it.url == url }
        historyList.add(0, article) // Add to top

        // Limit to 50 items
        if (historyList.size > 50) {
            historyList.removeAt(historyList.size - 1)
        }

        prefs.edit().putString("saved_history", gson.toJson(historyList)).apply()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
