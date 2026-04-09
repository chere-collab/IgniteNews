package com.example.tryanderror;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class LegalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal);

        Toolbar toolbar = findViewById(R.id.legalToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        TextView tvContent = findViewById(R.id.tvLegalContent);
        tvContent.setText(buildPolicyText());

        findViewById(R.id.btnAgreeLegal).setOnClickListener(v -> finish());
    }

    private String buildPolicyText() {
        return "1. 📊 INFORMATION WE COLLECT\n" +
               "IgniteNews collects your email and name via Google or Firebase Authentication. This is exclusively used for account identification and profile personalization across devices. \n\n" +
               "2. 🛡️ DATA SECURITY & SHARING\n" +
               "We leverage Google Cloud’s military-grade security (Firebase) to protect your identity. We do not sell your personal data to third parties. We use NewsAPI.org to provide the news you read; they do not receive your personal account details.\n\n" +
               "3. 🛰️ THIRD-PARTY SERVICES\n" +
               "Our app integrates with:\n" +
               "- Firebase (Authentication, Analytics, Firestore)\n" +
               "- NewsAPI.org (News Content)\n" +
               "Please consult their respective privacy policies for further details.\n\n" +
               "4. 🗑️ THE RIGHT TO BE FORGOTTEN\n" +
               "In accordance with global privacy standards (GDPR/CCPA), you can permanently delete your account and all associated cloud data via the 'Profile Settings' menu at any time.\n\n" +
               "5. 🔑 TERMS OF USAGE\n" +
               "IgniteNews is provided 'as is' for informational purposes. Users are expected to consume content responsibly. We reserve the right to updates to these terms to maintain excellence.\n\n" +
               "By using IgniteNews, you agree to these transparent privacy standards.";
    }
}
