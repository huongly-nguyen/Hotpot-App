package com.example.hotpot

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SignUp : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    var correctName: Boolean = false
    var correctEmail: Boolean = false
    var correctPassword: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        // resetting if Fragment gets called again
        correctName = false
        correctEmail = false
        correctPassword = false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_sign_up, container, false)

        val signInTab = view.findViewById<Button>(R.id.register_signIn_tab) // Assuming register_signIn_tab is your button ID

        // Set up an OnClickListener
        signInTab.setOnClickListener {
            // Create an Intent to start LoginActivity
            val intent = Intent(activity, LoginActivity::class.java)
            intent.putExtra(LoginActivity.LOGIN_TYPE, LoginActivity.LOGIN_SIGN_IN) // Optional: If you want to specify the fragment to show
            startActivity(intent)
            parentFragmentManager.popBackStack()
        }

        val editName: EditText = view.findViewById(R.id.signUpName);
        val editEmail: EditText = view.findViewById(R.id.signUpEmail);
        val editPassword: EditText = view.findViewById(R.id.signUpPassword);
        val signUpBtn = view.findViewById<Button>(R.id.signUp_signUp_btn)


        // setting checkmarks invisible
        editName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        editEmail.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        editPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

        signUpBtn.setOnClickListener {
            val name = editName.text.toString()
            val email = editEmail.text.toString()
            val password = editPassword.text.toString()

            if (isValidEmail(email) && isStrongPassword(password)) {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Get the newly created user
                            val firebaseUser = FirebaseAuth.getInstance().currentUser
                            val userId = firebaseUser?.uid
                            val firebaseDatabase = FirebaseDatabase.getInstance().reference.child("Usernames")

                            // Create a new user profile instance. Do NOT include the password here.
                            // For profilePictureUrl, you will need to upload the picture to Firebase Storage first and get the URL
                            val newUserProfile = UserProfile(
                                uid = userId,
                                name = name,
                                email = email,
                                profilePictureUrl = null, // You'll set this later after uploading the image
                                tags = listOf(), // Initialize with empty list or with some default tags
                                bio = "", // Initialize with empty string
                                friends = listOf() // Initialize with empty list
                            )

                            // Get a reference to the database
                            val databaseReference = FirebaseDatabase.getInstance().getReference("Users")

                            // Save the user profile
                            userId?.let {
                                databaseReference.child(it).setValue(newUserProfile).addOnCompleteListener { profileCreationTask ->
                                    if (profileCreationTask.isSuccessful) {
                                        // save Username in Usernames for later checkup
                                        firebaseDatabase.child(name).setValue(it);
                                        // Profile creation successful, proceed with your logic
                                        val intent = Intent(activity, LoadingActivity::class.java)
                                        startActivity(intent)
                                    } else {
                                        // Handle the error in profile creation
                                        Toast.makeText(context, "Profile creation failed: ${profileCreationTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                        } else {
                            Toast.makeText(context, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(requireContext(), "Invalid email or password, try again", Toast.LENGTH_SHORT).show()
            }
        }


        editName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // This method is called to notify you that the characters within `s` are about to be replaced with new text with a length of `count`.
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // This method is called to notify you that somewhere within `s`, the characters within `start` and `start + before` have been replaced with new text with a length of `count`.
            }

            override fun afterTextChanged(s: Editable?) {
                // Überprüfe, ob Text vorhanden und editName nicht ausgewählt ist
                if (!editName.isFocused && !s.isNullOrBlank()) {
                    checkIfUsernameExists(editName, s.toString())
                } else {
                    // Falls der Text leer ist oder editName ausgewählt ist, setze das Drawable auf null
                    editName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                }
            }
        })

        editEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // This method is called to notify you that the characters within `s` are about to be replaced with new text with a length of `count`.
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // This method is called to notify you that somewhere within `s`, the text has changed.
            }

            override fun afterTextChanged(s: Editable?) {
                // Check if the text is not empty and the email is valid
                val isEmailValid = s != null && isValidEmail(s.toString())
                val checkDrawable = if (isEmailValid) R.drawable.checkmark else 0
                editEmail.setCompoundDrawablesWithIntrinsicBounds(0, 0, checkDrawable, 0)
            }
        })

        editPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No action needed here
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No action needed here
            }

            override fun afterTextChanged(editable: Editable?) {
                editable?.let {
                    updatePasswordCriteria(it.toString())
                }
            }
        })


        editName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // Wenn editName nicht mehr ausgewählt ist, überprüfe den Text und zeige das Checkmark an, wenn nötig
                val drawableEndVisibility =
                    if (editName.text.isNullOrBlank()) View.INVISIBLE else View.VISIBLE
                correctName = editName.text.isNotEmpty()
                editName.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0, 0, if (drawableEndVisibility == View.VISIBLE) R.drawable.checkmark else 0, 0
                )
            }
        }
        editEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val email = editEmail.text.toString()
                val isEmailValid = isValidEmail(email)

                val drawableEndVisibility =
                    if (email.isBlank()) View.INVISIBLE else View.VISIBLE

                correctEmail = isEmailValid
                editEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0, 0, if (isEmailValid && drawableEndVisibility == View.VISIBLE) R.drawable.checkmark else 0, 0
                )
            }
        }


        editPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val password = editPassword.text.toString()
                val isStrong = isStrongPassword(password)

                val drawableEndVisibility =
                    if (password.isBlank()) View.INVISIBLE else View.VISIBLE

                correctPassword = isStrong
                editPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0, 0, if (isStrong && drawableEndVisibility == View.VISIBLE) R.drawable.checkmark else 0, 0
                )
            }
        }

        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // Überprüfe, ob der Klick außerhalb der EditTexts war
                if (editName.isFocused || editPassword.isFocused || editEmail.isFocused) {
                    val outRect = android.graphics.Rect()
                    editName.getGlobalVisibleRect(outRect)
                    editEmail.getGlobalVisibleRect(outRect)
                    if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        // Der Klick war außerhalb der EditTexts, entferne den Fokus
                        editName.clearFocus()
                        editEmail.clearFocus()
                        editPassword.clearFocus()
                    }
                }
            }
            false
        }
        return view
    }

    private fun checkIfUsernameExists(editName : EditText, username: String) {
        val usernamesReference = FirebaseDatabase.getInstance().getReference("Usernames")

        usernamesReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.hasChild(username)) {
                    // Der Benutzername existiert bereits, zeige einen Toast an
                    Toast.makeText(context, "Der Benutzername ist bereits vergeben", Toast.LENGTH_SHORT).show()
                    // Setze das Drawable auf null, um zu signalisieren, dass der Benutzername nicht verfügbar ist
                    editName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                } else {
                    // Der Benutzername ist verfügbar, zeige das grüne Häkchen an
                    editName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.checkmark, 0)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors if needed
            }
        })
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isStrongPassword(password: String): Boolean {
        // Define your password criteria here
        val minLength = 8
        val containsUppercase = password.any { it.isUpperCase() }
        val containsLowercase = password.any { it.isLowerCase() }
        val containsDigit = password.any { it.isDigit() }
        val containsSpecialChar = password.contains(Regex("[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]+"))

        val isStrong = password.length >= minLength &&
                containsUppercase &&
                containsLowercase &&
                containsDigit &&
                containsSpecialChar

        return isStrong
    }


    private fun updatePasswordCriteria(password: String) {
        val passwordCriteriaTextView = view?.findViewById<TextView>(R.id.passwordCriteriaTextView)

        val hasValidLength = password.length in 8..20
        val hasUppercase = password.any { it.isUpperCase() }
        val hasNumber = password.any { it.isDigit() }
        val hasSpecialChar = password.contains(Regex("[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]+"))

        context?.let { ctx ->
            val validLengthColor = if (hasValidLength) "#00FF00" else "#FF0000" // Green or Red
            val uppercaseColor = if (hasUppercase) "#00FF00" else "#FF0000" // Green or Red
            val numberColor = if (hasNumber) "#00FF00" else "#FF0000" // Green or Red
            val specialCharColor = if (hasSpecialChar) "#00FF00" else "#FF0000" // Green or Red

            val criteriaText = StringBuilder()
            criteriaText.append("<font color=\"$validLengthColor\">")
                .append(if (hasValidLength) "✔ " else "✖ ")
                .append("</font> 8-20 Characters<br>")

            criteriaText.append("<font color=\"$uppercaseColor\">")
                .append(if (hasUppercase) "✔ " else "✖ ")
                .append("</font> At least one capital letter<br>")

            criteriaText.append("<font color=\"$specialCharColor\">")
                .append(if (hasSpecialChar) "✔ " else "✖ ")
                .append("</font> At least one special character<br>")

            criteriaText.append("<font color=\"$numberColor\">")
                .append(if (hasNumber) "✔ " else "✖ ")
                .append("</font> At least one number<br>")


            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                passwordCriteriaTextView?.text = Html.fromHtml(criteriaText.toString(), Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                passwordCriteriaTextView?.text = Html.fromHtml(criteriaText.toString())
            }

            passwordCriteriaTextView?.visibility = View.VISIBLE
        }
    }


    override fun onResume() {
        super.onResume()
        val sharedPrefs = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        if (sharedPrefs.getBoolean("loadingComplete", false)) {
            // Reset the flag
            sharedPrefs.edit().putBoolean("loadingComplete", false).apply()

            // Navigate to DietFiltersFragment
            (activity as? LoginActivity)?.showDietFiltersFragment()
        }
    }

}

