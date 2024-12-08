// Friend.kt
package com.example.hotpot
data class Friend(
    val friendUID: String,
    val name: String = "Friend",
    val imageUrl: String = "",
    val storyTitle: String = "",
    val storyDescription: String = ""
)