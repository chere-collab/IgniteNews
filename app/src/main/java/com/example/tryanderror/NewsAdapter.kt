package com.example.tryanderror

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class NewsAdapter(
    private var articles: List<NewsArticle>,
    private var bookmarkedUrls: Set<String> = emptySet(),
    private val onBookmarkClick: (NewsArticle, Boolean) -> Unit,
    private val onItemClick: (NewsArticle, ImageView) -> Unit,
    private val onSourceClick: (String?) -> Unit
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    fun updateBookmarks(newUrls: Set<String>) {
        this.bookmarkedUrls = newUrls
        notifyDataSetChanged()
    }

    fun updateArticles(newArticles: List<NewsArticle>) {
        this.articles = newArticles
        notifyDataSetChanged()
    }

    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.newsTitle)
        val source: TextView = view.findViewById(R.id.newsSource)
        val date: TextView = view.findViewById(R.id.newsDate)
        val thumbnail: ImageView = view.findViewById(R.id.newsThumbnail)
        val doubleTapStar: ImageView = view.findViewById(R.id.ivDoubleTapStar)
        val bookmark: CheckBox = view.findViewById(R.id.bookmarkIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.news_item, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val article = articles[position]
        holder.itemView.startAnimation(AnimationUtils.loadAnimation(holder.itemView.context, R.anim.item_animation))
        
        // Premium Horizontal Look: If in a carousel, limit width to see the next card
        val layoutManager = (holder.itemView.parent as? RecyclerView)?.layoutManager
        if (layoutManager is androidx.recyclerview.widget.LinearLayoutManager && 
            layoutManager.orientation == androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL) {
            val params = holder.itemView.layoutParams
            params.width = (300 * holder.itemView.context.resources.displayMetrics.density).toInt()
            holder.itemView.layoutParams = params
        }
        holder.title.text = article.title
        holder.source.text = article.source?.name
        holder.date.text = article.publishedAt
        
        // Feature 3: Publisher Link Styling
        holder.source.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.ignite_orange))
        holder.source.setOnClickListener {
            onSourceClick(article.source?.name)
        }
        
        // Disable listener temporarily to prevent accidental trigger while setting state
        holder.bookmark.setOnCheckedChangeListener(null)
        holder.bookmark.isChecked = bookmarkedUrls.contains(article.url)

        holder.thumbnail.transitionName = "shared_article_image"

        Glide.with(holder.itemView.context)
            .load(article.urlToImage)
            .placeholder(android.R.drawable.progress_indeterminate_horizontal)
            .error(android.R.drawable.ic_menu_gallery)
            .into(holder.thumbnail)

        var lastClickTime: Long = 0

        holder.thumbnail.setOnClickListener {
            val clickTime = System.currentTimeMillis()
            if (clickTime - lastClickTime < 300) {
                // Double tap!
                holder.bookmark.isChecked = true
                onBookmarkClick(article, true)
                
                holder.doubleTapStar.visibility = View.VISIBLE
                holder.doubleTapStar.alpha = 0f
                holder.doubleTapStar.scaleX = 0.5f
                holder.doubleTapStar.scaleY = 0.5f
                
                holder.doubleTapStar.animate()
                    .alpha(1f).scaleX(1.5f).scaleY(1.5f)
                    .setDuration(200)
                    .withEndAction {
                        holder.doubleTapStar.animate()
                            .alpha(0f).scaleX(1f).scaleY(1f)
                            .setDuration(300)
                            .withEndAction { holder.doubleTapStar.visibility = View.GONE }
                            .start()
                    }.start()
                lastClickTime = 0
            } else {
                lastClickTime = clickTime
                // Single tap action
                holder.thumbnail.postDelayed({
                    if (lastClickTime == clickTime) {
                        onItemClick(article, holder.thumbnail)
                    }
                }, 300)
            }
        }

        // Keep standard instant click for the rest of the card (title, date, bg)
        holder.itemView.setOnClickListener {
            onItemClick(article, holder.thumbnail)
        }

        // Use OnClickListener instead of OnCheckedChangeListener to avoid recycler-view trigger loops
        holder.bookmark.setOnClickListener {
            val isChecked = holder.bookmark.isChecked
            onBookmarkClick(article, isChecked)
            
            // Subtle click vibration for tactile feel
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    override fun getItemCount(): Int = articles.size
}
