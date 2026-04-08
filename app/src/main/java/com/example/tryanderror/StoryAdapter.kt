package com.example.tryanderror

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class StoryAdapter(
    private var articles: List<NewsArticle>,
    private val onItemClick: (NewsArticle) -> Unit
) : RecyclerView.Adapter<StoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.storyCircleImage)
        val source: TextView = view.findViewById(R.id.storySourceText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_story, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article = articles[position]
        holder.source.text = article.source?.name ?: "News"
        
        Glide.with(holder.itemView.context)
            .load(article.urlToImage)
            .placeholder(R.drawable.ic_news_placeholder)
            .error(R.drawable.ic_news_placeholder)
            .into(holder.image)
            
        holder.itemView.setOnClickListener { onItemClick(article) }
    }

    override fun getItemCount(): Int = articles.size

    fun updateStories(newArticles: List<NewsArticle>) {
        this.articles = newArticles
        notifyDataSetChanged()
    }
}
