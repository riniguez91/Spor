package com.example.spor_tfg

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.shapes.Shape
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.afollestad.materialdialogs.DialogBehavior
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.ModalDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.example.spor_tfg.databinding.ActivityVideoEditorBinding
import com.google.android.gms.tasks.Task
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.shape.Shapeable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import com.gowtham.library.utils.LogMessage
import com.gowtham.library.utils.TrimVideo
import org.w3c.dom.Text
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream


class VideoEditorActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityVideoEditorBinding

    private var storage: FirebaseStorage = Firebase.storage
    private var auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore

    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView

    lateinit var modalView: View

    // Buttons
    lateinit var btnSpotlight: ShapeableImageView
    lateinit var btnBlueCircle: ShapeableImageView
    lateinit var btnUpArrow: ShapeableImageView
    lateinit var btnPolygon: ShapeableImageView

    // Video view
    lateinit var editingActionsLL: LinearLayout
    lateinit var videoView: VideoView
    lateinit var tagsLL: LinearLayout
    lateinit var mediaMetadataRetriever: MediaMetadataRetriever
    lateinit var progressBar: ProgressBar
    lateinit var chooseVideoLL: LinearLayout
    lateinit var mainRL: RelativeLayout
    lateinit var frameButtons: LinearLayout
    lateinit var frameConfirmationBttn: Button
    lateinit var frameCancelBttn: Button
    lateinit var veDrawView: VEDrawView
    var videoLastPos: Int = 0
    lateinit var bmFrame: Bitmap
    lateinit var selectedTag: String
    lateinit var selectedFrame: Bitmap
    lateinit var videoFolder: String
    lateinit var uri: Uri

    // Frame editing flags
    var arrowFlag: Boolean = false
    var polygonFlag: Boolean = false

    // Tags
    lateinit var loadedTagsLL: LinearLayout
    lateinit var atkLabel: TextView
    lateinit var defLabel: TextView
    lateinit var tiLabel: TextView
    lateinit var hbLabel: TextView
    lateinit var corLabel: TextView
    lateinit var gkLabel: TextView
    lateinit var fouLabel: TextView
    lateinit var fkLabel: TextView
    lateinit var pyLabel: TextView
    lateinit var offLabel: TextView
    lateinit var goaLabel: TextView
    lateinit var dgoaLabel: TextView
    lateinit var ycarLabel: TextView
    lateinit var rcarLabel: TextView

    lateinit var doc: HashMap<Any, Any>

    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("InflateParams", "CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_editor)

        binding = ActivityVideoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        modalView = layoutInflater.inflate(R.layout.video_editor_tag_modal, null)

        // Navigation hooks
        navigationView = findViewById(R.id.video_editor_navigation_view)
        navigationView.setNavigationItemSelectedListener(this)

        // Drag & Drop hooks
        btnSpotlight = findViewById(R.id.btn_spotlight)
        btnBlueCircle = findViewById(R.id.btn_blue_circle)
        btnUpArrow = findViewById(R.id.btn_up_arrow)
        btnPolygon = findViewById(R.id.btn_polygon)

        // Video hooks
        editingActionsLL = findViewById(R.id.editing_actions_ll)
        videoView = findViewById(R.id.videoView)
        tagsLL = findViewById(R.id.video_editor_tags_ll)
        progressBar = findViewById(R.id.video_editor_pb)
        chooseVideoLL = findViewById(R.id.choose_video_ll)
        mainRL = findViewById(R.id.video_editor_main_rl)
        frameButtons = findViewById(R.id.video_editor_frame_buttons)
        frameConfirmationBttn = findViewById(R.id.video_editor_frame_confirmation)
        frameCancelBttn = findViewById(R.id.video_editor_frame_cancel)
        veDrawView = findViewById(R.id.ve_draw_view)

        // Tag hooks
        loadedTagsLL = findViewById(R.id.loaded_tags_ll)
        atkLabel = findViewById(R.id.video_editor_atk_label)
        defLabel = findViewById(R.id.video_editor_def_label)
        tiLabel = findViewById(R.id.video_editor_ti_label)
        hbLabel = findViewById(R.id.video_editor_hb_label)
        corLabel = findViewById(R.id.video_editor_cor_label)
        gkLabel = findViewById(R.id.video_editor_gk_label)
        fouLabel = findViewById(R.id.video_editor_fou_label)
        fkLabel = findViewById(R.id.video_editor_fk_label)
        pyLabel = findViewById(R.id.video_editor_py_label)
        offLabel = findViewById(R.id.video_editor_off_label)
        goaLabel = findViewById(R.id.video_editor_goa_label)
        dgoaLabel = findViewById(R.id.video_editor_dgoa_label)
        ycarLabel = findViewById(R.id.video_editor_ycar_label)
        rcarLabel = findViewById(R.id.video_editor_rcar_label)

        // Init toolbar
        initToolbar()

        // Preselect
        preselectToolbar()

        // Get team info
        doc =  (this.application as MyApp).getDocVar()

        // Load video click func
        loadVideoIconClickFunc()

        // Add tags click func
        tagsClickFunc()

        //Kotlin
        val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK && it.data != null) {
                // Video URI
                uri = Uri.parse(TrimVideo.getTrimmedVideoPath(it.data))
                println("URI:: $uri")

                MaterialDialog(this).show {
                    input(hint = "Video name...") { dialog, text ->
                        // Video folder path name
                        videoFolder = text.toString().lowercase()
                        // Video name
                        val videoName: String = text.toString().lowercase() + ".mp4"
                        val idx: Int = (doc["videos"] as HashMap<*, *>).size

                        // Update field in user doc
                        db.collection("user").document(auth.uid.toString()).update(
                            mapOf(
                                "videos.${idx}" to videoName
                            )
                        )

                        // Upload video to firestore
                        val file = Uri.fromFile(File(uri.toString()))
                        val videoRef = storage.reference.child("${auth.uid.toString()}/videos/$videoName")
                        val uploadTask = videoRef.putFile(file)
                        uploadTask.addOnFailureListener{
                            Toast.makeText(this@VideoEditorActivity, "There was an error uploading the video, please try again.", Toast.LENGTH_SHORT).show()
                        }.addOnSuccessListener {
                            Toast.makeText(this@VideoEditorActivity, "Uploaded successfully!", Toast.LENGTH_SHORT).show()
                            // Set video in videoView
                            initVideo("", uri)
                        }
                    }
                    title(text = "Enter video name")
                    positiveButton(text = "Submit")
                }
            } else
                LogMessage.v("videoTrimResultLauncher data is null")
        }

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it!!.resultCode == Activity.RESULT_OK) {
                val videoURI = it.data!!.data
                // Edit video functionality
                TrimVideo.activity(videoURI.toString())
                    .setHideSeekBar(true)
                    .start(this, startForResult)
            }
        }

        // Spotlight button func
        spotlight()

        // Blue botton circle func
        blueCircle()

        // Up arrow func
        upArrow()

        // Polygon func
        polygon()

        veDrawView.init()
        veDrawView.bringToFront()

        // Dimensions for the VEDrawView canvas
        /*val vto = paint.viewTreeObserver
        vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                paint.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val width = paint.measuredWidth
                val height = paint.measuredHeight
                paint.init(height, width)
            }
        })*/
    }

    private fun loadTags() {
        val framesRef = storage.reference.child("${auth.uid.toString()}/videos/${videoFolder}/")
        // Obtain list of tags
        framesRef.listAll().addOnSuccessListener { listResult ->
            // Iterate through them and get their metadata to load the tag on the app
            listResult.items.forEach{ item ->
                item.metadata.addOnSuccessListener {
                    addLoadedTag(it.getCustomMetadata("tag")!!, it.getCustomMetadata("time_int")!!.toInt())
                    // it.name TODO("This is the name of file")
                }
            }
        }.addOnFailureListener {

        }
    }

    private fun changeArrowFlagStatus(flag: Boolean, color: Int) {
        arrowFlag = flag
        veDrawView.setArrowVal(flag)
        btnUpArrow.setBackgroundColor(color)
    }

    private fun changePolygonFlagStatus(flag: Boolean, color: Int) {
        polygonFlag = flag
        veDrawView.setPolygonVal(flag)
        btnPolygon.setBackgroundColor(color)
    }

    private fun upArrow() {
        btnUpArrow.setOnClickListener {
            when {
                // We make sure only one flag is active at a time
                polygonFlag -> {
                    changePolygonFlagStatus(false, Color.TRANSPARENT)
                    changeArrowFlagStatus(true, Color.WHITE)
                    if (veDrawView.polygonInserted) {
                        // No new polygons will be added for this shape
                        veDrawView.polygonInserted = false
                        // Increment polygonNo since the current polygon is finished
                        veDrawView.polygonNo++
                    }
                }
                arrowFlag -> {
                    changeArrowFlagStatus(false, Color.TRANSPARENT)
                }
                else -> {
                    changeArrowFlagStatus(true, Color.WHITE)
                }
            }
        }

    }

    private fun polygon() {
        btnPolygon.setOnClickListener {
            when {
                // We make sure only one flag is active at a time
                arrowFlag -> {
                    changeArrowFlagStatus(false, Color.TRANSPARENT)
                    changePolygonFlagStatus(true, Color.WHITE)
                }
                polygonFlag -> {
                    changePolygonFlagStatus(false, Color.TRANSPARENT)
                }
                else -> {
                    changePolygonFlagStatus(true, Color.WHITE)
                }
            }

            if (veDrawView.polygonInserted) {
                // No new polygons will be added for this shape
                veDrawView.polygonInserted = false
                // Increment polygonNo since the current polygon is finished
                veDrawView.polygonNo++
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun blueCircle() {
        val tag: String = "blue circle"
        btnBlueCircle.tag = tag

        btnBlueCircle.setOnLongClickListener {
            val item = ClipData.Item(it.tag as? CharSequence)

            val dragData = ClipData(
                it.tag as? CharSequence,
                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                item)

            // Create bigger shadow image (actual size of what will be dropped on the canvas)
            val bigBlueCircle: ShapeableImageView = findViewById(R.id.big_blue_circle)
            // bigBlueCircle.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.blue_circle))
            bigBlueCircle.layoutParams = RelativeLayout.LayoutParams(300, 300)

            // Instantiate the drag shadow builder.
            val myShadow = View.DragShadowBuilder(bigBlueCircle)
            /*Toast.makeText(this, "Height: ${myShadow.view.height} || Width: ${myShadow.view.width}\n" +
                    "LayoutParams Height: ${myShadow.view.layoutParams.height} || LayoutParams Width: ${myShadow.view.layoutParams.width}",
                Toast.LENGTH_LONG).show()*/

            // Start the drag.
            it.startDragAndDrop(dragData,  // The data to be dragged
                myShadow,  // The drag shadow builder
                btnBlueCircle,      // No need to use local data
                0          // Flags (not currently used, set to 0)
            )

            true
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun spotlight() {
        val tag: String = "spotlight"
        btnSpotlight.tag = tag

        btnSpotlight.setOnLongClickListener {
            val item = ClipData.Item(it.tag as? CharSequence)

            val dragData = ClipData(
                it.tag as? CharSequence,
                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                item)

            // Create bigger shadow image (actual size of what will be dropped on the canvas)
            val bigSpotlight: ShapeableImageView = findViewById(R.id.big_spotlight)
            bigSpotlight.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.spotlight_v3))
            bigSpotlight.layoutParams = RelativeLayout.LayoutParams(600, 600)

            // Instantiate the drag shadow builder.
            val myShadow = View.DragShadowBuilder(bigSpotlight)
            /*Toast.makeText(this, "Height: ${myShadow.view.height} || Width: ${myShadow.view.width}\n" +
                    "LayoutParams Height: ${myShadow.view.layoutParams.height} || LayoutParams Width: ${myShadow.view.layoutParams.width}",
                Toast.LENGTH_LONG).show()*/

            // Start the drag.
            it.startDragAndDrop(dragData,  // The data to be dragged
                myShadow,  // The drag shadow builder
                btnSpotlight,      // No need to use local data
                0          // Flags (not currently used, set to 0)
            )

            true
        }

    }

    private fun tagsClickFunc() {
        val textViewArray = arrayOf<TextView>(atkLabel, defLabel, tiLabel, hbLabel, corLabel, gkLabel, fouLabel, fkLabel, pyLabel, offLabel,
                                              goaLabel, dgoaLabel, ycarLabel, rcarLabel)

        for (tv in textViewArray) {
            tv.setOnClickListener {
                // Pause videoView
                videoView.pause()

                selectedTag = tv.text.toString()

                // Get time(returned in ms) and insert it into tags
                videoLastPos = videoView.currentPosition
                modalView.findViewById<TextView>(R.id.tag_modal_text).text = tv.text
                val mins: Int = (videoView.currentPosition / 1000) / 60
                val secs: Int = (videoView.currentPosition / 1000 ) % 60
                modalView.findViewById<TextView>(R.id.tag_modal_time).text = String.format("%02d:%02d", mins, secs)
                showCustomViewDialog(BottomSheet(LayoutMode.WRAP_CONTENT))
            }
        }


    }

    private fun showCustomViewDialog(dialogBehavior: DialogBehavior = ModalDialog) {
        val dialog = MaterialDialog(this, dialogBehavior).show {
            title(text = "Confirm action")
            message(text = "Are you sure you want to add the following tag")
            customView(R.layout.video_editor_tag_modal, modalView, scrollable = true, horizontalPadding = true)
            positiveButton(text = "Confirm") { dialog ->
                // Allow user to optionally edit frame by adding spotlights, arrows etc.
                editFrame()

                // Resume video
                videoView.start()
            }
            negativeButton(android.R.string.cancel)
        }
    }

    private fun editFrame() {
        changeToFrameEditing(true)
        // Create bitmap from
        println("Video last pos: $videoLastPos")
        bmFrame = mediaMetadataRetriever.getFrameAtTime((videoLastPos * 1000).toLong(), MediaMetadataRetriever.OPTION_CLOSEST)!!
        mainRL.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryDark))

        veDrawView.background = BitmapDrawable(resources, bmFrame)
        veDrawView.setOnDragListener { v, e ->

            // Image being dragged
            val image = e.localState as View

            // Handles each of the expected events.
            when (e.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    // Determines if this View can accept the dragged data.
                    e.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                    v.invalidate()
                    true
                }
                DragEvent.ACTION_DRAG_ENTERED -> {
                    // Returns true; the value is ignored.
                    v.invalidate()
                    true
                }
                DragEvent.ACTION_DRAG_LOCATION -> {
                    v.invalidate()
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    // Returns true; the value is ignored.
                    v.invalidate()
                    true
                }
                DragEvent.ACTION_DROP -> {
                    // Create a copy of the image and add it to the view (we can have multiple training equipment images of the same type)
                    val imgClone: ShapeableImageView = ShapeableImageView(this@VideoEditorActivity)
                    image as ShapeableImageView
                    // Set image attributes
                    imgClone.setImageDrawable(image.drawable)
                    imgClone.requestLayout()
                    imgClone.setOnClickListener {
                        // TODO("Maybe show dialog to confirm user really wants to remove image view")
                        (imgClone.parent as ViewGroup).removeView(imgClone)
                    }

                    // Set dimensions
                    if (image.tag == "spotlight") {
                        imgClone.alpha = 0.77f
                        imgClone.x = e.x - (2 * image.width)
                        imgClone.y = e.y - (2 * image.height)
                        imgClone.layoutParams = ViewGroup.LayoutParams(600, 600)
                    }
                    else if (image.tag == "blue circle") {
                        imgClone.x = e.x - (image.width)
                        imgClone.y = e.y - (image.height)
                        imgClone.layoutParams = ViewGroup.LayoutParams(300, 300)
                    }
                    // Add view
                    // to the frame bitmap
                    veDrawView.addView(imgClone)


                    true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
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

        // User confirms frame editing
        frameConfirmationBttn.setOnClickListener {
            // Insert the tag into firestore
            uploadFrameToFirestore()

            // Insert tag into the video layout
            addLoadedTag(selectedTag, videoLastPos)

            // Go back and play video where it was last paused
            videoView.seekTo(videoLastPos)
            veDrawView.resetView()
            veDrawView.removeAllViews()

            // Make previous elements visible again and frame editing elements invisible
            changeToFrameEditing(false)
        }

        // User cancels frame editing
        frameCancelBttn.setOnClickListener {
            // Go back and play video where it was last paused
            videoView.seekTo(videoLastPos)
            veDrawView.resetView()
            veDrawView.removeAllViews()

            // Make previous elements visible again and frame editing elements invisible
            changeToFrameEditing(false)
        }

    }

    @SuppressLint("InflateParams")
    private fun addLoadedTag(text: String, time: Int) {
        // Set minutes and seconds from int position
        val mins: Int = (time / 1000) / 60
        val secs: Int = (time / 1000 ) % 60

        // Set tag info (time + text)
        val tag: View = layoutInflater.inflate(R.layout.video_editor_tag, null)
        val tagInModal: View = layoutInflater.inflate(R.layout.video_editor_tag, null)
        tag.findViewById<TextView>(R.id.tag_text).text = text
        tag.findViewById<TextView>(R.id.tag_time).text = String.format("%02d:%02d", mins, secs)
        tagInModal.findViewById<TextView>(R.id.tag_text).text = text
        tagInModal.findViewById<TextView>(R.id.tag_time).text = String.format("%02d:%02d", mins, secs)

        // Add tag to view
        loadedTagsLL.addView(tag)

        // Add on click func
        tag.findViewById<TextView>(R.id.tag_text).setOnClickListener {
            MaterialDialog(this).show {
                customView(R.layout.video_editor_tag_modal, tagInModal, scrollable = true, horizontalPadding = true)
                title(text = "Confirm choice")
                message(text = "Are you sure you want to remove the following tag?")
                positiveButton(text = "Confirm") {
                    // Remove tag from the layout
                    loadedTagsLL.removeView(tag)

                    // Remove tag from the database
                    val imageRef = storage.reference.child("${auth.uid.toString()}/videos/${videoFolder}/frame_${time}.jpeg")
                    imageRef.delete().addOnSuccessListener { Toast.makeText(this@VideoEditorActivity, "Tag deleted successfully!", Toast.LENGTH_SHORT).show() }
                        .addOnFailureListener{ Toast.makeText(this@VideoEditorActivity, "There was an error deleting the tag, please try again later.", Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }

    private fun uploadFrameToFirestore() {
        // File metadata
        val metadata = storageMetadata {
            setCustomMetadata("time_int", videoLastPos.toString())
            setCustomMetadata("tag", selectedTag)
        }

        // File path
        val frameRef = storage.reference.child("${auth.uid.toString()}/videos/${videoFolder}/frame_$videoLastPos.jpeg")
        val baos = ByteArrayOutputStream()

        // Get view
        val bmp: Bitmap = Bitmap.createBitmap(veDrawView.width, veDrawView.height, Bitmap.Config.ARGB_8888)
        val canvas: Canvas = Canvas(bmp)
        veDrawView.draw(canvas)
        selectedFrame = bmp

        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uploadTask = frameRef.putBytes(data, metadata)
        uploadTask.addOnFailureListener {
            // Handle unsuccessful uploads
            Toast.makeText(this, "Something went wrong while uploading image, please try again later.", Toast.LENGTH_SHORT).show()
        }.addOnSuccessListener { taskSnapshot ->
            Toast.makeText(this@VideoEditorActivity, "Uploaded successfully!", Toast.LENGTH_SHORT).show()
            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
            // ...
        }
    }

    fun changeToFrameEditing(flag: Boolean) {
        if (flag) {
            // Make videoView invisible and veDrawView visible
            editingActionsLL.visibility = View.VISIBLE
            videoView.visibility = View.INVISIBLE
            veDrawView.visibility = View.VISIBLE

            // Show frame buttons and hide tags
            frameButtons.visibility = View.VISIBLE
            tagsLL.visibility = View.INVISIBLE
        }
        else {
            // Make videoView visible and veDrawView invisible
            editingActionsLL.visibility = View.GONE
            videoView.visibility = View.VISIBLE
            veDrawView.visibility = View.INVISIBLE

            // Hide frame buttons and show tags
            frameButtons.visibility = View.GONE
            tagsLL.visibility = View.VISIBLE
        }
    }

    @JvmOverloads
     fun initVideo(videoName: String = "", videoURI: Uri = Uri.EMPTY) {
        // Make video choosing linear layout invisible
        chooseVideoLL.visibility = View.INVISIBLE
        // Set parent container to video height
        mainRL.layoutParams.height = RelativeLayout.LayoutParams.WRAP_CONTENT

        // Creating MediaController
        val mediaController: MediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        mediaMetadataRetriever = MediaMetadataRetriever()

        // Assign videoView options
        progressBar.visibility = View.VISIBLE
        videoView.setMediaController(mediaController)
        videoView.canSeekBackward()
        videoView.canSeekForward()
        videoView.setOnPreparedListener {
            progressBar.visibility = View.INVISIBLE
            videoView.start()
        }
        if (videoURI == Uri.EMPTY) {
            storage.reference.child(auth.uid.toString()+ "/videos/" + videoName).downloadUrl.addOnSuccessListener {
                // Set videoView path
                videoView.setVideoPath(it.toString())
                // Set mediaMetadataRetriever path
                mediaMetadataRetriever.setDataSource(it.toString())
            }.addOnFailureListener {
                TODO("Handle errors here")
            }
        }
        else {
            // Set videoView URI
            videoView.setVideoURI(videoURI)
            // Set mediaMetadataRetriever URI
            mediaMetadataRetriever.setDataSource(this@VideoEditorActivity, videoURI)
        }
    }

    private fun loadVideoIconClickFunc() {
        // Upload video
        findViewById<TextView>(R.id.video_editor_modal_upload).setOnClickListener {
            recordVideo()
        }

        // Download video
        findViewById<TextView>(R.id.video_editor_modal_load).setOnClickListener {
            // initVideo("", Uri.parse("/storage/emulated/0/Android/data/com.example.spor_tfg/files/TrimmedVideo/trimmed_video_2022_3_18_16_56_3..mp4"))
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
                // Set path to the video frames
                println("Video path:: $text")
                initVideo(text.toString())
                videoFolder = text.toString().split(".")[0]
                loadTags()
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