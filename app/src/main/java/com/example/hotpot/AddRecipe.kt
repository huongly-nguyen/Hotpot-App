package com.example.hotpot

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import androidx.core.widget.NestedScrollView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.skydoves.expandablelayout.ExpandableLayout

class AddRecipe : AppCompatActivity() {
    private lateinit var addRecipeStepsBtn: Button
    private lateinit var recipeStepEditText: EditText
    private lateinit var scrollViewParent: NestedScrollView
    private lateinit var dietaryFilterContainer: LinearLayout
    private lateinit var addIngredientButton : Button
    private val ingredientList = mutableListOf<String>()
    private lateinit var imageUri : Uri
    private val PICK_IMAGE_REQUEST = 1

    private lateinit var cookingTimeButton : ImageButton
    private lateinit var cookingTimeTextView: TextView

    private lateinit var difficultyCountButton : ImageButton
    private lateinit var difficultyCountTextView: TextView

    private lateinit var ratingCountButton: ImageButton
    private lateinit var ratingCountTextView: TextView

    private lateinit var descriptionTextView: TextView

    private lateinit var storageRef : StorageReference

    val dietaryFilterOptions = arrayOf("vegan", "gluten", "halal", "keto",
        "kosher", "lactose", "paleo", "peanut", "pescatarian", "shellfish", "soy", "vegetarian")
    private var selectedFilters = mutableListOf<String>()

    @SuppressLint("ClickableViewAccessibility", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_recipe)

        addRecipeStepsBtn = findViewById(R.id.addRecipeSteps_Btn)
        recipeStepEditText = findViewById(R.id.stepsEditText)
        scrollViewParent = findViewById(R.id.nestedScrollView)
        addIngredientButton = findViewById(R.id.addIngredient_btn)

        cookingTimeButton = findViewById(R.id.cookingTimeButton)
        cookingTimeTextView = findViewById(R.id.cookingTimeTextView)
        difficultyCountButton = findViewById(R.id.difficultyCountButton)
        difficultyCountTextView = findViewById(R.id.difficultyCountTextView)
        ratingCountButton = findViewById(R.id.ratingCountButton)
        ratingCountTextView = findViewById(R.id.ratingCount)

        descriptionTextView = findViewById(R.id.descriptionTextView)
        val recipeImageView: ImageView = findViewById(R.id.recipeImageView)

        val recipeNameEditText: EditText = findViewById(R.id.AddRecipe_recipeName)
        val recipeStepEditText: EditText = findViewById(R.id.stepsEditText)

        //Firebase storage reference
        val storage = FirebaseStorage.getInstance()
        storageRef = storage.reference.child("recipes")

        //difficulties
        val difficultyOptions = arrayOf("Easy", "Medium", "Hard")

        //rating
        val ratingOptions1 = Array(5) { (it + 1).toString() }
        val ratingOptions2 = Array(10) { it.toString() }

        val expandableLayout: ExpandableLayout = findViewById(R.id.expandable)

        recipeNameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                recipeNameEditText.setText("")
            }
        }

        val closeBtn = findViewById<ImageButton>(R.id.addRecipe_CloseBtn)
        closeBtn.setOnClickListener {
            finish()
        }

        val cancelBtn = findViewById<Button>(R.id.AddRecipe_CancelBtn)
        cancelBtn.setOnClickListener {
            finish()
        }

        val saveRecipeBtn = findViewById<Button>(R.id.AddRecipe_SaveBtn)
        saveRecipeBtn.setOnClickListener {
            val recipeName = recipeNameEditText.text.toString().trim()
            val description = descriptionTextView.text.toString().trim()
            val difficulty = difficultyCountTextView.text.toString().trim()
            val cookingTime = cookingTimeTextView.text.toString().trim()
            val rating = ratingCountTextView.text.toString().trim()


            // Check if any of the required fields is null or empty
            if (recipeName.isEmpty() || description.isEmpty() || difficulty.isEmpty() ||
                cookingTime.isEmpty() || rating.isEmpty() || ingredientList.isEmpty() ||
                recipeImageView == null) {

                Toast.makeText(this, "Bitte fülle alle Felder aus", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    // Dein Code zum Speichern des Rezepts
                    uploadNewRecipe()
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this,
                        "Ein Fehler ist aufgetreten. Bitte versuche es erneut.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        }

        // add image of recipe
        recipeImageView.setOnClickListener {
            chooseImage()
        }

        // add ingredient
        addIngredientButton.setOnClickListener {
            showAddIngredientDialog()
        }

        addRecipeStepsBtn.setOnClickListener {
            // Hier wird der Button in ein EditText umgewandelt
            addRecipeStepsBtn.visibility = View.GONE
            recipeStepEditText.visibility = View.VISIBLE
        }

        expandableLayout.setOnClickListener {
            if (expandableLayout.isExpanded) {
                // Wenn es ausgefahren ist, dann einfahren
                expandableLayout.collapse()
            } else {
                // Wenn es eingefahren ist, dann ausfahren
                expandableLayout.expand()
            }
        }

        descriptionTextView.setOnClickListener {
            showDescriptionInputDialog()
        }

        dietaryFilterContainer = findViewById(R.id.dietaryFilterContainer)

        val addDietaryFilterBtn: Button = findViewById(R.id.dietaryFilter_Btn)
        addDietaryFilterBtn.setOnClickListener {
                    showDietaryFilters();
                }

        scrollViewParent.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // Überprüfe, ob der Klick außerhalb der EditTexts war
                if (recipeNameEditText.isFocused || recipeStepEditText.isFocused) {
                    val outRectName = android.graphics.Rect()
                    val outRectStep = android.graphics.Rect()

                    recipeNameEditText.getGlobalVisibleRect(outRectName)
                    recipeStepEditText.getGlobalVisibleRect(outRectStep)

                    val padding = 16

                    outRectName.set(outRectName.left + padding, outRectName.top + padding, outRectName.right - padding, outRectName.bottom - padding)
                    outRectStep.set(outRectStep.left + padding, outRectStep.top + padding, outRectStep.right - padding, outRectStep.bottom - padding)

                    if (!outRectName.contains(event.rawX.toInt(), event.rawY.toInt()) &&
                        !outRectStep.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        // Der Klick war außerhalb der EditTexts, entferne den Fokus
                        recipeNameEditText.clearFocus()
                        recipeStepEditText.clearFocus()

                        // Verberge die Tastatur
                        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(scrollViewParent.windowToken, 0)
                    }
                }
            }
            false
        }

        ratingCountButton.setOnClickListener {
            showRatingDialog("Select Rating", ratingOptions1, ratingOptions2, ratingCountTextView)
        }

        difficultyCountButton.setOnClickListener {
            showDifficultyDropdown("Select Difficulty", difficultyOptions, difficultyCountTextView)
        }

        cookingTimeButton.setOnClickListener {
            showTimePickerDialog("Cooking Time", cookingTimeTextView)
        }
    }

    private fun showDescriptionInputDialog() {
        val recipeNameEditText: EditText = findViewById(R.id.AddRecipe_recipeName)

        // Deaktiviere das EditText, um Fokus zu verhindern
        recipeNameEditText.isEnabled = false

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Description")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)
        input.requestFocus()

        builder.setPositiveButton("OK") { _, _ ->
            val description = input.text.toString()
            descriptionTextView.text = description
            descriptionTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)

            recipeNameEditText.isEnabled = true
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            recipeNameEditText.isEnabled = true
            dialog.cancel()
        }

        builder.show()
    }

    private fun showTimePickerDialog(title: String, targetTextView: TextView) {
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val formattedTime = formatTime(hourOfDay, minute)
                targetTextView.text = formattedTime
            },
            0,
            0,
            true
        )
        timePickerDialog.setTitle(title)
        timePickerDialog.show()
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val hoursString = if (hour > 0) "${hour} hrs " else ""
        val minutesString = if (minute > 0) "${minute} min" else ""

        return if (hoursString.isNotEmpty() && minutesString.isNotEmpty()) {
            "$hoursString $minutesString"
        } else {
            "$hoursString$minutesString"
        }
    }

    private fun showDifficultyDropdown(title: String, options: Array<String>, targetTextView: TextView) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setItems(options) { _, which ->
            val selectedOption = options[which]
            targetTextView.text = selectedOption
        }
        builder.show()
    }

    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            imageUri = data.data!!  // Das Uri-Objekt wird mit dem ausgewählten Bild aktualisiert

            // Update the ImageView with the selected image
            val recipeImageView: ImageView = findViewById(R.id.recipeImageView)
            recipeImageView.setImageURI(imageUri)
        }
    }

    private fun uploadImageToStorage(imageUri: Uri, imageName: String) {
        val ImageName = sanitizeRecipeNameForStorage(imageName)
        val fireBaseStorage = FirebaseStorage.getInstance().reference
        storageRef = fireBaseStorage.child("recipes").child(ImageName)

        val uploadTask = storageRef.putFile(imageUri)

        uploadTask.addOnSuccessListener { taskSnapshot ->
            // Image uploaded successfully
            val downloadUrl = taskSnapshot.metadata?.reference?.downloadUrl
            // Save the download URL to your recipe data or perform other actions
        }.addOnFailureListener { exception ->
            // Handle unsuccessful uploads
            Log.e("Firebase", "Error uploading image: ${exception.message}")
        }
    }


    private fun showRatingDialog(title: String, options1: Array<String>, options2: Array<String>, targetTextView: TextView) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.HORIZONTAL

        val numberPicker1 = NumberPicker(this)
        numberPicker1.minValue = 0
        numberPicker1.maxValue = options1.size - 1
        numberPicker1.displayedValues = options1

        val numberPicker2 = NumberPicker(this)
        numberPicker2.minValue = 0

        // Wenn die erste Zahl "5" ist, setze die maximale Zahl im zweiten NumberPicker auf "0"
        if (options1[numberPicker1.value] == "5") {
            numberPicker2.maxValue = 0
        } else {
            numberPicker2.maxValue = options2.size - 1
        }

        numberPicker2.displayedValues = options2

        layout.addView(numberPicker1)
        layout.addView(numberPicker2)

        builder.setView(layout)

        builder.setPositiveButton("OK") { _, _ ->
            val selectedValue1 = options1[numberPicker1.value]
            val selectedValue2 = options2[numberPicker2.value]

            // Wenn die erste Zahl "5" ist, setze die zweite Zahl auf "0"
            val selectedValue2Final = if (selectedValue1 == "5") "0" else selectedValue2

            val decimalRating = "$selectedValue1.$selectedValue2Final"
            targetTextView.text = decimalRating
            targetTextView.visibility = View.VISIBLE
            findViewById<ImageView>(R.id.star_imageView).setImageResource(R.drawable.star_filled_icon)
        }
        builder.show()
    }

    private fun showAddIngredientDialog() {
        val recipeNameEditText: EditText = findViewById(R.id.AddRecipe_recipeName)

        // Deaktiviere das EditText, um Fokus zu verhindern
        recipeNameEditText.isEnabled = false

        val builder = AlertDialog.Builder(this)
        val inflater: LayoutInflater = layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_add_ingredient, null)
        builder.setView(dialogView)

        val editTextIngredient: EditText = dialogView.findViewById(R.id.editTextIngredient)
        val editTextIngredientNumber: EditText = dialogView.findViewById(R.id.editTextIngredientNumber)
        editTextIngredient.requestFocus()

        // Hier füge das Dropdown-Menü für Zutatenkategorien hinzu
        val categorySpinner: Spinner = createCategorySpinner()
        val spinnerContainer: LinearLayout = dialogView.findViewById(R.id.spinnerContainer)
        spinnerContainer.addView(categorySpinner)

        builder.setPositiveButton("Add") { _, _ ->
            var ingredientName = editTextIngredient.text.toString()
            val ingredientNumberStr = editTextIngredientNumber.text.toString()

            // Überprüfe, ob ingredientNumberStr eine gültige Zahl ist
            val ingredientNumber = if (ingredientNumberStr.isNotEmpty() && ingredientNumberStr.isDigitsOnly()) {
                ingredientNumberStr.toInt()
            } else {
                // Zeige Toast und beende die Funktion, wenn keine gültige Zahl eingegeben wurde
                Toast.makeText(this, "Invalid input for ingredient number", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val selectedCategory = categorySpinner.selectedItem.toString()

            // Entferne Sonderzeichen und Leerzeichen am Ende des Zutatennamens
            ingredientName = checkIngredientName(ingredientName)

            if (ingredientName.isNotEmpty()) {
                // Hier kannst du die Zutat zusammen mit der ausgewählten Kategorie speichern
                val fullIngredient = "$ingredientName - $ingredientNumber $selectedCategory"
                ingredientList.add(fullIngredient)
                Log.d("test", "add $fullIngredient")
                findViewById<TextView>(R.id.ingredientTextView).visibility = View.VISIBLE

                updateIngredientListView()
            }

            // Aktiviere das EditText nach Bestätigung im Dialog
            recipeNameEditText.isEnabled = true
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            // Aktiviere das EditText nach Abbruch des Dialogs
            recipeNameEditText.isEnabled = true
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun createCategorySpinner(): Spinner {
        val categories = arrayOf(
            "Gram (g)",
            "Count",
            "Milliliter (ml)",
            "Teaspoon",
            "Tablespoon",
            "Cup",
            "Can"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val categorySpinner = Spinner(this)
        categorySpinner.adapter = adapter

        return categorySpinner
    }


    private fun updateIngredientListView() {
        // Hier kannst du die Anzeige der Zutatenliste aktualisieren
        // Zum Beispiel, kannst du eine TextView in deinem Layout haben, um die Zutaten anzuzeigen
        val ingredientTextView: TextView = findViewById(R.id.ingredientTextView)

        // Erstelle einen String, der alle Zutaten enthält, durch Trennung mit Zeilenumbrüchen
        val ingredientsText = ingredientList.joinToString("\n")

        // Setze den erstellten Text in die TextView
        ingredientTextView.text = ingredientsText
    }

    private fun showDietaryFilters() {
        selectedFilters.clear()
        val dietaryFilterArray = dietaryFilterOptions
        val checkedItems = BooleanArray(dietaryFilterArray.size) { false }

        AlertDialog.Builder(this@AddRecipe).apply {
            setTitle("Recipe contains")
            setMultiChoiceItems(dietaryFilterArray, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
                if (isChecked) {
                    selectedFilters.add(dietaryFilterOptions[which])
                } else {
                    selectedFilters.remove(dietaryFilterOptions[which])
                }
            }
            setPositiveButton("Save") { dialog, _ ->
                // Aktualisiere die Anzeige der diätetischen Filter
                updateDietaryFiltersView()
                findViewById<TextView>(R.id.dietaryFiltersTextView).visibility = View.VISIBLE

                dialog.dismiss()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            create()
            show()
        }
    }

    /**
     * add recipe button uploads recipe up to the database
     */
    private fun uploadNewRecipe() {
        // get recipeName
        val userUID = FirebaseAuth.getInstance().uid.toString()

        val recipeNameEditText: EditText = findViewById(R.id.AddRecipe_recipeName)
        val recipeName = recipeNameEditText.text.toString()

        // getting tags
        val selectedFilters = selectedFilters

        // cooking steps
        val stepsEditText: EditText = findViewById(R.id.stepsEditText)
        val steps = stepsEditText.text.toString()

        // rating
        val ratingCountTextView: TextView = findViewById(R.id.ratingCount)
        val rating = ratingCountTextView.text.toString()

        // difficulty
        val difficultyCountTextView: TextView = findViewById(R.id.difficultyCountTextView)
        val difficulty = difficultyCountTextView.text.toString()

        // cooking time
        val cookingTimeTextView: TextView = findViewById(R.id.cookingTimeTextView)
        val time = cookingTimeTextView.text.toString()

        if (!::imageUri.isInitialized) {
            // Wenn das Bild nicht ausgewählt wurde, zeige einen Toast und beende die Funktion
            Toast.makeText(this@AddRecipe, "Bitte wähle ein Bild aus", Toast.LENGTH_SHORT).show()
            return
        }

        // Firebase-Reference
        val databaseReference = FirebaseDatabase.getInstance().reference.child("Recipes")

        databaseReference.orderByKey().limitToLast(1).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var newRecipeId = 0

                if (snapshot.exists()) {
                    val lastRecipe = snapshot.children.first()
                    newRecipeId = lastRecipe.key?.toInt() ?: 0
                    newRecipeId++
                }

                // setup new recipe
                val newRecipeReference = databaseReference.child(newRecipeId.toString())
                newRecipeReference.child("name").setValue(checkIngredientName(recipeName))
                newRecipeReference.child("instructions").setValue(steps)

                // com.example.hotpot.com.example.hotpot.ui.theme.com.example.hotpot.Upload the image to Firebase Storage
                val imageName = "${checkIngredientName(recipeName)}"
                uploadImageToStorage(imageUri, imageName)  // Hier wird die imageUri an die Funktion übergeben

                // save ImageURL in a node named imageURL with a link to the picture as its value
                val imageUrl = "recipes/$imageName"
                newRecipeReference.child("imageUrl").setValue(imageUrl)

                // for each tag, create new directory
                for ((index, tag) in selectedFilters.withIndex()) {
                    newRecipeReference.child("tags").child(index.toString()).setValue(tag)
                }

                /// save ingredients in different directories
                val ingredientsReference = newRecipeReference.child("ingredients")
                for ((index, ingredient) in ingredientList.withIndex()) {
                    // Splitte die Zutat in Name, Einheitsmenge und Kategorie
                    val (ingredientName, ingredientNumberStr, category) = parseIngredient(ingredient)

                    // Erstelle ein Unterverzeichnis für jede Zutat
                    val ingredientReference = ingredientsReference.child(ingredientName)
                    ingredientReference.child(category).setValue(ingredientNumberStr)
                }

                // save description
                val descriptionEditText: TextView = findViewById(R.id.descriptionTextView)
                val description = descriptionEditText.text.toString()
                newRecipeReference.child("description").setValue(description)

                // difficulty, rating, cooking time
                newRecipeReference.child("details").setValue("difficulty: $difficulty | rating: $rating | time: $time")

                newRecipeReference.child("credit").setValue(userUID)

                Log.d("Firebase", "Neues Rezept hochgeladen mit ID: $newRecipeId");
                Toast.makeText(this@AddRecipe, "Rezept erfolgreich hochgeladen!", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Fehler beim Hochladen des Rezepts: ${error.message}")
            }
        })
    }

    private fun sanitizeRecipeNameForStorage(recipeName: String): String {
        return recipeName.replace(" ", "")
    }


    // Funktion zum Parsen der Zutat in Name, Einheitsmenge und Kategorie
    private fun parseIngredient(ingredient: String): Triple<String, String, String> {
        val parts = ingredient.split(" - ")

        // first letter captial and erase special characters at the end
        val ingredientName = checkIngredientName(parts[0])

        // Der Teil mit der Einheitsmenge und Kategorie
        val amountAndCategory = parts[1].split(" ")

        // Die Einheitsmenge
        val ingredientNumberStr = amountAndCategory[0]

        // Die Kategorie
        val category = amountAndCategory[1]

        return Triple(ingredientName, ingredientNumberStr, category)
    }

    private fun updateDietaryFiltersView() {
        // Hier kannst du die Anzeige der diätetischen Filter aktualisieren
        // Zum Beispiel, kannst du eine TextView in deinem Layout haben, um die Filter anzuzeigen
        val dietaryFiltersTextView: TextView = findViewById(R.id.dietaryFiltersTextView)

        dietaryFiltersTextView.text = ""

        // Erstelle einen String, der alle diätetischen Filter enthält, durch Trennung mit Zeilenumbrüchen
        val dietaryFiltersText = selectedFilters.joinToString("\n")

        // Setze den erstellten Text in die TextView
        dietaryFiltersTextView.text = dietaryFiltersText
    }
}
    fun checkIngredientName(name: String): String {
        var checkedName = name

        // Entferne Leerzeichen am Ende
        checkedName = checkedName.trimEnd()

        val specialCharacters = setOf(
            '!', '"', '§', '$', '%', '&', '/', '(', ')', '=', '?', '`', '´',
            '^', '°', '*', '+', '#', ',', ';', ':', '-', '_', '<', '>', '|'
        )
        while (checkedName.isNotEmpty() && specialCharacters.contains(checkedName.last())) {
            checkedName = checkedName.dropLast(1)
        }

        return checkedName.capitalize()  // Nutze capitalize(), um den ersten Buchstaben großzuschreiben
    }

