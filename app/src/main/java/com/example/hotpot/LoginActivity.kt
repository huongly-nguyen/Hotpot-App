package com.example.hotpot

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    companion object {
        const val LOGIN_TYPE = "login_type"
        const val LOGIN_SIGN_IN = "login_sign_in"
        const val LOGIN_SIGN_UP = "login_sign_up"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_login)

        val fragmentType = intent.getStringExtra(LOGIN_TYPE)

        // gets string from MainActivity -> depending what Button the User clicked, opens up the clicked fragment
        when(fragmentType) {
            LOGIN_SIGN_IN -> showSignInFragment();
            LOGIN_SIGN_UP -> showSignUpFragment();

            else -> showSignInFragment() // default in case
        }
    }

    internal fun showSignUpFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, SignUp())
            //.addToBackStack(null)
            .commit()
    }

    internal fun showSignInFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, SignIn())
            //.addToBackStack(null)
            .commit()
    }

    internal fun showDietFiltersFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, DietFilters())
            //.addToBackStack(null)
            .commit()
    }
    internal fun isInputOkay(message: String) {
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT
        )
        snackbar.show();
    }

}