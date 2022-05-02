package com.example.spor_tfg

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import com.example.spor_tfg.databinding.ActivityMainScreenBinding
import com.example.spor_tfg.databinding.ActivityScreenshotsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

class Screenshots : AppCompatActivity() {

    private lateinit var binding: ActivityScreenshotsBinding

    // Firestore
    private var storage: FirebaseStorage = Firebase.storage
    private val db: FirebaseFirestore = Firebase.firestore
    private var auth: FirebaseAuth = Firebase.auth

    // Visual elements
    lateinit var screenshotsGridLL: GridLayout

    var sessionName: String = ""
    var idx: Int = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screenshots)

        binding = ActivityScreenshotsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hooks
        screenshotsGridLL = findViewById(R.id.ss_plays_grid_layout)

        // Get session name
        sessionName = intent.getStringExtra("session_name").toString()

        // Get screenshots
        getScreenshots()

        // Init toolbar
        initToolbar()
    }

    private fun getScreenshots() {
        // Get reference
        val screenshotRef = storage.reference.child("${auth.uid.toString()}/plays/$sessionName/screenshots")
        screenshotRef.listAll().addOnSuccessListener { listResult ->
            // Iterate over list of screenshots
            listResult.items.forEach { storageReference ->
                storageReference.getBytes(Long.MAX_VALUE).addOnSuccessListener {
                    // Decode ByteArray so we can convert it to an image
                    val bm = BitmapFactory.decodeByteArray(it, 0, it.size)
                    // Rotate image 90 degrees
                    val matrix: Matrix = Matrix()
                    matrix.postRotate(360f)
                    val imageBm: Bitmap = Bitmap.createBitmap(bm, 0 ,0, bm.width, bm.height, matrix, true)
                    createScreenshotCard(imageBm)
                }.addOnFailureListener { exception ->
                    println(exception.message)
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "There was an error downloading the information, please try again later.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("InflateParams")
    fun createScreenshotCard(imageBitmap: Bitmap) {
        // Load variables
        val inflater = LayoutInflater.from(this)
        val cardLayout = inflater.inflate(R.layout.screenshot, null, false) as LinearLayout

        // Fill out inflated card elements
        cardLayout.findViewById<TextView>(R.id.screenshot_title).text = "Play $idx"
        cardLayout.findViewById<ImageView>(R.id.screenshot_iv).setImageDrawable(BitmapDrawable(resources, imageBitmap))

        // Add to grid
        screenshotsGridLL.addView(cardLayout)

        // Increment idx
        idx++

    }

    private fun initToolbar() {
        val toolbar: Toolbar = findViewById(R.id.screenshots_toolbar)
        toolbar.title = "Spor"
        toolbar.title
        setSupportActionBar(findViewById<View>(R.id.screenshots_toolbar) as Toolbar)
    }
}