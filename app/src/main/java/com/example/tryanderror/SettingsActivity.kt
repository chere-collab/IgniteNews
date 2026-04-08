package com.example.tryanderror

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth

import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvEmail: TextView
    private lateinit var btnLogout: Button
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerLanguage: Spinner
    private lateinit var ivProfile: CircleImageView
    private lateinit var btnChangePhoto: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var tvReadStats: TextView
    private lateinit var btnExportBookmarks: android.widget.Button
    private lateinit var switchDarkMode: com.google.android.material.switchmaterial.SwitchMaterial
    
    private val auth = FirebaseAuth.getInstance()
    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    private val PREFS_NAME = "news_prefs"
    private val KEY_CATEGORY = "default_category"
    private val KEY_LANGUAGE = "default_language"
    private val KEY_DARK_MODE = "dark_mode"

    private val categories = listOf("Technology" to "technology", "Politics" to "politics_search", "Business" to "business", "Sports" to "sports", "Entertainment" to "entertainment", "Science" to "science", "Health" to "health")
    private val languages = listOf("English" to "en", "Arabic" to "ar", "French" to "fr", "Spanish" to "es", "German" to "de")

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.settingsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        tvEmail = findViewById(R.id.tvUserEmail)
        btnLogout = findViewById(R.id.btnLogout)
        spinnerCategory = findViewById(R.id.spinnerDefaultCategory)
        spinnerLanguage = findViewById(R.id.spinnerDefaultLanguage)
        ivProfile = findViewById(R.id.ivProfile)
        btnChangePhoto = findViewById(R.id.btnChangePhoto)
        tvReadStats = findViewById(R.id.tvReadStats)
        btnExportBookmarks = findViewById(R.id.btnExportBookmarks)
        switchDarkMode = findViewById(R.id.switchDarkMode)

        val user = auth.currentUser
        tvEmail.text = user?.email ?: "Not logged in"
        
        // Load Profile Image
        loadProfileImage()

        // Load Stats
        loadReadingStats()

        setupSpinners()
        setupDarkMode()

        btnChangePhoto.setOnClickListener {
            pickImage.launch("image/*")
        }

        btnExportBookmarks.setOnClickListener {
            exportBookmarks()
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        findViewById<View>(R.id.btnPrivacyPolicy).setOnClickListener {
            val intent = Intent(this, LegalActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.btnDeleteAccount).setOnClickListener {
            showDeleteAccountConfirmation()
        }
    }

    private fun showDeleteAccountConfirmation() {
        val user = auth.currentUser ?: return
        val dialogView = layoutInflater.inflate(R.layout.dialog_reauth, null)
        val passwordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etReauthPassword)
        val tlPassword = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tlReauthPassword)

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("PERMANENTLY DELETE", null) // Set null to handle click manually
            .setNegativeButton("CANCEL", null)
            .create()

        dialog.show()

        // Handle positive button manually to keep dialog open on validation error
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val password = passwordInput.text.toString()
            tlPassword.error = null

            if (password.isNotEmpty()) {
                dialog.dismiss()
                reauthenticateAndDelete(password)
            } else {
                tlPassword.error = "Password required to confirm deletion."
            }
        }
    }

    private fun reauthenticateAndDelete(password: String) {
        val user = auth.currentUser ?: return
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, password)

        user.reauthenticate(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // 1. Delete Firestore Data
                db.collection("users").document(user.uid).delete()
                
                // 2. Delete Auth Account
                user.delete().addOnCompleteListener { deleteIdx ->
                    if (deleteIdx.isSuccessful) {
                        Toast.makeText(this, "Account and data permanently deleted.", Toast.LENGTH_SHORT).show()
                        auth.signOut()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            } else {
                Toast.makeText(this, "Authentication Failed: Incorrect password.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProfileImage() {
        val user = auth.currentUser ?: return
        
        // Load from completely free local SharedPreferences instead of Firebase
        val prefs = getSharedPreferences("UserProfilePrefs", MODE_PRIVATE)
        val base64String = prefs.getString("profile_${user.uid}", null)
        
        if (base64String != null && base64String.isNotEmpty()) {
            try {
                val bytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ivProfile.setImageBitmap(bitmap)
            } catch (e: Exception) {
                if (user.photoUrl != null) Glide.with(this).load(user.photoUrl).into(ivProfile)
            }
        } else if (user.photoUrl != null) {
            // Fallback to Google profile photo
            Glide.with(this).load(user.photoUrl).into(ivProfile)
        }
    }

    private fun uploadImage(uri: android.net.Uri) {
        val user = auth.currentUser ?: return
        Toast.makeText(this, "Saving profile picture...", Toast.LENGTH_SHORT).show()
        
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            
            val maxDim = 300
            val scale = maxDim.toFloat() / Math.max(bitmap.width, bitmap.height)
            val scaledBitmap = if (scale < 1) {
                android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else bitmap

            val outputStream = java.io.ByteArrayOutputStream()
            // Compress heavily
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, outputStream)
            val base64String = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.DEFAULT)

            // Save totally free into local SharedPreferences
            val prefs = getSharedPreferences("UserProfilePrefs", MODE_PRIVATE)
            prefs.edit().putString("profile_${user.uid}", base64String).apply()
            
            Toast.makeText(this, "Profile picture saved successfully!", Toast.LENGTH_SHORT).show()
            loadProfileImage()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSpinners() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Category Spinner
        val catNames = categories.map { it.first }
        val catAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, catNames)
        spinnerCategory.adapter = catAdapter
        val savedCat = prefs.getString(KEY_CATEGORY, "technology")
        val savedCatIndex = categories.indexOfFirst { it.second == savedCat }
        spinnerCategory.setSelection(if (savedCatIndex != -1) savedCatIndex else 0)

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                prefs.edit().putString(KEY_CATEGORY, categories[position].second).apply()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // Language Spinner
        val langNames = languages.map { it.first }
        val langAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, langNames)
        spinnerLanguage.adapter = langAdapter
        val savedLangCode = prefs.getString(KEY_LANGUAGE, "en")
        val savedLangIndex = languages.indexOfFirst { it.second == savedLangCode }
        spinnerLanguage.setSelection(if (savedLangIndex != -1) savedLangIndex else 0)

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                prefs.edit().putString(KEY_LANGUAGE, languages[position].second).apply()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun setupDarkMode() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false)
        switchDarkMode.isChecked = isDarkMode
        
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply()
            val mode = if (isChecked) {
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            } else {
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            }
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    private fun loadReadingStats() {
        val userId = auth.currentUser?.uid ?: return
        val oneWeekAgo = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000))
        
        db.collection("users").document(userId).collection("views")
            .whereGreaterThan("timestamp", oneWeekAgo)
            .get()
            .addOnSuccessListener { result ->
                val count = result.size()
                tvReadStats.text = "You've read $count articles this week! 📚"
            }
            .addOnFailureListener {
                tvReadStats.text = "Could not load stats."
            }
    }

    private fun exportBookmarks() {
        val userId = auth.currentUser?.uid ?: return
        Toast.makeText(this, "Exporting bookmarks...", Toast.LENGTH_SHORT).show()
        
        db.collection("users").document(userId).collection("bookmarks")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Toast.makeText(this, "No bookmarks to export!", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                
                val report = StringBuilder("My Bookmarked Articles:\n\n")
                for (doc in result) {
                    val title = doc.getString("title")
                    val url = doc.getString("url")
                    report.append("- $title\n  $url\n\n")
                }
                
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Bookmarks Report")
                shareIntent.putExtra(Intent.EXTRA_TEXT, report.toString())
                startActivity(Intent.createChooser(shareIntent, "Save or Send Report"))
            }
            .addOnFailureListener {
                Toast.makeText(this, "Export failed.", Toast.LENGTH_SHORT).show()
            }
    }
}
