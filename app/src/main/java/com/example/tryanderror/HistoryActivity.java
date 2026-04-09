package com.example.tryanderror;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NewsAdapter historyAdapter;
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Toolbar toolbar = findViewById(R.id.historyToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.historyRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        historyAdapter = new NewsAdapter(
                new ArrayList<>(),
                new HashSet<>(),
                new NewsAdapter.OnNewsClickListener() {
                    @Override
                    public void onBookmarkClick(NewsArticle article, boolean isChecked) {
                        // Toggle bookmarks from history
                    }

                    @Override
                    public void onItemClick(NewsArticle article, ImageView imageView) {
                        Intent intent = new Intent(HistoryActivity.this, NewsDetailActivity.class);
                        intent.putExtra("url", article.getUrl());
                        intent.putExtra("title", article.getTitle());
                        intent.putExtra("description", article.getDescription());
                        intent.putExtra("content", article.getContent());
                        intent.putExtra("publishedAt", article.getPublishedAt());
                        intent.putExtra("source", article.getSource() != null ? article.getSource().getName() : null);
                        intent.putExtra("urlToImage", article.getUrlToImage());
                        startActivity(intent);
                    }

                    @Override
                    public void onSourceClick(String source) {
                        Intent intent = new Intent(HistoryActivity.this, NewsActivity.class);
                        intent.putExtra("search_query", source);
                        startActivity(intent);
                    }
                }
        );
        recyclerView.setAdapter(historyAdapter);

        loadHistory();
    }

    private void loadHistory() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        SharedPreferences prefs = getSharedPreferences("HistoryPrefs_" + user.getUid(), Context.MODE_PRIVATE);
        String json = prefs.getString("saved_history", "[]");
        Type type = new TypeToken<List<NewsArticle>>(){}.getType();
        List<NewsArticle> articles = gson.fromJson(json, type);
        if (articles == null) {
            articles = new ArrayList<>();
        }

        TextView emptyText = findViewById(R.id.emptyHistoryText);
        if (articles.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText("History is empty. Read some articles first! \uD83D\uDCD6");
        } else {
            emptyText.setVisibility(View.GONE);
            historyAdapter.updateArticles(articles);
        }
    }
}
