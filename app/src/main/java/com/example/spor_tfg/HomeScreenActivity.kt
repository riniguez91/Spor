package com.example.spor_tfg

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.example.spor_tfg.databinding.ActivityHomeScreenBinding
import com.example.spor_tfg.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.w3c.dom.Text


class HomeScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeScreenBinding

    // Firestore references
    private val db: FirebaseFirestore = Firebase.firestore
    private var auth: FirebaseAuth = Firebase.auth
    private lateinit var doc: HashMap<Any, Any>

    // Options
    lateinit var chooseFirstOptionsLL: LinearLayout
    lateinit var managePlayers: TextView
    lateinit var trainingSessions: TextView

    var sessionsFlag: Boolean = false
    lateinit var sessionsLL: LinearLayout
    lateinit var backArrow: ImageButton
    lateinit var loadSession: TextView
    lateinit var addSession: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)

        binding = ActivityHomeScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hooks
        chooseFirstOptionsLL = findViewById(R.id.choose_first_options_ll)
        managePlayers = findViewById<TextView>(R.id.homes_manage_players)
        trainingSessions = findViewById<TextView>(R.id.homes_training_sessions)

        sessionsLL = findViewById(R.id.choose_session_option_ll)
        backArrow = findViewById(R.id.homes_back_arrow)
        loadSession = findViewById(R.id.homes_load_session)
        addSession = findViewById(R.id.homes_add_session)

        // Load firestore data
        loadFirestoreData()

        // Manage players click func
        managePlayersFunc()

        // Training sessions click func
        trainingSessionsFunc()

        // Add training session click func
        addTrainingSessionFunc()

        // Load training session click func
        loadTrainingSessionFunc()

        // Back arrow click func
        backArrowFunc()
    }

    private fun backArrowFunc() {
        backArrow.setOnClickListener {
            changeVisibleOptions(sessionsFlag)
        }
    }

    private fun loadTrainingSessionFunc() {
        loadSession.setOnClickListener {
            val intent = Intent(this, MainScreen::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun addTrainingSessionFunc() {
        addSession.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("addSessionFlag", true)
            startActivity(intent)
            finish()
        }
    }

    private fun loadFirestoreData() {
        val playersRef = db.collection("user").document(auth.uid.toString())
        playersRef.get().addOnSuccessListener { document ->
            if (document != null) {
                    // Cast and ignore linter warnings
                    @Suppress("UNCHECKED_CAST")
                    doc = document.data as HashMap<Any, Any>

                    // Store doc as a global variable for all classes
                    (this.application as MyApp).setDocVar(doc)
                }
            }.addOnFailureListener { exception ->
                Log.d("Failure", "get failed with ", exception)
            }
    }

    private fun managePlayersFunc() {
        managePlayers.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun trainingSessionsFunc() {
        trainingSessions.setOnClickListener {
            val intent = Intent(this, MainScreen::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun changeVisibleOptions(flag: Boolean) {
        if (!flag) {
            // Make choose sessions elements visible
            sessionsFlag = true
            sessionsLL.visibility = View.VISIBLE
            backArrow.visibility = View.VISIBLE
            chooseFirstOptionsLL.visibility = View.GONE
        }
        else {
            // Make manage players / choose sessions visible
            sessionsFlag = false
            sessionsLL.visibility = View.GONE
            backArrow.visibility = View.GONE
            chooseFirstOptionsLL.visibility = View.VISIBLE
        }
    }
}