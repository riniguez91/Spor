package com.example.spor_tfg

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import com.example.spor_tfg.databinding.ActivityWarmUpBinding
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class WarmUp : AppCompatActivity(), AddExerciseDialogFragment.NoticeDialogListener, AddTaskDialogFragment.NoticeDialogListener {

    private lateinit var binding: ActivityWarmUpBinding

    // Firestore
    private var storage: FirebaseStorage = Firebase.storage
    private val db: FirebaseFirestore = Firebase.firestore
    private var auth: FirebaseAuth = Firebase.auth

    // General variables
    var sessionName: String = ""
    var warmups: HashMap<String, ArrayList<HashMap<String, String>>> = HashMap<String, ArrayList<HashMap<String, String>>>().apply {
        this["exercises"] = ArrayList()
        this["tasks"] = ArrayList()
    }

    // XML variables
    lateinit var exercisesGL: GridLayout
    lateinit var tasksGL: GridLayout
    lateinit var addExerciseButton: ImageButton
    lateinit var addTaskButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_warm_up)

        binding = ActivityWarmUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hooks
        exercisesGL = findViewById(R.id.warmup_exercise_grid_layout)
        tasksGL = findViewById(R.id.warmup_tasks_grid_layout)
        addExerciseButton = findViewById(R.id.warmup_add_exercise)
        addTaskButton = findViewById(R.id.warmup_add_task)

        // Init toolbar
        initToolbar()

        // Get session name
        sessionName = intent.getStringExtra("session_name").toString()

        // Load the data (if any)
        loadData()

        // Show add exercise modal
        addExerciseButton.setOnClickListener {
            val dialog = AddExerciseDialogFragment()
            dialog.show(supportFragmentManager, "AddExerciseDialogFragment")
        }

        // Show add task modal
        addTaskButton.setOnClickListener {
            val dialog = AddTaskDialogFragment()
            dialog.show(supportFragmentManager, "AddTaskDialogFragment")
        }
    }

    private fun loadData() {
        val warmUpRef = storage.reference.child("${auth.uid.toString()}/plays/$sessionName/warmup")
        warmUpRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { it ->
            if (it.isNotEmpty()) {
                warmups = Gson().fromJson(it.toString(Charsets.UTF_8), object : TypeToken<HashMap<String, ArrayList<HashMap<String, String>>>>() {}.type)
                warmups.forEach { (type, list) ->
                    println(list)
                    list.forEach { hm ->
                        if (type == "exercises")
                            addExercise(hm["title"]!!, hm["desc"]!!, hm["type"]!!, hm["time"]!!)
                        else if (type == "tasks")
                            addTask(hm["title"]!!, hm["desc"]!!, hm["type"]!!, hm["time"]!!, hm["restTime"]!!, hm["noSets"]!!)
                    }
                }
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun addExercise(title: String, desc: String, type: String, time: String) {
        // Get inflated view
        val inflater = LayoutInflater.from(this)
        val cardLayout = inflater.inflate(R.layout.warmup_exercise, null, false) as LinearLayout

        // Set inflated view info
        cardLayout.findViewById<TextView>(R.id.exercise_title).text = title
        cardLayout.findViewById<TextView>(R.id.exercise_desc).text = desc
        cardLayout.findViewById<TextView>(R.id.exercise_activation).text = type
        cardLayout.findViewById<TextView>(R.id.exercise_time).text = getString(R.string.time, time)

        // Add to gridlayout (always at the penultimate position so we have the add button at the last)
        exercisesGL.addView(cardLayout, exercisesGL.childCount-1)
        (cardLayout.layoutParams as GridLayout.LayoutParams).marginEnd = 80
        (cardLayout.layoutParams as GridLayout.LayoutParams).bottomMargin = 50
    }

    @SuppressLint("InflateParams")
    private fun addTask(title: String, desc: String, type: String, time: String, restTime: String, noSets: String) {
        // Get inflated view
        val inflater = LayoutInflater.from(this)
        val cardLayout = inflater.inflate(R.layout.warmup_task, null, false) as LinearLayout

        // Set inflated view info
        cardLayout.findViewById<TextView>(R.id.task_title).text = title
        cardLayout.findViewById<TextView>(R.id.task_desc).text = desc
        cardLayout.findViewById<TextView>(R.id.task_activation).text = type
        cardLayout.findViewById<TextView>(R.id.task_time).text = getString(R.string.time, time)
        cardLayout.findViewById<TextView>(R.id.task_rest_time).text = getString(R.string.restTime, restTime)
        cardLayout.findViewById<TextView>(R.id.task_no_sets).text = getString(R.string.noSets, noSets)

        // Add to gridlayout (always at the penultimate position so we have the add button at the last)
        tasksGL.addView(cardLayout, tasksGL.childCount-1)
        (cardLayout.layoutParams as GridLayout.LayoutParams).marginEnd = 80
        (cardLayout.layoutParams as GridLayout.LayoutParams).bottomMargin = 50
    }

    // The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the NoticeDialogFragment.NoticeDialogListener interface
    override fun onDialogPositiveClick(dialog: DialogFragment, inflatedView: LinearLayout, typeOfWarmUp: String) {
        if (typeOfWarmUp == "exercise") {
            // Get values
            val exerciseTitleTIL: TextInputLayout = inflatedView.findViewById(R.id.exercise_title_ilayout)
            val exerciseDescTIL: TextInputLayout = inflatedView.findViewById(R.id.exercise_desc_ilayout)
            val exerciseTimeTIL: TextInputLayout = inflatedView.findViewById(R.id.exercise_time_ilayout)
            val exerciseTypeTIL: TextInputLayout = inflatedView.findViewById(R.id.exercise_type_ilayout)

            // Player values
            val title: String = exerciseTitleTIL.editText?.text.toString()
            val desc: String = exerciseDescTIL.editText?.text.toString()
            val time: String = exerciseTimeTIL.editText?.text.toString()
            val type: String = exerciseTypeTIL.editText?.text.toString()

            // Add to database
            val exercise: HashMap<String, String> = hashMapOf("title" to title, "desc" to desc, "type" to type, "time" to time)
            warmups["exercises"]?.add(exercise)
            uploadWarmUp()

            // Add to layout
            addExercise(title, desc, type, time)
        }
        else if (typeOfWarmUp == "task") {
            // Get values
            val taskTitleTIL: TextInputLayout = inflatedView.findViewById(R.id.task_title_ilayout)
            val taskDescTIL: TextInputLayout = inflatedView.findViewById(R.id.task_desc_ilayout)
            val taskTimeTIL: TextInputLayout = inflatedView.findViewById(R.id.task_time_ilayout)
            val taskRestTimeTIL: TextInputLayout = inflatedView.findViewById(R.id.task_rest_time_ilayout)
            val taskNoSetsTIL: TextInputLayout = inflatedView.findViewById(R.id.task_no_sets_ilayout)
            val taskTypeTIL: TextInputLayout = inflatedView.findViewById(R.id.task_type_ilayout)

            // Player values
            val title: String = taskTitleTIL.editText?.text.toString()
            val desc: String = taskDescTIL.editText?.text.toString()
            val time: String = taskTimeTIL.editText?.text.toString()
            val restTime: String = taskRestTimeTIL.editText?.text.toString()
            val noSets: String = taskNoSetsTIL.editText?.text.toString()
            val type: String = taskTypeTIL.editText?.text.toString()

            // Add to database
            val task: HashMap<String, String> = hashMapOf("title" to title, "desc" to desc, "type" to type, "time" to time, "restTime" to restTime, "noSets" to noSets)
            warmups["tasks"]?.add(task)
            uploadWarmUp()

            // Add to layout
            addTask(title, desc, type, time, restTime, noSets)
        }
    }

    private fun uploadWarmUp() {
        val ref = storage.reference.child("${auth.uid.toString()}/plays/${sessionName}/warmup")
        val jsonString: String = Gson().toJson(warmups, HashMap::class.java)
        val uploadTask = ref.putBytes(jsonString.toByteArray())
        uploadTask.addOnFailureListener {
            // Handle unsuccessful uploads
            Toast.makeText(
                this,
                "Something went wrong while uploading, please try again later.",
                Toast.LENGTH_SHORT
            ).show()
        }.addOnSuccessListener { taskSnapshot ->
            Toast.makeText(this, "Uploaded successfully!", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        // User touched the dialog's negative button
        // Close dialog
        dialog.dismiss()
    }

    private fun initToolbar() {
        val toolbar: Toolbar = findViewById(R.id.warmup_toolbar)
        toolbar.title = "Spor"
        toolbar.title
        setSupportActionBar(findViewById<View>(R.id.warmup_toolbar) as Toolbar)
    }
}