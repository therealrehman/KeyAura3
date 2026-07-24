package com.example.animatedkeyboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class ContactUsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_us)

        findViewById<CardView>(R.id.btnSendEmail).setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:a.rehmanmazher11@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "KeyAura Feedback")
            }
            startActivity(Intent.createChooser(emailIntent, "Send Email"))
        }
    }
}
