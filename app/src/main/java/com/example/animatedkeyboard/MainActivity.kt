package com.example.animatedkeyboard

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.animatedkeyboard.settings.KeyboardSettings
import com.example.animatedkeyboard.theme.KeyboardTheme
import com.example.animatedkeyboard.theme.ThemeRepository
import com.example.animatedkeyboard.theme.ThemeType
import com.example.animatedkeyboard.ui.ColorPickerView
import java.io.File
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private val settings by lazy { KeyboardSettings.getInstance(this) }
    private fun dp(v: Float) = v * resources.displayMetrics.density
    private fun dpi(v: Float) = dp(v).toInt()

    // FIX 9: gallery se image pick → internal storage copy → theme select
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult
            try {
                val file = File(filesDir, "keyboard_bg.jpg")
                contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("Cannot open image")
                settings.keyboardImagePath = file.absolutePath
                settings.selectedThemeId = "custom_image"
                buildThemeRow()
                Toast.makeText(this, "Keyboard photo applied ✓", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnEnable = findViewById<LinearLayout>(R.id.btnEnable)
        val btnChoose = findViewById<LinearLayout>(R.id.btnChoose)
        val btnAbout = findViewById<LinearLayout>(R.id.btnAbout)
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
            startActivity(Intent(this, AboutActivity::class.java))
        }

        // Sound + Vibration toggles
        findViewById<LinearLayout>(R.id.btnToggleSound).setOnClickListener {
            settings.soundEnabled = !settings.soundEnabled
            updateToggleStates()
        }
        findViewById<LinearLayout>(R.id.btnToggleVibration).setOnClickListener {
            settings.hapticEnabled = !settings.hapticEnabled
            updateToggleStates()
        }

        // FIX 3: persistent key volume slider — jo set karo wahi rahega
        val slider = findViewById<SeekBar>(R.id.volumeSlider)
        val volumeLabel = findViewById<TextView>(R.id.volumeLabel)
        slider.progress = (settings.keyVolume * 100).roundToInt()
        volumeLabel.text = "${slider.progress}%"
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    settings.keyVolume = progress / 100f
                    volumeLabel.text = "$progress%"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        findViewById<CardView>(R.id.btnTune).setOnClickListener {
            startActivity(Intent(this, TuneSelectionActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnToggleNinjaMode).setOnClickListener {
            settings.ninjaModeEnabled = !settings.ninjaModeEnabled
            updateToggleStates()
        }

        findViewById<TextView>(R.id.btnClearImage).setOnClickListener {
            settings.keyboardImagePath = null
            if (settings.selectedThemeId == "custom_image") {
                settings.selectedThemeId = "rainbow"
            }
            buildThemeRow()
        }

        buildThemeRow()
    }

    override fun onResume() {
        super.onResume()
        updateButtonStates()
        updateToggleStates()
        buildThemeRow()
    }

    // ---------- Themes (FIX 6/8/9) ----------

    private fun buildThemeRow() {
        val row = findViewById<LinearLayout>(R.id.themeRow)
        row.removeAllViews()
        val themes = ThemeRepository.selectableThemes(settings)
        for (theme in themes) {
            row.addView(themeCard(theme, settings.selectedThemeId == theme.id))
        }
        findViewById<TextView>(R.id.btnClearImage).visibility =
            if (settings.keyboardImagePath != null) View.VISIBLE else View.GONE
    }

    private fun themeCard(theme: KeyboardTheme, selected: Boolean): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpi(86f), dpi(104f)).apply {
                marginEnd = dpi(10f)
            }
            background = GradientDrawable().apply {
                cornerRadius = dp(14f)
                setColor(Color.parseColor("#0C1020"))
                if (selected) setStroke(dpi(2f), theme.accentColor)
            }
            isClickable = true
        }

        // Color preview circle (rainbow = sweep, image = 🖼)
        if (theme.type == ThemeType.CUSTOM_IMAGE) {
            val icon = TextView(this).apply {
                text = "🖼️"
                textSize = 26f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dpi(52f), dpi(52f))
            }
            card.addView(icon)
        } else {
            val preview = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpi(52f), dpi(52f))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    if (theme.type == ThemeType.ANIMATED_MULTI) {
                        gradientType = GradientDrawable.SWEEP_GRADIENT
                        colors = intArrayOf(
                            Color.parseColor("#FF5050"), Color.parseColor("#FFDC00"),
                            Color.parseColor("#00FF96"), Color.parseColor("#3296FF"),
                            Color.parseColor("#B432FF"), Color.parseColor("#FF5050")
                        )
                    } else {
                        gradientType = GradientDrawable.RADIAL_GRADIENT
                        gradientRadius = dp(30f)
                        colors = intArrayOf(
                            ThemeRepository.lighten(theme.accentColor, 0.5f),
                            theme.accentColor,
                            ThemeRepository.darken(theme.accentColor, 0.55f)
                        )
                    }
                }
            }
            card.addView(preview)
        }

        val name = TextView(this).apply {
            text = theme.name
            textSize = 11f
            setTextColor(if (selected) theme.accentColor else Color.parseColor("#AAB0C8"))
            gravity = Gravity.CENTER
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpi(6f) }
        }
        card.addView(name)

        card.setOnClickListener { onThemeClicked(theme) }
        return card
    }

    private fun onThemeClicked(theme: KeyboardTheme) {
        when (theme.type) {
            ThemeType.CUSTOM_COLOR -> showColorPickerDialog()
            ThemeType.CUSTOM_IMAGE -> pickImage.launch("image/*")
            else -> {
                settings.selectedThemeId = theme.id
                buildThemeRow()
            }
        }
    }

    // FIX 8: color wheel dialog
    private fun showColorPickerDialog() {
        val picker = ColorPickerView(this).apply {
            setColor(settings.customThemeColor)
            setPadding(dpi(8f), dpi(8f), dpi(8f), dpi(8f))
        }
        AlertDialog.Builder(this)
            .setTitle("Choose keyboard color")
            .setView(picker)
            .setPositiveButton("Apply") { _, _ ->
                settings.customThemeColor = picker.getColor()
                settings.selectedThemeId = "custom_color"
                buildThemeRow()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---------- Existing helpers ----------

    private fun applyLogoGradient(logoText: TextView) {
        logoText.post {
            val width = logoText.paint.measureText(logoText.text.toString())
            if (width > 0f) {
                logoText.paint.shader = LinearGradient(
                    0f, 0f, width, 0f,
                    Color.parseColor("#4488FF"),
                    Color.parseColor("#FF64C8"),
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

    private fun updateToggleStates() {
        val iconSound = findViewById<TextView>(R.id.iconSound)
        val labelSound = findViewById<TextView>(R.id.labelSound)
        val iconVibration = findViewById<TextView>(R.id.iconVibration)
        val labelVibration = findViewById<TextView>(R.id.labelVibration)

        iconSound.alpha = if (settings.soundEnabled) 1.0f else 0.35f
        labelSound.text = if (settings.soundEnabled) "Sound" else "Sound off"

        iconVibration.alpha = if (settings.hapticEnabled) 1.0f else 0.35f
        labelVibration.text = if (settings.hapticEnabled) "Vibration" else "Vibration off"

        val labelNinjaMode = findViewById<TextView>(R.id.labelNinjaMode)
        labelNinjaMode.text = if (settings.ninjaModeEnabled) "ON" else "OFF"
        labelNinjaMode.setTextColor(
            if (settings.ninjaModeEnabled) 0xFF4CD964.toInt() else 0xFF888888.toInt()
        )
    }
}
