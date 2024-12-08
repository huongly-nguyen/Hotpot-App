import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.hotpot.Friend
import com.example.hotpot.MainActivity
import com.example.hotpot.R
import com.example.hotpot.Recipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class FriendAdapter(
    private val friendList: List<Friend>,
    private val clickListener: OnItemClickListener,
    private val friendStoriesFragment: FriendStoriesFragment
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val currentUser = FirebaseAuth.getInstance().currentUser

    companion object {
        private const val VIEW_TYPE_CURRENT_USER = 0
        private const val VIEW_TYPE_FRIEND = 1
    }

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivFriendImage: ImageView = itemView.findViewById(R.id.friendImageButton)
        val nameTextView: TextView = itemView.findViewById(R.id.friendNameStories)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val clickedFriend = friendList[position - 1] // Adjust index for friendList
                    clickListener.onItemClick(clickedFriend)
                }
            }
        }
    }

    inner class CurrentUserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCurrentUserImage: ImageView = itemView.findViewById(R.id.currentUserImageButton)
        val currentUserNameTextView: TextView = itemView.findViewById(R.id.currentUserNameStory)
        private val currentUserAddUserStoryBtn: ImageView = itemView.findViewById(R.id.currentUserAddUserStory)

        init {
            itemView.setOnClickListener {
                // Show UserStory des aktuellen Benutzers
                val currentUserId = currentUser?.uid ?: ""
                val currentUserReference =
                    FirebaseDatabase.getInstance().reference.child("Users").child(currentUserId)

                currentUserReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(userSnapshot: DataSnapshot) {
                        val userStory =
                            userSnapshot.child("UserStory").child("0").getValue(Recipe::class.java)

                        if (userStory == null) {
                            // UserStory ist leer oder nicht vorhanden
                            Toast.makeText(itemView.context, "Keine UserStory vorhanden", Toast.LENGTH_SHORT).show()
                        } else {
                            // UserStory existiert, öffne das UserStoryDetailsFragment mit den Rezeptdetails
                            friendStoriesFragment.openUserStoryFragment(userStory)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FriendStoriesFragment", "Firebase database error: ${error.message}")
                    }
                })
            }

            currentUserAddUserStoryBtn.setOnClickListener {
                showAlertDialog(itemView.context)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CURRENT_USER -> {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_current_user_story, parent, false)
                CurrentUserViewHolder(itemView)
            }
            VIEW_TYPE_FRIEND -> {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_friend, parent, false)
                FriendViewHolder(itemView)
            }
            else -> throw IllegalArgumentException("Invalid viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CurrentUserViewHolder -> {
                currentUser?.let { user ->
                    val userReference = FirebaseDatabase.getInstance().reference.child("Users")
                        .child(currentUser.uid)

                    // Use a ValueEventListener to get the value of the "name" field
                    userReference.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val userName = snapshot.child("name").value?.toString()
                            holder.currentUserNameTextView.text = userName

                            // Load profile picture
                            val currentUserStorageReference: StorageReference =
                                FirebaseStorage.getInstance().getReference("profilePictures")
                                    .child(user.uid)

                            currentUserStorageReference.downloadUrl.addOnSuccessListener { uri ->
                                holder.ivCurrentUserImage.load(uri)
                            }.addOnFailureListener {
                                // Handle failure to load image
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle database error
                        }
                    })
                }
            }
            is FriendViewHolder -> {
                val currentFriend = friendList[position - 1] // Adjust index for friendList
                holder.nameTextView.text = currentFriend.name

                val storageReference: StorageReference =
                    FirebaseStorage.getInstance().getReference("profilePictures")
                        .child(currentFriend.friendUID)

                storageReference.downloadUrl.addOnSuccessListener { uri ->
                    holder.ivFriendImage.load(uri)
                }.addOnFailureListener {
                    // Handle failure to get download URL
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return friendList.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_CURRENT_USER else VIEW_TYPE_FRIEND
    }

    private fun showAlertDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Bestätigung")
            .setMessage("Möchten Sie das derzeitige Rezept in Ihrer Story hochladen?")
            .setPositiveButton("Ja") { _, _ ->
                if (context is MainActivity) {
                    context.setCurrentRecipeAsCurrentUserStory()
                }
            }
            .setNegativeButton("Abbrechen") { dialog, _ ->
                dialog.dismiss()
            }

        builder.create().show()
    }
}
