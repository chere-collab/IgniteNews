package com.example.tryanderror

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import android.util.Patterns
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_UP = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_main)

        auth = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val emailField = findViewById<TextInputEditText>(R.id.email)
        val passwordField = findViewById<TextInputEditText>(R.id.password)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val btnGoogle = findViewById<Button>(R.id.btnGoogleSignUp)
        val goToSignInLink = findViewById<TextView>(R.id.goToSignIn)

        val tlFullName = findViewById<TextInputLayout>(R.id.tlFullName)
        val tlEmail = findViewById<TextInputLayout>(R.id.tlEmail)
        val tlPassword = findViewById<TextInputLayout>(R.id.tlPassword)

        registerButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val fullNameField = findViewById<TextInputEditText>(R.id.fullName)
            val fullName = fullNameField.text.toString().trim()

            // Reset errors
            tlFullName.error = null
            tlEmail.error = null
            tlPassword.error = null

            // 1. Validate Full Name
            if (fullName.isEmpty()) {
                tlFullName.error = "Please enter your name"
                return@setOnClickListener
            }

            // 2. Strong Email Validation
            if (email.isEmpty()) {
                tlEmail.error = "Email address is required"
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tlEmail.error = "Enter a valid email address"
                return@setOnClickListener
            }

            // 3. Robust Password Validation
            val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$".toRegex()
            
            if (password.isEmpty()) {
                tlPassword.error = "Password is required"
                return@setOnClickListener
            }
            if (!password.matches(passwordPattern)) {
                tlPassword.error = "Password must be 8+ chars with Upper, Lower, Number, and Special character"
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Save Name to Profile
                        val profileUpdate = com.google.firebase.auth.userProfileChangeRequest {
                            displayName = fullName
                        }
                        auth.currentUser?.updateProfile(profileUpdate)
                        
                        Toast.makeText(this, "Account created! Welcome 🔥", Toast.LENGTH_LONG).show()
                        goToNews()
                    } else {
                        val error = when (task.exception) {
                            is com.google.firebase.auth.FirebaseAuthUserCollisionException -> 
                                "This email is already registered. Sign in! 🛡️"
                            is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> 
                                "Password too weak. Choose a stronger one! 🔐"
                            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> 
                                "Invalid email format. Check your address! 📧"
                            else -> task.exception?.message ?: "Sign Up failed"
                        }
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                    }
                }
        }

        btnGoogle.setOnClickListener {
            // Force account selection picker every time
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_UP)
            }
        }

        goToSignInLink.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_UP) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-Up failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Welcome to IgniteNews 🔥", Toast.LENGTH_SHORT).show()
                    goToNews()
                } else {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun goToNews() {
        startActivity(Intent(this, NewsActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
