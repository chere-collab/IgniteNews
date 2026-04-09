package com.example.tryanderror;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.speech.tts.TextToSpeech;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import android.util.TypedValue;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsDetailActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private WebView webView;
    private ProgressBar progressBar;
    private TextToSpeech tts;
    private TextView tvDetailTitle, tvDetailDate, tvDetailDescription;

    private int currentTextZoom = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);

        Toolbar toolbar = findViewById(R.id.detailToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        webView = findViewById(R.id.newsWebView);
        progressBar = findViewById(R.id.detailProgressBar);

        ImageView headerImage = findViewById(R.id.detailHeaderImage);
        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsingToolbar);

        // Extract data
        String url = getIntent().getStringExtra("url");
        String imageUrl = getIntent().getStringExtra("urlToImage");
        String title = getIntent().getStringExtra("title");
        String description = getIntent().getStringExtra("description");
        String content = getIntent().getStringExtra("content");
        String date = getIntent().getStringExtra("publishedAt");
        String sourceExtra = getIntent().getStringExtra("source");
        String source = sourceExtra != null ? sourceExtra : "IgniteNews";

        collapsingToolbar.setTitle(source);

        // Load the beautiful parallax image
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(this).load(imageUrl).into(headerImage);
        }

        // Track Reading History (Secure Cloud-First)
        addToHistory(url, title, source, imageUrl);

        // Feature 7: Reading Progress Logic
        LinearProgressIndicator readingBar = findViewById(R.id.readingProgressBar);
        NestedScrollView nestedScroll = findViewById(R.id.detailNestedScroll);

        nestedScroll.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            View child = v.getChildAt(0);
            if (child != null) {
                int totalContentHeight = child.getMeasuredHeight() - v.getMeasuredHeight();
                if (totalContentHeight > 0) {
                    int progress = (int) (((float) scrollY / totalContentHeight) * 100);
                    readingBar.setProgress(progress);
                }
            }
        });

        tts = new TextToSpeech(this, this);

        TextView detailSourceText = findViewById(R.id.tvDetailSource);
        Chip chipFollow = findViewById(R.id.chipFollowSource);

        detailSourceText.setText(source);

        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailDate = findViewById(R.id.tvDetailDate);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);


        
        // Use content if description is missing, or combine them for a better "detail" experience
        String fullSummary = "";
        if (description != null) fullSummary += description;
        if (content != null && !content.isEmpty()) {
            if (!fullSummary.isEmpty()) fullSummary += "\n\n";
            fullSummary += content;
        }
        
        if (fullSummary.isEmpty()) {
            tvDetailDescription.setVisibility(View.GONE);
        } else {
            tvDetailDescription.setText(fullSummary);
        }

        tvDetailTitle.setText(title);
        tvDetailDate.setText(date);

        // Feature 61: Deep Follow Logic (Hardened + Local-First)
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && source != null && !source.isEmpty()) {
            String userId = user.getUid();
            SharedPreferences publisherPrefs = getSharedPreferences("PublisherPrefs_" + userId, Context.MODE_PRIVATE);
            Set<String> followedSources = publisherPrefs.getStringSet("followed_sources", new HashSet<>());
            Set<String> currentFollows = new HashSet<>(followedSources);

            // Initial Check
            if (currentFollows.contains(source)) {
                chipFollow.setText("Following");
                chipFollow.setChipBackgroundColorResource(R.color.ignite_orange);
                chipFollow.setTextColor(Color.WHITE);
            }

            chipFollow.setOnClickListener(v -> {
                Set<String> latestFollows = new HashSet<>(publisherPrefs.getStringSet("followed_sources", new HashSet<>()));

                if (latestFollows.contains(source)) {
                    // Unfollow
                    latestFollows.remove(source);
                    publisherPrefs.edit().putStringSet("followed_sources", latestFollows).apply();

                    chipFollow.setText("+ Follow");
                    chipFollow.setChipBackgroundColorResource(android.R.color.transparent);
                    chipFollow.setTextColor(ContextCompat.getColor(this, R.color.ignite_orange));
                    Toast.makeText(this, "Stopped following " + source, Toast.LENGTH_SHORT).show();
                } else {
                    // Follow
                    latestFollows.add(source);
                    publisherPrefs.edit().putStringSet("followed_sources", latestFollows).apply();

                    chipFollow.setText("Following");
                    chipFollow.setChipBackgroundColorResource(R.color.ignite_orange);
                    chipFollow.setTextColor(Color.WHITE);
                    Toast.makeText(this, "Now following " + source + "!", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            chipFollow.setVisibility(View.GONE);
        }

        if (url != null) {
            trackArticleView(url);
            if (NetworkUtils.isNetworkAvailable(this)) {
                setupWebView(url);
                setupZoomGestures();
                fetchRelatedNews(title, source);
            } else {
                // Feature 72: High-Speed Vault Recovery
                NewsArticle cached = SmartCache.getCachedByUrl(this, url);
                if (cached != null) {
                    String cachedSource = cached.getSource() != null && cached.getSource().getName() != null ? cached.getSource().getName() : "News Vault";
                    String cachedDesc = cached.getDescription() != null ? cached.getDescription() : "Full content is unavailable offline. However, we've saved this key summary for you.";
                    String cachedContent = cached.getContent() != null ? cached.getContent() : "";
                    
                    String offlineHtml = "<html>\n" +
                            "<body style=\"padding: 24px; font-family: sans-serif; background-color: #FFFFFF; color: #333333; line-height: 1.6;\">\n" +
                            "    <h1 style=\"color: #FF5722; font-size: 24px;\">" + cached.getTitle() + "</h1>\n" +
                            "    <p style=\"color: #666666; font-size: 14px; font-weight: bold;\">Source: " + cachedSource + "</p>\n" +
                            "    <hr style=\"border: 0; border-top: 1px solid #EEEEEE; margin: 24px 0;\">\n" +
                            "    <p style=\"font-size: 18px;\">" + cachedDesc + "</p>\n" +
                            "    <p style=\"font-size: 18px; margin-top: 16px;\">" + cachedContent + "</p>\n" +
                            "    <div style=\"background: #FFF3E0; padding: 16px; border-radius: 8px; margin-top: 24px;\">\n" +
                            "        <p style=\"margin: 0; font-size: 12px; color: #E64A19;\">🚀 <b>Elite Reading Mode:</b> You are reading an offline version saved by your Smart Vault.</p>\n" +
                            "    </div>\n" +
                            "</body>\n" +
                            "</html>";
                    webView.loadDataWithBaseURL(null, offlineHtml, "text/html", "UTF-8", null);
                    Toast.makeText(this, "Reading from Vault (Offline)", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Network Error: This story isn't in your vault yet.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        } else {
            finish();
        }
    }

    private void trackArticleView(String url) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> viewData = new HashMap<>();
        viewData.put("url", url);
        viewData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("users").document(user.getUid()).collection("views").add(viewData);
    }

    private void setupZoomGestures() {
        TextView zoomIndicator = findViewById(R.id.tvZoomIndicator);
        GestureDetector detector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private final float THRESHOLD = 30f; // Sensitivity

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (e1 == null || e2 == null) return false;

                // Only if mostly horizontal
                if (Math.abs(distanceX) > Math.abs(distanceY) * 2) {
                    float deltaX = e2.getX() - e1.getX();

                    if (Math.abs(deltaX) > THRESHOLD) {
                        int zoomChange = (int) (deltaX / 10);
                        currentTextZoom += zoomChange;

                        // Clamp
                        if (currentTextZoom < 50) currentTextZoom = 50;
                        if (currentTextZoom > 280) currentTextZoom = 280;

                        // Apply to WebView
                        webView.getSettings().setTextZoom(currentTextZoom);

                        // Show Indicator
                        zoomIndicator.setText("Text Size: " + currentTextZoom + "%");
                        zoomIndicator.setVisibility(View.VISIBLE);
                        zoomIndicator.setAlpha(1f);

                        // Fade Out
                        zoomIndicator.animate().alpha(0f).setStartDelay(1200).setDuration(500).withEndAction(() -> zoomIndicator.setVisibility(View.GONE)).start();

                        return true;
                    }
                }
                return false;
            }
        });

        webView.setOnTouchListener((v, event) -> {
            detector.onTouchEvent(event);
            return false; // Continue for normal webview scrolling
        });
    }

    private void fetchRelatedNews(String title, String source) {
        String query = "news";
        if (title != null) {
            String[] parts = title.split(" ");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(3, parts.length); i++) {
                sb.append(parts[i]).append(" ");
            }
            query = sb.toString().trim();
        } else if (source != null) {
            query = source;
        }

        RecyclerView rv = findViewById(R.id.relatedRecyclerView);
        ShimmerFrameLayout shimmer = findViewById(R.id.shimmerRelated);

        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        shimmer.startShimmer();
        shimmer.setVisibility(View.VISIBLE);
        rv.setVisibility(View.GONE);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String userId = user.getUid();
        
        SharedPreferences prefs = getSharedPreferences("BookmarkPrefs_" + userId, Context.MODE_PRIVATE);
        String json = prefs.getString("saved_articles", "[]");
        Type type = new TypeToken<List<NewsArticle>>() {}.getType();
        List<NewsArticle> saved = new Gson().fromJson(json, type);
        if (saved == null) saved = new ArrayList<>();
        
        Set<String> bookmarkedUrls = new HashSet<>();
        for (NewsArticle a : saved) {
            if (a.getUrl() != null) bookmarkedUrls.add(a.getUrl());
        }

        RetrofitInstance.getApi().searchNews(query, "en", "publishedAt", RetrofitInstance.API_KEY)
                .enqueue(new Callback<NewsResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<NewsResponse> call, @NonNull Response<NewsResponse> response) {
                        shimmer.stopShimmer();
                        shimmer.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            List<NewsArticle> responseArticles = response.body().getArticles();
                            List<NewsArticle> articles = new ArrayList<>();
                            String currentUrl = getIntent().getStringExtra("url");
                            
                            if (responseArticles != null) {
                                for (NewsArticle a : responseArticles) {
                                    if (a.getUrl() != null && !a.getUrl().equals(currentUrl)) {
                                        articles.add(a);
                                        if (articles.size() >= 8) break;
                                    }
                                }
                            }

                            View header = findViewById(R.id.relatedSectionHeader);
                            if (articles.isEmpty()) {
                                header.setVisibility(View.GONE);
                                return;
                            }

                            header.setVisibility(View.VISIBLE);
                            rv.setVisibility(View.VISIBLE);
                            rv.setAlpha(0f);
                            rv.animate().alpha(1f).setDuration(500).start();

                            rv.setAdapter(new NewsAdapter(
                                    articles,
                                    bookmarkedUrls,
                                    new NewsAdapter.OnNewsClickListener() {
                                        @Override
                                        public void onBookmarkClick(NewsArticle art, boolean checked) {
                                            handleLocalBookmark(art, checked);
                                        }

                                        @Override
                                        public void onItemClick(NewsArticle art, ImageView img) {
                                            Intent intent = new Intent(NewsDetailActivity.this, NewsDetailActivity.class);
                                            intent.putExtra("url", art.getUrl());
                                            intent.putExtra("title", art.getTitle());
                                            intent.putExtra("description", art.getDescription());
                                            intent.putExtra("content", art.getContent());
                                            intent.putExtra("publishedAt", art.getPublishedAt());
                                            intent.putExtra("urlToImage", art.getUrlToImage());
                                            intent.putExtra("source", art.getSource() != null ? art.getSource().getName() : null);
                                            startActivity(intent);
                                            finish(); // Slide to next one smoothly
                                        }

                                        @Override
                                        public void onSourceClick(String src) {
                                            Intent intent = new Intent(NewsDetailActivity.this, NewsActivity.class);
                                            intent.putExtra("search_query", src);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                            startActivity(intent);
                                        }
                                    }
                            ));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<NewsResponse> call, @NonNull Throwable t) {
                        shimmer.stopShimmer();
                        shimmer.setVisibility(View.GONE);
                    }
                });
    }

    private void handleLocalBookmark(NewsArticle article, boolean isChecked) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String userId = user.getUid();
        
        SharedPreferences prefs = getSharedPreferences("BookmarkPrefs_" + userId, Context.MODE_PRIVATE);
        String json = prefs.getString("saved_articles", "[]");
        Type type = new TypeToken<List<NewsArticle>>() {}.getType();
        List<NewsArticle> saved = new Gson().fromJson(json, type);
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
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
            }
        } else {
            for (int i = 0; i < saved.size(); i++) {
                if (saved.get(i).getUrl() != null && saved.get(i).getUrl().equals(article.getUrl())) {
                    saved.remove(i);
                    break;
                }
            }
            Toast.makeText(this, "Removed", Toast.LENGTH_SHORT).show();
        }
        prefs.edit().putString("saved_articles", new Gson().toJson(saved)).apply();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            if (tts != null) {
                tts.setLanguage(Locale.US);
            }
        } else {
            Toast.makeText(this, "TTS Initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupWebView(String url) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        webView.loadUrl(url);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);

        // Feature 50: Setup Direct 'Aa' Text Action View
        MenuItem item = menu.findItem(R.id.action_text_size);
        if (item != null && item.getActionView() != null) {
            View actionView = item.getActionView();
            View tv = actionView.findViewById(R.id.tvActionTextSize);
            if (tv != null) {
                tv.setOnClickListener(v -> showTextOptions());
            }
        }

        // Feature 102: Force White Icons for Visibility (Elite Contrast)
        MaterialToolbar toolbar = findViewById(R.id.detailToolbar);
        int white = ContextCompat.getColor(this, android.R.color.white);
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(white); // Back arrow
        }

        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            if (menuItem.getIcon() != null) {
                android.graphics.drawable.Drawable icon = DrawableCompat.wrap(menuItem.getIcon()).mutate();
                DrawableCompat.setTint(icon, white);
                menuItem.setIcon(icon);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_text_size) {
            showTextOptions();
            return true;
        } else if (itemId == R.id.action_listen) {
            speakNews();
            return true;
        } else if (itemId == R.id.action_share) {
            shareNews();
            return true;
        } else if (itemId == R.id.action_download) {
            downloadArticle();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showTextOptions() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_text_zoom, null);
        Slider slider = dialogView.findViewById(R.id.zoomSlider);
        TextView zoomLabel = dialogView.findViewById(R.id.tvDialogZoomLabel);
        // Sync initial state
        slider.setValue(currentTextZoom);
        zoomLabel.setText("Text Size: " + currentTextZoom + "%");

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Done", null)
                .show();

        // Live Slider Logic
        slider.addOnChangeListener((slider1, value, fromUser) -> {
            if (fromUser) {
                currentTextZoom = (int) value;
                webView.getSettings().setTextZoom(currentTextZoom);
                zoomLabel.setText("Text Size: " + currentTextZoom + "%");

                // Feature 103: Sync Native Text Sizes
                float scale = currentTextZoom / 100f;
                if (tvDetailTitle != null) tvDetailTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26 * scale);
                if (tvDetailDescription != null) tvDetailDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 * scale);
                if (tvDetailDate != null) tvDetailDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14 * scale);
            }
        });
    }

    private void speakNews() {
        String titleExtra = getIntent().getStringExtra("title");
        String title = titleExtra != null ? titleExtra : "";
        String descExtra = getIntent().getStringExtra("description");
        String description = descExtra != null ? descExtra : "";
        String textToSpeak = "Title: " + title + ". Description: " + description;

        if (tts != null && tts.isSpeaking()) {
            tts.stop();
            Toast.makeText(this, "Speech stopped", Toast.LENGTH_SHORT).show();
        } else {
            if (tts != null) {
                tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                Toast.makeText(this, "Reading news...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void downloadArticle() {
        String titleExtra = getIntent().getStringExtra("title");
        String title = titleExtra != null ? titleExtra.replaceAll("[^a-zA-Z0-9]", "_") : "Article";
        PrintManager manager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        String jobName = "IgniteNews_" + title;

        Toast.makeText(this, "Generating Research PDF...", Toast.LENGTH_LONG).show();

        try {
            // Feature 101: High-Fidelity PDF Export
            PrintDocumentAdapter adapter;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                adapter = webView.createPrintDocumentAdapter(jobName);
            } else {
                adapter = webView.createPrintDocumentAdapter();
            }

            if (manager != null) {
                manager.print(jobName, adapter, new PrintAttributes.Builder().build());
            }

            // Tactile success
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "PDF creation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareNews() {
        String url = getIntent().getStringExtra("url");
        if (url == null) return;
        String titleExtra = getIntent().getStringExtra("title");
        String title = titleExtra != null ? titleExtra : "Ignite News Insight";
        String sourceExtra = getIntent().getStringExtra("source");
        String source = sourceExtra != null ? sourceExtra : "IgniteNews";

        try {
            // Feature 87: Elite Share Card Generation
            int width = 1080;
            int height = 1080;
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            // 1. Draw Deep Matte Background
            canvas.drawColor(Color.parseColor("#121212"));

            // 2. Setup Typography Paint
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.WHITE);
            paint.setTextSize(72f);
            paint.setTypeface(Typeface.DEFAULT_BOLD);

            // 3. Draw Wrapped Headline
            TextPaint textPaint = new TextPaint(paint);
            StaticLayout staticLayout;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                staticLayout = StaticLayout.Builder.obtain(title, 0, title.length(), textPaint, width - 160)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1.2f)
                        .build();
            } else {
                staticLayout = new StaticLayout(title, textPaint, width - 160, Layout.Alignment.ALIGN_NORMAL, 1.2f, 0f, false);
            }

            canvas.save();
            canvas.translate(80f, 200f);
            staticLayout.draw(canvas);
            canvas.restore();

            // 4. Draw Signature Accent (Ignite Orange Source)
            paint.setColor(Color.parseColor("#FF5722"));
            paint.setTextSize(48f);
            canvas.drawText(source.toUpperCase(), 80f, height - 120f, paint);

            // 5. Draw Signature App Logo (The Infinite Fold)
            Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.app_logo);
            if (logo != null) {
                Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, 100, 100, true);
                canvas.drawBitmap(scaledLogo, width - 180f, height - 160f, null);
            }

            // 6. Save to Secure Cache
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "share_card.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            // 7. Dispatch the Image Intent
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Read the full story: " + url + "\n\nShared via IgniteNews");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Elite Card via"));

        } catch (Exception e) {
            // Fallback to text if graphics fail
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this news!");
            shareIntent.putExtra(Intent.EXTRA_TEXT, title + "\n\nRead more at: " + url);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    private void addToHistory(String url, String title, String source, String imageUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || url == null || title == null) return;

        NewsArticle article = new NewsArticle(title, "", url, imageUrl, "Viewed recently", new Source(source), "");
        SharedPreferences prefs = getSharedPreferences("HistoryPrefs_" + user.getUid(), Context.MODE_PRIVATE);
        Gson gsonLocal = new Gson();
        String json = prefs.getString("saved_history", "[]");
        Type type = new TypeToken<List<NewsArticle>>() {}.getType();
        List<NewsArticle> historyList = gsonLocal.fromJson(json, type);
        if (historyList == null) historyList = new ArrayList<>();

        // Remove if duplicate exists to bring it to top
        for (int i = 0; i < historyList.size(); i++) {
            if (historyList.get(i).getUrl() != null && historyList.get(i).getUrl().equals(url)) {
                historyList.remove(i);
                break;
            }
        }
        historyList.add(0, article); // Add to top

        // Limit to 50 items
        if (historyList.size() > 50) {
            historyList.remove(historyList.size() - 1);
        }

        prefs.edit().putString("saved_history", gsonLocal.toJson(historyList)).apply();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
