package com.example.spor_tfg

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.*
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.children
import androidx.drawerlayout.widget.DrawerLayout
import com.afollestad.materialdialogs.MaterialDialog
import com.example.spor_tfg.databinding.ActivityPaintBinding
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.gson.Gson
import petrov.kristiyan.colorpicker.ColorPicker
import petrov.kristiyan.colorpicker.ColorPicker.OnFastChooseColorListener
import java.io.ByteArrayOutputStream
import java.util.*


class PaintActivity : AppCompatActivity() {  // NavigationView.OnNavigationItemSelectedListener

    private lateinit var binding: ActivityPaintBinding
    private var auth: FirebaseAuth = Firebase.auth
    private var storage: FirebaseStorage = Firebase.storage

    lateinit var myApp: MyApp

    // in order to get the reference of the View
    lateinit var paint: DrawView
    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView

    // creating objects of type button
    lateinit var savePlay: ShapeableImageView
    lateinit var color: ShapeableImageView
    lateinit var undo: ShapeableImageView
    lateinit var bin: ShapeableImageView

    lateinit var singleAnim: ShapeableImageView
    lateinit var multiAnim: ShapeableImageView
    lateinit var done: ShapeableImageView
    lateinit var animationColorButton: ShapeableImageView
    lateinit var playAnim: ShapeableImageView
    lateinit var saveAnim: ShapeableImageView

    private var trainingEquipmentFlag: Boolean = false
    lateinit var cone: ShapeableImageView
    lateinit var football: ShapeableImageView
    lateinit var dummy: ShapeableImageView
    lateinit var goalie: ShapeableImageView
    lateinit var field: ShapeableImageView

    lateinit var playersLinearLayout: LinearLayout
    private val db: FirebaseFirestore = Firebase.firestore

    // Animation variables
    private var singleAnimFlag: Boolean = false
    private var multiAnimFlag: Boolean = false
    private lateinit var animPath: Path
    // animationPaths[frame] = framePaths
    private var animationPaths: HashMap<Int, HashMap<Int, Path>> = HashMap<Int, HashMap<Int, Path>>()
    private var framePaths: HashMap<Int, Path> = HashMap()
    private var frameNo: Int = 1
    private var animationColor: Int = Color.WHITE

    // Points to be serialized into the database
    private var points: HashMap<Int, HashMap<Int, ArrayList<Pair<Float, Float>>>> = HashMap()

    var queryCount: Int = 0
    var teamSize: Int = 0

    // Color array
    val colors: ArrayList<String> = ArrayList()

    private lateinit var doc: HashMap<Any, Any>
    private lateinit var team: HashMap<Any, Any>

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityPaintBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // getting the reference of the views from their ids
        paint = findViewById(R.id.draw_view)
        paint.background = ContextCompat.getDrawable(this@PaintActivity, R.drawable.football_field_horizontal_2)
        undo = findViewById<View>(R.id.btn_undo) as ShapeableImageView
        savePlay = findViewById<View>(R.id.btn_save_play) as ShapeableImageView
        color = findViewById<View>(R.id.btn_color) as ShapeableImageView
        bin = findViewById<View>(R.id.btn_bin) as ShapeableImageView

        singleAnim = findViewById<View>(R.id.btn_single_anim) as ShapeableImageView
        multiAnim = findViewById<View>(R.id.btn_multi_anim) as ShapeableImageView
        done = findViewById<View>(R.id.btn_multi_anim_done) as ShapeableImageView
        animationColorButton = findViewById<View>(R.id.btn_animation_color) as ShapeableImageView
        playAnim = findViewById<View>(R.id.btn_play_anim) as ShapeableImageView
        saveAnim = findViewById<View>(R.id.btn_save_anim) as ShapeableImageView

        cone = findViewById<View>(R.id.btn_training_cone) as ShapeableImageView
        football = findViewById<View>(R.id.btn_football) as ShapeableImageView
        dummy = findViewById<View>(R.id.btn_dummy) as ShapeableImageView
        goalie = findViewById<View>(R.id.btn_goal_net) as ShapeableImageView
        field = findViewById<View>(R.id.btn_change_field) as ShapeableImageView

        playersLinearLayout = findViewById(R.id.players_ll)
        navigationView = findViewById(R.id.navigation_view)

        // Integrate toolbar menu functionality
        initToolbar()
        // navigationView.setNavigationItemSelectedListener(this@PaintActivity)

        // Preselect home element
        // preselectToolbar()

        // Get team info
        myApp = this.application as MyApp
        doc = myApp.getDocVar()

        // Load player image buttons array to insert into the view
        loadPlayers()

        // Set color array for colorpicker
        setColorPickerColors()

        // Add undo image button functionality
        // Removes last stroke from the canvas
        undo.setOnClickListener {
            Log.d("myTag", "Undo button pressed")
            paint.undo()
        }

        // Add save button functionality
        // Saves current canvas to storage as a PNG
        savePlay.setOnClickListener {
            val dialog = MaterialDialog(this).show {
                title(text = "Confirm action")
                message(text = "Are you sure you want save the following play?")
                positiveButton(text = "Confirm") { dialog ->
                    // Play name
                    val calendar: Calendar = Calendar.getInstance()
                    val playName =
                        "play_${calendar.get(Calendar.HOUR_OF_DAY)}${calendar.get(Calendar.MINUTE)}${
                            calendar.get(Calendar.SECOND)
                        }"

                    val baos = ByteArrayOutputStream()

                    // Get view
                    val bmp = paint.save()
                    val canvas = Canvas(bmp!!)
                    paint.draw(canvas)

                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val data = baos.toByteArray()

                    // File path
                    val playRef =
                        storage.reference.child("${auth.uid.toString()}/plays/${intent.getStringExtra("session_name")}/screenshots/$playName.jpeg")
                    val uploadTask = playRef.putBytes(data) // putBytes(data, metadata)
                    uploadTask.addOnFailureListener {
                        // Handle unsuccessful uploads
                        Toast.makeText(
                            this@PaintActivity,
                            "Something went wrong while uploading image, please try again later.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }.addOnSuccessListener { taskSnapshot ->
                        Toast.makeText(this@PaintActivity, "Uploaded successfully!", Toast.LENGTH_SHORT)
                            .show()
                        // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                        // ...
                    }
                }
                negativeButton(android.R.string.cancel)
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
                    colorPicker.setDefaultColorButton(color)
                }

                override fun onCancel() {
                    colorPicker.dismissDialog()
                }
            }) // set the number of color columns
                // you want to show in dialog.
                .setColumns(5) // set a default color selected
                // in the dialog
                .setColors(colors)
                .setRoundColorButton(true)
                // .setDefaultColorButton(Color.parseColor("#000000"))
                .show()
        }

        bin.setOnClickListener {
            paint.clearBoard()
            animationPaths.clear()
            frameNo = 1
        }

        singleAnim.setOnClickListener {
            when {
                // We make sure only one flag is active at a time
                multiAnimFlag -> {
                    deactivateMultiAnim()
                    framePaths = HashMap()
                    if (animationPaths.isNotEmpty()) {
                        frameNo++
                    }
                    singleAnimFlag = true
                    singleAnim.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
                }
                // Unselect single animation mode
                singleAnimFlag -> {
                    deactivateSingleAnim()
                }
                // Select single animation mode
                else -> {
                    singleAnimFlag = true
                    singleAnim.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
                }
            }
        }

        multiAnim.setOnClickListener {
            when {
                // We make sure only one flag is active at a time
                singleAnimFlag -> {
                    deactivateSingleAnim()
                    multiAnimFlag = true
                    multiAnim.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
                }
                // Unselect multi animation mode
                multiAnimFlag -> {
                    deactivateMultiAnim()
                }
                // Select multi animation mode
                else -> {
                    multiAnimFlag = true
                    multiAnim.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
                }
            }
        }

        done.setOnClickListener {
            if (framePaths.isNotEmpty()) {
                frameNo++
                Toast.makeText(this, "Incremented frame number to: $frameNo", Toast.LENGTH_LONG).show()
            }
            else {
                Toast.makeText(this, "You have not added any animations", Toast.LENGTH_LONG).show()
            }
            framePaths = HashMap()
            // Unselect animation flags since we are playing the animations
            singleAnimFlag = false
            singleAnim.setBackgroundColor(Color.TRANSPARENT)
            multiAnimFlag = false
            multiAnim.setBackgroundColor(Color.TRANSPARENT)
        }

        animationColorButton.setOnClickListener {
            val aColorPicker = ColorPicker(this@PaintActivity)

            aColorPicker
                .setColumns(5) // set a default color selected
                // in the dialog
                .setColors(colors)
                // .setDefaultColorButton(Color.WHITE)
                .setRoundColorButton(true)
                // .setColorButtonTickColor(Color.BLACK)
                .setOnFastChooseColorListener(object : OnFastChooseColorListener {
                override fun setOnFastChooseColorListener(position: Int, color: Int) {
                    // get the integer value of color
                    // selected from the dialog box and
                    // set it as the stroke color
                    animationColor = color
                    aColorPicker.setDefaultColorButton(color)
                }

                override fun onCancel() {
                    aColorPicker.dismissDialog()
                }
            }).show()
        }

        playAnim.setOnClickListener {
            // {frame: {list_of_paths}, frame2: {list_of_paths}}
            // {1: {path0, path1 ...}, 2: {path0, path1}
            // {1: {{image_button1_id: path}, {image_button2_id: path}, ...}, ...}
            // animationPaths: HashMap<Int, HashMap<Int, Path>>

            deactivateSingleAnim()
            deactivateMultiAnim()

            for ((frame, paths) in animationPaths) {
                // Do something with frame (such as paint it in each animation path)
                for ((id, path) in paths) {
                    // val id: Int = anim
                    // val path: Path? = animationPaths[frame]?.get(id)
                    val image: View = paint.findViewById(id)
                    val dst: Path = Path()

                    // Offset the path such that the image seems to follow the animation from the middle
                    // Since it is actually following another path that allows for this
                    path.offset(image.width / 2.toFloat()*-1, image.height / 2.toFloat()*-1, dst)
                    val pathAnimator: ValueAnimator = ObjectAnimator.ofFloat(image, "x", "y", dst)
                    pathAnimator
                        .setDuration(5000)
                        .startDelay = ((frame-1) * 5000).toLong()
                    pathAnimator.start()
                }
            }
        }

        saveAnim.setOnClickListener {
            if (animationPaths.isEmpty()) {
                val dialog = MaterialDialog(this).show {
                    title(text = "Error")
                    message(text = "There are no animations to upload, try again by adding some to the canvas!")
                    positiveButton(text = "Confirm")
                    negativeButton(text = "Cancel")
                }
            }
            else {
                val dialog = MaterialDialog(this).show {
                    title(text = "Confirm action")
                    message(text = "Are you sure you want save the following animations?")
                    positiveButton(text = "Confirm") { dialog ->
                        // File path
                        val playRef =
                            storage.reference.child("${auth.uid.toString()}/plays/${intent.getStringExtra("session_name")}/animations")

                        val jsonString: String = Gson().toJson(points, HashMap::class.java)
                        val uploadTask = playRef.putBytes(jsonString.toByteArray())
                        uploadTask.addOnFailureListener {
                            // Handle unsuccessful uploads
                            Toast.makeText(
                                this@PaintActivity,
                                "Something went wrong while uploading the animation/s, please try again later.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.addOnSuccessListener { taskSnapshot ->
                            Toast.makeText(this@PaintActivity, "Uploaded successfully!", Toast.LENGTH_SHORT)
                                .show()
                            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                            // ...
                        }
                    }
                    negativeButton(android.R.string.cancel)
                }
            }
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

    @Suppress("UNCHECKED_CAST")
    private fun getAnimations() {
        val deserializedPoints = intent.getSerializableExtra("animations") as HashMap<*, *>
        val listOfIDS: ArrayList<Int> = ArrayList()
        if (!deserializedPoints.isNullOrEmpty()) {
            deserializedPoints as HashMap<String, HashMap<String, ArrayList<HashMap<String, Float>>>>
            // Paint animations on screen
            deserializedPoints.forEach { (frame, paths) ->
                // Do something with frame (such as paint it in each animation path)
                println("Paths:: $paths")
                playersLinearLayout.children.forEach {
                    println("Child id is:: ${it.id}")
                }
                for ((id, path) in paths) {
                    val dPath: Path = Path()
                    path.forEachIndexed { index, pair ->
                        println("Index of path is:: $id")
                        // Simulate user draggin with the given coordinates
                        when (index) {
                            0 -> dPath.moveTo(pair["first"]!!, pair["second"]!!)
                            path.lastIndex -> dPath.addCircle(pair["first"]!!, pair["second"]!!, 8f, Path.Direction.CW)
                            else -> dPath.lineTo(pair["first"]!!, pair["second"]!!)
                        }
                    }

                    // Add path and image to paint
                    val fp = Stroke(animationColor, 3, dPath)
                    paint.animPaths.add(fp)

                    // Keep track of inserted IDs
                    if (!listOfIDS.contains(Integer.parseInt(id))) {
                        // Insert images with animations into the canvas
                        val image: ImageButton = playersLinearLayout.findViewById(Integer.parseInt(id))
                        (image.parent as ViewGroup).removeView(image)
                        paint.addView(image)
                        // Place starting coordinates
                        println("Image width:: ${image.width}")
                        println("Image height:: ${image.height}")
                        image.x = path[0]["first"]!! - (163 / 2)
                        image.y = path[0]["second"]!! - (155 / 2)
                        // Make sure we add it to the list of unique IDs
                        listOfIDS.add(Integer.parseInt(id))
                    }

                    // Add paths to animation path so the user can click on the play button and the animation will start
                    val framePath = animationPaths[Integer.parseInt(frame)]
                    if (!framePath.isNullOrEmpty()) {
                        framePath[Integer.parseInt(id)] = dPath
                    }
                    else {
                        animationPaths[Integer.parseInt(frame)] = HashMap<Int, Path>().apply {
                            this[Integer.parseInt(id)] = dPath
                        }
                    }

                }
            }
        }
    }

    private fun deactivateSingleAnim() {
        singleAnimFlag = false
        singleAnim.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun deactivateMultiAnim() {
        multiAnimFlag = false
        multiAnim.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun setColorPickerColors() {
        colors.add("#FFFFFF") // White
        colors.add("#00171f") // Rich Black
        colors.add("#003459") // Prussian Blue
        colors.add("#007ea7") // Celadon Blue
        colors.add("#00a8e8") // Carolina Blue
        colors.add("#61210f") // Liver Organ
        colors.add("#EA2B1F") // Red Pigment
        colors.add("#edae49") // Sunray
        colors.add("#F9DF74") // Jasmine
        colors.add("#f9edcc") // Cornsilk
        colors.add("#79B791") // Dark Sea Green
        colors.add("#D45113") // Burnt Orange
        colors.add("#f9a03f") // Deep Saffron
        colors.add("#F8DDA4") // Peach
        colors.add("#B0FF92") // Mint green
    }

    private fun initToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = "Spor"
        toolbar.title
        setSupportActionBar(findViewById<View>(R.id.toolbar) as Toolbar)

        /*drawerLayout = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        val toggle: ActionBarDrawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout as DrawerLayout?,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        (drawerLayout as DrawerLayout?)?.addDrawerListener(toggle)
        toggle.syncState()*/
    }

    private fun preselectToolbar() {
        val menuItem: MenuItem = navigationView.menu.getItem(0)
        menuItem.isChecked = true
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadPlayers() {
        // Cast and ignore linter warnings
        @Suppress("UNCHECKED_CAST")
        team = doc["team"] as HashMap<Any, Any>

        for ((idx, player) in team) {
            @Suppress("UNCHECKED_CAST")
            player as HashMap<Any, Any>
            if (player["status"] == "available" || player["status"] == "partially available") {
                teamSize++
                downloadImage(player["image"]  as String, idx.toString().toInt())
            }
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
            val imageBitmap: Bitmap = Bitmap.createScaledBitmap(bmp, 115, 115, false)
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
        }.addOnCompleteListener {
            queryCount++
            println("Query count:: $queryCount || Size:: $teamSize")
            if (queryCount == teamSize) getAnimations()
        }
    }

    private fun clickFunc(imageButton: ImageButton, bitmap: Bitmap) {
        imageButton.setOnClickListener {
            println("Team: $team")
            @Suppress("UNCHECKED_CAST")
            // Cast to HashMap else we can not iterate through each player key
            val player: HashMap<*, *>? = team[imageButton.id.toString()] as HashMap<*, *>?

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
            txt.text = player?.get("name").toString()
            val popupPos: TextView = playerModalView.findViewById(R.id.popup_position)
            popupPos.text = player?.get("position").toString()
            val popupJerseyNo: TextView = playerModalView.findViewById(R.id.popup_jersey_no)
            popupJerseyNo.text = player?.get("jersey_number").toString()

            // Jersey switch
            val jerseys: Array<Int> = arrayOf(R.drawable.jersey_1, R.drawable.jersey_2, R.drawable.jersey_3, R.drawable.jersey_4, R.drawable.jersey_5, R.drawable.jersey_6)
            val jerseyImage: ShapeableImageView = playerModalView.findViewById(R.id.jersey_image)
            var currJerseyImage: Int = 0
            jerseyImage.setOnClickListener {
                currJerseyImage++
                // Makes sure we never get out of array bounds
                // Resets it to 0 if that's the case
                currJerseyImage %= jerseys.size
                jerseyImage.setImageDrawable(ContextCompat.getDrawable(this@PaintActivity, jerseys[currJerseyImage]))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun createDragOperation(imageButton: ImageButton) {
        val tag: String = "player icon"
        imageButton.tag = tag

        // Player image drag
        imageButton.setOnLongClickListener {
            val item = ClipData.Item(it.tag as? CharSequence)

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

        // Cone drag
        coneDrag()

        // Football drag
        footballDrag()

        // Dummy drag
        dummyDrag()

        // Goalie drag()
        goalieDrag()

        // Change football field
        changeFieldClick()
    }

    private fun changeFieldClick() {
        // Get the football fields array
        val fieldsArray: Array<Int> = arrayOf(R.drawable.football_field_horizontal_2, R.drawable.football_field_horizontal, R.drawable.football_field_horizontal_3)
        var currImage: Int = 0
        field.setOnClickListener {
            currImage++
            // Makes sure we never get out of array bounds
            // Resets it to 0 if that's the case
            currImage %= fieldsArray.size
            paint.background = ContextCompat.getDrawable(this@PaintActivity, fieldsArray[currImage])
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun coneDrag() {
        val tag: String = "cone"
        cone.tag = tag

        cone.setOnLongClickListener {
            trainingEquipmentFlag = true
            deactivateSingleAnim()
            deactivateMultiAnim()
            val item = ClipData.Item(it.tag as? CharSequence)

            val dragData = ClipData(
                it.tag as? CharSequence,
                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                item)

            // Instantiate the drag shadow builder.
            val myShadow = View.DragShadowBuilder(cone)

            // Start the drag.
            it.startDragAndDrop(dragData,  // The data to be dragged
                myShadow,  // The drag shadow builder
                cone,      // No need to use local data
                0          // Flags (not currently used, set to 0)
            )

            true
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun footballDrag() {
        val tag: String = "football"
        football.tag = tag

        football.setOnLongClickListener {
            trainingEquipmentFlag = true
            deactivateSingleAnim()
            deactivateMultiAnim()
            val item = ClipData.Item(it.tag as? CharSequence)

            val dragData = ClipData(
                it.tag as? CharSequence,
                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                item)

            // Instantiate the drag shadow builder.
            val myShadow = View.DragShadowBuilder(football)

            // Start the drag.
            it.startDragAndDrop(dragData,  // The data to be dragged
                myShadow,  // The drag shadow builder
                football,      // No need to use local data
                0          // Flags (not currently used, set to 0)
            )

            true
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun dummyDrag() {
        val tag: String = "dummy"
        dummy.tag = tag

        dummy.setOnLongClickListener {
            trainingEquipmentFlag = true
            deactivateSingleAnim()
            deactivateMultiAnim()
            val item = ClipData.Item(it.tag as? CharSequence)

            val dragData = ClipData(
                it.tag as? CharSequence,
                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                item)

            // Instantiate the drag shadow builder.
            val myShadow = View.DragShadowBuilder(dummy)

            // Start the drag.
            it.startDragAndDrop(dragData,  // The data to be dragged
                myShadow,  // The drag shadow builder
                dummy,      // No need to use local data
                0          // Flags (not currently used, set to 0)
            )

            true
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun goalieDrag() {
        val tag: String = "goalie"
        goalie.tag = tag

        goalie.setOnLongClickListener {
            trainingEquipmentFlag = true
            deactivateSingleAnim()
            deactivateMultiAnim()
            val item = ClipData.Item(it.tag as? CharSequence)

            val dragData = ClipData(
                it.tag as? CharSequence,
                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                item)

            // Instantiate the drag shadow builder.
            val myShadow = View.DragShadowBuilder(goalie)

            // Start the drag.
            it.startDragAndDrop(dragData,  // The data to be dragged
                myShadow,  // The drag shadow builder
                goalie,      // No need to use local data
                0          // Flags (not currently used, set to 0)
            )

            true
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun allowDragIntoCanvas() {
        // Set the drag event listener for the View.
        paint.setOnDragListener { v, e ->

            // Image being dragged
            val image = e.localState as View

            // Handles each of the expected events.
            when (e.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    // Determines if this View can accept the dragged data.
                    e.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)

                    // Get starting coordinates for the animation path
                    if ((singleAnimFlag || multiAnimFlag) && image.parent == paint) {
                        animPath = Path()
                        animPath.moveTo(e.x, e.y)
                        // Add to auxiliary serialized hashmap
                        // Check if there is a hashmap associated to a frame
                        if (points[frameNo].isNullOrEmpty()) {
                            points[frameNo] = HashMap<Int, ArrayList<Pair<Float, Float>>>().apply {
                                this[image.id] = ArrayList<Pair<Float, Float>>().apply{
                                    this.add(Pair(e.x, e.y))
                                }
                            }
                        }
                        // No need to create hashmap again
                        else {
                            points[frameNo]?.set(image.id, ArrayList<Pair<Float, Float>>().apply{
                                this.add(Pair(e.x, e.y))
                            })
                        }
                        v.invalidate()
                    }
                    true
                }
                DragEvent.ACTION_DRAG_ENTERED -> {
                    // Returns true; the value is ignored.
                    v.invalidate()
                    true
                }
                DragEvent.ACTION_DRAG_LOCATION -> {
                    if (singleAnimFlag || multiAnimFlag) {
                        animPath.lineTo(e.x, e.y)
                        // Add to auxiliary serialized hashmap
                        points[frameNo]?.get(image.id)?.add(Pair(e.x, e.y))
                        v.invalidate()
                    }
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    // Returns true; the value is ignored.
                    image.visibility = View.VISIBLE
                    v.invalidate()
                    true
                }
                DragEvent.ACTION_DROP -> {
                    // We make sure it anchors to the center of the image and not the top-left corner as the default implementation
                    val x = e.x - (image.width / 2)
                    val y = e.y - (image.height / 2)

                    if (image.parent == paint) {
                        // Check if its a simple drag & drop
                        if (singleAnimFlag || multiAnimFlag) {
                            // Add to auxiliary serialized hashmap
                            // Add to auxiliary serialized hashmap
                            points[frameNo]?.get(image.id)?.add(Pair(e.x, e.y))

                            // TODO("Create animPathClone by value since it is by reference right now")
                            val animPathClone: Path = animPath
                            animPathClone.addCircle(e.x, e.y, 8f, Path.Direction.CW)
                            val fp = Stroke(animationColor, 3, animPathClone)
                            paint.animPaths.add(fp)
                            insertAnimations(frameNo, image)
                            if (singleAnimFlag) {
                                frameNo++
                                framePaths = HashMap()
                            }
                        }
                            image.animate()
                                .x(x)
                                .y(y)
                                .setDuration(700)
                                .start()
                        }
                        // Else we know its an animation drawing
                        // else {
                    // }
                    else {
                        // Create a copy of the image and add it to the view (we can have multiple training equipment images of the same type)
                        if (image.tag == "cone" || image.tag == "football" || image.tag == "dummy" || image.tag == "goalie") {
                            val trainingEquipImg: ShapeableImageView = ShapeableImageView(this@PaintActivity)
                            image as ShapeableImageView
                            trainingEquipImg.setImageDrawable(image.drawable)
                            trainingEquipImg.x = x
                            trainingEquipImg.y = y
                            trainingEquipImg.setOnClickListener {
                                // TODO("Maybe show dialog to confirm user really wants to remove image view")
                                (trainingEquipImg.parent as ViewGroup).removeView(trainingEquipImg)
                            }
                            paint.addView(trainingEquipImg)
                        }
                        else {
                            // Container of the draggable image
                            (image.parent as ViewGroup).removeView(image)
                            // Paint canvas
                            image.x = x
                            image.y = y
                            paint.addView(image)
                        }
                    }

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

    private fun insertAnimations(frame: Int, image: View) {
        // {frame: {list_of_paths}, frame2: {list_of_paths}}
        // {1: {path0, path1 ...}, 2: {path0, path1}
        // {1: {{image_button1_id, path}, {image_button2_id, path}, ...}, ...}
        framePaths[image.id] = animPath
        animationPaths[frame] = framePaths
        Toast.makeText(this, "Added frame on frame number: $frameNo", Toast.LENGTH_SHORT).show()
        animPath = Path()

    }

    /*override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_whiteboard -> {
                true
            }
            R.id.nav_profile -> {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.nav_video_editor -> {
                val intent = Intent(this, VideoEditorActivity::class.java)
                startActivity(intent)
                true
            }
            else -> true
        }
    }*/
}

