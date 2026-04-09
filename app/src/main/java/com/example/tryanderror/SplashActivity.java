package com.example.tryanderror;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        LinearLayout logoContainer = findViewById(R.id.logoContainer);

        // Premium Fade-in Animation
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(1500);
        logoContainer.startAnimation(fadeIn);

        // Delay for 2.5 seconds to show brand splash
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                checkDestination();
            }
        }, 2500);
    }

    private void checkDestination() {
        SharedPreferences prefs = getSharedPreferences("news_prefs", Context.MODE_PRIVATE);
        boolean isFirstTime = prefs.getBoolean("isFirstTime", true);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        Intent intent;
        if (isFirstTime) {
            intent = new Intent(this, OnboardingActivity.class);
        } else if (user != null) {
            intent = new Intent(this, NewsActivity.class);
        } else {
            intent = new Intent(this, SignInActivity.class);
        }

        startActivity(intent);
        finish();

        // Sleek cross-fade transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
