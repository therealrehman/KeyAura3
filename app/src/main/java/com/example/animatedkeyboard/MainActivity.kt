package com.example.animatedkeyboard

import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.animatedkeyboard.settings.KeyboardSettings

class MainActivity : AppCompatActivity() {

    private val settings by lazy { KeyboardSettings.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // FIX: btnEnable/btnChoose are LinearLayout containers in the XML (icon + two
        // lines of text + chevron), not Button widgets — findViewById<Button> on them
        // threw a ClassCastException on every launch.
        val btnEnable = findViewById<LinearLayout>(R.id.btnEnable)
        val btnChoose = findViewById<LinearLayout>(R.id.btnChoose)
        val btnAbout = findViewById<TextView>(R.id.btnAbout)
        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)
        val logoText = findViewById<TextView>(R.id.logoText)

        applyLogoGradient(logoText)

        btnEnable.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        btnChoose.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        btnMenu.setOnClickListener {
            // Show about dialog or navigate to about
            startActivity(Intent(this, AboutActivity::class.java))
        }

        // FIX: Sound + Vibration toggles moved here from the old in-keyboard
        // Settings panel — that key on the keyboard now opens the emoji panel.
        val btnToggleSound = findViewById<LinearLayout>(R.id.btnToggleSound)
        val btnToggleVibration = findViewById<LinearLayout>(R.id.btnToggleVibration)

        btnToggleSound.setOnClickListener {
            settings.soundEnabled = !settings.soundEnabled
            updateToggleStates()
        }
        btnToggleVibration.setOnClickListener {
            settings.hapticEnabled = !settings.hapticEnabled
            updateToggleStates()
        }
    }

    override fun onResume() {
        super.onResume()
        updateButtonStates()
        updateToggleStates()
    }

    // FIX: replaces the invalid <gradient> tag that used to be nested inside the
    // TextView in XML (not valid there — <gradient> only works inside a <shape>
    // drawable). This applies the same blue-to-pink brand gradient via a real shader.
    private fun applyLogoGradient(logoText: TextView) {
        logoText.post {
            val width = logoText.paint.measureText(logoText.text.toString())
            if (width > 0f) {
                logoText.paint.shader = LinearGradient(
                    0f, 0f, width, 0f,
                    android.graphics.Color.parseColor("#4488FF"),
                    android.graphics.Color.parseColor("#FF64C8"),
                    Shader.TileMode.CLAMP
                )
                logoText.invalidate()
            }
        }
    }

    private fun updateButtonStates() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledMethods = imm.enabledInputMethodList
        val isEnabled = enabledMethods.any { it.packageName == packageName }

        val btnEnable = findViewById<LinearLayout>(R.id.btnEnable)
        val btnEnableLabel = findViewById<TextView>(R.id.btnEnableLabel)

        if (isEnabled) {
            btnEnableLabel.text = "Keyboard Enabled ✓"
            btnEnable.isEnabled = false
            btnEnable.alpha = 0.6f
        } else {
            btnEnableLabel.text = getString(R.string.enable_keyboard)
            btnEnable.isEnabled = true
            btnEnable.alpha = 1.0f
        }
    }

    // FIX: Reflects current Sound/Vibration state via dimmed icon + label text,
    // since these are simple icon buttons rather than switches with a built-in
    // on/off visual.
    private fun updateToggleStates() {
        val iconSound = findViewById<ImageView>(R.id.iconSound)
        val labelSound = findViewById<TextView>(R.id.labelSound)
        val iconVibration = findViewById<ImageView>(R.id.iconVibration)
        val labelVibration = findViewById<TextView>(R.id.labelVibration)

        iconSound.alpha = if (settings.soundEnabled) 1.0f else 0.35f
        labelSound.text = if (settings.soundEnabled) "Sound" else "Sound off"

        iconVibration.alpha = if (settings.hapticEnabled) 1.0f else 0.35f
        labelVibration.text = if (settings.hapticEnabled) "Vibration" else "Vibration off"
    }
}