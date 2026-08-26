package com.example.animatedkeyboard

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {

    private var currentStep = 0
    private val steps = listOf(
        Step(
            emoji = "⌨️",
            title = "Welcome to KeyAura!",
            desc = "The most beautiful animated keyboard for Android.\n\nLet's set it up in 2 simple steps.",
            btnText = "Let's Go →"
        ),
        Step(
            emoji = "⚙️",
            title = "Step 1: Enable KeyAura",
            desc = "Go to Settings → Language & Input → Virtual Keyboard → Manage Keyboards\n\nTurn ON KeyAura keyboard.",
            btnText = "Open Settings →",
            action = "open_settings"
        ),
        Step(
            emoji = "✅",
            title = "Step 2: Select KeyAura",
            desc = "Tap any text field anywhere on your phone.\n\nTap the keyboard icon at the bottom → Select KeyAura.",
            btnText = "Select Keyboard →",
            action = "select_keyboard"
        ),
        Step(
            emoji = "🎨",
            title = "Choose Your Theme",
            desc = "Pick from animated themes like Aurora, Ember, Ocean — or solid colors.\n\nWatch a short ad to unlock animated themes for 3 hours!",
            btnText = "Go to Themes →",
            action = "go_main"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If onboarding already done, skip to MainActivity
        val prefs = getSharedPreferences("keyaura_onboarding", MODE_PRIVATE)
        if (prefs.getBoolean("completed", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0A0D1A"))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(48), dp(32), dp(48))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val emojiView = TextView(this).apply {
            textSize = 64f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(24) }
        }

        val titleView = TextView(this).apply {
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(16) }
        }

        val descView = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.parseColor("#AAB0C8"))
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            lineHeight = dp(26)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(48) }
        }

        val btnNext = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#4488FF"))
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(16), dp(24), dp(16))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(12f).toFloat()
                setColor(Color.parseColor("#4488FF"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(16) }
        }

        val skipBtn = TextView(this).apply {
            text = "Skip →"
            textSize = 13f
            setTextColor(Color.parseColor("#556080"))
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val dotsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(32) }
        }

        fun updateDots() {
            dotsContainer.removeAllViews()
            steps.indices.forEach { i ->
                val dot = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).also {
                        it.marginEnd = dp(6)
                    }
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setColor(if (i == currentStep) Color.parseColor("#4488FF") else Color.parseColor("#2A2D3E"))
                    }
                }
                dotsContainer.addView(dot)
            }
        }

        fun updateStep() {
            val step = steps[currentStep]
            emojiView.text = step.emoji
            titleView.text = step.title
            descView.text = step.desc
            btnNext.text = step.btnText
            skipBtn.visibility = if (currentStep == 0) View.VISIBLE else View.GONE
            updateDots()
        }

        btnNext.setOnClickListener {
            val step = steps[currentStep]
            when (step.action) {
                "open_settings" -> {
                    startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                    currentStep++
                    updateStep()
                }
                "select_keyboard" -> {
                    (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
                    currentStep++
                    updateStep()
                }
                "go_main" -> {
                    getSharedPreferences("keyaura_onboarding", MODE_PRIVATE)
                        .edit().putBoolean("completed", true).apply()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                else -> {
                    if (currentStep < steps.size - 1) {
                        currentStep++
                        updateStep()
                    }
                }
            }
        }

        skipBtn.setOnClickListener {
            getSharedPreferences("keyaura_onboarding", MODE_PRIVATE)
                .edit().putBoolean("completed", true).apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        container.addView(emojiView)
        container.addView(titleView)
        container.addView(descView)
        container.addView(dotsContainer)
        container.addView(btnNext)
        container.addView(skipBtn)
        root.addView(container)
        setContentView(root)
        updateStep()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun dp(v: Float) = (v * resources.displayMetrics.density)

    data class Step(
        val emoji: String,
        val title: String,
        val desc: String,
        val btnText: String,
        val action: String = "next"
    )
}
