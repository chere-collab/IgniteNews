package com.example.tryanderror;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.ViewHolder> {

    public interface OnStoryClickListener {
        void onItemClick(NewsArticle article);
    }

    private List<NewsArticle> articles;
    private OnStoryClickListener listener;

    public StoryAdapter(List<NewsArticle> articles, OnStoryClickListener listener) {
        this.articles = articles;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView source;

        public ViewHolder(View view) {
            super(view);
            image = view.findViewById(R.id.storyCircleImage);
            source = view.findViewById(R.id.storySourceText);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_story, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NewsArticle article = articles.get(position);
        holder.source.setText(article.getSource() != null ? article.getSource().getName() : "News");

        Glide.with(holder.itemView.getContext())
                .load(article.getUrlToImage())
                .placeholder(R.drawable.ic_news_placeholder)
                .error(R.drawable.ic_news_placeholder)
                .into(holder.image);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(article);
            }
        });
    }

    @Override
    public int getItemCount() {
        return articles != null ? articles.size() : 0;
    }

    public void updateStories(List<NewsArticle> newArticles) {
        this.articles = newArticles;
        notifyDataSetChanged();
    }
}
