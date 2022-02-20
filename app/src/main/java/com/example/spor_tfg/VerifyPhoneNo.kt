package com.example.spor_tfg

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class VerifyPhoneNo : AppCompatActivity() {

    // Vars
    private var auth: FirebaseAuth = Firebase.auth
    lateinit var storedVerificationId: String
    lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    lateinit var otpInput: EditText
    lateinit var verifyButton: Button
    lateinit var progressBar: ProgressBar

    // User vars
    lateinit var name: String
    lateinit var username: String
    lateinit var email: String
    lateinit var password: String
    lateinit var phoneNo: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_phone_no)

        otpInput = findViewById(R.id.otp_input)
        verifyButton = findViewById(R.id.phone_button)
        progressBar = findViewById(R.id.phone_progress_bar)

        /*val db = Firebase.database("https://spor-tfg-default-rtdb.europe-west1.firebasedatabase.app")
        val myRef = db.getReference("users")
        val key: String? = myRef.push().key*/

        // Get variables sent from SignUp activity
        name = intent.getStringExtra("name").toString()
        username = intent.getStringExtra("username").toString()
        email = intent.getStringExtra("email").toString()
        password = intent.getStringExtra("password").toString()
        phoneNo = intent.getStringExtra("phoneNo").toString()

        sendVerificationToUser(phoneNo)

        verifyButton.setOnClickListener {
            val credential: PhoneAuthCredential = PhoneAuthProvider.getCredential(storedVerificationId, otpInput.text.toString())
            signInWithPhoneAuthCredential(credential)
        }
    }

    private fun sendVerificationToUser(phoneNo: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNo)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this@VerifyPhoneNo)                 // Activity (for callback binding)
            .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun registerUser() {
        // Register user in the database
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this@VerifyPhoneNo) { task2 ->
                if (task2.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser

                    // Sign out user such that it has to be entered at login screen
                    auth.signOut()

                    // Re-direct to login screen
                    val intent: Intent = Intent(this@VerifyPhoneNo, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task2.exception)
                    progressBar.visibility = View.INVISIBLE
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    // Re-direct to login screen
                    val intent: Intent = Intent(this@VerifyPhoneNo, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        progressBar.visibility = View.VISIBLE
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this@VerifyPhoneNo) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")

                    // Register user
                    registerUser()
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                        progressBar.visibility = View.INVISIBLE
                        Toast.makeText(this@VerifyPhoneNo, (task.exception as FirebaseAuthInvalidCredentialsException).message.toString(), Toast.LENGTH_SHORT).show()
                        // Re-direct to login screen
                        val intent: Intent = Intent(this@VerifyPhoneNo, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    // Update UI
                }
            }
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            Log.d(TAG, "onVerificationCompleted:$credential")
            /* val code: String = credential.smsCode.toString()
            progressBar.visibility = View.VISIBLE
            signInWithPhoneAuthCredential(PhoneAuthProvider.getCredential(storedVerificationId, code)) */
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // This callback is invoked in an invalid request for verification is made,
            // for instance if the the phone number format is not valid.
            Log.w(TAG, "onVerificationFailed", e)

            // Show a message and update the UI
            Toast.makeText(this@VerifyPhoneNo, e.message, Toast.LENGTH_SHORT).show()
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            Log.d(TAG, "onCodeSent:$verificationId")

            // Save verification ID and resending token so we can use them later
            storedVerificationId = verificationId
            resendToken = token
        }
    }
}