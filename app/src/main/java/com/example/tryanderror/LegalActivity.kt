package com.example.tryanderror

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class LegalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_legal)

        val toolbar = findViewById<Toolbar>(R.id.legalToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val tvContent = findViewById<TextView>(R.id.tvLegalContent)
        tvContent.text = buildPolicyText()

        findViewById<android.view.View>(R.id.btnAgreeLegal).setOnClickListener {
            finish()
        }
    }

    private fun buildPolicyText(): String {
        return """
            1. 📊 INFORMATION WE COLLECT
            IgniteNews collects your email and name via Google or Firebase Authentication. This is exclusively used for account identification and profile personalization across devices. 

            2. 🛡️ DATA SECURITY & SHARING
            We leverage Google Cloud’s military-grade security (Firebase) to protect your identity. We do not sell your personal data to third parties. We use NewsAPI.org to provide the news you read; they do not receive your personal account details.

            3. 🛰️ THIRD-PARTY SERVICES
            Our app integrates with:
            - Firebase (Authentication, Analytics, Firestore)
            - NewsAPI.org (News Content)
            - Wikipedia REST API (Deep Research Facts)
            Please consult their respective privacy policies for further details.

            4. 🗑️ THE RIGHT TO BE FORGOTTEN
            In accordance with global privacy standards (GDPR/CCPA), you can permanently delete your account and all associated cloud data via the 'Profile Settings' menu at any time.

            5. 🔑 TERMS OF USAGE
            IgniteNews is provided 'as is' for informational purposes. Users are expected to consume content responsibly. We reserve the right to updates to these terms to maintain excellence.

            By using IgniteNews, you agree to these transparent privacy standards.
        """.trimIndent()
    }
}
