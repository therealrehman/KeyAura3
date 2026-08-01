package com.example.animatedkeyboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan

class AboutActivity : AppCompatActivity() {

    private val BG       = Color.parseColor("#06070F")
    private val SURFACE  = Color.parseColor("#0F1120")
    private val SURFACE2 = Color.parseColor("#161828")
    private val BORDER   = Color.parseColor("#1E2240")
    private val ACCENT   = Color.parseColor("#4C8AFF")
    private val TEXT     = Color.parseColor("#E8EAF6")
    private val MUTED    = Color.parseColor("#7B82A8")
    private val SUCCESS  = Color.parseColor("#00C853")
    private val WARNING  = Color.parseColor("#FFC400")

    private lateinit var tabContainer: LinearLayout
    private lateinit var contentScroll: ScrollView
    private lateinit var contentLayout: LinearLayout
    private var selectedTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        window.navigationBarColor = BG

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // Top bar
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(SURFACE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(12), dp(16), dp(12))
        }
        val backBtn = TextView(this).apply {
            text = "←"
            textSize = 22f
            setTextColor(ACCENT)
            setPadding(dp(10), dp(8), dp(16), dp(8))
            setOnClickListener { finish() }
        }
        val topTitle = TextView(this).apply {
            text = "KeyAura"
            textSize = 18f
            setTextColor(TEXT)
            typeface = Typeface.DEFAULT_BOLD
        }
        topBar.addView(backBtn)
        topBar.addView(topTitle)

        // Tab bar
        tabContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(SURFACE)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val tabs = listOf("About", "Privacy", "Terms", "Contact")
        tabs.forEachIndexed { i, name ->
            val tab = TextView(this).apply {
                text = name
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(14), dp(4), dp(12))
                setTextColor(if (i == 0) ACCENT else MUTED)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                tag = i
                setOnClickListener { selectTab(i, tabs.size) }
            }
            tabContainer.addView(tab)
        }

        // Tab indicator line
        val tabLine = View(this).apply {
            setBackgroundColor(BORDER)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
        }

        // Scroll content
        contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(40))
        }
        contentScroll = ScrollView(this).apply {
            addView(contentLayout)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        root.addView(topBar)
        root.addView(tabContainer)
        root.addView(tabLine)
        root.addView(contentScroll)
        setContentView(root)

        val startTab = intent.getIntExtra("tab", 0)
        selectTab(startTab, 4)
    }

    private fun selectTab(index: Int, total: Int) {
        selectedTab = index
        for (i in 0 until tabContainer.childCount) {
            val t = tabContainer.getChildAt(i) as? TextView
            t?.setTextColor(if (i == index) ACCENT else MUTED)
        }
        contentLayout.removeAllViews()
        contentScroll.scrollTo(0, 0)
        when (index) {
            0 -> showAbout()
            1 -> showPrivacy()
            2 -> showTerms()
            3 -> showContact()
        }
    }

    // ─── ABOUT ──────────────────────────────────────────────────────────────

    private fun showAbout() {
        hero("⌨️", "KeyAura", "A next-generation animated keyboard for Android by OKLabs.")

        sectionCard("🚀 What is KeyAura?", """
KeyAura is a fully custom animated Android keyboard built from the ground up using a Canvas-based rendering engine. Every key press triggers a smooth, GPU-accelerated color burst animation — making typing expressive and beautiful.

Unlike other keyboards, KeyAura does not use the standard Android KeyboardView at all. Every pixel is drawn in code for maximum visual polish and performance.
        """.trimIndent(), ACCENT)

        row4("15+\nThemes", "0\nAds", "100%\nOn-Device", "2\nLayouts")

        sectionCard("✨ Key Features", """
• Animated themes with per-key color bursts
• 8 solid themes: Midnight, Forest, Wine, Slate, Royal, White, Grey, Black
• Custom keyboard photo background from your gallery
• Full native Urdu script keyboard (اردو layout)
• Emoji panel with search & recent emojis
• Clipboard history panel with 2-column grid
• Voice typing support (mic key)
• Swipe gesture tunes — custom musical notes
• Birdy Bird mini-game built into the keyboard
• Settings accessible directly from the keyboard
• Zero keystroke logging — nothing you type is ever stored or sent
        """.trimIndent(), SUCCESS)

        sectionCard("🏢 About OKLabs", """
OKLabs is an independent Android development studio focused on building tools that put user experience and privacy first. We are the sole creators and maintainers of KeyAura.

We build apps we use ourselves — which means every bug you find is one we've probably hit too.
        """.trimIndent(), MUTED)

        sectionCard("📦 App Info", """
App Name: KeyAura
Developer: OKLabs
Package: com.example.animatedkeyboard
Category: Keyboard & Input Methods
Platform: Android 7.0 (API 24) and above
Version: See Google Play for latest
        """.trimIndent(), MUTED)
    }

    // ─── PRIVACY POLICY ─────────────────────────────────────────────────────

    private fun showPrivacy() {
        hero("🔒", "Privacy Policy", "Last updated: July 31, 2026")

        sectionCard("📌 Introduction", """
OKLabs ("we", "us", "our") built KeyAura as a free Android keyboard app. This Privacy Policy explains how we handle your information. By using KeyAura, you agree to this policy.

We believe privacy is a right, not a feature. KeyAura is designed from the ground up to process everything locally on your device.
        """.trimIndent(), ACCENT)

        sectionCard("✅ What We NEVER Do", """
We NEVER:
• Record, read, log, or transmit what you type
• Store your passwords, messages, or personal conversations
• Sell your data to any third party
• Share your data with advertisers or data brokers
• Access your contacts, photos, or files
• Track your location
• Collect any personally identifiable information

Your typing stays on your device. Period.
        """.trimIndent(), SUCCESS)

        sectionCard("📱 Data Stored Locally (On Your Device Only)", """
KeyAura stores the following only on your device, never on our servers:

• Your selected theme and color preferences
• Sound, haptic, and volume settings
• Clipboard history (if you use the clipboard panel)
• Recent emoji usage
• Swipe tune selection
• Word preferences for suggestions

All this data stays on your phone and is deleted when you uninstall the app or clear app data.
        """.trimIndent(), MUTED)

        sectionCard("🔑 Permissions Explained", """
INPUT METHOD (Required)
Allows KeyAura to appear as a keyboard. Android's system controls what the keyboard can access — we cannot read secure/password fields.

MICROPHONE (Optional)
Used ONLY when you tap the mic key for voice typing. Processed by Android's built-in speech engine. Never used without your explicit action.

CLIPBOARD (Optional)
Read only when you open the clipboard panel or copy something while the keyboard is visible. Stored locally only.

VIBRATION
For haptic feedback on key presses. No data involved.

INTERNET
Required by Android for the app to run on modern devices. KeyAura itself makes no outbound connections to OKLabs servers.
        """.trimIndent(), WARNING)

        sectionCard("🔗 Third-Party Services", """
Google Play Services: The app is distributed via Google Play, which may collect install/crash metadata per Google's own Privacy Policy.

What we DO NOT use:
✗ Firebase Analytics
✗ Firebase Crashlytics  
✗ AdMob or any ad network
✗ Facebook SDK
✗ Any analytics or tracking SDK

There are NO ads in KeyAura. We do not monetize through advertising.
        """.trimIndent(), MUTED)

        sectionCard("👶 Children's Privacy", """
KeyAura is not directed at children under 13. We do not knowingly collect data from children. Since KeyAura collects no personal data by design, there is inherently no child data at risk.

If you believe a child has been affected, contact us at a.rehmanmazher11@gmail.com and we will address it promptly.
        """.trimIndent(), MUTED)

        sectionCard("⚖️ Your Rights", """
Since we store nothing on our servers, there is no personal profile for us to access or delete. You control all your data:

• Clear app data: Android Settings → Apps → KeyAura → Clear Data
• Clear clipboard: From within the KeyAura clipboard panel
• Delete everything: Uninstall the app

EU/EEA users (GDPR) and California users (CCPA) have the right to know what data we hold. Our honest answer: none on our servers.
        """.trimIndent(), MUTED)

        sectionCard("📢 Policy Updates", """
We may update this policy for legal or feature changes. The updated date will always be shown at the top. Continued use of KeyAura after changes means you accept the updated policy.

Contact: a.rehmanmazher11@gmail.com
        """.trimIndent(), MUTED)
    }

    // ─── TERMS ──────────────────────────────────────────────────────────────

    private fun showTerms() {
        hero("📋", "Terms & Conditions", "Last updated: July 31, 2026")

        sectionCard("1. Acceptance", """
By installing or using KeyAura, you agree to these Terms & Conditions. If you do not agree, please uninstall the app. These Terms form a binding agreement between you and OKLabs.
        """.trimIndent(), ACCENT)

        sectionCard("2. License", """
OKLabs grants you a limited, non-exclusive, non-transferable, revocable license to use KeyAura on your Android device for personal, non-commercial purposes.

You may NOT:
• Reverse engineer, decompile, or extract source code
• Sublicense, sell, or redistribute the app
• Create derivative works
• Use the app for commercial purposes without written consent
        """.trimIndent(), MUTED)

        sectionCard("3. Permitted Use", """
You may use KeyAura to:
• Type text in any Android application
• Use voice typing, emoji, and clipboard features
• Customize themes, sounds, and vibration for personal use
• Use the Urdu script keyboard for personal communication
        """.trimIndent(), SUCCESS)

        sectionCard("4. Prohibited Conduct", """
You agree NOT to:
• Use the app to violate any law or regulation
• Attempt to interfere with or compromise the app's security
• Use automated tools to interact with the app
• Misrepresent the app's origin or ownership
• Use the app in any way that damages OKLabs or its users
        """.trimIndent(), Color.parseColor("#FF5252"))

        sectionCard("5. Intellectual Property", """
KeyAura, OKLabs, and all associated logos, designs, animations, and themes are the exclusive property of OKLabs. You may not use our name or branding without prior written permission.
        """.trimIndent(), MUTED)

        sectionCard("6. Disclaimer of Warranties", """
THE APP IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND. OKLABS DOES NOT WARRANT THAT THE APP WILL BE ERROR-FREE, UNINTERRUPTED, OR MEET YOUR SPECIFIC REQUIREMENTS.

Use of the app is at your own risk.
        """.trimIndent(), WARNING)

        sectionCard("7. Limitation of Liability", """
TO THE MAXIMUM EXTENT PERMITTED BY LAW, OKLABS SHALL NOT BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL, OR CONSEQUENTIAL DAMAGES ARISING FROM YOUR USE OF THE APP.

OKLabs' total liability shall not exceed the amount you paid for the app in the past 12 months (which, since the app is free, is zero).
        """.trimIndent(), MUTED)

        sectionCard("8. Termination", """
OKLabs may terminate your license to use the app at any time if you violate these Terms. Upon termination, you must stop using and delete the app.
        """.trimIndent(), MUTED)

        sectionCard("9. Changes to Terms", """
We may update these Terms at any time. Changes take effect when posted. Continued use of the app after changes means you accept the new Terms.

Contact: a.rehmanmazher11@gmail.com
        """.trimIndent(), MUTED)
    }

    // ─── CONTACT ────────────────────────────────────────────────────────────

    private fun showContact() {
        hero("✉️", "Contact Us", "We read every message — usually within 48 hours.")

        sectionCard("📧 Email Support", "a.rehmanmazher11@gmail.com\n\nTap to open your email app.", ACCENT, clickable = "a.rehmanmazher11@gmail.com")

        sectionCard("⏱ Response Times", """
Bug reports       →  Within 48 hours
Privacy inquiries →  Within 72 hours
Feature requests  →  Reviewed weekly
General questions →  Within 3–5 business days
        """.trimIndent(), MUTED)

        sectionCard("🐛 Reporting Bugs", """
When reporting a bug, please include:

• Your Android version (e.g. Android 14)
• Device model (e.g. Samsung Galaxy A54)
• Steps to reproduce the problem
• What you expected vs what happened
• Screenshot or screen recording if possible

Send to: a.rehmanmazher11@gmail.com
Subject: [KeyAura Bug] Brief description
        """.trimIndent(), Color.parseColor("#FF5252"))

        sectionCard("💡 Feature Requests", """
We love hearing from users! If you have an idea for a new theme, layout, language, or feature, email us at:

a.rehmanmazher11@gmail.com
Subject: [KeyAura Feature] Your idea

We review all requests and implement the most popular ones in future updates.
        """.trimIndent(), SUCCESS)

        sectionCard("🔒 Privacy Inquiries", """
For questions about your data, data deletion requests, or privacy concerns:

a.rehmanmazher11@gmail.com
Subject: [KeyAura Privacy] Your inquiry

We respond to all privacy requests within 72 hours.
        """.trimIndent(), MUTED)

        sectionCard("📱 Find Us on Google Play", """
Search "KeyAura" on Google Play Store or visit:
play.google.com/store/apps

You can also leave a review on the Play Store — it helps other users discover the app and helps us know what to improve.
        """.trimIndent(), ACCENT)
    }

    // ─── HELPER VIEWS ───────────────────────────────────────────────────────

    private fun hero(emoji: String, title: String, subtitle: String) {
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(24), dp(16), dp(24))
            setBackgroundColor(Color.parseColor("#0A0C1A"))
            val bg = android.graphics.drawable.GradientDrawable()
            bg.cornerRadius = dp(16).toFloat()
            bg.setColor(Color.parseColor("#0A0C1A"))
            bg.setStroke(dp(1), BORDER)
            background = bg
        }
        val emojiTv = TextView(this).apply {
            text = emoji
            textSize = 40f
            gravity = Gravity.CENTER
        }
        val titleTv = TextView(this).apply {
            text = title
            textSize = 24f
            setTextColor(TEXT)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(6))
        }
        val subTv = TextView(this).apply {
            text = subtitle
            textSize = 13f
            setTextColor(MUTED)
            gravity = Gravity.CENTER
        }
        v.addView(emojiTv)
        v.addView(titleTv)
        v.addView(subTv)
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(16)
        contentLayout.addView(v, lp)
    }

    private fun sectionCard(title: String, body: String, accentColor: Int, clickable: String? = null) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            val bg = android.graphics.drawable.GradientDrawable()
            bg.cornerRadius = dp(14).toFloat()
            bg.setColor(SURFACE)
            bg.setStroke(dp(1), BORDER)
            background = bg
        }
        val titleTv = TextView(this).apply {
            text = title
            textSize = 15f
            setTextColor(TEXT)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(10))
        }

        val leftAccent = View(this).apply {
            setBackgroundColor(accentColor)
            layoutParams = LinearLayout.LayoutParams(dp(3), ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val bodyTv = TextView(this).apply {
            textSize = 13f
            setTextColor(MUTED)
            lineSpacingMultiplier = 1.5f
            if (clickable != null) {
                val ss = SpannableString(body)
                val start = body.indexOf(clickable)
                if (start >= 0) {
                    ss.setSpan(ForegroundColorSpan(ACCENT), start, start + clickable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ss.setSpan(object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$clickable")))
                        }
                    }, start, start + clickable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                setText(ss)
                movementMethod = LinkMovementMethod.getInstance()
                highlightColor = Color.TRANSPARENT
            } else {
                text = body
            }
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }
        row.addView(leftAccent, LinearLayout.LayoutParams(dp(3), ViewGroup.LayoutParams.WRAP_CONTENT).apply { rightMargin = dp(12) })
        row.addView(bodyTv, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        card.addView(titleTv)
        card.addView(row)

        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(12)
        contentLayout.addView(card, lp)
    }

    private fun row4(vararg items: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = items.size.toFloat()
        }
        items.forEach { item ->
            val parts = item.split("\n")
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(16), dp(8), dp(16))
                val bg = android.graphics.drawable.GradientDrawable()
                bg.cornerRadius = dp(12).toFloat()
                bg.setColor(SURFACE)
                bg.setStroke(dp(1), BORDER)
                background = bg
            }
            val num = TextView(this).apply {
                text = parts[0]
                textSize = 22f
                setTextColor(ACCENT)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }
            val lbl = TextView(this).apply {
                text = parts.getOrElse(1) { "" }
                textSize = 10f
                setTextColor(MUTED)
                gravity = Gravity.CENTER
                setPadding(0, dp(4), 0, 0)
            }
            cell.addView(num)
            cell.addView(lbl)
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            lp.setMargins(dp(4), 0, dp(4), 0)
            row.addView(cell, lp)
        }
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(12)
        contentLayout.addView(row, lp)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
