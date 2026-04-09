package com.example.tryanderror;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.Pair;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvEmail;
    private Button btnLogout;
    private Spinner spinnerCategory;
    private Spinner spinnerLanguage;
    private CircleImageView ivProfile;
    private FloatingActionButton btnChangePhoto;
    private TextView tvReadStats;
    private Button btnExportBookmarks;
    private SwitchMaterial switchDarkMode;

    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseStorage storage = FirebaseStorage.getInstance();

    private static final String PREFS_NAME = "news_prefs";
    private static final String KEY_CATEGORY = "default_category";
    private static final String KEY_LANGUAGE = "default_language";
    private static final String KEY_DARK_MODE = "dark_mode";

    private List<Pair<String, String>> categories = new ArrayList<>();
    private List<Pair<String, String>> languages = new ArrayList<>();

    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadImage(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        categories.add(new Pair<>("Technology", "technology"));
        categories.add(new Pair<>("Politics", "politics_search"));
        categories.add(new Pair<>("Business", "business"));
        categories.add(new Pair<>("Sports", "sports"));
        categories.add(new Pair<>("Entertainment", "entertainment"));
        categories.add(new Pair<>("Science", "science"));
        categories.add(new Pair<>("Health", "health"));

        languages.add(new Pair<>("English", "en"));
        languages.add(new Pair<>("Arabic", "ar"));
        languages.add(new Pair<>("French", "fr"));
        languages.add(new Pair<>("Spanish", "es"));
        languages.add(new Pair<>("German", "de"));

        Toolbar toolbar = findViewById(R.id.settingsToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        tvEmail = findViewById(R.id.tvUserEmail);
        btnLogout = findViewById(R.id.btnLogout);
        spinnerCategory = findViewById(R.id.spinnerDefaultCategory);
        spinnerLanguage = findViewById(R.id.spinnerDefaultLanguage);
        ivProfile = findViewById(R.id.ivProfile);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        tvReadStats = findViewById(R.id.tvReadStats);
        btnExportBookmarks = findViewById(R.id.btnExportBookmarks);
        switchDarkMode = findViewById(R.id.switchDarkMode);

        FirebaseUser user = auth.getCurrentUser();
        tvEmail.setText(user != null && user.getEmail() != null ? user.getEmail() : "Not logged in");

        // Load Profile Image
        loadProfileImage();

        // Load Stats
        loadReadingStats();

        setupSpinners();
        setupDarkMode();

        btnChangePhoto.setOnClickListener(v -> pickImage.launch("image/*"));

        btnExportBookmarks.setOnClickListener(v -> exportBookmarks());

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btnPrivacyPolicy).setOnClickListener(v -> startActivity(new Intent(SettingsActivity.this, LegalActivity.class)));

        findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> showDeleteAccountConfirmation());
    }

    private void showDeleteAccountConfirmation() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reauth, null);
        TextInputEditText passwordInput = dialogView.findViewById(R.id.etReauthPassword);
        TextInputLayout tlPassword = dialogView.findViewById(R.id.tlReauthPassword);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("PERMANENTLY DELETE", null) // Set null to handle click manually
                .setNegativeButton("CANCEL", null)
                .create();

        dialog.show();

        // Handle positive button manually to keep dialog open on validation error
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String password = passwordInput.getText() != null ? passwordInput.getText().toString() : "";
            tlPassword.setError(null);

            if (!password.isEmpty()) {
                dialog.dismiss();
                reauthenticateAndDelete(password);
            } else {
                tlPassword.setError("Password required to confirm deletion.");
            }
        });
    }

    private void reauthenticateAndDelete(String password) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 1. Delete Firestore Data
                db.collection("users").document(user.getUid()).delete();

                // 2. Delete Auth Account
                user.delete().addOnCompleteListener(deleteIdx -> {
                    if (deleteIdx.isSuccessful()) {
                        Toast.makeText(SettingsActivity.this, "Account and data permanently deleted.", Toast.LENGTH_SHORT).show();
                        auth.signOut();
                        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
            } else {
                Toast.makeText(SettingsActivity.this, "Authentication Failed: Incorrect password.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProfileImage() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // Load from completely free local SharedPreferences instead of Firebase
        SharedPreferences prefs = getSharedPreferences("UserProfilePrefs", MODE_PRIVATE);
        String base64String = prefs.getString("profile_" + user.getUid(), null);

        if (base64String != null && !base64String.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(base64String, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                ivProfile.setImageBitmap(bitmap);
            } catch (Exception e) {
                if (user.getPhotoUrl() != null) {
                    Glide.with(this).load(user.getPhotoUrl()).into(ivProfile);
                }
            }
        } else if (user.getPhotoUrl() != null) {
            // Fallback to Google profile photo
            Glide.with(this).load(user.getPhotoUrl()).into(ivProfile);
        }
    }

    private void uploadImage(Uri uri) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        Toast.makeText(this, "Saving profile picture...", Toast.LENGTH_SHORT).show();

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            int maxDim = 300;
            float scale = (float) maxDim / Math.max(bitmap.getWidth(), bitmap.getHeight());
            Bitmap scaledBitmap = scale < 1 ? Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * scale), (int) (bitmap.getHeight() * scale), true) : bitmap;

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream);
            String base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);

            SharedPreferences prefs = getSharedPreferences("UserProfilePrefs", MODE_PRIVATE);
            prefs.edit().putString("profile_" + user.getUid(), base64String).apply();

            Toast.makeText(this, "Profile picture saved successfully!", Toast.LENGTH_SHORT).show();
            loadProfileImage();

        } catch (Exception e) {
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSpinners() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Category Spinner
        List<String> catNames = new ArrayList<>();
        for (Pair<String, String> category : categories) {
            catNames.add(category.first);
        }
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, catNames);
        spinnerCategory.setAdapter(catAdapter);

        String savedCat = prefs.getString(KEY_CATEGORY, "technology");
        int savedCatIndex = 0;
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).second.equals(savedCat)) {
                savedCatIndex = i;
                break;
            }
        }
        spinnerCategory.setSelection(savedCatIndex);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putString(KEY_CATEGORY, categories.get(position).second).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Language Spinner
        List<String> langNames = new ArrayList<>();
        for (Pair<String, String> language : languages) {
            langNames.add(language.first);
        }
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, langNames);
        spinnerLanguage.setAdapter(langAdapter);

        String savedLangCode = prefs.getString(KEY_LANGUAGE, "en");
        int savedLangIndex = 0;
        for (int i = 0; i < languages.size(); i++) {
            if (languages.get(i).second.equals(savedLangCode)) {
                savedLangIndex = i;
                break;
            }
        }
        spinnerLanguage.setSelection(savedLangIndex);

        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putString(KEY_LANGUAGE, languages.get(position).second).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupDarkMode() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        switchDarkMode.setChecked(isDarkMode);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
            int mode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            AppCompatDelegate.setDefaultNightMode(mode);
        });
    }

    private void loadReadingStats() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        String userId = user.getUid();

        Timestamp oneWeekAgo = new Timestamp(new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L));

        db.collection("users").document(userId).collection("views")
                .whereGreaterThan("timestamp", oneWeekAgo)
                .get()
                .addOnSuccessListener(result -> {
                    int count = result.size();
                    tvReadStats.setText("You've read " + count + " articles this week! \uD83D\uDCDA");
                })
                .addOnFailureListener(e -> tvReadStats.setText("Could not load stats."));
    }

    private void exportBookmarks() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        String userId = user.getUid();

        Toast.makeText(this, "Exporting bookmarks...", Toast.LENGTH_SHORT).show();

        db.collection("users").document(userId).collection("bookmarks")
                .get()
                .addOnSuccessListener(result -> {
                    if (result.isEmpty()) {
                        Toast.makeText(SettingsActivity.this, "No bookmarks to export!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    StringBuilder report = new StringBuilder("My Bookmarked Articles:\n\n");
                    for (DocumentSnapshot doc : result) {
                        String title = doc.getString("title");
                        String url = doc.getString("url");
                        report.append("- ").append(title).append("\n  ").append(url).append("\n\n");
                    }

                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Bookmarks Report");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, report.toString());
                    startActivity(Intent.createChooser(shareIntent, "Save or Send Report"));
                })
                .addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "Export failed.", Toast.LENGTH_SHORT).show());
    }
}
