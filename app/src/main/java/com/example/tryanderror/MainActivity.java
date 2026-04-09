package com.example.tryanderror;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_UP = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        auth = FirebaseAuth.getInstance();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        TextInputEditText emailField = findViewById(R.id.email);
        TextInputEditText passwordField = findViewById(R.id.password);
        Button registerButton = findViewById(R.id.registerButton);
        Button btnGoogle = findViewById(R.id.btnGoogleSignUp);
        TextView goToSignInLink = findViewById(R.id.goToSignIn);

        TextInputLayout tlFullName = findViewById(R.id.tlFullName);
        TextInputLayout tlEmail = findViewById(R.id.tlEmail);
        TextInputLayout tlPassword = findViewById(R.id.tlPassword);

        registerButton.setOnClickListener(v -> {
            String email = emailField.getText() != null ? emailField.getText().toString().trim() : "";
            String password = passwordField.getText() != null ? passwordField.getText().toString().trim() : "";
            
            TextInputEditText fullNameField = findViewById(R.id.fullName);
            String fullName = fullNameField.getText() != null ? fullNameField.getText().toString().trim() : "";

            // Reset errors
            tlFullName.setError(null);
            tlEmail.setError(null);
            tlPassword.setError(null);

            // 1. Validate Full Name
            if (fullName.isEmpty()) {
                tlFullName.setError("Please enter your name");
                return;
            }

            // 2. Strong Email Validation
            if (email.isEmpty()) {
                tlEmail.setError("Email address is required");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tlEmail.setError("Enter a valid email address");
                return;
            }

            // 3. Robust Password Validation
            String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

            if (password.isEmpty()) {
                tlPassword.setError("Password is required");
                return;
            }
            if (!password.matches(passwordPattern)) {
                tlPassword.setError("Password must be 8+ chars with Upper, Lower, Number, and Special character");
                return;
            }

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Save Name to Profile
                            UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build();
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                user.updateProfile(profileUpdate);
                                
                                // Feature: Welcome & Verification Email
                                user.sendEmailVerification().addOnCompleteListener(emailTask -> {
                                    if (emailTask.isSuccessful()) {
                                        Toast.makeText(MainActivity.this, "Check your inbox! Please verify your email before signing in. \uD83D\uDCE7", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(MainActivity.this, "Account created! Please verify your email. \uD83D\uDD25", Toast.LENGTH_SHORT).show();
                                    }
                                    
                                    // Block automatic login!
                                    auth.signOut();
                                    startActivity(new Intent(MainActivity.this, SignInActivity.class));
                                    finish();
                                });
                            }
                        } else {
                            Exception exception = task.getException();
                            String error;
                            if (exception instanceof FirebaseAuthUserCollisionException) {
                                error = "This email is already registered. Sign in! \uD83D\uDEE1\uFE0F";
                            } else if (exception instanceof FirebaseAuthWeakPasswordException) {
                                error = "Password too weak. Choose a stronger one! \uD83D\uDD10";
                            } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                                error = "Invalid email format. Check your address! \uD83D\uDCE7";
                            } else {
                                error = exception != null && exception.getMessage() != null ? exception.getMessage() : "Sign Up failed";
                            }
                            Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnGoogle.setOnClickListener(v -> {
            // Force account selection picker every time
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent signInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_UP);
            });
        });

        goToSignInLink.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SignInActivity.class));
            finish();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_UP) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null && account.getIdToken() != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-Up failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Welcome to IgniteNews \uD83D\uDD25", Toast.LENGTH_SHORT).show();
                        goToNews();
                    } else {
                        Toast.makeText(MainActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToNews() {
        startActivity(new Intent(this, NewsActivity.class));
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
