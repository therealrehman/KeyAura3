package com.example.animatedkeyboard

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Display app version
        val tvVersion = findViewById<TextView>(R.id.tvVersion)
        try {
            val pkgInfo = packageManager.getPackageInfo(packageName, 0)
            tvVersion.text = pkgInfo.versionName
        } catch (e: Exception) {
            tvVersion.text = "1.0.0"
        }

        findViewById<CardView>(R.id.btnAbout).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
        findViewById<CardView>(R.id.btnContactUs).setOnClickListener {
            startActivity(Intent(this, ContactUsActivity::class.java))
        }
        findViewById<CardView>(R.id.btnPrivacyPolicy).setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }
    }
}
