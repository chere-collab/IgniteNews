package com.example.tryanderror;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Base64;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.JSONArray;
import java.io.IOException;

public class NewsActivity extends AppCompatActivity {

    private NewsAdapter newsAdapter;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvCategories;
    private CategoryAdapter categoryAdapter;
    private SearchView searchView;
    private ShimmerFrameLayout shimmerLayout;
    private ViewPager2 heroViewPager;
    private CardView carouselCard;
    private DrawerLayout drawerLayout;
    private NavigationView navView;

    // Auto-scroll
    private Handler autoScrollHandler = new Handler(Looper.getMainLooper());
    private Runnable autoScrollRunnable = null;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private Set<String> bookmarkedUrls = new HashSet<>();

    private Gson gson = new Gson();
    private final String PREFS_NAME = "news_prefs";
    private final String KEY_CACHE = "news_cache";
    private final String KEY_CATEGORY = "default_category";
    private final String KEY_LANGUAGE = "default_language";

    private String currentCategory = "technology";
    private String currentLanguage = "en";
    private String currentSortBy = "publishedAt";
    private List<NewsArticle> currentArticles = new ArrayList<>();
    private long lastFetchTime = 0L; // Feature 31: Throttling

    // API KEY (Unified)
    private final String API_KEY = RetrofitInstance.API_KEY;

    private StoryAdapter storyAdapter;
    private RecyclerView rvStories;
    private CardView suggestionCard;
    private SuggestionAdapter suggestionAdapter;

    private String lastFetchLanguage = null;
    private String lastFetchQuery = null;
    
    private List<String> trendingTopics = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        // Feature 1: The Stories Strip
        rvStories = findViewById(R.id.rvStories);
        rvStories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        storyAdapter = new StoryAdapter(new ArrayList<>(), article -> handleStoryClick(article));
        rvStories.setAdapter(storyAdapter);

        // Feature 3: Check for search intent from Bookmarks
        String searchIntentQuery = getIntent().getStringExtra("search_query");

        // Enable Firestore Persistence for Offline Bookmarks (Permanent saving even with low signal)
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        recyclerView = findViewById(R.id.newsRecyclerView);
        progressBar = findViewById(R.id.newsProgressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        rvCategories = findViewById(R.id.rvCategories);
        searchView = findViewById(R.id.newsSearchView);
        shimmerLayout = findViewById(R.id.shimmerLayout);
        heroViewPager = findViewById(R.id.heroViewPager);
        carouselCard = findViewById(R.id.carouselCard);

        setupNavigationDrawer();

        // Feature 80: Setup Predictive Search
        suggestionCard = findViewById(R.id.suggestionCard);
        RecyclerView rvSuggestions = findViewById(R.id.rvSuggestions);
        rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        
        suggestionAdapter = new SuggestionAdapter(new ArrayList<>(), suggestion -> {
            searchView.setQuery(suggestion, true);
            suggestionCard.setVisibility(View.GONE);
        });
        rvSuggestions.setAdapter(suggestionAdapter);

        trendingTopics.add("AI Innovation");
        trendingTopics.add("SpaceX");
        trendingTopics.add("Climate Change");
        trendingTopics.add("World Economy");
        trendingTopics.add("Tech Trends");
        trendingTopics.add("Health Science");
        trendingTopics.add("Blockchain");
        trendingTopics.add("Renewable Energy");
        trendingTopics.add("Future of Work");
        trendingTopics.add("Global Politics");
        trendingTopics.add("Mars Mission");
        trendingTopics.add("Quantum Computing");

        // Redesigned Header Elements
        Toolbar toolbar = findViewById(R.id.newsToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        findViewById(R.id.btnSortArrows).setOnClickListener(v -> showSortMenu());

        // Force Search Text Visibility (Theme-Aware)
        EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchEditText != null) {
            int primaryColor = ContextCompat.getColor(this, R.color.text_primary);
            searchEditText.setTextColor(primaryColor);
            searchEditText.setHintTextColor(Color.GRAY);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Setup Premium Category Bubbles
        List<Category> categories = new ArrayList<>();
        categories.add(new Category("Following", "following", "⭐️", "#FFD700"));
        categories.add(new Category("Technology", "technology", "💻", "#FF5722"));
        categories.add(new Category("Politics", "politics_search", "⚖️", "#607D8B"));
        categories.add(new Category("Business", "business", "💼", "#3F51B5"));
        categories.add(new Category("Sports", "sports", "⚽", "#4CAF50"));
        categories.add(new Category("Entertainment", "entertainment", "🎬", "#9C27B0"));
        categories.add(new Category("Science", "science", "🔬", "#00BCD4"));
        categories.add(new Category("Health", "health", "❤️", "#E91E63"));

        categoryAdapter = new CategoryAdapter(categories, category -> {
            currentCategory = category.getKey();
            // Feature 12: Save Category Choice Permanently
            SharedPreferences.Editor prefsEditor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
            prefsEditor.putString(KEY_CATEGORY, category.getKey()).apply();

            searchView.setQuery("", false);
            searchView.clearFocus();

            // Feature 64: Deep Channel Switching
            if ("following".equals(category.getKey())) {
                FirebaseUser user = auth.getCurrentUser();
                if (user == null) return;
                SharedPreferences userPrefs = getSharedPreferences("PublisherPrefs_" + user.getUid(), Context.MODE_PRIVATE);
                Set<String> followed = userPrefs.getStringSet("followed_sources", new HashSet<>());

                if (followed == null || followed.isEmpty()) {
                    currentArticles = new ArrayList<>();
                    updateAdapter();
                    Toast.makeText(NewsActivity.this, "Follow some sources first!", Toast.LENGTH_LONG).show();
                } else {
                    fetchNews("following", false, false);
                }
            } else if ("politics_search".equals(category.getKey())) {
                fetchSearchNews("world politics", false, false, currentSortBy);
            } else {
                fetchNews(category.getKey(), false, false);
            }
        });

        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);
        categoryAdapter.selectCategory(currentCategory != null ? currentCategory : "technology");

        // Load Preferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentCategory = prefs.getString(KEY_CATEGORY, "technology");
        if (currentCategory == null) currentCategory = "technology";
        currentLanguage = prefs.getString(KEY_LANGUAGE, "en");
        if (currentLanguage == null) currentLanguage = "en";
        categoryAdapter.selectCategory(currentCategory);

        // Feature 3: Apply the search intent if coming from bookmarks
        if (searchIntentQuery != null) {
            searchView.post(() -> searchView.setQuery(searchIntentQuery, true));
        }

        // Load Bookmarks from Firestore
        loadBookmarks();

        // Feature 81: Handle Search with Predictive Suggestions
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query != null && !query.trim().isEmpty()) {
                    String fullQuery = getCurrentEffectiveQuery(query);
                    fetchSearchNews(fullQuery, false, false, "relevancy");
                }
                return true;
            }

            private Handler suggestHandler = new Handler(Looper.getMainLooper());
            private Runnable suggestRunnable = null;

            @Override
            public boolean onQueryTextChange(String newText) {
                if (suggestRunnable != null) {
                    suggestHandler.removeCallbacks(suggestRunnable);
                }

                if (newText == null || newText.trim().isEmpty()) {
                    suggestionCard.setVisibility(View.GONE);
                } else {
                    // Start typing debounce (100ms)
                    suggestRunnable = () -> fetchSuggestions(newText);
                    suggestHandler.postDelayed(suggestRunnable, 100);
                }
                return true;
            }
        });

        // Feature 2: Scroll-to-Top Logic
        View fabScroll = findViewById(R.id.fabScrollToTop);
        fabScroll.setOnClickListener(v -> recyclerView.smoothScrollToPosition(0));

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int offset = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                if (offset > 2) {
                    if (fabScroll.getVisibility() == View.GONE) {
                        fabScroll.setVisibility(View.VISIBLE);
                        fabScroll.setAlpha(0f);
                        fabScroll.animate().alpha(1f).setDuration(300).start();
                    }
                } else {
                    if (fabScroll.getVisibility() == View.VISIBLE) {
                        fabScroll.animate().alpha(0f).setDuration(300).withEndAction(() -> fabScroll.setVisibility(View.GONE)).start();
                    }
                }
            }
        });

        int orangeColor = ContextCompat.getColor(this, R.color.ignite_orange);
        swipeRefreshLayout.setColorSchemeColors(orangeColor);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Feature 8: Haptic Feedback on Refresh
            swipeRefreshLayout.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

            CharSequence querySeq = searchView.getQuery();
            String query = querySeq != null ? querySeq.toString() : "";
            if (!query.trim().isEmpty()) {
                String fullQuery = getCurrentEffectiveQuery(query);
                fetchSearchNews(fullQuery, true, false, currentSortBy);
            } else {
                fetchNews(currentCategory, true, true);
            }
        });

        // Load from Cache First
        loadFromCache();

        // Subscribe to High-Priority News Alerts
        FirebaseMessaging.getInstance().subscribeToTopic("technology");

        // Then fetch from network
        fetchNews(currentCategory);

        // Setup Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_bookmarks) {
                startActivity(new Intent(NewsActivity.this, BookmarksActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                bottomNav.setSelectedItemId(R.id.nav_home);
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(NewsActivity.this, SettingsActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                bottomNav.setSelectedItemId(R.id.nav_home);
                return true;
            }
            return false;
        });

        updateProfileIcon();
    }

    private void setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        ImageButton btnMenu = findViewById(R.id.btnMenu);

        // Open drawer on hamburger click
        btnMenu.setOnClickListener(v -> {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Handle Header Info
        View headerView = navView.getHeaderView(0);
        TextView tvUserEmail = headerView.findViewById(R.id.tvNavUserEmail);
        ImageView ivProfile = headerView.findViewById(R.id.ivNavProfile);

        FirebaseUser user = auth.getCurrentUser();
        tvUserEmail.setText(user != null && user.getEmail() != null ? user.getEmail() : "Guest User");

        if (user != null) {
            SharedPreferences prefs = getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE);
            String base64String = prefs.getString("profile_" + user.getUid(), null);
            if (base64String != null && !base64String.isEmpty()) {
                try {
                    byte[] bytes = Base64.decode(base64String, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    ivProfile.setImageBitmap(bitmap);
                } catch (Exception e) {}
            } else if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(ivProfile);
            }
        }

        // Navigation Item Selection
        navView.setNavigationItemSelectedListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.nav_home) {
                searchView.setQuery("", false);
                searchView.clearFocus();
                fetchNews(currentCategory != null ? currentCategory : "technology");
                recyclerView.smoothScrollToPosition(0);
            } else if (itemId == R.id.nav_bookmarks) {
                startActivity(new Intent(NewsActivity.this, BookmarksActivity.class));
            } else if (itemId == R.id.nav_history) {
                startActivity(new Intent(NewsActivity.this, HistoryActivity.class));
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(NewsActivity.this, SettingsActivity.class));
            } else if (itemId == R.id.nav_share) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out IgniteNews! The elite way to stay informed. \uD83D\uDD25\nhttps://play.google.com/store/apps/details?id=com.example.tryanderror");
                startActivity(Intent.createChooser(shareIntent, "Share with Colleagues"));
            } else if (itemId == R.id.nav_about) {
                startActivity(new Intent(NewsActivity.this, AboutActivity.class));
            } else if (itemId == R.id.nav_privacy) {
                startActivity(new Intent(NewsActivity.this, LegalActivity.class));
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String newCat = prefs.getString(KEY_CATEGORY, "technology");
        if (newCat == null) newCat = "technology";
        String newLang = prefs.getString(KEY_LANGUAGE, "en");
        if (newLang == null) newLang = "en";

        if (!newCat.equals(currentCategory) || !newLang.equals(currentLanguage)) {
            currentCategory = newCat;
            currentLanguage = newLang;
            categoryAdapter.selectCategory(currentCategory);
            fetchNews(currentCategory);
        }

        updateProfileIcon();
    }

    private void updateProfileIcon() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        MenuItem profileItem = bottomNav.getMenu().findItem(R.id.nav_settings);
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        SharedPreferences prefs = getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE);
        String base64String = prefs.getString("profile_" + user.getUid(), null);

        if (base64String != null && !base64String.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(base64String, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                RoundedBitmapDrawable roundedDrawable = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
                roundedDrawable.setCircular(true);
                profileItem.setIcon(roundedDrawable);
                profileItem.setIconTintList(null);
            } catch (Exception e) {}
        } else if (user.getPhotoUrl() != null) {
            Glide.with(this).asBitmap().load(user.getPhotoUrl()).circleCrop().into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), resource);
                    drawable.setCircular(true);
                    profileItem.setIcon(drawable);
                    profileItem.setIconTintList(null);
                }

                @Override
                public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
    }

    private void saveToCache(List<NewsArticle> articles) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = gson.toJson(articles);
        prefs.edit().putString(KEY_CACHE, json).apply();
    }

    private void loadFromCache() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_CACHE, null);
        if (json != null) {
            Type type = new TypeToken<List<NewsArticle>>() {}.getType();
            currentArticles = gson.fromJson(json, type);
            if (currentArticles == null) currentArticles = new ArrayList<>();
            updateAdapter();
        }
    }

    private void loadBookmarks() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        SharedPreferences prefs = getSharedPreferences("BookmarkPrefs_" + user.getUid(), Context.MODE_PRIVATE);
        String json = prefs.getString("saved_articles", "[]");
        Type type = new TypeToken<List<NewsArticle>>() {}.getType();
        List<NewsArticle> saved = gson.fromJson(json, type);
        if (saved == null) saved = new ArrayList<>();

        bookmarkedUrls.clear();
        for (NewsArticle article : saved) {
            if (article.getUrl() != null) {
                bookmarkedUrls.add(article.getUrl());
            }
        }
        updateAdapter();
    }

    private void handleBookmarkClick(NewsArticle article, boolean isChecked) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        SharedPreferences prefs = getSharedPreferences("BookmarkPrefs_" + user.getUid(), Context.MODE_PRIVATE);
        String json = prefs.getString("saved_articles", "[]");
        Type type = new TypeToken<List<NewsArticle>>() {}.getType();
        List<NewsArticle> saved = gson.fromJson(json, type);
        if (saved == null) saved = new ArrayList<>();

        if (isChecked) {
            boolean exists = false;
            for (NewsArticle a : saved) {
                if (a.getUrl() != null && a.getUrl().equals(article.getUrl())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                saved.add(article);
                if (article.getUrl() != null) bookmarkedUrls.add(article.getUrl());
                Toast.makeText(this, "Saved to Bookmarks!", Toast.LENGTH_SHORT).show();
            }
        } else {
            for (int i = 0; i < saved.size(); i++) {
                if (saved.get(i).getUrl() != null && saved.get(i).getUrl().equals(article.getUrl())) {
                    saved.remove(i);
                    break;
                }
            }
            if (article.getUrl() != null) bookmarkedUrls.remove(article.getUrl());
            Toast.makeText(this, "Removed from Bookmarks", Toast.LENGTH_SHORT).show();
        }

        prefs.edit().putString("saved_articles", gson.toJson(saved)).apply();
        newsAdapter.updateBookmarks(new HashSet<>(bookmarkedUrls));

        findViewById(android.R.id.content).performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    private void fetchNews(String category) {
        fetchNews(category, false, true);
    }

    private void fetchNews(String category, boolean wasRefreshing, boolean showCarousel) {
        if ("following".equals(category)) {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) return;
            SharedPreferences prefs = getSharedPreferences("PublisherPrefs_" + user.getUid(), Context.MODE_PRIVATE);
            Set<String> followed = prefs.getStringSet("followed_sources", new HashSet<>());

            if (followed == null || followed.isEmpty()) {
                currentArticles = new ArrayList<>();
                updateAdapter();
                Toast.makeText(this, "Your personal feed is empty. Follow some sources!", Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder queryBuilder = new StringBuilder();
            int i = 0;
            for (String source : followed) {
                if (i > 0) queryBuilder.append(" OR ");
                queryBuilder.append("\"").append(source).append("\"");
                i++;
            }
            fetchSearchNews(queryBuilder.toString(), wasRefreshing, false, "publishedAt");
            return;
        }

        long now = System.currentTimeMillis();
        if (!wasRefreshing && now - lastFetchTime < 30000 && category.equals(currentCategory) && currentLanguage.equals(lastFetchLanguage)) {
            loadFromCache();
            return;
        }

        setLoading(true);
        lastFetchLanguage = currentLanguage;
        carouselCard.setVisibility(showCarousel ? View.VISIBLE : View.GONE);

        RetrofitInstance.getApi().getTopHeadlines(null, category, null, currentLanguage, API_KEY)
                .enqueue(createNewsCallback(showCarousel, wasRefreshing, category));
    }

    private void fetchSearchNews(String query, boolean wasRefreshing, boolean showCarousel, String sortBy) {
        setLoading(true);
        lastFetchQuery = query;
        carouselCard.setVisibility(showCarousel ? View.VISIBLE : View.GONE);
        RetrofitInstance.getApi().searchNews(query, currentLanguage, sortBy, API_KEY)
                .enqueue(createNewsCallback(false, wasRefreshing, query));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    private void showSortMenu() {
        PopupMenu popup = new PopupMenu(this, findViewById(R.id.btnSortArrows));
        CharSequence querySeq = searchView.getQuery();
        String query = querySeq != null ? querySeq.toString() : "";

        popup.getMenu().add("Newest First").setOnMenuItemClickListener(item -> {
            if (!query.trim().isEmpty()) {
                currentSortBy = "publishedAt";
                String fullQuery = getCurrentEffectiveQuery(query);
                fetchSearchNews(fullQuery, false, false, currentSortBy);
            } else {
                Collections.sort(currentArticles, (a, b) -> {
                    String d1 = a.getPublishedAt() != null ? a.getPublishedAt() : "";
                    String d2 = b.getPublishedAt() != null ? b.getPublishedAt() : "";
                    return d2.compareTo(d1);
                });
                updateAdapter();
            }
            return true;
        });

        popup.getMenu().add("Popularity").setOnMenuItemClickListener(item -> {
            if (!query.trim().isEmpty()) {
                currentSortBy = "popularity";
                String fullQuery = getCurrentEffectiveQuery(query);
                fetchSearchNews(fullQuery, false, false, currentSortBy);
            } else {
                Toast.makeText(this, "Popularity sort is only available in Search", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        popup.getMenu().add("Alphabetical (A-Z)").setOnMenuItemClickListener(item -> {
            Collections.sort(currentArticles, (a, b) -> {
                String t1 = a.getTitle() != null ? a.getTitle() : "";
                String t2 = b.getTitle() != null ? b.getTitle() : "";
                return t1.compareTo(t2);
            });
            updateAdapter();
            return true;
        });

        popup.show();
    }

    private String getCurrentEffectiveQuery(String userQuery) {
        String context = "";
        if ("politics_search".equals(currentCategory)) {
            context = "politics";
        } else if ("following".equals(currentCategory) || currentCategory == null) {
            context = "";
        } else {
            context = currentCategory;
        }

        if (context.trim().isEmpty()) {
            return userQuery;
        } else {
            return "+(" + userQuery + ") +" + context;
        }
    }

    private void refreshCurrent() {
        CharSequence querySeq = searchView.getQuery();
        String query = querySeq != null ? querySeq.toString() : "";
        if (!query.trim().isEmpty()) {
            String fullQuery = getCurrentEffectiveQuery(query);
            fetchSearchNews(fullQuery, false, false, currentSortBy);
        } else {
            fetchNews(currentCategory != null ? currentCategory : "technology");
        }
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            if (!swipeRefreshLayout.isRefreshing()) {
                shimmerLayout.setVisibility(View.VISIBLE);
                shimmerLayout.startShimmer();
                recyclerView.setVisibility(View.GONE);
            }
        } else {
            shimmerLayout.stopShimmer();
            shimmerLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
                vibrate();
            }
        }
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(50);
        }
    }

    private Callback<NewsResponse> createNewsCallback(boolean isHome, boolean wasRefreshingOnStart, String contextLabel) {
        return new Callback<NewsResponse>() {
            @Override
            public void onResponse(@NonNull Call<NewsResponse> call, @NonNull Response<NewsResponse> response) {
                boolean isUserRefresh = wasRefreshingOnStart || swipeRefreshLayout.isRefreshing();
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    lastFetchTime = System.currentTimeMillis();
                    List<NewsArticle> newArticles = response.body().getArticles();
                    if (newArticles == null) newArticles = new ArrayList<>();

                    if ("following".equals(currentCategory)) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            SharedPreferences prefs = getSharedPreferences("PublisherPrefs_" + user.getUid(), Context.MODE_PRIVATE);
                            Set<String> followed = prefs.getStringSet("followed_sources", new HashSet<>());
                            if (followed != null && !followed.isEmpty()) {
                                List<NewsArticle> filtered = new ArrayList<>();
                                for (NewsArticle a : newArticles) {
                                    if (a.getSource() != null && followed.contains(a.getSource().getName())) {
                                        filtered.add(a);
                                    }
                                }
                                newArticles = filtered;
                            }
                        }
                    }

                    if (newArticles.isEmpty()) {
                        if ("following".equals(currentCategory)) {
                            Toast.makeText(NewsActivity.this, "No recent stories from these specific sources.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(NewsActivity.this, "No results for [" + contextLabel + "] in [" + currentLanguage + "]", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        saveToCache(newArticles);
                        SmartCache.saveOfflineArticles(NewsActivity.this, newArticles);

                        if (isHome) {
                            int heroLimit = Math.min(newArticles.size(), 5);
                            List<NewsArticle> heroList = new ArrayList<>(newArticles.subList(0, heroLimit));
                            List<NewsArticle> feedList = new ArrayList<>(newArticles.subList(heroLimit, newArticles.size()));
                            setupHeroCarousel(heroList);
                            currentArticles = feedList;
                        } else {
                            carouselCard.setVisibility(View.GONE);
                            currentArticles = newArticles;
                        }
                        if (isUserRefresh) {
                            Toast.makeText(NewsActivity.this, "You're up to date!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    updateAdapter();
                } else {
                    if (response.code() == 429) {
                        Toast.makeText(NewsActivity.this, "Searching fast! Take a 30s breather...", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(NewsActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                    loadFromCache();
                }
            }

            @Override
            public void onFailure(@NonNull Call<NewsResponse> call, @NonNull Throwable t) {
                setLoading(false);
                loadFromCache();
            }
        };
    }

    private void setupHeroCarousel(List<NewsArticle> articles) {
        if (articles.isEmpty()) {
            carouselCard.setVisibility(View.GONE);
            return;
        }
        carouselCard.setVisibility(View.VISIBLE);
        HeroAdapter adapter = new HeroAdapter(articles, (article, imageView) -> {
            recordView();
            Intent intent = new Intent(NewsActivity.this, NewsDetailActivity.class);
            intent.putExtra("url", article.getUrl());
            intent.putExtra("title", article.getTitle());
            intent.putExtra("description", article.getDescription());
            intent.putExtra("content", article.getContent());
            intent.putExtra("publishedAt", article.getPublishedAt());
            intent.putExtra("urlToImage", article.getUrlToImage());
            intent.putExtra("source", article.getSource() != null ? article.getSource().getName() : null);

            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    NewsActivity.this,
                    imageView,
                    "shared_article_image"
            );
            startActivity(intent, options.toBundle());
        }, sourceName -> {
            if (sourceName != null && !sourceName.trim().isEmpty()) {
                searchView.setQuery(sourceName, true);
            }
        });
        heroViewPager.setAdapter(adapter);

        if (autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                int count = adapter.getItemCount();
                if (count > 1) {
                    int next = (heroViewPager.getCurrentItem() + 1) % count;
                    heroViewPager.setCurrentItem(next, true);
                }
                autoScrollHandler.postDelayed(this, 3500);
            }
        };
        autoScrollHandler.postDelayed(autoScrollRunnable, 3500);
    }

    private void updateAdapter() {
        if (newsAdapter == null) {
            newsAdapter = new NewsAdapter(
                    currentArticles,
                    bookmarkedUrls,
                    new NewsAdapter.OnNewsClickListener() {
                        @Override
                        public void onBookmarkClick(NewsArticle article, boolean isChecked) {
                            handleBookmarkClick(article, isChecked);
                        }

                        @Override
                        public void onItemClick(NewsArticle article, ImageView imageView) {
                            recordView();
                            Intent intent = new Intent(NewsActivity.this, NewsDetailActivity.class);
                            intent.putExtra("url", article.getUrl());
                            intent.putExtra("title", article.getTitle());
                            intent.putExtra("description", article.getDescription());
                            intent.putExtra("content", article.getContent());
                            intent.putExtra("publishedAt", article.getPublishedAt());
                            intent.putExtra("urlToImage", article.getUrlToImage());
                            intent.putExtra("source", article.getSource() != null ? article.getSource().getName() : null);

                            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    NewsActivity.this,
                                    imageView,
                                    "shared_article_image"
                            );
                            startActivity(intent, options.toBundle());
                        }

                        @Override
                        public void onSourceClick(String sourceName) {
                            if (sourceName != null && !sourceName.trim().isEmpty()) {
                                searchView.setQuery(sourceName, true);
                            }
                        }
                    }
            );
            recyclerView.setAdapter(newsAdapter);
        } else {
            newsAdapter.updateArticles(currentArticles);
            newsAdapter.updateBookmarks(new HashSet<>(bookmarkedUrls));
        }

        // Feature 59: Update the visual Stories strip
        int limit = Math.min(currentArticles.size(), 8);
        storyAdapter.updateStories(new ArrayList<>(currentArticles.subList(0, limit)));
    }

    private void handleStoryClick(NewsArticle article) {
        Intent intent = new Intent(NewsActivity.this, NewsDetailActivity.class);
        intent.putExtra("url", article.getUrl());
        intent.putExtra("title", article.getTitle());
        intent.putExtra("description", article.getDescription());
        intent.putExtra("content", article.getContent());
        intent.putExtra("publishedAt", article.getPublishedAt());
        intent.putExtra("urlToImage", article.getUrlToImage());
        intent.putExtra("source", article.getSource() != null ? article.getSource().getName() : null);
        startActivity(intent);
    }

    private void recordView() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        String userId = user.getUid();
        
        Timestamp timestamp = Timestamp.now();
        db.collection("users").document(userId).collection("views").add(new Object() {
            public Timestamp getTimestamp() { return timestamp; }
        });
    }

    private interface OnHeroClickListener {
        void onClick(NewsArticle article, ImageView imageView);
    }
    
    private interface OnHeroSourceClickListener {
        void onSourceClick(String sourceName);
    }

    private class HeroAdapter extends RecyclerView.Adapter<HeroAdapter.ViewHolder> {
        private List<NewsArticle> articles;
        private OnHeroClickListener onClick;
        private OnHeroSourceClickListener onSourceClick;

        public HeroAdapter(List<NewsArticle> articles, OnHeroClickListener onClick, OnHeroSourceClickListener onSourceClick) {
            this.articles = articles;
            this.onClick = onClick;
            this.onSourceClick = onSourceClick;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView image;
            TextView title;
            TextView source;

            ViewHolder(View view) {
                super(view);
                image = view.findViewById(R.id.heroImage);
                title = view.findViewById(R.id.heroTitle);
                source = view.findViewById(R.id.heroSource);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hero_news, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NewsArticle article = articles.get(position);
            holder.title.setText(article.getTitle());
            holder.source.setText(article.getSource() != null ? article.getSource().getName() : "Ignite News");
            holder.source.setTextColor(ContextCompat.getColor(NewsActivity.this, R.color.ignite_orange));
            holder.source.setOnClickListener(v -> {
                if (onSourceClick != null) {
                    onSourceClick.onSourceClick(article.getSource() != null ? article.getSource().getName() : null);
                }
            });

            holder.image.setTransitionName("shared_article_image");
            Glide.with(holder.itemView.getContext())
                    .load(article.getUrlToImage())
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(holder.image);
            holder.itemView.setOnClickListener(v -> {
                if (onClick != null) {
                    onClick.onClick(article, holder.image);
                }
            });
        }

        @Override
        public int getItemCount() {
            return articles != null ? articles.size() : 0;
        }
    }
    private void fetchSuggestions(String query) {
        if (query == null || query.trim().length() < 2) {
            return;
        }

        String url = "https://suggestqueries.google.com/complete/search?client=firefox&hl=en&q=" + query;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                // Ignore failure
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonData = response.body().string();
                        JSONArray jsonArray = new JSONArray(jsonData);
                        JSONArray suggestionsArray = jsonArray.getJSONArray(1);

                        List<String> suggestions = new ArrayList<>();
                        for (int i = 0; i < Math.min(6, suggestionsArray.length()); i++) {
                            suggestions.add(suggestionsArray.getString(i));
                        }

                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!suggestions.isEmpty()) {
                                suggestionAdapter.updateSuggestions(suggestions);
                                suggestionCard.setVisibility(View.VISIBLE);
                            } else {
                                suggestionCard.setVisibility(View.GONE);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
