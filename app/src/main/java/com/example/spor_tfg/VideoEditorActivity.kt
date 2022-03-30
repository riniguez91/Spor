package com.example.spor_tfg

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.example.spor_tfg.databinding.ActivityVideoEditorBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage


class VideoEditorActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityVideoEditorBinding

    private var storage: FirebaseStorage = Firebase.storage
    private var auth: FirebaseAuth = Firebase.auth

    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView

    lateinit var videoView: VideoView
    lateinit var progressBar: ProgressBar
    lateinit var chooseVideoLL: LinearLayout

    lateinit var doc: HashMap<Any, Any>

    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_editor)

        binding = ActivityVideoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Navigation hooks
        navigationView = findViewById(R.id.video_editor_navigation_view)
        navigationView.setNavigationItemSelectedListener(this)

        // Video hooks
        videoView = findViewById(R.id.videoView)
        progressBar = findViewById(R.id.video_editor_pb)
        chooseVideoLL = findViewById(R.id.choose_video_ll)

        // Init toolbar
        initToolbar()

        // Preselect
        preselectToolbar()

        // Get team info
        doc =  (this.application as MyApp).getDocVar()

        // Load video click func
        loadVideoIconClickFunc()

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it!!.resultCode == Activity.RESULT_OK) {
                val videoURI = it.data!!.data
                // Upload video to firestore after calling edit video functionality
                println(videoURI)
            }
        }

    }

    private fun initVideo(videoName: String) {
        // Creating MediaController
        val mediaController: MediaController = MediaController(this)
        mediaController.setAnchorView(videoView)

        // Download video
        progressBar.visibility = View.VISIBLE
        storage.reference.child(auth.uid.toString()+ "/videos/" + videoName).downloadUrl.addOnSuccessListener {
            videoView.setMediaController(mediaController)
            videoView.canSeekBackward()
            videoView.canSeekForward()
            videoView.setVideoPath(it.toString())
            videoView.setOnPreparedListener {
                progressBar.visibility = View.INVISIBLE
                videoView.start()
            }
        }.addOnFailureListener {
            TODO("Handle errors here")
        }
    }

    private fun loadVideoIconClickFunc() {
        // Upload video
        findViewById<TextView>(R.id.video_editor_modal_upload).setOnClickListener {
            recordVideo()
        }

        // Download video
        findViewById<TextView>(R.id.video_editor_modal_load).setOnClickListener {
            loadVideo()
        }

    }

    private fun recordVideo() {
        val intent: Intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        activityResultLauncher.launch(intent)
    }

    @SuppressLint("CheckResult")
    private fun loadVideo() {
        val myItems = mutableListOf<String>()
        @Suppress("UNCHECKED_CAST")
        (doc["videos"] as HashMap<Any, Any>).forEach { myItems.add(it.value as String) }
        MaterialDialog(this).show {
            title(text = "Available videos")
            message(
                text = "Please select the video you wish to edit: "
            )
            listItemsSingleChoice(items = myItems) { dialog, index, text ->
                chooseVideoLL.visibility = View.INVISIBLE
                initVideo(text.toString())
            }
            positiveButton(text = "Confirm")
        }
    }

    private fun initToolbar() {
        val toolbar: Toolbar = findViewById(R.id.video_editor_toolbar)
        toolbar.title = "Spor"
        toolbar.title
        setSupportActionBar(findViewById<View>(R.id.video_editor_toolbar) as Toolbar)

        drawerLayout = findViewById<View>(R.id.video_editor_drawer_layout) as DrawerLayout
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
        val menuItem: MenuItem = navigationView.menu.getItem(2)
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
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.nav_video_editor -> {
                true
            }
            else -> true
        }
    }
}