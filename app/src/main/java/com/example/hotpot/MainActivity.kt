package com.example.hotpot

import FridgeFragment
import FriendStoriesFragment
import RecipeDetailsFragment
import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.UUID

// TODO: Change colours to proper green hotpot colours
@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val GALLERY_REQUEST_CODE = 1
        private const val CAMERA_REQUEST_CODE = 2
    }

    private var selectedRecipe: Recipe? = null
    private var currentUserTags = mutableListOf<String>()

    private lateinit var bottomNavigationView: BottomNavigationView
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main_activity)
        FirebaseApp.initializeApp(this)

        // Fetch current user's tags
        fetchCurrentUserTags()

        // First recipe is null, that's why no recipe details open up
        showRandomMeal()

        // show full recipe name in a alertDialog when TextView is clicked on
        val recipeNameAlertDialog = findViewById<TextView>(R.id.recipe_name)

        recipeNameAlertDialog.setOnClickListener {
            val fullText = recipeNameAlertDialog.text.toString()

            val alertDialog = AlertDialog.Builder(this)
                .setMessage(fullText)
                .setCancelable(true)
                .create()

            alertDialog.show()
        }

        if (savedInstanceState == null) {
            // Load your fragment_friend_stories into the FriendsFrameLayout
            loadFragment(FriendStoriesFragment())
            // set background of fragment to transparent
            findViewById<FrameLayout>(R.id.friendsFrameLayout).setBackgroundColor(0x00000000)

        }

        findViewById<Button>(R.id.random_meal_btn).setOnClickListener {
            showRandomMeal()
        }


        findViewById<Button>(R.id.show_recipe_btn).setOnClickListener {
            selectedRecipe?.let { recipe ->
                openRecipeDetailsFragment(recipe)
            }
        }

        findViewById<ImageButton>(R.id.addRecipeOverlayBtn).setOnClickListener {
            showAddRecipePopupMenu()
        }

        findViewById<ImageButton>(R.id.addToFavoritesBtn).setOnClickListener {
            selectedRecipe?.let { recipe ->
                showAddToFavoritesDialog(recipe)
            }
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_settings -> {
                    val intent = Intent(this, AccountActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }

                R.id.navigation_home -> {
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
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }

                R.id.navigation_fridge -> {
                    //checkAndRequestPermissions()
                    val intent = Intent(this, IngredientsList::class.java)
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }
                // handle other menu item clicks here
                else -> false
            }
        }
    }
    private fun checkAndRequestPermissions() {
        val neededPermissions = listOf(
            Manifest.permission.CAMERA
        ).filterNot { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
        if (neededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, neededPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            showPictureDialog()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allPermissionsGranted) {
                showPictureDialog()
            } else {
                // Check if 'Don't Ask Again' is selected for any permission
                val shouldShowRationale = permissions.any { shouldShowRequestPermissionRationale(it) }
                if (!shouldShowRationale) {
                    // User selected 'Don't Ask Again'. Guide them to app settings.
                    showSettingsDialog()
                } else {
                    showPermissionDeniedExplanation()
                }
            }
        }
    }


    private fun showPermissionDeniedExplanation() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This permission is needed to add photos. Please allow it to proceed.")
            .setPositiveButton("Try Again") { dialog, which ->
                // Try again to request the permission.
                checkAndRequestPermissions()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                // Dismiss the dialog and don't request permission again.
                dialog.dismiss()
            }
            .create()
            .show()
    }


    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This permission is needed to add photos. Please allow it in App Settings.")
            .setPositiveButton("App Settings") { dialog, which ->
                // Open App Settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .create()
            .show()
    }


    private fun showPictureDialog() {
        val items = arrayOf("Select photo from gallery", "Capture photo from camera")
        AlertDialog.Builder(this)
            .setTitle("Add Photo")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> choosePhotoFromGallery()
                    1 -> takePhotoFromCamera()
                }
            }
            .show()
    }


    private fun choosePhotoFromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
    }


    private fun takePhotoFromCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {
                    val selectedImageUri = data?.data
                    selectedImageUri?.let { uri ->
                        uploadImageToFirebase(uri)
                    }
                }
                CAMERA_REQUEST_CODE -> {
                    val photo = data?.extras?.get("data") as? Bitmap
                    photo?.let {
                        uploadImageToFirebase(it)
                    }
                }
            }
        }
    }


    private fun saveImageUrlToFirebaseDatabase(imageUrl: String) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("/images").push()
        val imageInfo = hashMapOf(
            "url" to imageUrl,
            "timestamp" to System.currentTimeMillis()
        )
        databaseRef.setValue(imageInfo)
            .addOnSuccessListener {
                Toast.makeText(this, "Story upload successful saveurltofire", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save image in database saveurltofire", Toast.LENGTH_SHORT).show()
            }
    }


    private fun uploadImageToFirebase(imageUri: Uri) {
        val fileName = UUID.randomUUID().toString()
        val storageRef = FirebaseStorage.getInstance().reference.child("images/$fileName")

        storageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    // Here you can handle the uploaded image URL (e.g., save it in the Firebase database)
                    saveImageUrlToFirebaseDatabase(imageUrl)
                    // Save the image as a story
                    saveImageAsStory(imageUrl)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Upload failed uploadimagetofireuri", Toast.LENGTH_SHORT).show()
            }
    }


    private fun uploadImageToFirebase(bitmap: Bitmap) {
        val fileName = UUID.randomUUID().toString() + ".jpg"
        val storageRef = FirebaseStorage.getInstance().getReference("images/$fileName")
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        storageRef.putBytes(data)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    saveImageUrlToFirebaseDatabase(imageUrl)
                    saveImageAsStory(imageUrl)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Upload failed uploadimagetofirebit", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveImageAsStory(imageUrl: String) {
        // Assume that the imageUrl is the URL of the image in Firebase Storage that you have already uploaded.
        // Now you want to save this as a story to your user profile so your friends can see it.

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            // Create a unique key for the story (or use a specific one if you want to overwrite an existing story)
            val storyKey = FirebaseDatabase.getInstance().reference.child("Users").child(it).child("UserStories").push().key
            storyKey?.let { key ->
                // Create a map with the story details you want to save
                val storyDetails = hashMapOf(
                    "imageUrl" to imageUrl,
                    "timestamp" to ServerValue.TIMESTAMP, // Use Firebase ServerValue.TIMESTAMP for the server to fill in the timestamp
                    // Add other details like title or description if you have them
                    "storyTitle" to "A title for the story", // Replace with actual title
                    "storyDescription" to "A description for the story" // Replace with actual description
                )

                // Save the story details to the "UserStories" node under the current user's profile
                FirebaseDatabase.getInstance().reference
                    .child("Users")
                    .child(it)
                    .child("UserStories")
                    .child(key)
                    .setValue(storyDetails)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Story saved successfully!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save story: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        } ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchCurrentUserTags() {
        // Fetch current user's tags
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            val currentUserTagsReference = FirebaseDatabase.getInstance().reference.child("Users").child(it).child("Tags")

            currentUserTagsReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Clear the existing tags
                    currentUserTags.clear()

                    // Iterate through the children and add each tag to the list
                    for (tagSnapshot in dataSnapshot.children) {
                        val tagValue = tagSnapshot.getValue(String::class.java)
                        tagValue?.let {
                            currentUserTags.add(it)
                        }
                    }

                    // Now, currentUserTags list contains all the tag values for the current user
                    // You can use this list as needed in your application
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                    Log.e("Firebase", "Error fetching user tags: ${error.message}")
                }
            })
        } ?: run {
            // Handle case where userId is null
            Log.e("Firebase", "Error retrieving user ID.")
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val fragmentManager: FragmentManager = supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()

        // Replace the contents of FriendsFrameLayout with the provided fragment
        fragmentTransaction.replace(R.id.friendsFrameLayout, fragment)

        // Add the transaction to the back stack (optional)
        fragmentTransaction.addToBackStack(null)

        // Commit the transaction
        fragmentTransaction.commit()
    }

    private fun openRecipeDetailsFragment(recipe: Recipe) {
        val recipeDetailsFragment = RecipeDetailsFragment()
        val bundle = Bundle()
        bundle.putSerializable("RECIPE_DATA", recipe)
        recipeDetailsFragment.arguments = bundle
        recipeDetailsFragment.show(supportFragmentManager, recipeDetailsFragment.tag)
    }

    private fun showAddToFavoritesDialog(recipe: Recipe) {
        val options = arrayOf("Füge zu Favoriten hinzu")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Optionen auswählen")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        // Füge zu Favoriten hinzu ausgewählt
                        addToFavorites(recipe, this@MainActivity)
                    }
                }
            }
        val dialog = builder.create()
        dialog.show()
    }

    public fun setCurrentRecipeAsCurrentUserStory() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        userId?.let {
            val userStoryReference = FirebaseDatabase.getInstance().reference
                .child("Users")
                .child(it)
                .child("UserStory")
                .child("0") // Change here to save the recipe under a node with ID 0

            // Delete existing data under "UserStory/0"
            userStoryReference.removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Save the recipe under "UserStory/0"
                    userStoryReference.setValue(selectedRecipe).addOnSuccessListener {
                        Toast.makeText(this@MainActivity, "${selectedRecipe?.name} als derzeitige UserStory eingestellt", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(this@MainActivity, "Error saving UserStory: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Error deleting existing data: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } ?: run {
            Toast.makeText(this@MainActivity, "Error retrieving user ID.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showRandomMeal() {
        val databaseReference = FirebaseDatabase.getInstance().reference.child("Recipes")

        // Liste zum Sammeln der Rezepte
        val recipes: MutableList<Recipe> = mutableListOf()

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (recipeSnapshot in dataSnapshot.children) {
                    val name = recipeSnapshot.child("name").getValue(String::class.java)
                    val description = recipeSnapshot.child("description").getValue(String::class.java)
                    val instructions = recipeSnapshot.child("instructions").getValue(String::class.java)
                    val details = recipeSnapshot.child("details").getValue(String::class.java)
                    val tags = recipeSnapshot.child("tags").getValue<List<String>>()
                    val creditUserID = recipeSnapshot.child("credit").getValue(String::class.java).toString()

                    Log.d("firebase", "creditsUserID: $creditUserID")

                    val ingredientsSnapshot = recipeSnapshot.child("ingredients")
                    val ingredientsMap = mutableMapOf<String, Any>()

                    for (ingredientSnapshot in ingredientsSnapshot.children) {
                        val ingredientName = ingredientSnapshot.key
                        val ingredientDetails = ingredientSnapshot.getValue<Map<String, Any>>()
                        ingredientsMap[ingredientName!!] = ingredientDetails!!
                    }

                    // Füge nur nicht-null Daten zur Liste hinzu
                    if (name != null && description != null && instructions != null && details != null && tags != null) {
                        val recipe = Recipe(name, "", description, ingredientsMap, instructions, details, tags, creditUserID)

                        // check if the recipe has a tag that the currentUser is allergic to etc.
                        if (recipe.tags.intersect(currentUserTags).isEmpty()) {
                            recipes.add(recipe)
                        }
                    }
                }
                // Wenn Rezepte vorhanden sind
                if (recipes.isNotEmpty()) {
                    // Wähle ein zufälliges Rezept aus
                    selectedRecipe = recipes.random()

                    // continue randomizing till a recipe with no common tags come up and not the same recipe
                    if (selectedRecipe?.tags?.intersect(currentUserTags)?.isEmpty() == false ||
                        selectedRecipe?.name == findViewById<TextView>(R.id.recipe_name).text.toString()
                    ) {
                        showRandomMeal()
                    } else {
                        // Suche nach dem Credit für das ausgewählte Rezept
                        searchCreditForSelectedRecipe(selectedRecipe)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Fehler beim Lesen der Datenbank: ${error.message}")
            }
        })
    }

    private fun searchCreditForSelectedRecipe(selectedRecipe: Recipe?) {
        selectedRecipe?.let {
            val creditUserID = it.credits

            Log.d("searchCredit", "CreditUsERID: $creditUserID")

            Log.d("Firebase", "Credits: $creditUserID")

            val usersReference = FirebaseDatabase.getInstance().reference.child("Users").child(
                it.credits.toString()
            ).child("name")

            usersReference.addListenerForSingleValueEvent(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(userNameSnapshot: DataSnapshot) {
                    val userName = userNameSnapshot.getValue(String::class.java) ?: "Unknown User"

                    Log.d("Firebase", "Username: $userName")

                    // Setze die Daten für das ausgewählte Rezept
                    findViewById<TextView>(R.id.recipe_name).text = it.name
                    findViewById<TextView>(R.id.recipe_info).text = it.details
                    findViewById<TextView>(R.id.recipe_credit).text = ("by $userName")

                    // Lade das Bild nur für das ausgewählte Rezept
                    val foodPictureImageView: ImageView = findViewById(R.id.food_picture)
                    loadImageFromFirebaseStorage(it.name, foodPictureImageView)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Fehler beim Lesen der Benutzerdaten: ${error.message}")
                }
            })
        }
    }



    // Funktion zum Laden des Bildes von der Storage und Anzeigen im ImageView
    private fun loadImageFromFirebaseStorage(imageFileName: String?, imageView: ImageView) {
        if (imageFileName.isNullOrBlank()) {
            return
        }
        val storageReference = FirebaseStorage.getInstance().reference
        val sanitizedImageFileName = sanitizeRecipeNameForStorage(imageFileName)
        val recipePictureReference = storageReference.child("recipes").child(sanitizedImageFileName)

        Log.d("FirebaseStorage", "Versuche Bild zu laden: $sanitizedImageFileName")

        // Holen Sie sich die Download-URL für das Bild
        recipePictureReference.downloadUrl.addOnSuccessListener { uri ->
            val imageUrl = uri.toString()

            // Verwende Glide, um das Bild von der URL in das ImageView zu laden
            Glide.with(this@MainActivity)
                .load(imageUrl).override(375,250)
                .into(imageView)

            Log.d("FirebaseStorage", "Bild erfolgreich geladen: $imageUrl")
        }.addOnFailureListener { exception ->
            // Handle den Fehler beim Abrufen der Download-URL
            Log.e("FirebaseStorage", "Fehler beim Laden des Bildes: ${exception.message}")
        }
    }


    private fun sanitizeRecipeNameForStorage(recipeName: String): String {
        return recipeName.replace(" ", "")
    }
    override fun onResume() {
    super.onResume()
    bottomNavigationView.selectedItemId = R.id.navigation_home // Replace 'navigation_home' with the actual ID of the home item
    }

    /**
     * prevents returning to login/signUp screen
     */
    @SuppressLint("InflateParams")
    private fun showAddRecipePopupMenu() {
        val anchorView = findViewById<ImageButton>(R.id.addRecipeOverlayBtn)

        // Erstelle die PopupWindow-Instanz
        val popupWindow = PopupWindow(
            layoutInflater.inflate(R.layout.fragment_add_recipe, null),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Finde die Buttons im Popup-Layout
        val addRecipe = popupWindow.contentView.findViewById<Button>(R.id.addRecipeBtn)
        val closeAddRecipeOverlay = popupWindow.contentView.findViewById<Button>(R.id.closeAddRecipeBtn)

        // Setze die Klick-Listener für die Buttons im Popup
        addRecipe.setOnClickListener {
            Log.d("PopupMenu", "Menu Item 1 clicked")
            // Hier kannst du die Aktion für "Menu Item 1" ausführen
            popupWindow.dismiss()

            // Navigate to AddRecipe
            val intent = Intent(this, AddRecipe::class.java)
            startActivity(intent)
        }

        closeAddRecipeOverlay.setOnClickListener {
            Log.d("PopupMenu", "Menu Item 2 clicked")
            // Hier kannst du die Aktion für "Menu Item 2" ausführen
            popupWindow.dismiss()
        }

        // Öffne das Popup-Menü an der Position des Anker-Views
        popupWindow.showAsDropDown(anchorView)
    }
}
private fun addToFavorites(selectedRecipe: Recipe, context: Context) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    userId?.let {
        val favoritesReference =
            FirebaseDatabase.getInstance().reference.child("Users").child(it).child("Favorites")

        // check if recipe is already in Favorites
        favoritesReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val recipeExists = dataSnapshot.children.any { favoriteSnapshot ->
                    val name = favoriteSnapshot.child("name").getValue(String::class.java)
                    name != null && name == selectedRecipe.name
                }

                if (recipeExists) {
                    // Das Rezept existiert bereits in den Favoriten
                    Log.d("Firebase", "Rezept existiert bereits in den Favoriten.")
                    showToast("Rezept ist schon favorisiert", context)
                } else {
                    // Das Rezept existiert noch nicht in den Favoriten, füge es hinzu
                    val randomKey = favoritesReference.push().key

                    if (randomKey != null) {
                        favoritesReference.child(randomKey).setValue(selectedRecipe)
                        // Hier kannst du weitere Eigenschaften des Rezepts hinzufügen
                        Log.d("Firebase", "Rezept erfolgreich zu Favoriten hinzugefügt.")
                        showToast("Rezept zu Favoriten hinzugefügt!", context)
                    } else {
                        // Falls randomKey null ist
                        Log.e(
                            "Firebase",
                            "Fehler beim Erstellen eines random Keys für das Rezept."
                        )
                        showToast("Fehler beim Hinzufügen zu Favoriten", context)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Fehler beim Lesen der Datenbank: ${error.message}")
                showToast("Fehler beim Hinzufügen zu Favoriten", context)
            }
        })
    } ?: run {
        // Falls userId null ist
        Log.e("Firebase", "Fehler beim Abrufen der Benutzer-ID.")
        showToast("Fehler beim Hinzufügen zu Favoriten", context)
    }
}
    private fun showToast(message: String, context: Context) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }


