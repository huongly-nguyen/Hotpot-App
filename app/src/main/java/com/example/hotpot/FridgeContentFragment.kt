package com.example.hotpot

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

private const val ARG_SELECTED_CATEGORY = "meat"

/**
 * A simple [Fragment] subclass.
 * Use the [FridgeContentFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FridgeContentFragment : Fragment() {
    private var selectedCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectedCategory = it.getString(ARG_SELECTED_CATEGORY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_fridge_content, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get correct database set from param
        val databaseReference = FirebaseDatabase.getInstance().reference.child("Ingredients").child(selectedCategory.toString())
        createObjectsInFridgeList(databaseReference)


        // Assuming you have a reference to the old activity layout
        val ingredientListLayout = activity?.findViewById<LinearLayout>(R.id.ingredientListLayout)

        // Handle the back button press
        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                // Set the visibility of the old activity layout back to visible
                ingredientListLayout?.visibility = View.VISIBLE
                Log.d("LayoutVisibility: ", ingredientListLayout?.visibility.toString())

                // Pop the fragment from the back stack
                requireActivity().supportFragmentManager.popBackStack()

                true // Event handled
            } else {
                false // Event not handled
            }
        }

        val backButton = view.findViewById<ImageView>(R.id.backButton)
        backButton?.setOnClickListener {
            // Set the visibility of the old activity layout back to visible
            ingredientListLayout?.visibility = View.VISIBLE

            // Pop the fragment from the back stack
            requireActivity().supportFragmentManager.popBackStack()
        }


    }

    private fun createObjectsInFridgeList(databaseReference: DatabaseReference) {
        databaseReference.get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val categoryContentLayout = view?.findViewById<LinearLayout>(R.id.categoryContentLayout)
                view?.findViewById<TextView>(R.id.categoryName)?.text = selectedCategory.toString()

                view?.findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)?.visibility = View.VISIBLE

                for (item in dataSnapshot.children) {
                    val name = item.child("name").value.toString()

                    // Create horizontal LinearLayout
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

                    // Create add button (right-aligned)
                    val addButton = ImageView(requireContext())
                    val buttonParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    addButton.layoutParams = buttonParams
                    addButton.setImageResource(R.drawable.plus_button)
                    addButton.setPadding(12, 12, 12, 12)
                    linearLayoutHorizontal.addView(addButton)

                    categoryContentLayout?.addView(linearLayoutHorizontal)

                    addButton.setOnClickListener {
                        // open popup window
                        showQuantityInputDialog(name)
                        Log.d("test", "$name was clicked ")
                    }
                }
            }
        }
    }

    private fun showQuantityInputDialog(ingredientName: String) {
        val builder = AlertDialog.Builder(requireContext())
        val view = View.inflate(requireContext(), R.layout.dialog_quantity_input, null)
        builder.setView(view)

        val quantityInput = view.findViewById<EditText>(R.id.quantityInput)
        val unitSpinner = view.findViewById<Spinner>(R.id.unitSpinner)

        // Definiere die Auswahlmöglichkeiten für das Spinner-Menü
        val unitOptions = arrayOf("Gram", "Milliliter",
            "Piece", "Teaspoon", "Tablespoon", "Cup", "Ounce", "Pound",
            "Liter", "Fluid Ounce", "Quart", "Gallon", "Count")

        // Verwende ein ArrayAdapter, um die Auswahlmöglichkeiten in das Spinner-Menü zu laden
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, unitOptions)
        unitSpinner.adapter = adapter

        builder.setPositiveButton("OK") { _, _ ->
            val quantity = quantityInput.text.toString()
            val selectedUnit = unitSpinner.selectedItem.toString()
            saveQuantityToFirebase(ingredientName, quantity, selectedUnit)
            Log.d("Quantity", "Ingredient: $ingredientName, Quantity: $quantity $selectedUnit")
            Toast.makeText(requireContext(), "Erfolgreich zum Kühlschrank hinzugefügt!", Toast.LENGTH_SHORT).show()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    @SuppressLint("SuspiciousIndentation")
    private fun saveQuantityToFirebase(ingredientName: String, quantity: String, unit: String) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid.toString()
        val databaseReference = FirebaseDatabase.getInstance().reference.child("Users").child(currentUserUid)


        val fridgeReference = databaseReference.child("Fridge")

        // Initialisiere ingredientReference mit einem Standardwert
        val ingredientReference : DatabaseReference = fridgeReference.child(ingredientName).child(unit.toString())
            ingredientReference.setValue(quantity)
                .addOnSuccessListener {
                    Log.d("Firebase", "Menge erfolgreich gespeichert")
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Fehler beim Speichern der Menge: ${e.message}")
                }
        }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 The selected category in shopping list.
         * @return A new instance of fragment FridgeContentFragment.
         */
        @JvmStatic
        fun newInstance(param1: String) =
            FridgeContentFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_CATEGORY, param1)
                }
            }
    }
}