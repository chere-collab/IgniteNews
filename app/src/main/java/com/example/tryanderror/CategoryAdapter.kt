package com.example.tryanderror

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

data class Category(val name: String, val key: String, val emoji: String, val colorHex: String)

class CategoryAdapter(
    private val categories: List<Category>,
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var selectedPosition = 0

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.categoryCard)
        val tvName: TextView = view.findViewById(R.id.tvCategoryName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.tvName.text = "${category.emoji}  ${category.name}"

        if (position == selectedPosition) {
            // Selected: vibrant brand color
            holder.card.setCardBackgroundColor(android.graphics.Color.parseColor(category.colorHex))
            holder.tvName.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.context, android.R.color.white))
        } else {
            // Unselected: adapts to dark/light mode via color resource
            holder.card.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.category_unselected_bg))
            holder.tvName.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.category_unselected_text))
        }

        holder.itemView.setOnClickListener {
            val prev = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(prev)
            notifyItemChanged(selectedPosition)
            onCategoryClick(category)
        }
    }

    override fun getItemCount() = categories.size

    fun selectCategory(key: String) {
        val idx = categories.indexOfFirst { it.key == key }
        if (idx != -1) {
            val prev = selectedPosition
            selectedPosition = idx
            notifyItemChanged(prev)
            notifyItemChanged(selectedPosition)
        }
    }
}
