package com.example.spor_tfg

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipDescription
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.DragEvent
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.spor_tfg.databinding.ActivityPaintBinding
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import petrov.kristiyan.colorpicker.ColorPicker
import petrov.kristiyan.colorpicker.ColorPicker.OnFastChooseColorListener
import java.io.OutputStream



class PaintActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityPaintBinding
    private var auth: FirebaseAuth = Firebase.auth
    private var storage: FirebaseStorage = Firebase.storage

    // in order to get the reference of the View
    lateinit var paint: DrawView
    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView

    // creating objects of type button
    lateinit var save: ImageButton
    lateinit var color: ImageButton
    lateinit var undo: ImageButton
    lateinit var bin: ImageButton
    lateinit var playersLinearLayout: LinearLayout
    private val db: FirebaseFirestore = Firebase.firestore

    private lateinit var teamsAL: ArrayList<Any>

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityPaintBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // getting the reference of the views from their ids
        paint = findViewById(R.id.draw_view)
        paint.background = ContextCompat.getDrawable(this@PaintActivity, R.drawable.football_field_horizontal)
        undo = findViewById<View>(R.id.btn_undo) as ImageButton
        save = findViewById<View>(R.id.btn_save) as ImageButton
        color = findViewById<View>(R.id.btn_color) as ImageButton
        bin = findViewById<View>(R.id.btn_bin) as ImageButton
        playersLinearLayout = findViewById(R.id.players_ll)
        navigationView = findViewById(R.id.navigation_view)

        // Integrate toolbar menu functionality
        initToolbar()
        navigationView.setNavigationItemSelectedListener(this@PaintActivity)

        // Preselect home element
        preselectToolbar()

        // Load player image buttons array to insert into the view
        loadPlayers()

        // Add undo image button functionality
        // Removes last stroke from the canvas
        undo.setOnClickListener {
            Log.d("myTag", "Undo button pressed")
            paint.undo()
        }

        // Add save button functionality
        // Saves current canvas to storage as a PNG
        save.setOnClickListener {
            val bmp = paint.save()

            // opening a OutputStream to write into the file
            val imageOutStream: OutputStream?
            val cv = ContentValues()

            // name of the file
            cv.put(MediaStore.Images.Media.DISPLAY_NAME, "drawing.png")

            // type of the file
            cv.put(MediaStore.Images.Media.MIME_TYPE, "image/png")

            // location of the file to be saved
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cv.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }

            // get the Uri of the file which is to be created in the storage
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
            try {
                // open the output stream with the above uri
                imageOutStream = contentResolver.openOutputStream(uri!!)

                // this method writes the files in storage
                bmp?.compress(Bitmap.CompressFormat.PNG, 100, imageOutStream)

                // close the output stream after use
                imageOutStream!!.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Add color button functionality
        // Saves current canvas to storage as a PNG
        color.setOnClickListener {
            val colorPicker = ColorPicker(this@PaintActivity)
            colorPicker.setOnFastChooseColorListener(object : OnFastChooseColorListener {
                override fun setOnFastChooseColorListener(position: Int, color: Int) {
                    // get the integer value of color
                    // selected from the dialog box and
                    // set it as the stroke color
                    paint.setColor(color)
                }

                override fun onCancel() {
                    colorPicker.dismissDialog()
                }
            }) // set the number of color columns
                // you want to show in dialog.
                .setColumns(5) // set a default color selected
                // in the dialog
                .setDefaultColorButton(Color.parseColor("#000000"))
                .show()
        }

        // pass the height and width of the custom view
        // to the init method of the DrawView object
        // pass the height and width of the custom view
        // to the init method of the DrawView object
        val vto = paint.viewTreeObserver
        vto.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                paint.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val width = paint.measuredWidth
                val height = paint.measuredHeight
                paint.init(height, width)
            }
        })

        // Allow drag into the canvas
        allowDragIntoCanvas()
    }

    private fun initToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = "Spor"
        toolbar.title
        setSupportActionBar(findViewById<View>(R.id.toolbar) as Toolbar)

        drawerLayout = findViewById<View>(R.id.drawer_layout) as DrawerLayout
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

    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadPlayers() {
        val playersRef = db.collection("user").document(auth.uid.toString())
        Log.d("UID", "User ID: ${auth.uid.toString()}")
        playersRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // Cast and ignore linter warnings
                    @Suppress("UNCHECKED_CAST")
                    // Get team array list from user/coach ID
                    teamsAL = document.data?.get("team") as ArrayList<Any>
                    for ((idx, playerAL) in teamsAL.withIndex()) {
                        @Suppress("UNCHECKED_CAST")
                        // Cast to HashMap else we can not iterate through each player key
                        val player: HashMap<Any, Any> = playerAL as HashMap<Any, Any>
                        for (key in player.keys) {
                            val playerValue = player[key]
                            if (key == "image") {
                               downloadImage(playerValue as String, idx)
                            }
                        }
                    }

                } else {
                    Log.d("NoDocument", "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d("Failure", "get failed with ", exception)
            }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun downloadImage(path: String, index: Int) {
        val ref = storage.getReferenceFromUrl(path)
        val oneMegabyte: Long = 1024 * 1024

        ref.getBytes(oneMegabyte).addOnSuccessListener {
            // Decode ByteArray so we can convert it to an image
            val bmp = BitmapFactory.decodeByteArray(it, 0, it.size)

            // Create the ImageView dynamically
            val imageButton = ImageButton(this@PaintActivity)
            imageButton.id = index

            // Setting image bitmaps
            val imageBitmap: Bitmap = Bitmap.createScaledBitmap(bmp, 150, 150, false)
            val popUpImageBitmap: Bitmap = Bitmap.createScaledBitmap(bmp, 500, 500, false)
            val roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(resources, imageBitmap)

            roundedBitmapDrawable.cornerRadius = 50.0f
            roundedBitmapDrawable.setAntiAlias(true)
            imageButton.setImageDrawable(roundedBitmapDrawable)
            imageButton.background = null

            createDragOperation(imageButton)
            clickFunc(imageButton, popUpImageBitmap)
            playersLinearLayout.addView(imageButton)
        }.addOnFailureListener {
            // Handle any errors
            Log.d("Download image error", it.toString())
        }
    }

    private fun clickFunc(imageButton: ImageButton, bitmap: Bitmap) {
        imageButton.setOnClickListener {
            val playerAL = teamsAL[imageButton.id]
            @Suppress("UNCHECKED_CAST")
            // Cast to HashMap else we can not iterate through each player key
            val player: HashMap<Any, Any> = playerAL as HashMap<Any, Any>

            /*for (key in player.keys) {

            }*/
            // Construct dialog
            val dialogBuilder = AlertDialog.Builder(this)
            val playerModalView: View = layoutInflater.inflate(R.layout.player_modal, null)
            dialogBuilder.setView(playerModalView)
            val dialog = dialogBuilder.create()
            dialog.show()

            // Fill in dialog
            val img = playerModalView.findViewById<ShapeableImageView>(R.id.popUpImageContainer)
            img.setImageBitmap(bitmap)
            val txt: TextView = playerModalView.findViewById(R.id.popup_title)
            txt.text = player["name"].toString()
            val popupPos: TextView = playerModalView.findViewById(R.id.popup_position)
            popupPos.text = player["position"].toString()
            val popupJerseyNo: TextView = playerModalView.findViewById(R.id.popup_jersey_no)
            popupJerseyNo.text = player["jersey_number"].toString()
            val btn: ImageView = playerModalView.findViewById<ImageView>(R.id.popup_close)
            btn.setOnClickListener {
                dialog.dismiss()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun createDragOperation(imageButton: ImageButton) {
        val tag: String = "player icon"
        imageButton.tag = tag

        imageButton.setOnLongClickListener {
            // Create a new ClipData.
            // This is done in two steps to provide clarity. The convenience method
            // ClipData.newPlainText() can create a plain text ClipData in one step.

            // Create a new ClipData.Item from the ImageView object's tag.
            val item = ClipData.Item(it.tag as? CharSequence)

            // Create a new ClipData using the tag as a label, the plain text MIME type, and
            // the already-created item. This creates a new ClipDescription object within the
            // ClipData and sets its MIME type to "text/plain".
            val dragData = ClipData(
                it.tag as? CharSequence,
                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                item)

            // Instantiate the drag shadow builder.
            val myShadow = View.DragShadowBuilder(imageButton)

            // Start the drag.
            it.startDragAndDrop(dragData,  // The data to be dragged
                myShadow,  // The drag shadow builder
                imageButton,      // No need to use local data
                0          // Flags (not currently used, set to 0)
            )

            imageButton.visibility = View.INVISIBLE
            true
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun allowDragIntoCanvas() {
        // Set the drag event listener for the View.
        paint.setOnDragListener { v, e ->

            // Player image
            val image = e.localState as View

            // Handles each of the expected events.
            when (e.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    // Determines if this View can accept the dragged data.
                    e.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                    true
                }
                DragEvent.ACTION_DRAG_ENTERED -> {
                    // Returns true; the value is ignored.
                    v.invalidate()
                    true
                }

                DragEvent.ACTION_DRAG_LOCATION -> {
                    // Unused method
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    // Returns true; the value is ignored.
                    image.visibility = View.VISIBLE
                    v.invalidate()
                    true
                }
                DragEvent.ACTION_DROP -> {
                    // Gets the item containing the dragged data.
                    val item: ClipData.Item = e.clipData.getItemAt(0)
                    // We make sure it anchors to the center of the image and not the top-left corner as the default implementation

                    if (image.parent == paint) {
                        // Check flag for animation here
                        Log.i("test","si es parent")
                    }

                    // Container of the draggable image
                    (image.parent as ViewGroup).removeView(image)

                    // Paint canvas
                    paint.addView(image)
                    image.animate()
                        .x(e.x)
                        .y(e.y)
                        .setDuration(700)
                        .start()
                    image.x = e.x - (image.width / 2)
                    image.y = e.y - (image.height / 2)




                    true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    image.visibility = View.VISIBLE
                    // Invalidates the view to force a redraw.
                    v.invalidate()
                    // Returns true; the value is ignored.
                    true
                }
                else -> {
                    // An unknown action type was received.
                    Log.e("DragDrop Example", "Unknown action type received by View.OnDragListener.")
                    false
                }
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_whiteboard -> {
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

