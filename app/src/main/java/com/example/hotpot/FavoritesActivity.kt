package com.example.hotpot

import RecipeDetailsFragment
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.auth.FirebaseAuth

class FavoritesActivity : AppCompatActivity() {

    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var favoritesAdapter: FavoritesAdapter
    private var favoriteRecipes: List<Recipe> = listOf()
    private lateinit var userId: String
    private lateinit var databaseReference: DatabaseReference
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_favorites)

        favoritesRecyclerView = findViewById(R.id.favorites_recycler_view)
        // Pass the click listener when creating the FavoritesAdapter
        favoritesAdapter = FavoritesAdapter(favoriteRecipes, object : OnRecipeClickListener {
            override fun onRecipeClick(position: Int) {
                val clickedRecipe = favoriteRecipes[position]
                // Handle the click event, for example, open a new activity or show details
                val detailsFragment = RecipeDetailsFragment()
                val bundle = Bundle()
                bundle.putSerializable("RECIPE_DATA", clickedRecipe)
                detailsFragment.arguments = bundle
                detailsFragment.show(supportFragmentManager, detailsFragment.tag)
            }
        })
        favoritesRecyclerView.adapter = favoritesAdapter
        favoritesRecyclerView.layoutManager = LinearLayoutManager(this)

        val returnButton = findViewById<ImageButton>(R.id.toolbar_return_button)
        returnButton.setOnClickListener {
            finish()
            onBackPressed()
        }

        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        databaseReference = FirebaseDatabase.getInstance().reference.child("Users").child(userId).child("Favorites")

        loadFavoriteRecipes()

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_settings
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_settings -> {
                    val intent = Intent(this, AccountActivity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.navigation_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.navigation_list -> {
                    val intent = Intent(this, ShoppingListActivity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.navigation_search -> {
                    val intent = Intent(this, FavoritesActivity::class.java)
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }
    }

    private fun loadFavoriteRecipes() {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                favoriteRecipes = emptyList()

                for (recipeSnapshot in dataSnapshot.children) {
                    val recipe = recipeSnapshot.getValue(Recipe::class.java)
                    recipe?.let { favoriteRecipes = favoriteRecipes + it }
                }

                favoritesAdapter.updateData(favoriteRecipes)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }
}




