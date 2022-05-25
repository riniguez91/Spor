package com.example.spor_tfg

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.example.spor_tfg.databinding.ActivityProfileBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.*
import java.util.concurrent.TimeUnit


class ProfileActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    AddPlayerDialogFragment.NoticeDialogListener {

    private lateinit var binding: ActivityProfileBinding

    lateinit var myApp: MyApp

    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView

    private var storage: FirebaseStorage = Firebase.storage
    private val db: FirebaseFirestore = Firebase.firestore
    private var auth: FirebaseAuth = Firebase.auth

    // Coach layout
    lateinit var profileLL: LinearLayout
    lateinit var coachImage: ShapeableImageView
    lateinit var coachTitle: TextView
    lateinit var coachUsername: TextView

    // Edit player elements
    var editPlayerFlag: Boolean = false
    // Name
    lateinit var cardName: TextView
    lateinit var editNameLayout: TextInputLayout
    lateinit var editNameField: TextInputEditText
    // Pos
    lateinit var cardPosHelper: TextView
    lateinit var cardPos: TextView
    lateinit var cardPosLayout: TextInputLayout
    lateinit var cardPosField: TextInputEditText
    // Jersey No
    lateinit var cardJerseyNoHelper: TextView
    lateinit var cardJerseyNo: TextView
    lateinit var cardJerseyNoLayout: TextInputLayout
    lateinit var cardJerseyNoField: TextInputEditText
    // Leading foot
    lateinit var cardLeadingFoot: TextView
    lateinit var cardLeadingFootLayout: TextInputLayout

    // Players layout
    lateinit var doc: HashMap<Any, Any>
    lateinit var cardLayout: LinearLayout

    // Selected players
    lateinit var selectedPlayersTitle: TextView
    lateinit var selectedPlayersGridLayout: GridLayout
    lateinit var selectedPlayersText: TextView
    lateinit var confirmSessionButton: Button
    var selectedPlayersHM: HashMap<Any, Any> = HashMap()

    var teamName: String = ""
    var currentGridLayout: GridLayout? = null
    var addSessionFlag: Boolean = false
    var sessionName: String = ""

    @SuppressLint("InflateParams")
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
        profileLL = findViewById(R.id.profile_ll)
        coachImage = findViewById(R.id.profile_coach_iv)
        coachTitle = findViewById(R.id.profile_welcome_text)
        coachUsername = findViewById(R.id.profile_username)

        // Selected players
        selectedPlayersTitle = findViewById(R.id.profile_selected_players_title)
        selectedPlayersGridLayout = findViewById(R.id.profile_selected_players_grid_layout)
        selectedPlayersText = findViewById(R.id.profile_selected_players_text)
        confirmSessionButton = findViewById(R.id.profile_confirm_session)

        // Init toolbar
        initToolbar()

        // Preselect
        preselectToolbar()

        addSessionFlag = intent.getBooleanExtra("addSessionFlag", false)

        // Get team info
        myApp = (this.application as MyApp)
        doc =  myApp.getDocVar()

        // Load coach data
        loadCoachData()

        // Load players into the cards
        loadPlayers()

        if (addSessionFlag)
            addSession()
    }

    @SuppressLint("CheckResult")
    private fun addSession() {
        selectedPlayersGridLayout.visibility = View.VISIBLE
        MaterialDialog(this).show {
            // Set standard session name
            val calendar: Calendar = Calendar.getInstance()
            sessionName = "session_${calendar.get(Calendar.HOUR_OF_DAY)}${calendar.get(Calendar.MINUTE)}${calendar.get(Calendar.SECOND)}"

            // Force user to click confirm button in order the modal closes
            this.cancelOnTouchOutside(false)

            // Fill in dialog
            title(text = "Session name")
            message(text = "Please type session name:")
            input(prefill = sessionName, allowEmpty = false) { dialog, text ->
                // Text submitted with the action button
                sessionName = text.toString()
            }
            positiveButton(text = "Submit")
        }
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
    @Suppress("UNCHECKED_CAST")
    private fun loadPlayers() {
        val totalTeams = doc["total_teams"] as HashMap<Any, Any>
        println("Total teams:: $totalTeams")
        for ((idx, team) in totalTeams) {
            // Set variable types
            idx as String
            team as HashMap<Any, Any>

            // Create title textview + horizontal scroll view + grid layout
            val title: TextView = TextView(this)
            val titleParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            titleParams.setMargins(60,0,0,0)
            title.layoutParams = titleParams
            title.typeface = ResourcesCompat.getFont(this, R.font.nexa_light)
            title.text = idx
            title.textSize = 26f
            title.setTextColor(ContextCompat.getColor(this, R.color.white))

            val horizontalScrollView: HorizontalScrollView = HorizontalScrollView(this)
            val hSVLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            hSVLayoutParams.setMargins(30,20,30,65)
            horizontalScrollView.layoutParams = hSVLayoutParams

            val gridLayout: GridLayout = GridLayout(this)
            val lParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            gridLayout.layoutParams = lParams
            gridLayout.rowCount = 1
            gridLayout.orientation = GridLayout.HORIZONTAL
            currentGridLayout = gridLayout

            // Add views
            profileLL.addView(title)
            horizontalScrollView.addView(gridLayout)
            profileLL.addView(horizontalScrollView)

            // Create card for each player in the team
            for ((idx2, player) in team ) {
                player as HashMap<Any, Any>
                createCard(player["image"] as String,
                    player["name"] as String,
                    player["position"] as String,
                    player["jersey_number"] as Long,
                    player["leading_foot"] as String,
                    player["status"] as String,
                    idx2.toString().toInt(),
                    gridLayout,
                    idx
                )
            }
            // Add "add player" icon
            val addPlayerButton: ImageButton = ImageButton(this)
            val addPlayerLParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = 10
                marginEnd = 40
            }
            addPlayerButton.layoutParams = addPlayerLParams
            addPlayerButton.setBackgroundColor(Color.TRANSPARENT)
            addPlayerButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_baseline_add_24))
            // Add to parent view
            gridLayout.addView(addPlayerButton)
            (addPlayerButton.layoutParams as GridLayout.LayoutParams).setGravity(Gravity.CENTER)

            teamName = idx.toString()

            // Set button on click func
            addPlayerButton.setOnClickListener {
                val dialog = AddPlayerDialogFragment()
                dialog.show(supportFragmentManager, "AddPlayerDialogFragment")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("InflateParams", "CheckResult")
    private fun createCard(image: String, name: String, position: String, jersey_number: Long, leading_foot: String,
                           status: String, id: Int, parentGrid: GridLayout, team: String) {
        // Load variables
        val inflater = LayoutInflater.from(this@ProfileActivity)
        cardLayout = inflater.inflate(R.layout.profile_player_card, null, false) as LinearLayout

        // Edit player hooks
        val card = cardLayout.findViewById<MaterialCardView>(R.id.card)
        // Name
        cardName = cardLayout.findViewById<TextView>(R.id.profile_player_card_name)
        editNameLayout = cardLayout.findViewById(R.id.edit_player_name_ilayout)
        editNameField = cardLayout.findViewById(R.id.edit_player_name_field)
        // Pos
        cardPosHelper = cardLayout.findViewById(R.id.profile_player_card_pos_helper)
        cardPos = cardLayout.findViewById(R.id.profile_player_card_pos)
        cardPosLayout = cardLayout.findViewById(R.id.edit_player_pos_ilayout)
        cardPosField = cardLayout.findViewById(R.id.edit_player_pos_field)
        // Jersey No
        cardJerseyNoHelper = cardLayout.findViewById(R.id.profile_player_card_jersey_no_helper)
        cardJerseyNo = cardLayout.findViewById(R.id.profile_player_card_jersey_no)
        cardJerseyNoLayout = cardLayout.findViewById(R.id.edit_player_jersey_no_ilayout)
        cardJerseyNoField = cardLayout.findViewById(R.id.edit_player_jersey_no_field)
        // Leading foot
        cardLeadingFoot = cardLayout.findViewById(R.id.profile_player_card_leading_foot)
        cardLeadingFootLayout = cardLayout.findViewById(R.id.edit_player_leading_foot_ilayout)

        // Buttons
        val playerStatus: ImageButton = cardLayout.findViewById<ImageButton>(R.id.profile_player_card_status)
        val editPlayerButton: Button = cardLayout.findViewById(R.id.profile_player_card_edit)
        val cancelCardEdit = cardLayout.findViewById<ImageButton>(R.id.profile_player_card_edit_cancel)
        val confirmCardEdit = cardLayout.findViewById<ImageButton>(R.id.profile_player_card_edit_confirm)
        val deleteCardEdit = cardLayout.findViewById<ImageButton>(R.id.profile_player_card_edit_delete)
        val addPlayer = cardLayout.findViewById<ImageButton>(R.id.profile_player_card_add)
        if (addSessionFlag) addPlayer.visibility = View.VISIBLE

        // Fill in card
        val cardImage: ShapeableImageView = cardLayout.findViewById<ShapeableImageView>(R.id.player_card_image)
        loadImage(image, 200, 200, cardImage)
        // Card ID
        cardLayout.id = id
        // Player name
        cardName.text = name
        editNameField.setText(name)
        // Position
        cardPos.text = position
        cardPosField.setText(position)
        // Jersey Number
        cardJerseyNo.text = jersey_number.toString()
        cardJerseyNoField.setText(jersey_number.toString())
        // Leading foot
        cardLeadingFoot.text = leading_foot

        when (status) {
            "available" -> {
                playerStatus.setBackgroundResource(R.drawable.available)
            }
            "partially available" -> {
                playerStatus.setBackgroundResource(R.drawable.partially_available)
            }
            "sick" -> {
                playerStatus.setBackgroundResource(R.drawable.sick)
                if (addSessionFlag) addPlayer.visibility = View.GONE
            }
            else -> {
                playerStatus.setBackgroundResource(R.drawable.injured)
                if (addSessionFlag) addPlayer.visibility = View.GONE
            }
        }

        @Suppress("UNCHECKED_CAST")
        // Add player click func
        addPlayer.setOnClickListener {
            selectedPlayersTitle.visibility = View.VISIBLE
            selectedPlayersText.visibility = View.GONE
            // Hide button since it has already been added to the view
            addPlayer.visibility = View.GONE
            editPlayerButton.visibility = View.GONE
            confirmSessionButton.visibility = View.VISIBLE

            // Add players onto the horizontal scroll view
            val sPlayerIB: ShapeableImageView = ShapeableImageView(this)
            val radius = 80f
            // val sPlayerLP: LinearLayout.LayoutParams = LinearLayout.LayoutParams()
            sPlayerIB.setImageDrawable(cardImage.drawable)
            sPlayerIB.id = cardLayout.id
            sPlayerIB.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = 30
            }

            // Add to the current selected players hashmap
            selectedPlayersHM[id.toString()] = ((doc["total_teams"] as HashMap<Any, Any>)[team] as HashMap<Any, Any>)[id.toString()] as HashMap<Any, Any>

            // Remove from selected players grid layout
            sPlayerIB.setOnClickListener {
                (sPlayerIB.parent as ViewGroup).removeView(sPlayerIB)
                selectedPlayersHM.remove(id.toString())
                addPlayer.visibility = View.VISIBLE
                editPlayerButton.visibility = View.VISIBLE
                if (selectedPlayersGridLayout.childCount == 1) {
                    selectedPlayersText.visibility = View.VISIBLE
                    confirmSessionButton.visibility = View.GONE
                    selectedPlayersTitle.visibility = View.GONE
                }
            }
            // Add to grid
            selectedPlayersGridLayout.addView(sPlayerIB)
            sPlayerIB.shapeAppearanceModel = sPlayerIB.shapeAppearanceModel
                .toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, radius)
                .build()
        }

        @Suppress("UNCHECKED_CAST")
        fun uploadHashMapToFirestore(value: HashMap<Any, Any>, path: String): UploadTask {
            // Upload bytes to firestore
            val byteOut = ByteArrayOutputStream()
            val out = ObjectOutputStream(byteOut)
            out.writeObject(value)

            return storage.reference.child(path).putBytes(byteOut.toByteArray())
        }

        // Confirm session click func
        @Suppress("UNCHECKED_CAST")
        confirmSessionButton.setOnClickListener {
            // Set players in training session
            doc["team"] = selectedPlayersHM
            uploadHashMapToFirestore(selectedPlayersHM, "${auth.uid.toString()}/plays/${sessionName}/selected_team")

            // Get session info hashmap
            val playInfoHM: HashMap<String, String> = intent.getSerializableExtra("playInfoHM") as HashMap<String, String>
            val ref: UploadTask = uploadHashMapToFirestore(playInfoHM as HashMap<Any, Any>, "${auth.uid.toString()}/plays/${sessionName}/${sessionName}")
            ref.addOnSuccessListener {
                Toast.makeText(this, "Session created successfully!", Toast.LENGTH_SHORT).show()
                // Redirect user to paint
                val intent = Intent(this, MainScreen::class.java)
                intent.putExtra("session_name", sessionName)
                startActivity(intent)
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "There was an error creating the play, please try again.", Toast.LENGTH_SHORT).show()
            }
        }

        // Status click func
        statusClickFunc(playerStatus, team, id)

        // Edit player click func
        editPlayerClickFunc(card, editPlayerButton, cancelCardEdit, confirmCardEdit, deleteCardEdit, editNameLayout, cardName, cardPosHelper, cardPos, cardPosLayout,
            cardJerseyNoHelper, cardJerseyNo, cardJerseyNoLayout, cardLeadingFoot, cardLeadingFootLayout, team, id, addPlayer)

        // Add card to grid layout
        parentGrid.addView(cardLayout)
    }

    @SuppressLint("InflateParams")
    private fun editPlayerClickFunc(card: MaterialCardView, editPlayerButton: Button, cancelCardEdit: ImageButton, confirmCardEdit: ImageButton, deleteCardEdit: ImageButton,
                                    editNameLayout: TextInputLayout, cardName: TextView, cardPosHelper: TextView, cardPos: TextView, cardPosLayout: TextInputLayout,
                                    cardJerseyNoHelper: TextView, cardJerseyNo: TextView, cardJerseyNoLayout: TextInputLayout, cardLeadingFoot: TextView,
                                    cardLeadingFootLayout: TextInputLayout, team: String, id: Int, addPlayer: ImageButton) {
        // Get original player values
        val playerName: String = cardName.text.toString()
        val playerPos: String = cardPos.text.toString()
        val playerJerseyNo: Long = cardJerseyNo.text.toString().toLong()
        val playerLeadingFoot: String = cardLeadingFoot.text.toString()

        // Edit player card click func
        editPlayerButton.setOnClickListener {
            // Change card visual elements
            changePlayerCardEdit(editPlayerFlag, card, editPlayerButton, cancelCardEdit, confirmCardEdit, deleteCardEdit, cardName, editNameLayout, cardPosHelper, cardPos, cardPosLayout,
                cardJerseyNoHelper, cardJerseyNo, cardJerseyNoLayout, cardLeadingFoot, cardLeadingFootLayout, addPlayer)

            // Set leading foot dropdown menu
            val items = listOf("Right footed", "Left footed", "Ambidextrous")
            val adapter = ArrayAdapter(this, R.layout.list_item, items)
            (cardLeadingFootLayout.editText as? AutoCompleteTextView)?.setAdapter(adapter)
        }

        // Cancel editing click func
        cancelCardEdit.setOnClickListener {
            // Change card visual elements
            changePlayerCardEdit(editPlayerFlag, card, editPlayerButton, cancelCardEdit, confirmCardEdit, deleteCardEdit, cardName, editNameLayout, cardPosHelper, cardPos, cardPosLayout,
                cardJerseyNoHelper, cardJerseyNo, cardJerseyNoLayout, cardLeadingFoot, cardLeadingFootLayout, addPlayer)
        }

        // Confirm editing click func
        confirmCardEdit.setOnClickListener {
            // Change card visual elements
            changePlayerCardEdit(editPlayerFlag, card, editPlayerButton, cancelCardEdit, confirmCardEdit, deleteCardEdit, cardName, editNameLayout, cardPosHelper, cardPos, cardPosLayout,
                                cardJerseyNoHelper, cardJerseyNo, cardJerseyNoLayout, cardLeadingFoot, cardLeadingFootLayout, addPlayer)

            // Get card values again (may or may not be changed)
            val inputName: String = editNameLayout.editText?.text.toString()
            val inputPos: String = cardPosLayout.editText?.text.toString()
            val inputJerseyNo: Long = cardJerseyNoLayout.editText?.text.toString().toLong()
            val inputLeadingFoot: String = cardLeadingFootLayout.editText?.text.toString()

            println("Original values:: $playerName | $playerPos | $playerJerseyNo | $playerLeadingFoot")
            println("Changed values:: $inputName | $inputPos | $inputJerseyNo | $inputLeadingFoot")

            if (playerName !=  inputName) {
                // Change player jersey no. on firestore
                updateFirestoreField(team, id, "name", inputName)
                // Change player name on card
                cardName.text = inputName
            }
            if (playerPos != inputPos) {
                // Change player jersey no. on firestore
                updateFirestoreField(team, id, "position", inputPos)
                // Change player pos on card
                cardPos.text = inputPos
            }
            if (playerJerseyNo != inputJerseyNo) {
                // Change player jersey no. on firestore
                updateFirestoreField(team, id, "jersey_number", inputJerseyNo.toString())
                // Change player jersey no. on card
                cardJerseyNo.text = inputJerseyNo.toString()
            }
            if (playerLeadingFoot != inputLeadingFoot) {
                // Change player leading foot on firestore
                updateFirestoreField(team, id, "leading_foot", inputLeadingFoot)
                // Change player leading foot on card
                cardLeadingFoot.text = inputLeadingFoot
            }

        }

        // Delete player on click func
        deleteCardEdit.setOnClickListener {
            MaterialDialog(this).show {
                title(text = "Confirm action")
                message(text = "Are you sure you want to delete the following player? This action cannot be undone.")
                negativeButton(text = "Cancel")
                positiveButton(text = "Confirm") {
                    // Delete player from firestore
                    db.collection("user").document(auth.uid.toString()).update(
                        mapOf(
                            "total_teams.$team.$id" to FieldValue.delete()
                        )
                    )
                    // Remove view
                    (card.parent.parent as ViewGroup).removeView(card.parent as View)
                    // Update doc
                    @Suppress("UNCHECKED_CAST")
                    ((doc["total_teams"] as HashMap<Any, Any>)[team] as HashMap<Any, Any>).remove(id.toString())
                    myApp.setDocVar(doc)
                }
            }
        }
    }

    private fun updateFirestoreField(team: String, id: Int, field: String, value: String) {
        db.collection("user").document(auth.uid.toString()).update(
            mapOf(
                "total_teams.$team.$id.$field" to value
            )
        )
    }

    private fun changePlayerCardEdit(flag: Boolean, card: MaterialCardView, editPlayerButton: Button, cancelCardEdit: ImageButton, confirmCardEdit: ImageButton,
                                     deleteCardEdit: ImageButton, cardName: TextView, editNameLayout: TextInputLayout, cardPosHelper: TextView, cardPos: TextView,
                                     cardPosLayout: TextInputLayout, cardJerseyNoHelper: TextView, cardJerseyNo: TextView, cardJerseyNoLayout: TextInputLayout,
                                     cardLeadingFoot: TextView, cardLeadingFootLayout: TextInputLayout, addPlayer: ImageButton) {
        // Set edit card elements
        if (!flag) {
            editPlayerFlag = true
            // Buttons
            editPlayerButton.visibility = View.GONE
            cancelCardEdit.visibility = View.VISIBLE
            confirmCardEdit.visibility = View.VISIBLE
            deleteCardEdit.visibility = View.VISIBLE
            // Name
            cardName.visibility = View.GONE
            editNameLayout.visibility = View.VISIBLE
            // Position
            cardPosHelper.visibility = View.GONE
            cardPos.visibility = View.GONE
            cardPosLayout.visibility = View.VISIBLE
            // Jersey No
            cardJerseyNoHelper.visibility = View.GONE
            cardJerseyNo.visibility = View.GONE
            cardJerseyNoLayout.visibility = View.VISIBLE
            // Leading foot
            cardLeadingFoot.visibility = View.GONE
            cardLeadingFootLayout.visibility = View.VISIBLE
            if (addSessionFlag) addPlayer.visibility = View.GONE
        }
        // Set card viewing elements
        else {
            editPlayerFlag = false
            // Buttons
            editPlayerButton.visibility = View.VISIBLE
            cancelCardEdit.visibility = View.GONE
            confirmCardEdit.visibility = View.GONE
            deleteCardEdit.visibility = View.GONE
            // Name
            cardName.visibility = View.VISIBLE
            editNameLayout.visibility = View.GONE
            // Position
            cardPosHelper.visibility = View.VISIBLE
            cardPos.visibility = View.VISIBLE
            cardPosLayout.visibility = View.GONE
            // Jersey No
            cardJerseyNoHelper.visibility = View.VISIBLE
            cardJerseyNo.visibility = View.VISIBLE
            cardJerseyNoLayout.visibility = View.GONE
            // Leading foot
            cardLeadingFoot.visibility = View.VISIBLE
            cardLeadingFootLayout.visibility = View.GONE
            if (addSessionFlag) addPlayer.visibility = View.VISIBLE
        }
    }

    @SuppressLint("CheckResult")
    private fun statusClickFunc(playerStatus: ImageButton, team: String, id: Int) {
        // Change available/injured state
        val myItems = listOf("Available", "Injured", "Partially Available", "Sick")
        playerStatus.setOnClickListener {
            val imageBtn: ImageButton = it as ImageButton
            MaterialDialog(this).show {
                title(text = "Change player status?")
                message(text = "Press confirm if you wish to change the player status, else press outside the " +
                        "window and it will close")
                listItemsSingleChoice(items = myItems, initialSelection = 0) { dialog, index, text ->
                    // Update firestore field
                    updateFirestoreField(team, id, "status", text.toString().lowercase())
                    // Change state accordingly
                    when {
                        text.toString().lowercase() == "available" -> {
                            imageBtn.setBackgroundResource(R.drawable.available)
                        }
                        text.toString().lowercase() == "partially available" -> {
                            imageBtn.setBackgroundResource(R.drawable.partially_available)
                        }
                        text.toString().lowercase() == "sick" -> {
                            imageBtn.setBackgroundResource(R.drawable.sick)
                        }
                        else -> {
                            imageBtn.setBackgroundResource(R.drawable.injured)
                        }
                    }
                }
                positiveButton(text = "Confirm")

            }
        }
    }

    // The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the NoticeDialogFragment.NoticeDialogListener interface
    override fun onDialogPositiveClick(dialog: DialogFragment, inflatedView: LinearLayout) {
        // Get values
        val playerName: TextInputLayout = inflatedView.findViewById(R.id.player_signin_name_ilayout)
        val playerPos: TextInputLayout = inflatedView.findViewById(R.id.player_signin_pos_ilayout)
        val playerJerseyNo: TextInputLayout = inflatedView.findViewById(R.id.player_signin_jersey_no_ilayout)
        val playerLeadingFoot: TextInputLayout = inflatedView.findViewById(R.id.player_signin_leading_foot_ilayout)

        // Player values
        val image: String = "gs://spor-tfg.appspot.com/i7LRb2uy9gSsuvBYDL1qL4qVPia2/player1.jpeg"
        val jerseyNumber: Long = playerJerseyNo.editText?.text.toString().toLong()
        val leadingFoot: String = playerLeadingFoot.editText?.text.toString()
        val name: String = playerName.editText?.text.toString()
        val position: String = playerPos.editText?.text.toString()
        val status: String = "available"

        println("Jersey No. is $jerseyNumber")

        // Add to firestore
        @Suppress("UNCHECKED_CAST")
        val team: HashMap<Any, Any> = ((doc["total_teams"] as HashMap<Any, Any>)[teamName] as HashMap<Any, Any>)

        val playerHM: HashMap<String, Any> = hashMapOf<String, Any>(
            "image" to image,
            "jersey_number" to jerseyNumber,
            "leading_foot" to leadingFoot,
            "name" to name,
            "position" to position,
            "status" to status
        )
        db.collection("user").document(auth.uid.toString()).update(
            mapOf(
                "total_teams.$teamName.${team.size}" to playerHM
            )
        )

        // Add to local doc
        team[team.size] = playerHM

        // Add to layout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            createCard(image, name, position, jerseyNumber, leadingFoot, status, team.size, currentGridLayout!!, teamName)
        }
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        // User touched the dialog's negative button
        // Close dialog
        dialog.dismiss()
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
            R.id.nav_home -> {
                val intent = Intent(this, MainScreen::class.java)
                startActivity(intent)
                finish()
                true
            }
            R.id.nav_profile -> {
                true
            }
            else -> true
        }
    }
}