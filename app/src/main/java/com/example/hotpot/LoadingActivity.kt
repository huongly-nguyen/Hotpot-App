package com.example.hotpot

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class LoadingActivity : AppCompatActivity() {
    private lateinit var loadingTextView: TextView
    private val words = "Let's get you started".split(" ")
    private var currentWordIndex = 0
    private val executorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_loading)

        loadingTextView = findViewById(R.id.loadingTextView)
        scheduleNextUpdate()
    }

    private fun scheduleNextUpdate() {
        executorService.schedule({
            runOnUiThread {
                updateText()
            }
        }, 1500, TimeUnit.MILLISECONDS)
    }

    private fun updateText() {
        if (currentWordIndex < words.size && !isFinishing) {
            loadingTextView.text = words[currentWordIndex]
            loadingTextView.alpha = 0f // Set text to fully transparent
            animateTextOpacity()

            currentWordIndex++
            scheduleNextUpdate()
        } else {
            completeLoading()
        }
    }

    private fun animateTextOpacity() {
        val opacityAnimation = ObjectAnimator.ofFloat(loadingTextView, "alpha", 0f, 1f)
        opacityAnimation.duration = 1000 // Duration of the opacity animation
        opacityAnimation.start()
    }

    private fun completeLoading() {
        val sharedPrefs = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("loadingComplete", true).apply()

        // Finish the activity when done
        // Consider starting the next activity here if that's part of the flow
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        executorService.shutdownNow()
        // Cancel the ongoing animation if the activity is destroyed
        loadingTextView.animate().cancel()
    }
}

