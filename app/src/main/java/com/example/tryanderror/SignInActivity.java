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
import com.google.firebase.auth.GoogleAuthProvider;

public class SignInActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        auth = FirebaseAuth.getInstance();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        TextInputEditText emailField = findViewById(R.id.email);
        TextInputEditText passwordField = findViewById(R.id.password);
        Button signInButton = findViewById(R.id.signInButton);
        Button btnGoogle = findViewById(R.id.btnGoogleSignIn);
        TextView goToRegisterLink = findViewById(R.id.goToRegister);

        TextInputLayout tlEmail = findViewById(R.id.tlEmail);
        TextInputLayout tlPassword = findViewById(R.id.tlPassword);

        signInButton.setOnClickListener(v -> {
            String email = emailField.getText() != null ? emailField.getText().toString().trim() : "";
            String password = passwordField.getText() != null ? passwordField.getText().toString().trim() : "";

            // Reset errors
            tlEmail.setError(null);
            tlPassword.setError(null);

            // 1. Basic Format Validation
            if (email.isEmpty()) {
                tlEmail.setError("Please enter your email");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tlEmail.setError("Invalid email format");
                return;
            }

            if (password.isEmpty()) {
                tlPassword.setError("Please enter your password");
                return;
            }

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            com.google.firebase.auth.FirebaseUser user = auth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                goToNews();
                            } else {
                                auth.signOut();
                                Toast.makeText(SignInActivity.this, "Please verify your email before signing in.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(SignInActivity.this, "Sign In failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnGoogle.setOnClickListener(v -> {
            // Force account selection picker every time
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent signInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            });
        });

        goToRegisterLink.setOnClickListener(v -> {
            startActivity(new Intent(SignInActivity.this, MainActivity.class));
            finish();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null && account.getIdToken() != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        goToNews();
                    } else {
                        Toast.makeText(SignInActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToNews() {
        startActivity(new Intent(this, NewsActivity.class));
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
