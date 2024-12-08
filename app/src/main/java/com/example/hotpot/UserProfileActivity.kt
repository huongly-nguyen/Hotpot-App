package com.example.hotpot

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class UserProfileActivity : AppCompatActivity() {

    private lateinit var profilePicture: ImageView
    private lateinit var usernameTextView: TextView
    private lateinit var bioTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var userRecipesRecyclerView: RecyclerView
    private lateinit var friendButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var editProfileButton: ImageButton

    private var isFriend: Boolean = false

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_user_profile)

        // Initialize UI elements
        usernameTextView = findViewById(R.id.usernameTextView)
        bioTextView = findViewById(R.id.bioTextView)
        profileImageView = findViewById(R.id.profileImageView)
        userRecipesRecyclerView = findViewById(R.id.userRecipesRecyclerView)
        friendButton = findViewById(R.id.friendButton)
        backButton = findViewById(R.id.backButtonUserProfile)
        editProfileButton = findViewById(R.id.editProfileButton)

        // Retrieve user data from intent
        val userId = intent.getStringExtra("userId").toString()

        loadUserRecipes(userId)
        loadUserData(userId)

        // Check if the current user is viewing their own profile
        val loggedInUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (loggedInUserId == userId) {
            // Hide the back button and friend button when the logged-in user is viewing their own profile
            backButton.visibility = View.GONE
            friendButton.visibility = View.GONE
        } else {
            // Hide the edit profile button when the viewed user is not the logged-in user
            editProfileButton.visibility = View.GONE
        }

        backButton.setOnClickListener {
            finish()
        }

        editProfileButton.setOnClickListener {
            // Check if the current user is viewing their own profile
            if (loggedInUserId == userId) {
                // Navigate to the EditProfileActivity
                val intent = Intent(this, EditProfileActivity::class.java)
                startActivity(intent)
            } else {
                // Inform the user they can only edit their own profile
                Toast.makeText(this, "You can only edit your own profile.", Toast.LENGTH_SHORT).show()
            }
        }


        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_search
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
                    val intent = Intent(this, SearchActivity::class.java)
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }
    }

    private fun loadUserRecipes(userId: String) {
        val userRecipesReference = FirebaseDatabase.getInstance().getReference("UserRecipes").child(userId)
        userRecipesReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userRecipes = mutableListOf<Recipe>()

                for (recipeSnapshot in snapshot.children) {
                    val recipeId = recipeSnapshot.key ?: ""
                    val recipeReference = FirebaseDatabase.getInstance().getReference("Recipes").child(recipeId)
                    recipeReference.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(recipeDataSnapshot: DataSnapshot) {
                            val recipe = recipeDataSnapshot.getValue(Recipe::class.java)
                            if (recipe != null) {
                                userRecipes.add(recipe)
                            }

                            // Check if this is the last recipe before updating the adapter
                            if (userRecipes.size == snapshot.childrenCount.toInt()) {
                                updateRecipeAdapter(userRecipes)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle database error
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }


    private fun updateRecipeAdapter(userRecipes: List<Recipe>) {
        val userRecipeAdapter = UserRecipeAdapter(userRecipes, FirebaseStorage.getInstance().reference, FirebaseDatabase.getInstance().reference)
        userRecipesRecyclerView.adapter = userRecipeAdapter
        userRecipesRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun loadUserData(userId: String) {
        val userData = FirebaseDatabase.getInstance().getReference("Users").child(userId)

        val loggedInUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (loggedInUserId != null) {
            // Determine friendship status
            determineFriendshipStatus(loggedInUserId, userId)
        } else {
            // User is not logged in, handle accordingly
        }

        userData.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userProfile = snapshot.getValue(UserProfile::class.java)
                if (userProfile != null) {
                    usernameTextView.text = userProfile.name
                    bioTextView.text = userProfile.bio

                    // Download profile picture from Firebase Storage
                    val storageReference = FirebaseStorage.getInstance().getReference("profilePictures").child(userId)
                    storageReference.downloadUrl.addOnSuccessListener { uri ->
                        // Load the downloaded image into Glide
                        Glide.with(this@UserProfileActivity)
                            .load(uri)
                            .into(profileImageView)
                    }.addOnFailureListener { exception ->
                        // Handle the failure to download the image
                        Log.e("Firebase", "Error fetching user profile image URL", exception)
                    }
                } else {
                    // Handle the case where userProfile is null
                    Log.e("Firebase", "UserProfile is null")
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
                // You might want to log or display a message indicating the error
            }
        })
    }


    private fun determineFriendshipStatus(loggedInUserId: String, viewedUserId: String) {
        val friendsReference = FirebaseDatabase.getInstance().getReference("Users").child(loggedInUserId).child("Friends")
        friendsReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isFriend = false

                for (friendSnapshot in snapshot.children) {
                    val friendUserId = friendSnapshot.value.toString()
                    if (friendUserId == viewedUserId) {
                        isFriend = true
                        Log.d("Friend", "Friend found")
                        break // No need to continue checking if already found
                    }
                }
                Log.d("Friend", "Is friend: $isFriend")
                setFriendButtonText(isFriend)

                // Handle Friend Button Click

                friendButton.setOnClickListener {
                    // Retrieve user IDs as needed
                    val loggedInUserId = loggedInUserId
                    val viewedUserId = viewedUserId

                    // Determine friendship status and handle click
                    determineFriendshipStatusAndHandleClick(loggedInUserId, viewedUserId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }

    private fun determineFriendshipStatusAndHandleClick(loggedInUserId: String, viewedUserId: String) {
        val friendsReference = FirebaseDatabase.getInstance().getReference("Users").child(loggedInUserId).child("Friends")
        friendsReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var isFriend = false

                for (friendSnapshot in snapshot.children) {
                    val friendUserId = friendSnapshot.value.toString()
                    if (friendUserId == viewedUserId) {
                        isFriend = true
                        Log.d("Friend", "Friend found")
                        break // No need to continue checking if already found
                    }
                }

                Log.d("Friend", "Is friend: $isFriend")

                // Handle Friend Button Click
                if (isFriend) {
                    // Already friends, show confirmation dialog before unfriending
                    showUnfriendConfirmationDialog(loggedInUserId, viewedUserId)
                } else {
                    // Not friends, add as friend
                    addFriend(loggedInUserId, viewedUserId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }

    private fun showUnfriendConfirmationDialog(loggedInUserId: String, viewedUserId: String ) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Confirm Unfriend")
        alertDialogBuilder.setMessage("Are you sure you want to unfriend this person?")
        alertDialogBuilder.setPositiveButton("Yes") { _, _ ->
            // User clicked "Yes," unfriend the person
            unfriend(loggedInUserId, viewedUserId)
        }
        alertDialogBuilder.setNegativeButton("No") { dialog, _ ->
            // User clicked "No," dismiss the dialog
            dialog.dismiss()
        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }


    private fun addFriend(loggedInUserId: String, viewedUserId: String) {
        // Add the viewed user as a friend for the logged-in user
        val loggedInUserFriendsReference = FirebaseDatabase.getInstance().getReference("Users").child(loggedInUserId).child("Friends")
        val loggedInUserFriendKey = loggedInUserFriendsReference.push().key
        Log.d("Friend", "LoggedInUserFriendKey: $loggedInUserFriendKey")
        loggedInUserFriendsReference.child(loggedInUserFriendKey!!).setValue(viewedUserId)

        // Add the logged-in user as a friend for the viewed user (if needed)
        val viewedUserFriendsReference = FirebaseDatabase.getInstance().getReference("Users").child(viewedUserId).child("Friends")
        val viewedUserFriendKey = viewedUserFriendsReference.push().key
        Log.d("Friend", "ViewedUserFriendKey: $viewedUserFriendKey")
        viewedUserFriendsReference.child(viewedUserFriendKey!!).setValue(loggedInUserId)

        // Update the UI
        setFriendButtonText(true)
    }




    private fun getFriendIndex(friendsReference: DatabaseReference, friendUserId: String, callback: (String) -> Unit) {
        friendsReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var index = ""
                for (childSnapshot in snapshot.children) {
                    if (childSnapshot.getValue(String::class.java) == friendUserId) {
                        index = childSnapshot.key ?: ""
                        // You can break out of the loop since the friend is found
                        break
                    }
                }
                callback(index)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
                callback("")
            }
        })
    }

    private fun unfriend(loggedInUserId: String, viewedUserId: String) {
        val loggedInUserFriendsReference = FirebaseDatabase.getInstance().getReference("Users").child(loggedInUserId).child("Friends")
        getFriendIndex(loggedInUserFriendsReference, viewedUserId) { index ->
            if (index.isNotEmpty()) {
                loggedInUserFriendsReference.child(index).removeValue()
                setFriendButtonText(false)
            }
        }

        val viewedUserFriendsReference = FirebaseDatabase.getInstance().getReference("Users").child(viewedUserId).child("Friends")
        getFriendIndex(viewedUserFriendsReference, loggedInUserId) { index ->
            if (index.isNotEmpty()) {
                viewedUserFriendsReference.child(index).removeValue()
            }
        }
    }



    private fun setFriendButtonText(isFriend: Boolean) {
        if (isFriend) {
            friendButton.text = getString(R.string.friends_button_text)
        } else {
            friendButton.text = getString(R.string.add_friend_button_text)
        }
    }
}

