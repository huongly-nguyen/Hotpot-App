import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.example.hotpot.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.net.Authenticator

class FridgeFragment : Fragment() {
    private lateinit var databaseReference : DatabaseReference;
    private lateinit var authenticatorReference : FirebaseAuth
    private lateinit var fridgeContentLayout : LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_fridge, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hier kannst du auf die Ansichten in deinem Fragment zugreifen
        val linearLayout: LinearLayout = view.findViewById(R.id.fridgeLinearLayoutList)

        databaseReference = FirebaseDatabase.getInstance().reference    // Get root reference

        // Assuming you have FirebaseAuth instance
        authenticatorReference = FirebaseAuth.getInstance()

        // Check if a user is currently signed in
        val currentUser = authenticatorReference.currentUser
        if (currentUser != null) {
            // The UID of the currently logged-in user
            val userUID = currentUser.uid

            Log.i("Currently logged in as ", userUID)

            // Now you can use this userUID in your function
            createObjectsInFridge(databaseReference, userUID)
        }

        // Assuming you have a reference to the old activity layout
        val ingredientListLayout = activity?.findViewById<LinearLayout>(R.id.ingredientListLayout)



        val backButton = view.findViewById<ImageView>(R.id.backButtonFridge)
        backButton?.setOnClickListener {
            // Set the visibility of the old activity layout back to visible
            ingredientListLayout?.visibility = View.VISIBLE

            // Pop the fragment from the back stack
            requireActivity().supportFragmentManager.popBackStack()
        }
    }



    fun createObjectsInFridge(databaseReference: DatabaseReference, userUID: String) {
        val userFridgeReference = databaseReference.child("Users").child(userUID).child("Fridge")

        userFridgeReference.get().addOnSuccessListener { fridgeSnapshot ->
            val categoryContentLayout = view?.findViewById<LinearLayout>(R.id.fridgeContentLayout)
            view?.findViewById<SearchView>(R.id.searchView)?.visibility = View.VISIBLE

            for (itemSnapshot in fridgeSnapshot.children) {
                // Log Item
                Log.d("Fridge", "Item: ${itemSnapshot.key}")

                // Iterate through items (Chicken, Beef, etc.)
                val itemName = itemSnapshot.key.toString()

                for (unitSnapshot in itemSnapshot.children) {
                    val unitName = unitSnapshot.key.toString()
                    val amountValue = unitSnapshot.value.toString()

                    createUIElement(itemName, unitName, amountValue)
                }
            }
        }
    }



    fun createUIElement(name: String, unit: String, amount: String) {
        fridgeContentLayout = view?.findViewById<LinearLayout>(R.id.fridgeContentLayout)!!
        val linearLayoutHorizontal = LinearLayout(requireContext())


        linearLayoutHorizontal.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        linearLayoutHorizontal.orientation = LinearLayout.HORIZONTAL

        // Create item text (left-aligned)
        val textView = TextView(requireContext())
        val textParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f // weight for left-aligned TextView
        )
        textView.layoutParams = textParams
        textView.text = name
        textView.setTextSize(25F)
        textView.typeface = ResourcesCompat.getFont(requireContext(), R.font.abeezee)
        textView.setPadding(0, 13, 0, 13)
        linearLayoutHorizontal.addView(textView)

        // Create amount text (center-aligned)
        val amountView = TextView(requireContext())
        val amountParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            0.5f
        )
        amountView.layoutParams = amountParams
        amountView.text = amount

        when (unit) {
            "Gram" -> amountView.text = "${amountView.text}g"
            "Milliliter" -> amountView.text = "${amountView.text}ml"
            "Piece" -> amountView.text = "${amountView.text}x"
            "Count" -> amountView.text = "${amountView.text}x"
            "Teaspoon" -> amountView.text = "${amountView.text}Tsp"
            "Tablespoon" -> amountView.text = "${amountView.text}tbsp"
            "Cup" -> amountView.text = "${amountView.text}c"
            "Ounce" -> amountView.text = "${amountView.text}oz"
            "Pound" -> amountView.text = "${amountView.text}lb"
            "Liter" -> amountView.text = "${amountView.text}L"
            "Fluid Ounce" -> amountView.text = "${amountView.text}fl oz"
            "Quart" -> amountView.text = "${amountView.text}qt"
            "Gallon" -> amountView.text = "${amountView.text}gal"
            // Add more units as needed
            else -> { /* Handle other cases if needed */ }
        }

        amountView.setTextSize(25F)
        amountView.typeface = ResourcesCompat.getFont(requireContext(), R.font.abeezee)
        amountView.setPadding(0, 13, 0, 13)
        linearLayoutHorizontal.addView(amountView)

        // Create edit- and trash-button (right-aligned)
        val editButton = ImageView(requireContext())
        val trashButton = ImageView(requireContext())

        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        editButton.layoutParams = buttonParams
        editButton.setImageResource(R.drawable.pen_button)
        editButton.setPadding(12, 12, 12, 12)
        linearLayoutHorizontal.addView(editButton)

        trashButton.layoutParams = buttonParams
        trashButton.setImageResource(R.drawable.trash_button)
        trashButton.setPadding(12, 12, 12, 12)
        linearLayoutHorizontal.addView(trashButton)

        editButton.setOnClickListener {
            Log.d("Buttons", "$name editButton clicked")

            // Create an AlertDialog
            val alertDialogBuilder = AlertDialog.Builder(requireContext())
            alertDialogBuilder.setTitle("Edit Quantity")

            // Create a LinearLayout to hold both EditText and Spinner
            val linearLayout = LinearLayout(requireContext())
            linearLayout.orientation = LinearLayout.HORIZONTAL

            // Create EditText
            val editText = EditText(requireContext())
            editText.gravity = Gravity.CENTER
            editText.hint = "Enter new quantity"
            linearLayout.addView(editText)

            // Create Spinner
            val unitSpinner = Spinner(requireContext())
            unitSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, getUnitOptions())

            // Set the default selection to the current unit
            val unitPosition = getUnitOptions().indexOf(unit)
            unitSpinner.setSelection(if (unitPosition != -1) unitPosition else 0)

            linearLayout.addView(unitSpinner)

            // Set the custom layout to the AlertDialog
            alertDialogBuilder.setView(linearLayout)

            alertDialogBuilder.setPositiveButton("Update") { _, _ ->
                // User clicked Update
                val newQuantity = editText.text.toString()
                val selectedUnit = unitSpinner.selectedItem.toString()

                // Validate newQuantity (you might want to add additional validation)
                if (newQuantity.isNotBlank()) {
                    // Update the quantity in the Firebase database
                    updateQuantityInDatabase(name, newQuantity, selectedUnit)
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
                }
            }

            alertDialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
                // User clicked Cancel
                dialog.dismiss()
            }

            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
        }

        trashButton.setOnClickListener {
            val alertDialogBuilder = AlertDialog.Builder(requireContext())
            alertDialogBuilder.setTitle("$name löschen")
            alertDialogBuilder.setMessage("Soll $name wirklich gelöscht werden?")

            alertDialogBuilder.setPositiveButton("Löschen") { _, _ ->
                // Benutzer hat "Löschen" ausgewählt
                Log.d("Buttons", "$name - Löschen bestätigt")

                deleteItemFromFridge(name)
            }

            alertDialogBuilder.setNegativeButton("Abbrechen") { dialog, _ ->
                // Benutzer hat "Abbrechen" ausgewählt
                Log.d("Buttons", "$name - Löschen abgebrochen")
                dialog.dismiss()
            }

            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
        }
        fridgeContentLayout?.addView(linearLayoutHorizontal)
    }

    private fun deleteItemFromFridge(itemName: String) {
        val currentUser = authenticatorReference.currentUser
        if (currentUser != null) {
            val userUID = currentUser.uid
            val itemReference = databaseReference.child("Users").child(userUID).child("Fridge")

            itemReference.get().addOnSuccessListener { fridgeSnapshot ->
                for (categorySnapshot in fridgeSnapshot.children) {
                    for (itemSnapshot in categorySnapshot.children) {
                        // Überprüfe, ob das aktuelle Item übereinstimmt
                        if (itemSnapshot.key == itemName) {
                            // Lösche das Item aus der Firebase-Datenbank
                            itemSnapshot.ref.removeValue()
                            removeUIElement(itemName)
                            return@addOnSuccessListener
                        }
                    }
                }
                Log.d("Firebase", "Item $itemName nicht gefunden")
            }.addOnFailureListener { e ->
                Log.e("Firebase", "Fehler beim Löschen des Items: ${e.message}")
            }
        }
    }

    private fun removeUIElement(itemName: String) {
        // Find and remove the UI element from fridgeContentLayout
        val childCount = fridgeContentLayout.childCount
        for (i in 0 until childCount) {
            val child = fridgeContentLayout.getChildAt(i)
            if (child is LinearLayout) {
                val textView = child.getChildAt(0) as? TextView
                if (textView?.text == itemName) {
                    // Remove the UI element
                    fridgeContentLayout.removeView(child)
                    return
                }
            }
        }
    }

    private fun updateQuantityInDatabase(itemName: String, newQuantity: String, selectedUnit: String) {
        val currentUser = authenticatorReference.currentUser

        if (currentUser != null) {
            val userUID = currentUser.uid
            val itemReference = databaseReference.child("Users").child(userUID).child("Fridge")

            itemReference.get().addOnSuccessListener { fridgeSnapshot ->
                for (categorySnapshot in fridgeSnapshot.children) {
                    for (itemSnapshot in categorySnapshot.children) {
                        // Check if the current item matches
                        if (itemSnapshot.key == itemName) {
                            // Remove all existing quantities for the item
                            itemSnapshot.ref.removeValue().addOnCompleteListener { removeTask ->
                                if (removeTask.isSuccessful) {
                                    // Add the updated quantity with the selected unit
                                    itemSnapshot.child(selectedUnit).ref.setValue(newQuantity)
                                        .addOnCompleteListener { addTask ->
                                            if (addTask.isSuccessful) {
                                                Log.d(
                                                    "Firebase",
                                                    "Quantity for $itemName updated to $newQuantity $selectedUnit"
                                                )
                                                Toast.makeText(
                                                    requireContext(),
                                                    "Quantity updated",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                // Update the UI with the new quantity and unit
                                                fridgeContentLayout.removeAllViews()
                                                createObjectsInFridge(
                                                    databaseReference,
                                                    authenticatorReference.uid.toString()
                                                )
                                            } else {
                                                Log.e(
                                                    "Firebase",
                                                    "Error adding updated quantity: ${addTask.exception?.message}"
                                                )
                                                Toast.makeText(
                                                    requireContext(),
                                                    "Error updating quantity",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                } else {
                                    Log.e(
                                        "Firebase",
                                        "Error removing existing quantities: ${removeTask.exception?.message}"
                                    )
                                    Toast.makeText(
                                        requireContext(),
                                        "Error updating quantity",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            return@addOnSuccessListener
                        }
                    }
                }
                Log.d("Firebase", "Item $itemName not found")
            }.addOnFailureListener { e ->
                Log.e("Firebase", "Error updating quantity: ${e.message}")
                Toast.makeText(requireContext(), "Error updating quantity", Toast.LENGTH_SHORT).show()
            }
        }
    }



    fun getUnitOptions(): List<String> {
        return listOf("Gram", "Milliliter", "Piece", "Count", "Teaspoon", "Tablespoon", "Cup", "Ounce", "Pound", "Liter", "Fluid Ounce", "Quart", "Gallon")
    }
}
