package com.example.spor_tfg

import android.app.ActivityOptions
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.util.Pair
import android.view.View
import android.widget.*
import com.example.spor_tfg.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    // vars
    private var auth: FirebaseAuth = Firebase.auth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var binding: ActivityLoginBinding
    val db = Firebase.database("https://spor-tfg-default-rtdb.europe-west1.firebasedatabase.app")
    val myRef = db.getReference("users")
    lateinit var username: EditText
    lateinit var password: EditText
    lateinit var login: Button
    lateinit var forgotPasswordLink: Button
    lateinit var googleLogin: Button
    lateinit var loading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Vars
        val loginImage: ImageView = findViewById(R.id.login_image)
        val loginHeading: TextView = findViewById(R.id.login_heading)
        val loginSubHeading: TextView = findViewById(R.id.login_subheading)
        val callSignUp = findViewById<View>(R.id.login_signup_button)
        username = binding.username
        password = binding.password
        login = binding.login
        forgotPasswordLink = findViewById(R.id.login_forgot_password_button)
        googleLogin = findViewById(R.id.login_google_button)
        loading = binding.loading

        // Initialize Google sign-in method
        // createRequest()

        // Set screen change from login to signup
        callSignUp.setOnClickListener {
            val intent: Intent = Intent(this, SignUp::class.java)

            val pairs = arrayOfNulls<Pair<View, String>>(7)
            pairs[0] = Pair<View, String>(loginImage, getString(R.string.transition_logo))
            pairs[1] = Pair<View, String>(loginHeading, getString(R.string.transition_text))
            pairs[2] =
                Pair<View, String>(loginSubHeading, getString(R.string.transition_subheading))
            pairs[3] = Pair<View, String>(username, getString(R.string.transition_username))
            pairs[4] = Pair<View, String>(password, getString(R.string.transition_password))
            pairs[5] = Pair<View, String>(login, getString(R.string.transition_signin))
            pairs[6] = Pair<View, String>(callSignUp, getString(R.string.transition_signup))

            val options: ActivityOptions =
                ActivityOptions.makeSceneTransitionAnimation(this, *pairs)
            startActivity(intent, options.toBundle())
        }

        // Set login button functionality
        login.setOnClickListener {
            loading.visibility = View.VISIBLE
            loginUser()
        }

        // Set forgot password link functionality
        forgotPasswordLink.setOnClickListener {
            val intent: Intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        // Set google button sign in functionality
        /*google_login.setOnClickListener {
            signIn()
        }*/

    }

    // CHECK THIS DOESN'T WORK PROPERLY
    /*public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if(currentUser != null){
            // Do we need to pass user here using intent.putExtra()?
            goToMainPage();
        }
    }*/

    fun loginUser() {
        if (validateUsername() and validatePassword()) {
            val userEnteredUsername: String = username.text.toString()
            val userEnteredPassword: String = password.text.toString()

            auth.signInWithEmailAndPassword(userEnteredUsername, userEnteredPassword)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success")
                        val user = auth.currentUser
                        // updateUI(user)
                        goToMainPage()
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "Authentication failed.",
                            Toast.LENGTH_SHORT).show()
                    }
                    loading.visibility = View.INVISIBLE
                }
        }
        loading.visibility = View.INVISIBLE
    }

    private fun goToMainPage() {
        val intent: Intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun validateUsername(): Boolean {
        val noWhiteSpace: Regex = Regex(".*\\s+.*")
        var success: Boolean = true
        val usernameText: String = username.text.toString()
        when {
            usernameText.isEmpty() -> {
                username.error = "Field cannot be empty!"
                success = false
            }
            usernameText.length < 5 -> {
                username.error = "Username doesn't match length type (5-15 characters)"
                success = false
            }
            usernameText.matches(noWhiteSpace) -> {
                username.error="No whitespaces allowed in the username!"
                success = false
            }
        }
        return success;
    }

    private fun validatePassword(): Boolean {
        var success: Boolean = true
        val passwordText: String = password.text.toString()
        if (passwordText.isEmpty()) {
            password.error = "Field cannot be empty!"
            success = false
        }
        return success;
    }

    /*private fun createRequest() {
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        // Build a GoogleSignInClient with the options specified by gso.
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }*/

    /*private fun signIn() {
        val signInIntent: Intent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }*/

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }*/
}