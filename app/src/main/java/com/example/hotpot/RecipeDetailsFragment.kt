import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.hotpot.R
import com.example.hotpot.Recipe
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide

import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

@Suppress("DEPRECATION")
class RecipeDetailsFragment : BottomSheetDialogFragment() {

    private lateinit var selectedIngredients: BooleanArray

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recipe_details, container, false)

        view.isFocusable = true
        view.isClickable = true

        val userReference = FirebaseAuth.getInstance().uid;

        val recipeTitleTextView: TextView = view.findViewById(R.id.recipe_title)
        val recipeImageImageView : ImageView = view.findViewById(R.id.recipeDetailsImageView)
        val recipeStepsTextView: TextView = view.findViewById(R.id.recipe_steps)
        val dietaryInfoTextView: TextView = view.findViewById(R.id.dietary_info)
        val descriptionTextView: TextView = view.findViewById(R.id.descriptionInfo)

        val recipeCookedBtn : TextView = view.findViewById(R.id.recipeCooked)

        val recipe = arguments?.getSerializable("RECIPE_DATA") as? Recipe

        recipe?.let {
            recipeTitleTextView.text = it.name
            loadImageFromFirebaseStorage(it.name, recipeImageImageView)
            recipeStepsTextView.text = it.instructions
            descriptionTextView.text = it.description
            dietaryInfoTextView.text = it.tags.joinToString(", ")
            selectedIngredients = BooleanArray(it.ingredients.size)
        }

        recipeCookedBtn.setOnClickListener {
            recipe?.let {
                userReference?.let { it1 -> deleteIngredientsFromFridge(it1, it.ingredients.keys) }
            }
        }


        val ingredientsButton: Button = view.findViewById(R.id.ingredients_button)
        ingredientsButton.setOnClickListener {
            recipe?.let {
                showIngredientsDialog(it.ingredients)
            }
        }

        val closeRecipeDetailsBtn: ImageButton = view.findViewById(R.id.closeRecipeDetailsBtn)
        closeRecipeDetailsBtn.setOnClickListener {
            Log.d("Fragment", "trying to close")
            dismiss()
        }

        return view
    }

    private fun sanitizeRecipeNameForStorage(recipeName: String): String {
        return recipeName.replace(" ", "")
    }

    private fun loadImageFromFirebaseStorage(imageFileName: String?, imageView: ImageView) {
        if (imageFileName.isNullOrBlank()) {
            return
        }

        val storageReference = FirebaseStorage.getInstance().reference
        val sanitizedImageFileName = sanitizeRecipeNameForStorage(imageFileName)
        val recipePictureReference = storageReference.child("recipes").child(sanitizedImageFileName)

        Log.d("FirebaseStorage", "Attempting to load image: $sanitizedImageFileName")

        // Get the download URL for the image
        recipePictureReference.downloadUrl.addOnSuccessListener { uri ->
            val imageUrl = uri.toString()

            // Use Glide to load the image from the URL into the ImageView
            Glide.with(requireContext())
                .load(imageUrl)
                .override(375, 250) // Adjust size as needed
                .into(imageView)

            Log.d("FirebaseStorage", "Image loaded successfully: $imageUrl")
        }.addOnFailureListener { exception ->
            // Handle the error when retrieving the download URL
            Log.e("FirebaseStorage", "Error loading the image: ${exception.message}")
        }
    }

    private fun deleteIngredientsFromFridge(userId: String, ingredients: Set<String>) {
        val fridgeRef = FirebaseDatabase.getInstance().getReference("Users/$userId/Fridge")

        for (ingredientName in ingredients) {
            fridgeRef.child(ingredientName).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Ingredient entry exists in the fridge
                        val fridgeAmount = (snapshot.value as? Number)?.toInt() ?: 0

                        // Remove the entry from the fridge if the updated amount is zero or negative
                        if (fridgeAmount <= 0) {
                            snapshot.ref.removeValue()
                            Toast.makeText(requireContext(), "Delete $ingredientName from Fridge", Toast.LENGTH_SHORT).show()
                        } else {
                            // You may want to handle the case where the updated amount is positive
                            Toast.makeText(requireContext(), "$ingredientName still has quantity in Fridge", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle onCancelled if needed
                }
            })
        }
    }

    private fun showIngredientsDialog(ingredients: Map<String, Any>) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        userId?.let {
            val ingredientEntries = ingredients.entries.toTypedArray()

            AlertDialog.Builder(requireContext())
                .setTitle("Add to Shopping List")
                .setMultiChoiceItems(ingredientEntries.map { "${it.key}: ${it.value}" }
                    .toTypedArray(), selectedIngredients) { _, which, isChecked ->
                    selectedIngredients[which] = isChecked
                }
                .setPositiveButton("Save") { dialog, _ ->
                    saveIngredientsToShoppingList(userId, selectedIngredients, ingredientEntries)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .setNeutralButton("Add All") { _, _ ->
                    selectAllIngredientsAndSave(userId, selectedIngredients, ingredientEntries)
                }
                .create()
                .show()
        }
    }

    private fun selectAllIngredients(size: Int) {
        for (i in 0 until size) {
            selectedIngredients[i] = true
        }
    }
    private fun selectAllIngredientsAndSave(userId: String, selectedIngredients: BooleanArray, ingredientEntries: Array<Map.Entry<String, Any>>) {
        selectAllIngredients(ingredientEntries.size)
        saveIngredientsToShoppingList(userId, selectedIngredients, ingredientEntries)
    }



    private fun saveIngredientsToShoppingList(userId: String, selectedIngredients: BooleanArray, ingredientEntries: Array<Map.Entry<String, Any>>) {
        val shoppingListRef = FirebaseDatabase.getInstance().getReference("Users/$userId/ShoppingList")
        val fridgeRef = FirebaseDatabase.getInstance().getReference("Users/$userId/Fridge")

        for (i in selectedIngredients.indices) {
            if (selectedIngredients[i]) {
                val ingredientEntry = ingredientEntries[i]
                val ingredientName = ingredientEntry.key
                val ingredientValue = ingredientEntry.value as? Map<*, *> ?: continue

                // Überprüfung, ob das Ingredient bereits im Kühlschrank existiert
                fridgeRef.child(ingredientName).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(fridgeSnapshot: DataSnapshot) {
                        if (fridgeSnapshot.exists()) {
                            // Ingredient existiert bereits im Kühlschrank
                            for ((unit, amount) in ingredientValue) {
                                val fridgeAmountData = fridgeSnapshot.child(unit.toString()).value as? Number ?: continue
                                val fridgeAmount = fridgeAmountData.toDouble()
                                val newAmount = amount as? Number ?: continue

                                // Überprüfe, ob die Menge im Kühlschrank kleiner ist als die Menge in der ShoppingList
                                if (fridgeAmount < newAmount.toDouble()) {
                                    // Die Differenz berechnen und zur ShoppingList hinzufügen
                                    val difference = newAmount.toDouble() - fridgeAmount
                                    shoppingListRef.child(ingredientName).child(unit.toString()).setValue(difference)
                                }
                            }
                        } else {
                            // Ingredient existiert nicht im Kühlschrank, einfach zur ShoppingList hinzufügen
                            for ((unit, amount) in ingredientValue) {
                                shoppingListRef.child(ingredientName).child(unit.toString()).setValue(amount)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle onCancelled if needed
                    }
                })
            }
        }
        Toast.makeText(requireContext(), "Ingredients added to ShoppingList", Toast.LENGTH_SHORT).show()
    }


    override fun onResume() {
        super.onResume()
        view?.findViewById<View>(R.id.recipe_title)?.requestFocus()
    }
}

