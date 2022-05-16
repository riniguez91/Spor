package com.example.spor_tfg

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Pair
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.spor_tfg.databinding.ActivityLoginBinding
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    // vars
    private var auth: FirebaseAuth = Firebase.auth
    private lateinit var binding: ActivityLoginBinding
    val db = Firebase.database("https://spor-tfg-default-rtdb.europe-west1.firebasedatabase.app")
    val myRef = db.getReference("users")
    lateinit var username: EditText
    lateinit var password: EditText
    lateinit var login: Button
    lateinit var forgotPasswordLink: Button
    lateinit var googleButton: Button
    lateinit var facebookButton: Button
    lateinit var loading: ProgressBar

    // Google sign-in
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 100
    private val TAG = "GOOGLE_SIGN_IN_TAG"

    // Facebook sign-in
    private val callbackManager = CallbackManager.Factory.create()

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
        googleButton = findViewById(R.id.login_google_button)
        facebookButton = findViewById(R.id.login_facebook_button)
        loading = binding.loading

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

        // Configure Google Sign In object
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
            .requestIdToken("398024589810-dqgl9olbi4ouki1oen23qj9ifs8ta24v.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        // Set google button sign in functionality
        googleSignIn()

        // Facebook sign in functionality
        facebookSignIn()

        // Check if there is a logged in user
        checkUser()
    }

    private fun facebookSignIn() {
        facebookButton.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email"))

            LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {

                override fun onSuccess(result: LoginResult) {
                    result.let { loginResult ->
                        val token = loginResult.accessToken
                        // Successful facebook sign-in
                        val credential = FacebookAuthProvider.getCredential(token.token)
                        auth.signInWithCredential(credential).addOnCompleteListener {
                            if (it.isSuccessful) {
                                // user is logged in and go to main page
                                goToMainPage()
                            }
                            else
                                Toast.makeText(this@LoginActivity, "An error has occurred, please try again later.", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            Log.d(TAG, "userGoogleAuth: ${it.message}")
                        }
                    }
                }

                override fun onCancel() {}

                override fun onError(error: FacebookException) {
                    Toast.makeText(this@LoginActivity, "An error has occurred, please try again later.", Toast.LENGTH_SHORT).show()
                }

            })
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun googleSignIn() {
        googleButton.setOnClickListener {
            val intent = googleSignInClient.signInIntent
            resultLauncher.launch(intent)
        }
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_CANCELED) {
            // There are no request codes
            val data: Intent? = result.data
            val accountTask = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Successful google sign-in
                val account = accountTask.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account!!.idToken, null)
                auth.signInWithCredential(credential).addOnSuccessListener { authResult ->
                    // get loggedIn user
                    val user = auth.currentUser
                    val uid = auth.currentUser!!.uid
                    val email = auth.currentUser!!.email

                    // check if user is new
                    if (authResult.additionalUserInfo!!.isNewUser)
                        Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()

                    // user is logged in and go to main page
                    goToMainPage()
                }.addOnFailureListener {
                    Log.d(TAG, "userGoogleAuth: ${it.message}")
                }
            }
            catch (e: Exception) {
                // Failed google sign-in
                Log.d(TAG, "resultLauncher: $e")
            }
        }
    }

    fun checkUser() {
        val currentUser = auth.currentUser
        auth.signOut()
        if(currentUser != null){
            goToMainPage();
        }
    }

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
        val intent: Intent = Intent(this, HomeScreenActivity::class.java)
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
}