package com.example.hotpot

import FridgeFragment
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView


class IngredientsList : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var meatBox : CardView
    private lateinit var vegeBox: CardView
    private lateinit var nutsBox: CardView
    private lateinit var herbsBox: CardView
    private lateinit var milkBox: CardView
    private lateinit var riceBox: CardView
    private lateinit var fruitsBox: CardView
    private lateinit var othersBox: CardView
    private lateinit var beverageBox: CardView
    private lateinit var pastaBox: CardView

    private lateinit var fridgeBtn : ImageButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ingredients_list)

        // Assuming you have a SearchView with the id "searchView" in your layout
        val searchView: SearchView = findViewById(R.id.searchView)
        val homeButton : ImageButton = findViewById(R.id.ingredientsList_HomeButton)

        meatBox = findViewById(R.id.meat_box);
        vegeBox = findViewById(R.id.vege_box)
        nutsBox = findViewById(R.id.nuts_box)
        herbsBox = findViewById(R.id.herbs_box)
        milkBox = findViewById(R.id.milk_box)
        pastaBox = findViewById(R.id.pasta_box)
        riceBox = findViewById(R.id.rice_box)
        beverageBox = findViewById(R.id.beverage_box)
        fruitsBox = findViewById(R.id.fruits_box)
        othersBox = findViewById(R.id.others_box)

        homeButton.setOnClickListener {
            finish();
        }

        fridgeBtn = findViewById(R.id.fridgeButton)
        fridgeBtn.setOnClickListener {
            val ingredientListLayout = findViewById<LinearLayout>(R.id.ingredientListLayout)
            ingredientListLayout.visibility = View.GONE

            // Öffne FridgeFragment und übergebe die ausgewählte Kategorie
            val fridgeFragment = FridgeFragment()
            val args = Bundle()
            args.putString("selectedCategory", "Meat")
            fridgeFragment.arguments = args

            // Füge das Fragment dem Fragmentmanager hinzu
            val transaction = this.supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fridgeFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        meatBox.setOnClickListener {
            val ingredientListLayout = findViewById<LinearLayout>(R.id.ingredientListLayout)
            ingredientListLayout.visibility = View.GONE

            val fridgeContentFragment = FridgeContentFragment.newInstance("Meat")
            val transaction = this.supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fridgeContentFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        vegeBox.setOnClickListener {
            val ingredientListLayout = findViewById<LinearLayout>(R.id.ingredientListLayout)
            ingredientListLayout.visibility = View.GONE

            val fridgeContentFragment = FridgeContentFragment.newInstance("Vegetables")
            val transaction = this.supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fridgeContentFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        nutsBox.setOnClickListener {
            val ingredientListLayout = findViewById<LinearLayout>(R.id.ingredientListLayout)
            ingredientListLayout.visibility = View.GONE

            val fridgeContentFragment = FridgeContentFragment.newInstance("Nuts")
            val transaction = this.supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fridgeContentFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        herbsBox.setOnClickListener {
            val ingredientListLayout = findViewById<LinearLayout>(R.id.ingredientListLayout)
            ingredientListLayout.visibility = View.GONE

            val fridgeContentFragment = FridgeContentFragment.newInstance("Herbs")
            val transaction = this.supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fridgeContentFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        milkBox.setOnClickListener {
            val ingredientListLayout = findViewById<LinearLayout>(R.id.ingredientListLayout)
            ingredientListLayout.visibility = View.GONE

            val fridgeContentFragment = FridgeContentFragment.newInstance("Diary")
            val transaction = this.supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fridgeContentFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        riceBox.setOnClickListener {
            val ingredientListLayout = findViewById<LinearLayout>(R.id.ingredientListLayout)
            ingredientListLayout.visibility = View.GONE

            val fridgeContentFragment = FridgeContentFragment.newInstance("Rice")
            val transaction = this.supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fridgeContentFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        fruitsBox.setOnClickListener {
            val ingredientListLayout = findViewById<LinearLayout>(R.id.ingredientListLayout)
            ingredientListLayout.visibility = View.GONE

            val fridgeContentFragment = FridgeContentFragment.newInstance("Fruits")
            val transaction = this.supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fridgeContentFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        othersBox.setOnClickListener {
            val ingredientListLayout = findViewById<LinearLayout>(R.id.ingredientListLayout)
            ingredientListLayout.visibility = View.GONE

            val fridgeContentFragment = FridgeContentFragment.newInstance("Others")
            val transaction = this.supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fridgeContentFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        pastaBox.setOnClickListener {
            val ingredientListLayout = findViewById<LinearLayout>(R.id.ingredientListLayout)
            ingredientListLayout.visibility = View.GONE

            val fridgeContentFragment = FridgeContentFragment.newInstance("Pasta")
            val transaction = this.supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fridgeContentFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        beverageBox.setOnClickListener {
            val ingredientListLayout = findViewById<LinearLayout>(R.id.ingredientListLayout)
            ingredientListLayout.visibility = View.GONE

            val fridgeContentFragment = FridgeContentFragment.newInstance("Beverages")
            val transaction = this.supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fridgeContentFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        customizeSearchView(searchView)

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_fridge
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
                    finish()
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
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    true
                }

                R.id.navigation_fridge -> {
                    true;
                }
                else -> false
            }
        }
    }

    private fun customizeSearchView(searchView: SearchView) {
        // Set color for search text
        val searchEditText: EditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text)
        searchEditText.setTextColor(ContextCompat.getColor(this, R.color.hotpot_light_green))

        // Set hint text color
        searchEditText.setHintTextColor(ContextCompat.getColor(this, R.color.hint_search_color))

        // Set magnifying glass icon color
        val searchIcon: ImageView = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon)
        searchIcon.setColorFilter(ContextCompat.getColor(this, R.color.hotpot_light_green))
    }
}
