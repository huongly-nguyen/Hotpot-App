package com.example.hotpot;

import java.io.Serializable;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

data class Recipe(
    var name: String,
    var imageUrl: String,
    var description: String,
    var ingredients: MutableMap<String, Any>,
    var instructions: String,
    var details: String,
    var tags: List<String>,
    var credits: String?
): Serializable
{
    // Leerer Konstruktor für Firebase
    constructor() : this("", "", "", mutableMapOf(), "", "", listOf(), "")
}

fun uploadRecipes(recipes: List<Recipe>) {
    val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference.child("Recipes")

    recipes.forEachIndexed { index, recipe ->
        val newRecipeReference = databaseReference.push()
        newRecipeReference.child("name").setValue(recipe.name)
        newRecipeReference.child("imageUrl").setValue(recipe.imageUrl)
        newRecipeReference.child("description").setValue(recipe.description)
        newRecipeReference.child("ingredients").setValue(recipe.ingredients)
        newRecipeReference.child("instructions").setValue(recipe.instructions)
        newRecipeReference.child("details").setValue(recipe.details)
        newRecipeReference.child("tags").setValue(recipe.tags)
        newRecipeReference.child("credit").setValue(recipe.credits)
    }
}
