package com.example.tryanderror

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SuggestionAdapter(
    private var suggestions: List<String>,
    private val onSuggestionClick: (String) -> Unit
) : RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder>() {

    class SuggestionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return SuggestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        val suggestion = suggestions[position]
        holder.text.text = suggestion
        holder.text.textSize = 14f
        holder.text.setPadding(32, 16, 32, 16)
        holder.itemView.setOnClickListener { onSuggestionClick(suggestion) }
    }

    override fun getItemCount(): Int = suggestions.size

    fun updateSuggestions(newSuggestions: List<String>) {
        this.suggestions = newSuggestions
        notifyDataSetChanged()
    }
}
