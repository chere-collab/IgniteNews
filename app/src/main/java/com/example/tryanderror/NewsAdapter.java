package com.example.tryanderror;

import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    public interface OnNewsClickListener {
        void onBookmarkClick(NewsArticle article, boolean isChecked);
        void onItemClick(NewsArticle article, ImageView thumbnail);
        void onSourceClick(String sourceName);
    }

    private List<NewsArticle> articles;
    private Set<String> bookmarkedUrls;
    private OnNewsClickListener listener;

    public NewsAdapter(List<NewsArticle> articles, Set<String> bookmarkedUrls, OnNewsClickListener listener) {
        this.articles = articles;
        this.bookmarkedUrls = bookmarkedUrls != null ? bookmarkedUrls : new HashSet<>();
        this.listener = listener;
    }

    public void updateBookmarks(Set<String> newUrls) {
        this.bookmarkedUrls = newUrls;
        notifyDataSetChanged();
    }

    public void updateArticles(List<NewsArticle> newArticles) {
        this.articles = newArticles;
        notifyDataSetChanged();
    }

    public static class NewsViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView source;
        TextView date;
        ImageView thumbnail;
        ImageView doubleTapStar;
        CheckBox bookmark;

        public NewsViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.newsTitle);
            source = view.findViewById(R.id.newsSource);
            date = view.findViewById(R.id.newsDate);
            thumbnail = view.findViewById(R.id.newsThumbnail);
            doubleTapStar = view.findViewById(R.id.ivDoubleTapStar);
            bookmark = view.findViewById(R.id.bookmarkIcon);
        }
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.news_item, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsArticle article = articles.get(position);
        holder.itemView.startAnimation(AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.item_animation));

        RecyclerView.LayoutManager layoutManager = null;
        if (holder.itemView.getParent() instanceof RecyclerView) {
            layoutManager = ((RecyclerView) holder.itemView.getParent()).getLayoutManager();
        }
        
        if (layoutManager instanceof LinearLayoutManager &&
                ((LinearLayoutManager) layoutManager).getOrientation() == LinearLayoutManager.HORIZONTAL) {
            ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
            params.width = (int) (300 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            holder.itemView.setLayoutParams(params);
        }

        holder.title.setText(article.getTitle());
        holder.source.setText(article.getSource() != null ? article.getSource().getName() : "");
        holder.date.setText(article.getPublishedAt());

        holder.source.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.ignite_orange));
        holder.source.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSourceClick(article.getSource() != null ? article.getSource().getName() : null);
            }
        });

        holder.bookmark.setOnCheckedChangeListener(null);
        holder.bookmark.setChecked(bookmarkedUrls.contains(article.getUrl()));

        holder.thumbnail.setTransitionName("shared_article_image");

        Glide.with(holder.itemView.getContext())
                .load(article.getUrlToImage())
                .placeholder(android.R.drawable.progress_indeterminate_horizontal)
                .error(android.R.drawable.ic_menu_gallery)
                .into(holder.thumbnail);

        final long[] lastClickTime = {0};

        holder.thumbnail.setOnClickListener(v -> {
            long clickTime = System.currentTimeMillis();
            if (clickTime - lastClickTime[0] < 300) {
                holder.bookmark.setChecked(true);
                if (listener != null) {
                    listener.onBookmarkClick(article, true);
                }

                holder.doubleTapStar.setVisibility(View.VISIBLE);
                holder.doubleTapStar.setAlpha(0f);
                holder.doubleTapStar.setScaleX(0.5f);
                holder.doubleTapStar.setScaleY(0.5f);

                holder.doubleTapStar.animate()
                        .alpha(1f).scaleX(1.5f).scaleY(1.5f)
                        .setDuration(200)
                        .withEndAction(() -> {
                            holder.doubleTapStar.animate()
                                    .alpha(0f).scaleX(1f).scaleY(1f)
                                    .setDuration(300)
                                    .withEndAction(() -> holder.doubleTapStar.setVisibility(View.GONE))
                                    .start();
                        }).start();
                lastClickTime[0] = 0;
            } else {
                lastClickTime[0] = clickTime;
                holder.thumbnail.postDelayed(() -> {
                    if (lastClickTime[0] == clickTime) {
                        if (listener != null) {
                            listener.onItemClick(article, holder.thumbnail);
                        }
                    }
                }, 300);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(article, holder.thumbnail);
            }
        });

        holder.bookmark.setOnClickListener(v -> {
            boolean isChecked = holder.bookmark.isChecked();
            if (listener != null) {
                listener.onBookmarkClick(article, isChecked);
            }
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        });
    }

    @Override
    public int getItemCount() {
        return articles != null ? articles.size() : 0;
    }
}
