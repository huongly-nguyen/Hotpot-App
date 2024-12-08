import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hotpot.Friend
import com.example.hotpot.R
import com.example.hotpot.Recipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


interface OnItemClickListener {
    fun onItemClick(friend: Friend)
}

@Suppress("DEPRECATION")
class FriendStoriesFragment : Fragment(), OnItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private val friendList = mutableListOf<Friend>()
    private val currentUser = FirebaseAuth.getInstance().currentUser  // Hier deklarieren und initialisieren

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_friend_stories, container, false)

        recyclerView = view.findViewById(R.id.friendsRecyclerView)

        val databaseReference = FirebaseDatabase.getInstance().reference
        val user = FirebaseAuth.getInstance().currentUser
        val friendsReference = databaseReference.child("Users").child(user!!.uid).child("Friends")

        friendsReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Clear the existing friendList before populating it
                friendList.clear()

                Log.d("FriendStoriesFragment", "Snapshot value: ${snapshot.value}")

                // Ensure that the snapshot has a value and is a HashMap before attempting to retrieve data
                if (snapshot.value is Map<*, *>) {
                    val friendUIDs = (snapshot.value as Map<*, *>).values.toList()

                    friendUIDs.forEach { friendUID ->
                        // Cast the friendUID to String
                        val friendUIDString = friendUID.toString()

                        val friendReference = databaseReference.child("Users").child(friendUIDString)

                        friendReference.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(friendSnapshot: DataSnapshot) {
                                var friendName = friendSnapshot.child("name").getValue(String::class.java)

                                if (friendName != null) {
                                    if (friendUIDString == FirebaseAuth.getInstance().uid) {
                                        friendName = "CurrentUser"
                                    }

                                    Log.d("FriendStoriesFragment", "Friend name: $friendName")
                                    Log.d("FriendStoriesFragment", "Friend UID: $friendUIDString")

                                    val friend = Friend(friendUIDString, friendName)
                                    friendList.add(friend)

                                    // Check if the adapter is not attached and friendList is not empty
                                    if (recyclerView.adapter == null && friendList.isNotEmpty()) {
                                        // Set up the RecyclerView adapter after adding the first friend
                                        val adapter = FriendAdapter(friendList,this@FriendStoriesFragment, this@FriendStoriesFragment)
                                        recyclerView.layoutManager =
                                            LinearLayoutManager(
                                                requireContext(),
                                                LinearLayoutManager.HORIZONTAL,
                                                false
                                            )
                                        recyclerView.adapter = adapter
                                    } else {
                                        // Notify the adapter about the data change if it's already attached
                                        recyclerView.adapter?.notifyDataSetChanged()
                                    }
                                } else {
                                    Log.e(
                                        "FriendStoriesFragment",
                                        "Friend name is null for UID: $friendUIDString"
                                    )
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e(
                                    "FriendStoriesFragment",
                                    "Firebase database error: ${error.message}"
                                )

                                Toast.makeText(
                                    requireContext(),
                                    "An error occurred. Please try again later.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle the error
            }
        })
        val currentUserUid = user?.uid ?: ""
        friendList.add(Friend(currentUserUid, "CurrentUser")) // placeholder name

        // set background to transparent
        recyclerView.setBackgroundColor(resources.getColor(android.R.color.transparent))

        return view
    }

    override fun onItemClick(friend: Friend) {
        val currentUserId = currentUser?.uid ?: ""

        if (friend.friendUID == currentUserId) {
            // Der aktuelle Benutzer wurde angeklickt, lade seine UserStory
            val currentUserReference =
                FirebaseDatabase.getInstance().reference.child("Users").child(currentUserId)

            currentUserReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userSnapshot: DataSnapshot) {
                    val userStory =
                        userSnapshot.child("UserStory").child("0").getValue(Recipe::class.java)

                    if (userStory == null) {
                        // UserStory ist leer oder nicht vorhanden
                        Toast.makeText(requireContext(), "Keine UserStory vorhanden", Toast.LENGTH_SHORT).show()
                    } else {
                        // UserStory existiert, öffne das UserStoryDetailsFragment mit den Rezeptdetails
                        openUserStoryFragment(userStory)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FriendStoriesFragment", "Firebase database error: ${error.message}")
                }
            })
        } else {
            // Ein anderer Freund wurde angeklickt, lade seine UserStory
            val usersReference =
                FirebaseDatabase.getInstance().reference.child("Users").child(friend.friendUID.toString())

            usersReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userSnapshot: DataSnapshot) {
                    val userStory =
                        userSnapshot.child("UserStory").child("0").getValue(Recipe::class.java)

                    if (userStory == null) {
                        // UserStory ist leer oder nicht vorhanden
                        Toast.makeText(requireContext(), "Keine UserStory vorhanden", Toast.LENGTH_SHORT).show()
                    } else {
                        // UserStory existiert, öffne das UserStoryDetailsFragment mit den Rezeptdetails
                        openUserStoryFragment(userStory)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FriendStoriesFragment", "Firebase database error: ${error.message}")
                }
            })
        }
    }

    public fun openUserStoryFragment(recipe: Recipe) {
        // Check if recipe is not null before proceeding
        recipe?.let {
            // Load the full recipe details before displaying the UserStoryDetailsFragment
            loadFullRecipeDetails(recipe)
        }
    }

    private fun loadFullRecipeDetails(recipe: Recipe) {
        val databaseReference = FirebaseDatabase.getInstance().reference
        val recipesReference = databaseReference.child("Recipes")

        // Directly query the child with the recipe name
        val recipeName = recipe.name ?: return

        recipesReference.orderByChild("name").equalTo(recipeName).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get the first child, assuming there's only one match
                    val recipeNode = dataSnapshot.children.first()

                    val fullRecipe = recipeNode.getValue(Recipe::class.java)

                    if (fullRecipe != null) {
                        // Full recipe details retrieved, open the UserStoryDetailsFragment
                        openUserStoryDetailsFragment(fullRecipe)
                    } else {
                        Log.e("FriendStoriesFragment", "Recipe not found.")
                    }
                } else {
                    Log.e("FriendStoriesFragment", "Recipe not found.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FriendStoriesFragment", "Firebase database error: ${error.message}")
            }
        })
    }

    private fun openUserStoryDetailsFragment(fullRecipe: Recipe) {
        val userStoryDetailsFragment = RecipeDetailsFragment()
        val bundle = Bundle()
        bundle.putSerializable("RECIPE_DATA", fullRecipe)
        userStoryDetailsFragment.arguments = bundle
        userStoryDetailsFragment.show(requireActivity().supportFragmentManager, userStoryDetailsFragment.tag)
    }
}