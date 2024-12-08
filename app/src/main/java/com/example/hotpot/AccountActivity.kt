package com.example.hotpot

import FridgeFragment
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

@Suppress("DEPRECATION")
class AccountActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_account)

        val profilePicture = findViewById<ImageView>(R.id.profilePictureImageView)
        val backButton = findViewById<ImageButton>(R.id.AccountToolbarReturnButton)
        val settingsButton = findViewById<ImageButton>(R.id.AccountToolbarSettingsButton)
        val fridgeButton = findViewById<Button>(R.id.fridgeButton)
        val favouritesButton = findViewById<Button>(R.id.favouritesButton)
        val allergyButton = findViewById<Button>(R.id.allergyButton)

        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance()

        loadUserData(database, auth, profilePicture)



        profilePicture.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)

            // see onActivityResult for further processing
        }

        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        fridgeButton.setOnClickListener {
            val intent = Intent(this, IngredientsList::class.java)
            startActivity(intent)
        }

        favouritesButton.setOnClickListener {
            val intent = Intent(this, FavoritesActivity::class.java)
            startActivity(intent)
        }

        allergyButton.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, DietFilters())
                .commit()
        }



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
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    true
                }

                R.id.navigation_list -> {
                    val intent = Intent(this, ShoppingListActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    true
                }

                R.id.navigation_search -> {
                    val intent = Intent(this, SearchActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    true
                }

                R.id.navigation_fridge -> {
                    //checkAndRequestPermissions()
                    val intent = Intent(this, IngredientsList::class.java)
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    true
                }

                else -> false
            }
        }
    }

    private fun loadUserData(database: FirebaseDatabase, auth: FirebaseAuth, profilePicture: ImageView) {
        // get user data from database
        val user = auth.currentUser
        val uid = user?.uid

        val ref = database.getReference("Users/$uid")
        ref.get().addOnSuccessListener {
            val profilePictureURL = "profilePictures/$uid"
            val userBio = it.child("bio").value.toString()
            val username = it.child("name").value.toString()

            // set username
            val usernameTextView = findViewById<TextView>(R.id.profileNameTextView)
            val userBioEditText = findViewById<TextView>(R.id.profileTextView)
            usernameTextView.text = username
            userBioEditText.text = userBio

            val storageRef = FirebaseStorage.getInstance().reference
            val fileRef = storageRef.child(profilePictureURL)
            Log.d("AccountActivity", fileRef.toString())
            Log.d("AccountActivity", fileRef.path)

            userBioEditText.setOnClickListener {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Enter Bio")

                // Set up the input
                val input = EditText(this)
                input.inputType = InputType.TYPE_CLASS_TEXT
                builder.setView(input)

                // Set up the buttons
                builder.setPositiveButton("OK") { _, _ ->
                    val userInput = input.text.toString()
                    userBioEditText.text = userInput
                    ref.child("bio").setValue(userInput)
                }
                builder.setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                builder.show()
            }

            fileRef.downloadUrl.addOnSuccessListener { uri ->
                val downloadUrl = uri.toString()

                Glide.with(this).load(downloadUrl).into(profilePicture)
            }.addOnFailureListener {
                Toast.makeText(this, "Error while loading profile picture", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener{
            Toast.makeText(this, "Error while loading user data", Toast.LENGTH_LONG).show()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == RESULT_OK && data != null) {
            // proceed and check what the selected image was...
            Log.d("AccountActivity", "Photo was selected")
            val selectedPhotoUri = data.data

            // upload to firebase storage
            val filename = FirebaseAuth.getInstance().uid.toString()
            val ref = FirebaseStorage.getInstance().getReference("/profilePictures/$filename")
            ref.putFile(selectedPhotoUri!!).addOnSuccessListener {
                Log.d("AccountActivity", "Successfully uploaded image: ${it.metadata?.path}")

                ref.downloadUrl.addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    Log.d("AccountActivity", "File location: $downloadUrl")

                    // save pfp url to database (uid)
                    val uid = FirebaseAuth.getInstance().uid.toString()
                    val pfpURL = FirebaseDatabase.getInstance().getReference("/Users/$uid/ProfilePictureURL")

                    pfpURL.setValue(uid).addOnSuccessListener {
                        Log.d("AccountActivity", "Successfully saved download url to database")

                    }.addOnFailureListener {
                        Log.d("AccountActivity", "Failed to save download url to database")
                    }
                }
            }

            val profilePicture = findViewById<ImageView>(R.id.profilePictureImageView)
            profilePicture.setImageURI(selectedPhotoUri)
        }
    }

}