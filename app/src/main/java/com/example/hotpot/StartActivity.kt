package com.example.hotpot

import android.os.Bundle
import android.widget.Button
import android.view.View
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class StartActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.start_screen);

        val signInBtn: View = findViewById<Button>(R.id.signInBtn);
        val signUpBtn: View = findViewById(R.id.signUpBtn);

        signInBtn.setOnClickListener {
            startLoginActivity(LoginActivity.LOGIN_SIGN_IN);
        }

        signUpBtn.setOnClickListener {
            startLoginActivity(LoginActivity.LOGIN_SIGN_UP);
        }
    }

    private fun startLoginActivity(fragmentType: String) {
        val intent = Intent(this, LoginActivity::class.java);
        intent.putExtra(LoginActivity.LOGIN_TYPE, fragmentType)
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        finish();
    }
}
