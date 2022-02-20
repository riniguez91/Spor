package com.example.spor_tfg

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ForgotPasswordActivity : AppCompatActivity() {

    // Vars
    private var auth: FirebaseAuth = Firebase.auth
    lateinit var fPasswordInput: EditText
    lateinit var fPasswordLink: Button
    lateinit var fPasswordPBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // Hooks
        fPasswordInput = findViewById(R.id.fpassword_input)
        fPasswordLink = findViewById(R.id.fpassword_button)
        fPasswordPBar = findViewById(R.id.fpassword_progress_bar)

        fPasswordLink.setOnClickListener {
            // Remove unnecessary white spaces
            val email = fPasswordInput.text.toString().trim { it <= ' ' }
            if (email.isEmpty()) {
                Toast.makeText(this@ForgotPasswordActivity, "Please enter a valid e-mail", Toast.LENGTH_SHORT).show()
            }
            else {
                fPasswordPBar.visibility = View.VISIBLE
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this@ForgotPasswordActivity, "E-mail has been sent, please check your inbox!", Toast.LENGTH_SHORT).show()
                        }
                        else {
                            Toast.makeText(this@ForgotPasswordActivity, task.exception!!.message.toString(), Toast.LENGTH_SHORT).show()
                        }
                        fPasswordPBar.visibility = View.INVISIBLE
                        finish()
                    }
            }
        }

    }
}