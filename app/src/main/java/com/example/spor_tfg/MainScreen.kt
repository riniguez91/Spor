package com.example.spor_tfg

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.opengl.Visibility
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.datetime.datePicker
import com.example.spor_tfg.databinding.ActivityMainScreenBinding
import com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.google.gson.Gson
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class MainScreen : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    AddPlayDialogFragment.NoticeDialogListener{

    private lateinit var binding: ActivityMainScreenBinding

    lateinit var myApp: MyApp

    // App navigation
    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView

    // Firestore
    private var storage: FirebaseStorage = Firebase.storage
    private val db: FirebaseFirestore = Firebase.firestore
    private var auth: FirebaseAuth = Firebase.auth

    // Plays grid layout
    lateinit var playsGL: GridLayout
    lateinit var addPlayButton: ImageButton
    lateinit var calendar: TextView

    lateinit var doc: HashMap<Any, Any>
    var sessionName: String = ""
    var filteredTimeInMillis: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)

        binding = ActivityMainScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Navbar hooks
        navigationView = findViewById(R.id.mscreen_navigation_view)
        navigationView.setNavigationItemSelectedListener(this)

        // Hooks
        playsGL = findViewById(R.id.ms_plays_grid_layout)
        addPlayButton = findViewById(R.id.mscreen_add_play_button)
        calendar = findViewById(R.id.ms_calendar)

        // Get team info
        myApp = this.application as MyApp
        doc =  myApp.getDocVar()

        // Get session name
        sessionName = intent.getStringExtra("session_name").toString()

        // Load plays
        loadSessions()

        // Set add play button click func
        addPlayFunc()

        // Init toolbar
        initToolbar()

        // Preselect
        preselectToolbar()

        // Set calendar on click func
        calendarOnClickFunc()
    }

    private fun calendarOnClickFunc() {
        calendar.setOnClickListener {
            MaterialDialog(this).show {
                datePicker { dialog, date ->
                    // Use date (Calendar)
                    filteredTimeInMillis = date.timeInMillis
                    playsGL.children.forEach { ll ->
                        if (ll is LinearLayout) {
                            ll.children.forEach {
                                val cardTime: Long = convertDateToLong(it.findViewById<TextView>(R.id.plays_card_timestamp).text.toString())
                                if (filteredTimeInMillis > cardTime)
                                    it.visibility = View.GONE
                                else
                                    it.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }
    }

    private fun addPlayFunc() {
        addPlayButton.setOnClickListener {
            val dialog = AddPlayDialogFragment()
            dialog.show(supportFragmentManager, "AddPlayDialogFragment")
        }
    }

    private fun cardWarmUpClickFunc(cardWarmUpButton: ImageView, currSessionName: String) {
        cardWarmUpButton.setOnClickListener {
            val intent = Intent(this, WarmUp::class.java)
            intent.putExtra("session_name", currSessionName)
            startActivity(intent)
        }
    }

    // Go to paint activity
    @Suppress("UNCHECKED_CAST")
    private fun cardPaintClickFunc(cardPaintButton: ImageView, currSessionName: String) {
        cardPaintButton.setOnClickListener {
            // Create animations hashmap
            val animations: HashMap<Int, HashMap<Int, ArrayList<Pair<Float, Float>>>> = HashMap()

            // Get selected team hashmap
            val teamRef = storage.reference.child("${auth.uid.toString()}/plays/${currSessionName}/selected_team")
            teamRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { byteArray ->
                val byteIn: ByteArrayInputStream = ByteArrayInputStream(byteArray)
                val oIn: ObjectInputStream = ObjectInputStream(byteIn)
                doc["team"] =  oIn.readObject() as HashMap<Any, Any>

                // Go to paint activity
                val intent = Intent(this, PaintActivity::class.java)
                intent.putExtra("session_name", currSessionName)
                intent.putExtra("animations", animations)
                startActivity(intent)
            }.addOnFailureListener {
                println(it.message)
            }
        }
    }

    // Go to video editor activity
    @Suppress("UNCHECKED_CAST")
    private fun cardVideoClickFunc(cardVideoButton: ImageView, currSessionName: String) {
        cardVideoButton.setOnClickListener {
            val intent = Intent(this, VideoEditorActivity::class.java)
            startActivity(intent)
        }
    }

    // Go to saved paints (plays)
    private fun cardSavedScreenshotsClickFunc(cardSavedScreenshotsButton: ImageView, currSessionName: String) {
        cardSavedScreenshotsButton.setOnClickListener {
            if (cardSavedScreenshotsButton.tag == "thumbnail") {
                // Go to screenshots activity
                val intent = Intent(this, Screenshots::class.java)
                intent.putExtra("session_name", currSessionName)
                startActivity(intent)
            }
            else {
                Toast.makeText(this, "There are no saved screenshots, try adding them first!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Go to saved videos
    private fun cardSavedAnimClickFunc(cardSavedAnimButton: ImageView, currSessionName: String) {
        cardSavedAnimButton.setOnClickListener {
            // Get animations
            // Create animations hashmap
            var animations: HashMap<Int, HashMap<Int, ArrayList<Pair<Float, Float>>>> = HashMap()

            // Create reference
            val teamRef = storage.reference.child("${auth.uid.toString()}/plays/${currSessionName}/selected_team")
            teamRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { byteArray ->
                val byteIn: ByteArrayInputStream = ByteArrayInputStream(byteArray)
                val oIn: ObjectInputStream = ObjectInputStream(byteIn)
                doc["team"] = oIn.readObject() as HashMap<Any, Any>

                val animationRef = storage.reference.child("${auth.uid.toString()}/plays/${currSessionName}/animations")
                animationRef.getBytes(Long.MAX_VALUE).addOnSuccessListener {
                    println("Got animations :D")
                    animations = Gson().fromJson(it.toString(Charsets.UTF_8), animations.javaClass)
                    // Go to paint activity
                    val intent = Intent(this, PaintActivity::class.java)
                    intent.putExtra("session_name", currSessionName)
                    intent.putExtra("animations", animations)
                    startActivity(intent)
                }.addOnFailureListener {
                    println(it.message)
                    Toast.makeText(this, "No animations saved, try adding them first!", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                println(it.message)
            }
        }
    }

    // Edit card
    private fun cardEditClickFunc(cardEditButton: ImageView) {
        cardEditButton.setOnClickListener {
            // TODO("Add functionality to edit play title objective and play")
        }
    }

    // Delete card
    private fun cardDeleteClickFunc(cardDeleteButton: ImageView, currSessionName: String) {
        cardDeleteButton.setOnClickListener {
            val dialog = MaterialDialog(this).show {
                title(text = "Confirm action")
                message(text = "Are you sure you want to delete the following play? This action is irreversible.")
                positiveButton(text = "Confirm") { dialog ->
                    val pathRef = storage.reference.child("${auth.uid.toString()}/plays/${currSessionName}")
                    println(pathRef)
                    pathRef.listAll().addOnSuccessListener {
                        it.items.forEach { item ->
                            item.delete()
                        }
                        it.prefixes.forEach { prefix ->
                            prefix.delete()
                        }
                        // Delete from grid (3 or 4 parent)
                        playsGL.removeView(cardDeleteButton.parent.parent.parent.parent as LinearLayout)

                        // Notify user
                        Toast.makeText(this@MainScreen, "Play has been deleted successfully!", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(this@MainScreen, "There was en error during the operation, please try again later.", Toast.LENGTH_SHORT).show()
                    }
                }
                negativeButton(text = "Cancel") {
                    it.dismiss()
                }
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun createPlayCard(name: String, objective: String, currSessionName: String, time: Long, thumbnailURL: String = "") {
        // Load variables
        val inflater = LayoutInflater.from(this)
        val cardLayout = inflater.inflate(R.layout.plays_card, null, false) as LinearLayout

        // Buttons
        val cardWarmUpButton: ImageView = cardLayout.findViewById(R.id.plays_card_warmup_activity)
        val cardPaintButton: ImageView = cardLayout.findViewById(R.id.plays_card_paint_activity)
        val cardVideoButton: ImageView = cardLayout.findViewById(R.id.plays_card_veditor_activity)
        val cardSavedScreenshotsButton: ImageView = cardLayout.findViewById(R.id.plays_card_saved_screenshots)
        val cardSavedAnimButton: ImageView = cardLayout.findViewById(R.id.plays_card_saved_anim)
        val cardEditButton: ImageView = cardLayout.findViewById(R.id.plays_card_edit)
        val cardDeleteButton: ImageView = cardLayout.findViewById(R.id.plays_card_delete)

        // Set on click for each button
        cardWarmUpClickFunc(cardWarmUpButton, currSessionName)
        cardPaintClickFunc(cardPaintButton, currSessionName)
        cardVideoClickFunc(cardVideoButton, currSessionName)
        cardSavedScreenshotsClickFunc(cardSavedScreenshotsButton, currSessionName)
        cardSavedAnimClickFunc(cardSavedAnimButton, currSessionName)
        cardEditClickFunc(cardEditButton)
        cardDeleteClickFunc(cardDeleteButton, currSessionName)

        // Get card thumbnail
        val cardImage: ImageView = cardLayout.findViewById<ImageView>(R.id.plays_card_thumbnail)

        // Set session background if it contains a screenshot
        if (thumbnailURL.isNotEmpty()) {
            storage.reference.child(thumbnailURL).list(1).addOnSuccessListener { listResult ->
                if (listResult.items.size > 0) {
                    listResult.items[0].getBytes(Long.MAX_VALUE).addOnSuccessListener {
                        // Decode ByteArray so we can convert it to an image
                        val bm = BitmapFactory.decodeByteArray(it, 0, it.size)

                        // Rotate image 90 degrees
                        val matrix: Matrix = Matrix()
                        matrix.postRotate(360f)
                        cardImage.setImageDrawable(BitmapDrawable(resources, Bitmap.createBitmap(bm, 0 ,0, bm.width, bm.height, matrix, true)))
                        cardImage.visibility = View.VISIBLE
                        cardSavedScreenshotsButton.tag = "thumbnail"
                    }
                }
            }
        }

        // Add card to scroll view
        cardLayout.findViewById<TextView>(R.id.plays_card_name).text = name

        // Add card timestamp
        cardLayout.findViewById<TextView>(R.id.plays_card_timestamp).text = convertLongToTime(time)

        val cardObjective: TextView =  cardLayout.findViewById<TextView>(R.id.plays_card_objective)
        cardObjective.text = getString(R.string.bold_objective, objective)
        playsGL.addView(cardLayout, playsGL.indexOfChild(playsGL.getChildAt(playsGL.childCount - 1)))
        cardImage.visibility = View.VISIBLE
    }

    fun convertLongToTime(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm")
        return format.format(date)
    }

    fun convertDateToLong(date: String): Long {
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm")
        return df.parse(date)!!.time
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadSessions() {
        // Check if there are any saved plays
        val playsRef = storage.reference.child("${auth.uid.toString()}/plays")
        playsRef.listAll().addOnSuccessListener { listResult ->
            // Check if there are saved sessions
            // if (listResult.prefixes.size != 0)
            // Iterate over the list of sessions
            listResult.prefixes.forEach { prefix ->
                val screenshotsRef: String = "${playsRef.path}/${prefix.name}/screenshots"
                val folderStoRef: StorageReference = prefix.child(prefix.name)
                folderStoRef.getBytes(Long.MAX_VALUE).addOnSuccessListener {
                    val byteIn: ByteArrayInputStream = ByteArrayInputStream(it)
                    val oIn: ObjectInputStream = ObjectInputStream(byteIn)
                    val cardData: HashMap<String, String> = oIn.readObject() as HashMap<String, String>
                    sessionName = folderStoRef.name

                    // Check if this session has any thumbnails
                    prefix.listAll().addOnSuccessListener { childListResult ->
                        // Get file creation
                        folderStoRef.metadata.addOnSuccessListener { metadata ->
                            val time: Long = metadata.creationTimeMillis
                            if (childListResult.prefixes.size > 0) createPlayCard(cardData["name"].toString(), cardData["objective"].toString(), folderStoRef.name, time, screenshotsRef)
                            else createPlayCard(cardData["name"].toString(), cardData["objective"].toString(), folderStoRef.name, time)
                        }
                    }

                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "There was an error deleting the tag, please try again later.", Toast.LENGTH_SHORT).show()
        }
    }

    // The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the NoticeDialogFragment.NoticeDialogListener interface
    override fun onDialogPositiveClick(dialog: DialogFragment, inflatedView: LinearLayout) {
        // Get textfields
        val modalName: TextInputLayout = inflatedView.findViewById(R.id.add_play_modal_name_ilayout)
        val modalObjective: TextInputLayout = inflatedView.findViewById(R.id.add_play_modal_objective_ilayout)

        // Values
        val name: String = modalName.editText?.text.toString()
        val objective: String = modalObjective.editText?.text.toString()

        // Create hashmap
        val playInfoHM: HashMap<String, String> = HashMap()
        playInfoHM["name"] = name
        playInfoHM["objective"] = objective

        createPlayCard(name, objective, "", Calendar.getInstance().timeInMillis)

        val intent = Intent(this, ProfileActivity::class.java)
        intent.putExtra("addSessionFlag", true)
        intent.putExtra("playInfoHM", playInfoHM)
        startActivity(intent)
        finish()
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        // User touched the dialog's negative button
        // Close dialog
        dialog.dismiss()
    }

    private fun initToolbar() {
        val toolbar: Toolbar = findViewById(R.id.mscreen_toolbar)
        toolbar.title = "Spor"
        toolbar.title
        setSupportActionBar(findViewById<View>(R.id.mscreen_toolbar) as Toolbar)

        drawerLayout = findViewById<View>(R.id.mscreen_drawer_layout) as DrawerLayout
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
        val menuItem: MenuItem = navigationView.menu.getItem(0)
        menuItem.isChecked = true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_home -> {
                true
            }
            R.id.nav_profile -> {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                true
            }
            else -> true
        }
    }
}