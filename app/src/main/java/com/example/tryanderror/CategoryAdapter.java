package com.example.tryanderror;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    private List<Category> categories;
    private OnCategoryClickListener listener;
    private int selectedPosition = 0;

    public CategoryAdapter(List<Category> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        TextView tvName;

        public ViewHolder(View view) {
            super(view);
            card = view.findViewById(R.id.categoryCard);
            tvName = view.findViewById(R.id.tvCategoryName);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.tvName.setText(category.getEmoji() + "  " + category.getName());

        if (position == selectedPosition) {
            // Selected: vibrant brand color
            holder.card.setCardBackgroundColor(Color.parseColor(category.getColorHex()));
            holder.tvName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white));
        } else {
            // Unselected: adapts to dark/light mode via color resource
            holder.card.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.category_unselected_bg));
            holder.tvName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.category_unselected_text));
        }

        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onCategoryClick(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories != null ? categories.size() : 0;
    }

    public void selectCategory(String key) {
        int idx = -1;
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getKey().equals(key)) {
                idx = i;
                break;
            }
        }
        if (idx != -1) {
            int prev = selectedPosition;
            selectedPosition = idx;
            notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
        }
    }
}
