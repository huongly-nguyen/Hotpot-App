package com.example.hotpot

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hotpot.databinding.ItemFavoriteRecipeBinding

interface OnRecipeClickListener {
    fun onRecipeClick(position: Int)
}

class FavoritesAdapter(private var recipes: List<Recipe>, private val clickListener: OnRecipeClickListener // Add this parameter
) : RecyclerView.Adapter<FavoritesAdapter.FavoritesViewHolder>() {
    class FavoritesViewHolder(val binding: ItemFavoriteRecipeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(recipe: Recipe, clickListener: OnRecipeClickListener) {
            // Bind data to the views
            binding.recipeNameTextView.text = recipe.name

            // Set click listener on the item view
            binding.root.setOnClickListener {
                // Pass the adapter position to the click listener
                clickListener.onRecipeClick(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritesViewHolder {
        val binding = ItemFavoriteRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FavoritesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoritesViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.bind(recipe, clickListener)
    }

    override fun getItemCount(): Int = recipes.size

    fun updateData(newData: List<Recipe>) {
        recipes = newData
        notifyDataSetChanged()
    }

}
