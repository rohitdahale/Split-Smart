package com.rohit.splitsmart.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.rohit.splitsmart.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        database = FirebaseDatabase.getInstance().reference

        setUpClickListeners()
    }

    private fun setUpClickListeners() {
        binding.btnSignUp.setOnClickListener {
            handleSignUp()
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnGoogleSignUp.setOnClickListener {
            Toast.makeText(this, "Google Sign Up coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSignUp() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        val fullName = binding.etName.text.toString().trim()

        // Validation checks
        when {
            email.isEmpty() -> {
                binding.tilEmail.error = "Email is Required"
                return
            }
            password.isEmpty() -> {
                binding.tilPassword.error = "Password is required"
                return
            }
            confirmPassword.isEmpty() -> {
                binding.tilConfirmPassword.error = "Confirm password is required"
                return
            }
            fullName.isEmpty() -> {
                binding.tilName.error = "Name is required"
                return
            }
            password != confirmPassword -> {
                binding.tilConfirmPassword.error = "Passwords do not match"
                return
            }
            password.length < 6 -> {
                binding.tilPassword.error = "Password must be at least 6 characters"
                return
            }
        }

        binding.btnSignUp.isEnabled = false

        // Create user with email and password
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    // Save user data to database after successful authentication
                    saveUserData(user?.uid, fullName, email)
                    updateUI(user)
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    updateUI(null)
                }
                binding.btnSignUp.isEnabled = true
            }
    }

    private fun saveUserData(userId: String?, fullName: String, email: String) {
        if (userId != null) {
            val userMap = mapOf(
                "name" to fullName,
                "email" to email
            )

            database.child("Users").child(userId).setValue(userMap)
                .addOnSuccessListener {
                    Log.d(TAG, "User data saved successfully")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving user data: ${e.message}")
                    Toast.makeText(
                        this,
                        "Failed to save user data. Please update profile later.",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            updateUI(currentUser)
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    companion object {
        private const val TAG = "SignUpActivity"
    }
}