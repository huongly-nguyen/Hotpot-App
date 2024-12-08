package com.example.hotpot

import FridgeFragment
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

//TODO: make search results clickable to go to user profile

//TODO: create user profiles


class SearchActivity : AppCompatActivity(), AdapterCallback {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var searchAdapter: SearchAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var filterUserButton: Button
    private lateinit var filterRecipeButton: Button
    private lateinit var filterSearchFilterButton : Button
    private var currentFilter: String = ""

    override fun deactivateUIElements() {
        // Deactivate or hide UI elements in your activity
        findViewById<BottomNavigationView>(R.id.bottom_navigation).isEnabled = false
        findViewById<LinearLayout>(R.id.searchFrameLayout).isEnabled = false
        searchView.clearFocus()
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_search)

        // Initialize RecyclerView and set its layout manager
        recyclerView = findViewById(R.id.searchItemRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val storageReference = FirebaseStorage.getInstance().reference
        val databaseReference = FirebaseDatabase.getInstance().reference

        // Initialize SearchAdapter with an empty list and reference to Firebase Storage
        searchAdapter = SearchAdapter(emptyList(), storageReference, databaseReference, this)

        recyclerView.adapter = searchAdapter


        searchView = findViewById(R.id.searchView)
        setupSearchView()
        searchView.queryHint = "select a button for better search first"


        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_search
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_settings -> {
                    val intent = Intent(this, AccountActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }

                R.id.navigation_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    true
                }

                R.id.navigation_list -> {
                    val intent = Intent(this, ShoppingListActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }

                R.id.navigation_search -> {
                    val intent = Intent(this, SearchActivity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.navigation_fridge -> {
                    //checkAndRequestPermissions()
                    val intent = Intent(this, IngredientsList::class.java)
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }

                else -> false
            }
        }

        // Initialize buttons
        filterUserButton = findViewById(R.id.searchFilterUser)
        filterRecipeButton = findViewById(R.id.searchFilterRecipe)
        filterSearchFilterButton = findViewById(R.id.searchFilter)

        // Set initial styles for buttons
        setButtonStyle(filterUserButton, false)
        setButtonStyle(filterRecipeButton, false)
        setButtonStyle(filterSearchFilterButton, false)

        filterUserButton.setOnClickListener {
            setButtonStyle(filterUserButton, true)
            setButtonStyle(filterRecipeButton, false)
            setButtonStyle(filterSearchFilterButton, false)

            // enable searchBar
            searchView = findViewById(R.id.searchView)
            setupSearchView()
            searchView.queryHint = " "
            // Handle user filter logic here
            currentFilter = "Users"
            updateSearchResults(searchView.query.toString(), currentFilter)
            // Enable searchView when a filter is selected
            searchView.isFocusable = true
            searchView.isFocusableInTouchMode = true
        }

        filterRecipeButton.setOnClickListener {
            setButtonStyle(filterUserButton, false)
            setButtonStyle(filterRecipeButton, true)
            setButtonStyle(filterSearchFilterButton, false)

            // enable searchBar
            searchView = findViewById(R.id.searchView)
            setupSearchView()
            searchView.queryHint = " "
            // Handle recipe filter logic here
            currentFilter = "Recipes"
            updateSearchResults(searchView.query.toString(), currentFilter)
            // Enable searchView when a filter is selected
            searchView.isFocusable = true
            searchView.isFocusableInTouchMode = true
        }

        filterSearchFilterButton.setOnClickListener {
            setButtonStyle(filterUserButton, false)
            setButtonStyle(filterRecipeButton, false)
            setButtonStyle(filterSearchFilterButton, true)

            // Enable searchBar
            searchView = findViewById(R.id.searchView)
            setupSearchView()
            searchView.queryHint = " "
            // Handle filter logic here
            currentFilter = "Filters"
            updateSearchResults(searchView.query.toString(), currentFilter)
            // Enable searchView when a filter is selected
            searchView.isFocusable = true
            searchView.isFocusableInTouchMode = true
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Überprüfe nur, wenn die SearchView den Fokus hat
                if (searchView.hasFocus()) {
                    // Überprüfe, ob keiner der Buttons ausgewählt ist
                    if (!filterUserButton.isActivated && !filterRecipeButton.isActivated && !filterSearchFilterButton.isActivated) {
                        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
                            if (hasFocus && !filterUserButton.isActivated && !filterRecipeButton.isActivated && !filterSearchFilterButton.isActivated) {
                                Toast.makeText(this@SearchActivity, "Bitte wähle einen Filter aus", Toast.LENGTH_SHORT).show()
                                // Leere die Query, wenn der Fokus erhalten bleibt
                                searchView.setQuery("", false)
                                searchView.clearFocus()
                                // Setze den Fokus erneut, um den Text zu löschen
                                searchView.requestFocus()
                            }
                        }
                    }
                }
                // Handle search query submission if needed
                if (query != null && query.isNotEmpty()) {
                    updateSearchResults(query.toLowerCase(), currentFilter)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Handle search query changes
                // You may want to filter your data based on the newText and update the RecyclerView
                updateSearchResults(newText?.toLowerCase().orEmpty(), currentFilter)
                return true
            }
        })
    }

    private fun updateSearchResults(query: String, filter: String) {
        if (query.isNotEmpty()) {
            // Perform the search and update the adapter with the filtered results
            val filteredResults = performSearch(query, filter)
            searchAdapter.updateData(filteredResults)
        } else {
            // Clear the adapter data when the query is empty
            searchAdapter.clearData()
        }
    }


    private fun performSearch(query: String, filter: String): List<Any> {
        val results = mutableListOf<Any>()

        // Prüfe, ob ein Filter ausgewählt wurde
        if (filter.isEmpty()) {
            // Zeige einen Toast an, wenn kein Filter ausgewählt wurde
            Toast.makeText(this, "Bitte wähle einen Filter aus", Toast.LENGTH_SHORT).show()
            // Fokus von der SearchView entfernen
            searchView.clearFocus()
            return results
        }

        val lowercaseQuery = query.toLowerCase()

        val database = FirebaseDatabase.getInstance()
        val reference = when (filter) {
            "Users" -> database.getReference("Users")
            "Recipes" -> database.getReference("Recipes")
            "Filters" -> database.getReference("Recipes")
            else -> throw IllegalArgumentException("Invalid filter: $filter")
        }

        // Query Firebase based on the search query
        reference.orderByChild("name").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val results = mutableListOf<Any>()

                for (snapshot in dataSnapshot.children) {
                    // Parse the data
                    val id = snapshot.key ?: ""
                    val name = snapshot.child("name").getValue(String::class.java) ?: ""

                    // Convert both the query and database data to lowercase for case-insensitive comparison
                    val lowercaseName = name.toLowerCase()

                    // Check if the lowercase name contains the lowercase query
                    when (filter) {
                        "Users" -> {
                            if (lowercaseName.contains(lowercaseQuery)) {
                                results.add(User(id, name))
                            }
                        }

                        "Recipes" -> {
                            // Check if the name contains the query
                            if (lowercaseName.contains(lowercaseQuery)) {
                                results.add(RecipeInSearch("recipe_image_url", name))
                            }
                        }

                        "Filters" -> {
                            val tagsSnapshot = snapshot.child("tags")
                            var tagFound = false

                            for (tagSnapshot in tagsSnapshot.children) {
                                val tag = tagSnapshot.value.toString().toLowerCase()

                                // Prüfe, ob der Tag mit den entsprechenden Buchstaben der Query übereinstimmt
                                val querySubstring = lowercaseQuery.substring(0, kotlin.math.min(lowercaseQuery.length, tag.length))
                                if (tag.startsWith(querySubstring)) {
                                    tagFound = true
                                    break
                                }
                            }
                            if (!tagFound) {
                                results.add(RecipeInSearch("recipe_image_url", name))
                            }
                        }
                    }
                }
                // Update the adapter with the filtered results
                searchAdapter.updateData(results)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors if needed
            }
        })
        return results
    }


    @Suppress ("DEPRECATION")
    private fun setButtonStyle(button: Button, isSelected: Boolean) {
        val layoutParams = button.layoutParams
        layoutParams.height = resources.getDimensionPixelSize(if (isSelected) R.dimen.button_height_selected else R.dimen.button_height_unselected)
        button.layoutParams = layoutParams

        button.setBackgroundResource(if (isSelected) R.drawable.circular_green_background else R.drawable.circular_outline_background)

        // Set background tint color directly

        button.backgroundTintList = ColorStateList.valueOf(resources.getColor(if (isSelected) R.color.hotpot_green else R.color.hotpot_dark_green))


        // Set text color to white for the selected button and black for unselected buttons
        button.setTextColor(resources.getColor(if (isSelected) R.color.white else R.color.hotpot_dark_green))
    }

}
