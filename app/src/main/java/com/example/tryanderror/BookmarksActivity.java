package com.example.tryanderror;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BookmarksActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NewsAdapter newsAdapter;
    private TextView emptyText;

    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private List<NewsArticle> bookmarkedArticles = new ArrayList<>();
    private Set<String> bookmarkedUrls = new HashSet<>();
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);

        Toolbar toolbar = findViewById(R.id.bookmarksToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        recyclerView = findViewById(R.id.bookmarksRecyclerView);
        emptyText = findViewById(R.id.emptyBookmarksText);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Setup Bottom Nav (Synchronized)
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_bookmarks);
        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(BookmarksActivity.this, NewsActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_bookmarks) {
                    return true;
                } else if (itemId == R.id.nav_settings) {
                    startActivity(new Intent(BookmarksActivity.this, SettingsActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                    return true;
                }
                return false;
            }
        });

        loadBookmarks();
        setupSwipeToDelete();
        updateProfileIcon();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                profileItem.setIconTintList(null); // Stop the orange tint from affecting your face!
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeHandler = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                NewsArticle article = bookmarkedArticles.get(position);

                // Keep backup for UNDO
                String backupUrl = article.getUrl();

                // Snap-Remove from list
                bookmarkedArticles.remove(position);
                if (backupUrl != null) {
                    bookmarkedUrls.remove(backupUrl);
                }
                newsAdapter.notifyItemRemoved(position);

                if (bookmarkedArticles.isEmpty()) {
                    emptyText.setVisibility(View.VISIBLE);
                }

                // Show Premium UNDO Logic
                Snackbar snackbar = Snackbar.make(recyclerView, "Bookmark Removed", Snackbar.LENGTH_LONG);
                snackbar.setAction("UNDO", v -> {
                    bookmarkedArticles.add(position, article);
                    if (backupUrl != null) {
                        bookmarkedUrls.add(backupUrl);
                    }
                    newsAdapter.notifyItemInserted(position);
                    if (!bookmarkedArticles.isEmpty()) {
                        emptyText.setVisibility(View.GONE);
                    }
                    recyclerView.scrollToPosition(position);
                });

                // If the user doesn't hit Undo, delete permanently
                snackbar.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        if (event != DISMISS_EVENT_ACTION) {
                            finalizeDelete(article);
                        }
                    }
                });
                snackbar.show();
            }
        };
        new ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView);
    }

    private void finalizeDelete(NewsArticle article) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        String userId = user.getUid();
        
        SharedPreferences prefs = getSharedPreferences("BookmarkPrefs_" + userId, Context.MODE_PRIVATE);
        String json = prefs.getString("saved_articles", "[]");
        Type type = new TypeToken<List<NewsArticle>>() {}.getType();
        List<NewsArticle> saved = gson.fromJson(json, type);
        if (saved == null) saved = new ArrayList<>();

        for (int i = 0; i < saved.size(); i++) {
            if (saved.get(i).getUrl() != null && saved.get(i).getUrl().equals(article.getUrl())) {
                saved.remove(i);
                break;
            }
        }
        prefs.edit().putString("saved_articles", gson.toJson(saved)).apply();
    }

    private void loadBookmarks() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        String userId = user.getUid();

        SharedPreferences prefs = getSharedPreferences("BookmarkPrefs_" + userId, Context.MODE_PRIVATE);
        String json = prefs.getString("saved_articles", "[]");
        Type type = new TypeToken<List<NewsArticle>>() {}.getType();
        List<NewsArticle> saved = gson.fromJson(json, type);
        if (saved == null) saved = new ArrayList<>();

        bookmarkedArticles.clear();
        bookmarkedUrls.clear();

        bookmarkedArticles.addAll(saved);
        for (NewsArticle article : saved) {
            if (article.getUrl() != null) {
                bookmarkedUrls.add(article.getUrl());
            }
        }

        if (bookmarkedArticles.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
        } else {
            emptyText.setVisibility(View.GONE);
        }

        updateAdapter();
    }

    private void updateAdapter() {
        newsAdapter = new NewsAdapter(
                bookmarkedArticles,
                bookmarkedUrls,
                new NewsAdapter.OnNewsClickListener() {
                    @Override
                    public void onBookmarkClick(NewsArticle article, boolean isChecked) {
                        if (!isChecked) {
                            removeBookmark(article);
                        }
                    }

                    @Override
                    public void onItemClick(NewsArticle article, ImageView imageView) {
                        Intent intent = new Intent(BookmarksActivity.this, NewsDetailActivity.class);
                        intent.putExtra("url", article.getUrl());
                        intent.putExtra("title", article.getTitle());
                        intent.putExtra("description", article.getDescription());
                        intent.putExtra("content", article.getContent());
                        intent.putExtra("publishedAt", article.getPublishedAt());
                        intent.putExtra("urlToImage", article.getUrlToImage());
                        intent.putExtra("source", article.getSource() != null ? article.getSource().getName() : null);

                        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                BookmarksActivity.this,
                                imageView,
                                "shared_article_image"
                        );
                        startActivity(intent, options.toBundle());
                    }

                    @Override
                    public void onSourceClick(String sourceName) {
                        Intent intent = new Intent(BookmarksActivity.this, NewsActivity.class);
                        intent.putExtra("search_query", sourceName);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                    }
                }
        );
        recyclerView.setAdapter(newsAdapter);
    }

    private void removeBookmark(NewsArticle article) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        String userId = user.getUid();
        
        SharedPreferences prefs = getSharedPreferences("BookmarkPrefs_" + userId, Context.MODE_PRIVATE);
        String json = prefs.getString("saved_articles", "[]");
        Type type = new TypeToken<List<NewsArticle>>() {}.getType();
        List<NewsArticle> saved = gson.fromJson(json, type);
        if (saved == null) saved = new ArrayList<>();

        for (int i = 0; i < saved.size(); i++) {
            if (saved.get(i).getUrl() != null && saved.get(i).getUrl().equals(article.getUrl())) {
                saved.remove(i);
                break;
            }
        }

        prefs.edit().putString("saved_articles", gson.toJson(saved)).apply();

        loadBookmarks();
    }
}
