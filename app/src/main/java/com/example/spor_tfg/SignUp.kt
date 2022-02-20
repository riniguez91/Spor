package com.example.spor_tfg

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class SignUp : AppCompatActivity() {

    // Vars
    lateinit var regName: EditText
    lateinit var regUsername: EditText
    lateinit var regEmail: EditText
    lateinit var regPhoneNo: EditText
    lateinit var regPassword: EditText
    lateinit var regRepeatPassword: EditText
    lateinit var regTCSCheckBox: CheckBox
    lateinit var regTCSLink: TextView
    lateinit var regBttn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Hooks
        regName = findViewById(R.id.register_fullname)
        regUsername = findViewById(R.id.register_username)
        regEmail = findViewById(R.id.register_email)
        regPhoneNo = findViewById(R.id.register_phone_no)
        regPassword = findViewById(R.id.register_password)
        regRepeatPassword = findViewById(R.id.register_repeat_password)
        regTCSCheckBox = findViewById(R.id.register_tcs_cb)
        regTCSLink = findViewById(R.id.register_tcs_link)
        regBttn = findViewById(R.id.register_signup_button)

        // Register button listener
        regBttn.setOnClickListener {
            registerUser()
        }

        // Terms & Conditions link
        regTCSLink.setOnClickListener {
            // Should go to terms & conditions page or backdrop
            TODO("Yet to be implemented.")
        }

        // Set screen change from signup to login
        val registerGoToLoginButton: View = findViewById<View>(R.id.register_go_to_login)
        registerGoToLoginButton.setOnClickListener {
            goToLogin()
        }
        val backArrow: View = findViewById<View>(R.id.register_back_arrow)
        backArrow.setOnClickListener {
            goToLogin()
        }
    }

    fun registerUser() {
        if (validateName() and validateUsername() and validateEmail() and validatePhoneNo()
            and validatePassword() and validateRepeatPassword() and validateTCS()) {
            // Get values to initialize user
            val name: String = regName.text.toString()
            val username: String = regUsername.text.toString()
            val email: String = removeWhiteSpace(regEmail.text.toString())
            val phoneNo: String = regPhoneNo.text.toString()
            val password: String = removeWhiteSpace(regPassword.text.toString())

            val intent: Intent = Intent(this, VerifyPhoneNo::class.java)
            // Send data to verifyPhone activity
            intent.putExtra("name", name)
            intent.putExtra("username", username)
            intent.putExtra("email", email)
            intent.putExtra("phoneNo", phoneNo)
            intent.putExtra("password", password)

            startActivity(intent)

            /*// Write new user
            if (key != null) {
                myRef.child(key)
                    .setValue(UserHelperClass(name, username, email, phoneNo, password))
            }
            Toast.makeText(this, "Your account has been created successfully!", Toast.LENGTH_SHORT).show()
            goToLogin()*/
        }
    }

    // Removes whitespaces from a string and returns formatted string
    fun removeWhiteSpace(value: String): String {
        return value.trim { it <= ' ' }
    }

    fun validateName(): Boolean {
        var success: Boolean = true
        if (regName.text.toString().isEmpty()) {
            regName.error = "Field cannot be empty!"
            success = false
        }
        return success;
    }

    fun validateUsername(): Boolean {
        val regex: Regex = Regex("^[a-zA-Z0-9-]{5,15}\$")
        var success: Boolean = true
        val username: String = regUsername.text.toString()
        if (username.isEmpty()) {
            regUsername.error = "Field cannot be empty!"
            success = false
        }
        else if (!username.matches(regex)) {
            regUsername.error="Should be 5-15 characters with lower/upper case characters, no white space and special symbol \"-\" only."
            success = false
        }
        return success;
    }

    fun validateEmail(): Boolean {
        val regex: Regex = Regex("[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+")
        var success: Boolean = true
        val email: String = regEmail.text.toString()
        if (email.isEmpty()) {
            regEmail.error = "Field cannot be empty!"
            success = false
        }
        else if (!email.matches(regex)) {
            regEmail.error = "Invalid email address"
            success = false
        }
        return success
    }

    fun validatePhoneNo(): Boolean {
        var success: Boolean = true
        if (regName.text.toString().isEmpty()) {
            regName.error = "Field cannot be empty!"
            success = false
        }
        return success;
    }

    fun validatePassword(): Boolean {
        // Minimum eight characters, at least one letter, one number and one special character
        val regex: Regex = Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\$!%*#?&])[A-Za-z\\d@\$!%*#?&]{8,}\$")
        var success: Boolean = true
        val password: String = regPassword.text.toString()
        if (regPassword.text.toString().isEmpty()) {
            regPassword.error = "Field cannot be empty!"
            success = false
        }
        else if (!password.matches(regex)) {
            regPassword.error = "Password is too weak, include minimum eight characters, at least one letter, " +
                    "one number and one special character"
            success = false
        }
        return success;
    }

    fun validateRepeatPassword(): Boolean {
        var success: Boolean = true
        val repeatPassword: String = regRepeatPassword.text.toString()
        if (regRepeatPassword.text.toString().isEmpty()) {
            regRepeatPassword.error = "Field cannot be empty!"
            success = false
        }
        else if (repeatPassword != regPassword.text.toString()) {
            regRepeatPassword.error = "Passwords don't match!"
            success = false
        }
        return success;
    }

    fun validateTCS(): Boolean {
        var success: Boolean = true
        if (!regTCSCheckBox.isChecked) {
            Toast.makeText(this@SignUp, "Please accept our Terms & Conditions", Toast.LENGTH_SHORT).show()
            success = false
        }
        return success
    }

    fun goToLogin() {
        val intent: Intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
}
