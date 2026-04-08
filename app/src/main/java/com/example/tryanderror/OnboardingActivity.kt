package com.example.tryanderror

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

data class OnboardingItem(val title: String, val description: String, val imageRes: Int)

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val items = listOf(
            OnboardingItem("Modern Tech News", "Get the latest updates from top sources categorized just for you.", android.R.drawable.ic_dialog_info),
            OnboardingItem("Voice Reader", "Don't just read the news, listen to it while you're on the go!", android.R.drawable.ic_lock_silent_mode_off),
            OnboardingItem("Real-time Alerts", "Stay ahead with push notifications for the biggest breaking stories.", android.R.drawable.ic_popup_reminder)
        )

        val viewPager = findViewById<ViewPager2>(R.id.onboardingViewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val btnStart = findViewById<Button>(R.id.btnStart)

        viewPager.adapter = OnboardingAdapter(items)
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        btnStart.setOnClickListener {
            val prefs = getSharedPreferences("news_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("isFirstTime", false).apply()
            
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}

class OnboardingAdapter(private val items: List<OnboardingItem>) : RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivOnboardingImage)
        val tvTitle: TextView = view.findViewById(R.id.tvOnboardingTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvOnboardingDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.ivImage.setImageResource(item.imageRes)
        holder.tvTitle.text = item.title
        holder.tvDesc.text = item.description
    }

    override fun getItemCount() = items.size
}
