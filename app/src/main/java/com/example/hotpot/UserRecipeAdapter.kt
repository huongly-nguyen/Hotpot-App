package com.example.hotpot

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference

class UserRecipeAdapter(
    private var dataset: List<Recipe>,
    private val storageReference: StorageReference,
    private val databaseReference: DatabaseReference
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val RECIPE_TYPE = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            RECIPE_TYPE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_recipe_in_user_profile, parent, false)
                RecipeViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is RecipeViewHolder -> {
                val recipe = dataset[position]
                holder.bind(recipe, storageReference)
            }
        }
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun getItemViewType(position: Int): Int {
        return RECIPE_TYPE
    }

    fun updateData(newDataset: List<Recipe>) {
        dataset = newDataset
        notifyDataSetChanged()
    }

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recipeNameTextView: TextView = itemView.findViewById(R.id.recipeNameTextView)
        private val recipeIconImageView: ImageView = itemView.findViewById(R.id.recipeIconImageView)

        fun bind(recipe: Recipe, storageReference: StorageReference) {
            recipeNameTextView.text = recipe.name

            // Load the image using Glide
            Glide.with(itemView.context).load(storageReference.child(recipe.imageUrl)).into(recipeIconImageView)

            // Handle the click event or navigate to the recipe details as needed
            // You can use a similar approach as in the SearchAdapter
        }
    }
}


