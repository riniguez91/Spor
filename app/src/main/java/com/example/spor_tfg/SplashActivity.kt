package com.example.spor_tfg

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Pair
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    // Variables
    private lateinit var topAnim: Animation
    private lateinit var bottomAnim: Animation
    private lateinit var image: ImageView
    private lateinit var logo: TextView
    private lateinit var slogan: TextView
    private val splashTimeOut:Long = 5000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        topAnim = AnimationUtils.loadAnimation(this, R.anim.top_animation)
        bottomAnim = AnimationUtils.loadAnimation(this, R.anim.bottom_animation)
        image = findViewById(R.id.splash_image)
        logo = findViewById(R.id.splash_header)
        slogan = findViewById(R.id.splash_slogan)

        // Hooks
        image.startAnimation(topAnim)
        logo.startAnimation(bottomAnim)
        slogan.startAnimation(bottomAnim)

        Handler(Looper.getMainLooper()).postDelayed({
            // This method will be executed once the timer is over
            // Start your app main activity
            val intent = Intent(this, LoginActivity::class.java)
            val pairs = arrayOfNulls<Pair<View, String>>(2)
            pairs[0] = Pair<View, String>(image, getString(R.string.transition_logo))
            pairs[1] = Pair<View, String>(logo, getString(R.string.transition_text))

            val options: ActivityOptions = ActivityOptions.makeSceneTransitionAnimation(this, *pairs)
            startActivity(intent, options.toBundle())
        }, splashTimeOut)
    }
}