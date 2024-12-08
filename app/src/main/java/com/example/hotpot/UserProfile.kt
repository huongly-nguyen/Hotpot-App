package com.example.hotpot

data class UserProfile(
    val uid: String? = null,
    var name: String? = null,
    var email: String? = null,
    var profilePictureUrl: String? = null, // URL to the user's profile picture
    var tags: List<String>? = null, // List of tags like "vegan", "keto", etc.
    var bio: String? = null, // Short bio, max 100 letters
    val friends: List<String>? = null // List of friend's UIDs
)
