package com.example.tryanderror;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.Arrays;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        List<OnboardingItem> items = Arrays.asList(
                new OnboardingItem("Modern Tech News", "Get the latest updates from top sources categorized just for you.", android.R.drawable.ic_dialog_info),
                new OnboardingItem("Voice Reader", "Don't just read the news, listen to it while you're on the go!", android.R.drawable.ic_lock_silent_mode_off),
                new OnboardingItem("Real-time Alerts", "Stay ahead with push notifications for the biggest breaking stories.", android.R.drawable.ic_popup_reminder)
        );

        ViewPager2 viewPager = findViewById(R.id.onboardingViewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        Button btnStart = findViewById(R.id.btnStart);

        viewPager.setAdapter(new OnboardingAdapter(items));
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {}).attach();

        btnStart.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("news_prefs", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("isFirstTime", false).apply();

            startActivity(new Intent(OnboardingActivity.this, MainActivity.class));
            finish();
        });
    }
}
