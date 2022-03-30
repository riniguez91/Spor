package com.example.spor_tfg

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.drawerlayout.widget.DrawerLayout
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.example.spor_tfg.databinding.ActivityProfileBinding
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlin.collections.HashMap


class ProfileActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityProfileBinding

    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView

    private var storage: FirebaseStorage = Firebase.storage
    private val db: FirebaseFirestore = Firebase.firestore
    private var auth: FirebaseAuth = Firebase.auth

    // Coach layout
    lateinit var coachImage: ShapeableImageView
    lateinit var coachTitle: TextView
    lateinit var coachUsername: TextView

    // Players layout
    lateinit var doc: HashMap<Any, Any>
    lateinit var gridLayout: GridLayout

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hooks
        navigationView = findViewById(R.id.profile_navigation_view)
        navigationView.setNavigationItemSelectedListener(this)

        // Coach layout hooks
        coachImage = findViewById(R.id.profile_coach_iv)
        coachTitle = findViewById(R.id.profile_welcome_text)
        coachUsername = findViewById(R.id.profile_username)

        // Player layout hooks
        gridLayout = findViewById(R.id.profile_players_grid_layout)

        // Init toolbar
        initToolbar()

        // Preselect
        preselectToolbar()

        // Get team info
        doc =  (this.application as MyApp).getDocVar()

        // Load coach data
        loadCoachData()

        // Load players into the cards
        loadPlayers()
    }

    private fun loadImage(path: String, width: Int, height: Int, component: Any) {
        val ref = storage.getReferenceFromUrl(path)
        val oneMegabyte: Long = 1024 * 1024

        ref.getBytes(oneMegabyte).addOnSuccessListener {
            // Decode ByteArray so we can convert it to an image
            val bmp = BitmapFactory.decodeByteArray(it, 0, it.size)

            // Setting image bitmaps
            val imageBitmap: Bitmap = Bitmap.createScaledBitmap(bmp, width, height, false)
            val roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(resources, imageBitmap)
            (component as ImageView).setImageDrawable(roundedBitmapDrawable)
        }.addOnFailureListener {
            // Handle any errors
            Log.d("Download image error", it.toString())
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadCoachData() {
        loadImage(doc["image"] as String, 350, 350, coachImage)
        val coachName: String = doc["fullname"] as String
        coachTitle.text = "Welcome, $coachName"
        coachUsername.text = doc["username"] as CharSequence?
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun loadPlayers() {
        // Cast and ignore linter warnings
        @Suppress("UNCHECKED_CAST")
        // Get team array list from user/coach ID
        val team = doc["team"] as HashMap<Any, Any>
        for ((idx, player) in team) {
            @Suppress("UNCHECKED_CAST")
            player as HashMap<Any, Any>
            createCard(player["image"] as String,
                player["name"] as String,
                player["position"] as String,
                player["jersey_number"] as Long,
                player["leading_foot"] as String,
                player["status"] as String,
                idx.toString().toInt()
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("InflateParams", "CheckResult")
    private fun createCard(image: String, name: String, position: String, jersey_number: Long, leading_foot: String,
                           status: String, id: Int) {
        // Load variables
        val inflater = LayoutInflater.from(this@ProfileActivity)
        val layout = inflater.inflate(R.layout.profile_player_card, null, false) as LinearLayout
        val playerStatus: ImageButton = layout.findViewById<ImageButton>(R.id.profile_player_card_status)

        // Fill in card
        loadImage(image, 200, 200, layout.findViewById<ShapeableImageView>(R.id.player_card_image))
        layout.id = id
        layout.findViewById<TextView>(R.id.profile_player_card_name).text = name
        layout.findViewById<TextView>(R.id.profile_player_card_position).text = position
        layout.findViewById<TextView>(R.id.profile_player_card_jersey_no).text = jersey_number.toString()
        layout.findViewById<TextView>(R.id.profile_player_card_leading_foot).text = leading_foot
        when (status) {
            "available" -> {
                playerStatus.setImageDrawable(ContextCompat.getDrawable(this@ProfileActivity, R.drawable.available))
            }
            "partially available" -> {
                playerStatus.setImageDrawable(ContextCompat.getDrawable(this@ProfileActivity, R.drawable.partially_available))
            }
            "sick" -> {
                playerStatus.setImageDrawable(ContextCompat.getDrawable(this@ProfileActivity, R.drawable.sick))
            }
            else -> {
                playerStatus.setImageDrawable(ContextCompat.getDrawable(this@ProfileActivity, R.drawable.injured))
            }
        }

        // Change available/injured state
        val myItems = listOf("Available", "Injured", "Partially Available", "Sick")
        playerStatus.setOnClickListener {
            // We get the card ID
            val cardID = (it.parent.parent.parent.parent as View).id
            val imageBtn: ImageButton = it as ImageButton
            MaterialDialog(this).show {
                title(text = "Change player status?")
                message(text = "Press confirm if you wish to change the player status, else press outside the " +
                                "window and it will close")
                listItemsSingleChoice(items = myItems, initialSelection = 0) { dialog, index, text ->
                    db.collection("user").document(auth.uid.toString()).update(
                         mapOf(
                                "team.$cardID.status" to text.toString().lowercase()
                            )
                        )
                    // Change state accordingly
                    when {
                        text.toString().lowercase() == "available" -> {
                            imageBtn.setImageDrawable(ContextCompat.getDrawable(this@ProfileActivity, R.drawable.available))
                        }
                        text.toString().lowercase() == "partially available" -> {
                            imageBtn.setImageDrawable(ContextCompat.getDrawable(this@ProfileActivity, R.drawable.partially_available))
                        }
                        text.toString().lowercase() == "sick" -> {
                            imageBtn.setImageDrawable(ContextCompat.getDrawable(this@ProfileActivity, R.drawable.sick))
                        }
                        else -> {
                            imageBtn.setImageDrawable(ContextCompat.getDrawable(this@ProfileActivity, R.drawable.injured))
                        }
                    }
                }
                positiveButton(text = "Confirm")

            }
        }

        layout.setOnClickListener {

        }

        // Add card to grid layout
        gridLayout.addView(layout)
    }

    private fun initToolbar() {
        val toolbar: Toolbar = findViewById(R.id.profile_toolbar)
        toolbar.title = "Spor"
        toolbar.title
        setSupportActionBar(findViewById<View>(R.id.profile_toolbar) as Toolbar)

        drawerLayout = findViewById<View>(R.id.profile_drawer_layout) as DrawerLayout
        val toggle: ActionBarDrawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout as DrawerLayout?,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        (drawerLayout as DrawerLayout?)?.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun preselectToolbar() {
        val menuItem: MenuItem = navigationView.menu.getItem(1)
        menuItem.isChecked = true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_whiteboard -> {
                val intent = Intent(this, PaintActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.nav_profile -> {
                true
            }
            R.id.nav_video_editor -> {
                val intent = Intent(this, VideoEditorActivity::class.java)
                startActivity(intent)
                true
            }
            else -> true
        }
    }
}