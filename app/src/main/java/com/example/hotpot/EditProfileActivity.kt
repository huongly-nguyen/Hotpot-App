package com.example.hotpot

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.FragmentTransaction
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

//TODO: fix diet tags
//TODO: style the entire thing to be kind of like other pages
//TODO: Think how to bind this window into AccountActivity, they kind of do the same thing
//TODO: make profile picture circular (cardview, see AccountActivity)
//TODO: Add back button

class EditProfileActivity : AppCompatActivity() {

    private lateinit var profilePictureImageView: ImageView

    private lateinit var editName: EditText
    private lateinit var editEmail: EditText
    private lateinit var editBio: EditText
    private lateinit var saveButton: Button
    private lateinit var updateProfilePictureButton: Button
    private lateinit var updateTagsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_edit_profile)

        // Initialize UI elements
        profilePictureImageView = findViewById(R.id.profilePictureImageView2)
        editName = findViewById(R.id.editName)
        editEmail = findViewById(R.id.editEmail)
        updateProfilePictureButton = findViewById(R.id.updateProfilePictureButton)
        updateTagsButton = findViewById(R.id.updateTagsButton)
        editBio = findViewById(R.id.editBio)
        saveButton = findViewById(R.id.saveButton)

        // Load existing profile data if needed
        loadProfileData()

        // Save button click listener
        saveButton.setOnClickListener {
            // Save updated profile data
            saveProfileData()
        }

        // Update Profile Picture button click listener
        updateProfilePictureButton.setOnClickListener {
            // Open a fragment or an activity to update the profile picture
            openProfilePictureUpdateFragment()
        }

        // Update Tags button click listener
        updateTagsButton.setOnClickListener {
            // Open the DietFilters fragment
            openDietFiltersFragment()
        }
    }

    private fun loadProfileData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val userReference = FirebaseDatabase.getInstance().getReference("Users").child(userId)
            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userProfile = snapshot.getValue(UserProfile::class.java)

                    // Populate the EditText fields with existing user data
                    editName.setText(userProfile?.name)
                    editEmail.setText(userProfile?.email)
                    editBio.setText(userProfile?.bio)
                    // load profile picture out of firebase storage
                    val storageReference = FirebaseStorage.getInstance().getReference("profilePictures")
                        .child(userId)
                    // Convert StorageReference to Uri
                    storageReference.downloadUrl.addOnSuccessListener { uri ->
                        // Use Coil for image loading
                        profilePictureImageView.load(uri)
                    }.addOnFailureListener {
                        // Handle failure to get download URL
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                }
            })
        }
    }


    private fun saveProfileData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            // Get values from EditText fields
            val name = editName.text.toString()
            val email = editEmail.text.toString()
            val bio = editBio.text.toString()

            // Create UserProfile object with updated values
            val updatedProfile = UserProfile(
                uid = userId,
                name = name,
                email = email,
                bio = bio

            )

            // Save the updated profile to Firebase or any other storage mechanism
            saveProfileToFirebase(updatedProfile)

            // Close the activity and update the profile in the previous activity
            finish()

        }
    }
    @Suppress("DEPRECATION")
    private fun openProfilePictureUpdateFragment() {
        // You can replace this with your actual logic to open a fragment or an activity for updating the profile picture
        // For example, you can use Intent to open a new activity
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 0)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == RESULT_OK && data != null) {
            // proceed and check what the selected image was...
            Log.d("EditProfileActivity", "Photo was selected")
            val selectedPhotoUri = data.data

            // upload to firebase storage
            val filename = FirebaseAuth.getInstance().uid.toString()
            val ref = FirebaseStorage.getInstance().getReference("/profilePictures/$filename")
            ref.putFile(selectedPhotoUri!!).addOnSuccessListener {
                Log.d("EditProfileActivity", "Successfully uploaded image: ${it.metadata?.path}")

                ref.downloadUrl.addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    Log.d("EditProfileActivity", "File location: $downloadUrl")

                    // save pfp url to database (uid)
                    val uid = FirebaseAuth.getInstance().uid.toString()
                    val pfpURL = FirebaseDatabase.getInstance().getReference("/Users/$uid/ProfilePictureURL")

                    pfpURL.setValue(uid).addOnSuccessListener {
                        Log.d("EditProfileActivity", "Successfully saved download url to database")

                    }.addOnFailureListener {
                        Log.d("EditProfileActivity", "Failed to save download url to database")
                    }
                }
            }


            profilePictureImageView.setImageURI(selectedPhotoUri)
        }
    }

    private fun openDietFiltersFragment() {
        val dietFiltersFragment = DietFilters()
        dietFiltersFragment.show(supportFragmentManager, "DietFilters")
    }


    private fun saveProfileToFirebase(updatedProfile: UserProfile) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            // Create a map to hold only the fields you want to update
            val updatedFieldsMap = mutableMapOf<String, Any?>()
            if (!updatedProfile.name.isNullOrBlank()) {
                updatedFieldsMap["name"] = updatedProfile.name
            }
            if (!updatedProfile.email.isNullOrBlank()) {
                updatedFieldsMap["email"] = updatedProfile.email
            }
            if (!updatedProfile.bio.isNullOrBlank()) {
                updatedFieldsMap["bio"] = updatedProfile.bio
            }

            // Update only the specified fields in the database
            val userReference = FirebaseDatabase.getInstance().getReference("Users").child(userId)
            userReference.updateChildren(updatedFieldsMap)
                .addOnSuccessListener {
                    Toast.makeText(this@EditProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this@EditProfileActivity, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
        }
    }


}
