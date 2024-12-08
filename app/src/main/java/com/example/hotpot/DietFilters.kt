package com.example.hotpot

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue

class DietFilters : DialogFragment() {
    private lateinit var checkBoxVegan: CheckBox
    private lateinit var checkBoxGluten: CheckBox
    private lateinit var checkBoxHalal: CheckBox
    private lateinit var checkBoxKeto: CheckBox
    private lateinit var checkBoxKosher: CheckBox
    private lateinit var checkBoxLactose: CheckBox
    private lateinit var checkBoxPaleo: CheckBox
    private lateinit var checkBoxPeanut: CheckBox
    private lateinit var checkBoxPescatarian: CheckBox
    private lateinit var checkBoxShellfish: CheckBox
    private lateinit var checkBoxSoy: CheckBox
    private lateinit var checkBoxVegetarian: CheckBox

    // FireBase Zugriffe
    private lateinit var userId: String
    private lateinit var databaseReference: DatabaseReference

    private var listener: OnFragmentInteractionListener? = null

    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_diet_filter, container, false)

        checkBoxVegan = view.findViewById(R.id.checkbox_vegan)
        checkBoxGluten = view.findViewById(R.id.checkbox_gluten)
        checkBoxHalal = view.findViewById(R.id.checkbox_halal)
        checkBoxKeto = view.findViewById(R.id.checkbox_keto)
        checkBoxKosher = view.findViewById(R.id.checkbox_kosher)
        checkBoxLactose = view.findViewById(R.id.checkbox_lactose)
        checkBoxPaleo = view.findViewById(R.id.checkbox_paleo)
        checkBoxPeanut = view.findViewById(R.id.checkbox_peanut)
        checkBoxPescatarian = view.findViewById(R.id.checkbox_pescatarian)
        checkBoxShellfish = view.findViewById(R.id.checkbox_shellfish)
        checkBoxSoy = view.findViewById(R.id.checkbox_soy)
        checkBoxVegetarian = view.findViewById(R.id.checkbox_vegetarian)

        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        databaseReference = FirebaseDatabase.getInstance().reference.child("Users").child(userId).child("Tags")

        // mark the checkBoxes that already are tagged
        loadDietaryFilters()


        val saveButton = view.findViewById<Button>(R.id.saveButton)
        saveButton.setOnClickListener {
            saveDietaryFilters();

            // Close the current fragment
            fragmentManager?.popBackStack()

            // close SettingsActivity
            listener?.onCloseFragment();

            // Navigate to MainActivity after saving changes
            val intent = Intent(requireActivity(), MainActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    private fun saveDietaryFilters() {
        val selectedFilters = mutableSetOf<String>()

        // for each mark, add tag to list that will be uploaded
        if (checkBoxVegan.isChecked) {
            selectedFilters.add("vegan")
        }
        if (checkBoxGluten.isChecked) {
            selectedFilters.add("gluten")
        }
        if (checkBoxHalal.isChecked) {
            selectedFilters.add("halal")
        }
        if (checkBoxKeto.isChecked) {
            selectedFilters.add("keto")
        }
        if (checkBoxKosher.isChecked) {
            selectedFilters.add("kosher")
        }
        if (checkBoxLactose.isChecked) {
            selectedFilters.add("lactose")
        }
        if (checkBoxPaleo.isChecked) {
            selectedFilters.add("paleo")
        }
        if (checkBoxPeanut.isChecked) {
            selectedFilters.add("peanut")
        }
        if (checkBoxPescatarian.isChecked) {
            selectedFilters.add("pescatarian")
        }
        if (checkBoxShellfish.isChecked) {
            selectedFilters.add("shellfish")
        }
        if (checkBoxSoy.isChecked) {
            selectedFilters.add("soy")
        }
        if (checkBoxVegetarian.isChecked) {
            selectedFilters.add("vegetarian")
        }

        // erase all tags of an user
        databaseReference.removeValue()

        // Save Filters as tags in the user-id
        databaseReference.setValue(selectedFilters.toList())
            .addOnSuccessListener {
                // saved successfully
                Toast.makeText(requireContext(), "Changes saved successfully", Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener { error ->
                // error
                Toast.makeText(
                    requireContext(),
                    "Error saving changes: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun loadDietaryFilters() {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userTags = snapshot.getValue<List<String>>()

                    // for each tag, mark checkbox
                    userTags?.forEach { tag ->
                        when (tag) {
                            "vegan" -> checkBoxVegan.isChecked = true
                            "gluten" -> checkBoxGluten.isChecked = true
                            "halal" -> checkBoxHalal.isChecked = true
                            "keto" -> checkBoxKeto.isChecked = true
                            "kosher" -> checkBoxKosher.isChecked = true
                            "lactose" -> checkBoxLactose.isChecked = true
                            "paleo" -> checkBoxPaleo.isChecked = true
                            "peanut" -> checkBoxPeanut.isChecked = true
                            "pescatarian" -> checkBoxPescatarian.isChecked = true
                            "shellfish" -> checkBoxShellfish.isChecked = true
                            "soy" -> checkBoxSoy.isChecked = true
                            "vegetarian" -> checkBoxVegetarian.isChecked = true
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // error loading
                Toast.makeText(
                    requireContext(),
                    "Error loading dietary filters: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}



