package com.example.animatedkeyboard

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
import android.widget.FrameLayout
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
import java.io.File
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private val settings by lazy { KeyboardSettings.getInstance(this) }
    private fun dp(v: Float) = v * resources.displayMetrics.density
    private fun dpi(v: Float) = dp(v).toInt()

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
                buildThemeRows()
                Toast.makeText(this, "Keyboard photo applied ✓", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Hide banner container — no ads
        findViewById<FrameLayout>(R.id.bannerContainer)?.visibility = View.GONE

        applyLogoGradient(findViewById(R.id.logoText))

        findViewById<LinearLayout>(R.id.btnEnable).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        findViewById<LinearLayout>(R.id.btnChoose).setOnClickListener {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }
        findViewById<ImageButton>(R.id.btnMenu).setOnClickListener { v ->
            val popup = android.widget.PopupMenu(this, v)
            popup.menu.add(0, 1, 0, "About KeyAura")
            popup.menu.add(0, 2, 1, "Privacy Policy")
            popup.menu.add(0, 3, 2, "Terms & Conditions")
            popup.menu.add(0, 4, 3, "Contact Us")
            popup.setOnMenuItemClickListener { item ->
                val intent = Intent(this, AboutActivity::class.java)
                intent.putExtra("tab", item.itemId - 1)
                startActivity(intent)
                true
            }
            popup.show()
        }
        findViewById<LinearLayout>(R.id.btnToggleSound).setOnClickListener {
            settings.soundEnabled = !settings.soundEnabled
            updateToggleStates()
        }
        findViewById<LinearLayout>(R.id.btnToggleVibration).setOnClickListener {
            settings.hapticEnabled = !settings.hapticEnabled
            updateToggleStates()
        }

        val slider = findViewById<SeekBar>(R.id.volumeSlider)
        val volumeLabel = findViewById<TextView>(R.id.volumeLabel)
        slider.progress = (settings.keyVolume * 100).roundToInt()
        volumeLabel.text = "${slider.progress}%"
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) { settings.keyVolume = p / 100f; volumeLabel.text = "$p%" }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Swipe Tune — directly open, no lock
        findViewById<CardView>(R.id.btnTune).setOnClickListener {
            startActivity(Intent(this, TuneSelectionActivity::class.java))
        }

        // Game — directly show unlocked message
        findViewById<CardView>(R.id.gameUnlockCard).setOnClickListener {
            Toast.makeText(this, "Game is unlocked! Open your keyboard and tap 🎮", Toast.LENGTH_LONG).show()
        }

        findViewById<TextView>(R.id.btnClearImage).setOnClickListener {
            settings.keyboardImagePath = null
            if (settings.selectedThemeId == "custom_image") settings.selectedThemeId = "rainbow"
            buildThemeRows()
        }

        buildThemeRows()
        updateAdStatusViews()
    }

    override fun onResume() {
        super.onResume()
        updateButtonStates()
        updateToggleStates()
        buildThemeRows()
        updateAdStatusViews()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    // ── Ad status views — show everything as unlocked ─────────────────────────

    private fun updateAdStatusViews() {
        findViewById<TextView>(R.id.themesAdStatus)?.apply {
            text = "✅ All animated themes unlocked"
            setTextColor(Color.parseColor("#00C853"))
            visibility = View.VISIBLE
        }
        findViewById<TextView>(R.id.tuneAdStatus)?.apply {
            text = "✅ All swipe tunes unlocked"
            setTextColor(Color.parseColor("#00C853"))
            visibility = View.VISIBLE
        }
        findViewById<TextView>(R.id.gameAdStatus)?.apply {
            text = "✅ Game unlocked — open keyboard to play!"
            setTextColor(Color.parseColor("#00C853"))
        }
        findViewById<TextView>(R.id.gameUnlockArrow)?.apply {
            text = "✓"
            setTextColor(Color.parseColor("#00C853"))
        }
    }

    // ── Theme Rows — all unlocked ─────────────────────────────────────────────

    private fun buildThemeRows() {
        val selected = settings.selectedThemeId

        val animRow = findViewById<LinearLayout>(R.id.animatedThemeRow)
        animRow.removeAllViews()
        val animList = listOf(ThemeRepository.defaultTheme) + ThemeRepository.animatedThemes +
                listOf(ThemeRepository.imageTheme)
        for (t in animList) animRow.addView(themeCard(t, selected == t.id))

        val solidRow = findViewById<LinearLayout>(R.id.solidThemeRow)
        solidRow.removeAllViews()
        for (t in ThemeRepository.solidThemes) solidRow.addView(themeCard(t, selected == t.id))

        findViewById<TextView>(R.id.btnClearImage).visibility =
            if (settings.keyboardImagePath != null) View.VISIBLE else View.GONE
    }

    private fun themeCard(theme: KeyboardTheme, selected: Boolean): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpi(86f), dpi(104f)).apply { marginEnd = dpi(10f) }
            background = GradientDrawable().apply {
                cornerRadius = dp(14f)
                setColor(Color.parseColor("#0C1020"))
                if (selected) setStroke(dpi(2f), theme.accentColor)
            }
            isClickable = true
        }

        if (theme.type == ThemeType.CUSTOM_IMAGE) {
            card.addView(TextView(this).apply {
                text = "🖼️"; textSize = 26f; gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dpi(52f), dpi(52f))
            })
        } else {
            card.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpi(52f), dpi(52f))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    when (theme.type) {
                        ThemeType.ANIMATED_MULTI -> {
                            gradientType = GradientDrawable.SWEEP_GRADIENT
                            colors = intArrayOf(
                                Color.parseColor("#FF5050"), Color.parseColor("#FFDC00"),
                                Color.parseColor("#00FF96"), Color.parseColor("#3296FF"),
                                Color.parseColor("#B432FF"), Color.parseColor("#FF5050")
                            )
                        }
                        ThemeType.SOLID -> {
                            gradientType = GradientDrawable.RADIAL_GRADIENT
                            gradientRadius = dp(30f)
                            colors = intArrayOf(
                                ThemeRepository.lighten(theme.accentColor, 0.3f),
                                theme.accentColor
                            )
                        }
                        else -> {
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
            })
        }

        card.addView(TextView(this).apply {
            text = theme.name; textSize = 11f
            setTextColor(if (selected) theme.accentColor else Color.parseColor("#AAB0C8"))
            gravity = Gravity.CENTER; maxLines = 1
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpi(4f) }
        })

        card.setOnClickListener {
            if (theme.type == ThemeType.CUSTOM_IMAGE) {
                pickImage.launch("image/*")
            } else {
                settings.selectedThemeId = theme.id
                buildThemeRows()
            }
        }
        return card
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun applyLogoGradient(tv: TextView) {
        tv.post {
            val w = tv.paint.measureText(tv.text.toString())
            if (w > 0f) {
                tv.paint.shader = LinearGradient(0f, 0f, w, 0f,
                    Color.parseColor("#4488FF"), Color.parseColor("#FF64C8"), Shader.TileMode.CLAMP)
                tv.invalidate()
            }
        }
    }

    private fun updateButtonStates() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val enabled = imm.enabledInputMethodList.any { it.packageName == packageName }
        val btnEnable = findViewById<LinearLayout>(R.id.btnEnable)
        val btnEnableLabel = findViewById<TextView>(R.id.btnEnableLabel)
        if (enabled) {
            btnEnableLabel.text = "Keyboard Enabled ✓"
            btnEnable.isEnabled = false; btnEnable.alpha = 0.6f
        } else {
            btnEnableLabel.text = getString(R.string.enable_keyboard)
            btnEnable.isEnabled = true; btnEnable.alpha = 1.0f
        }
    }

    private fun updateToggleStates() {
        val iconSound = findViewById<TextView>(R.id.iconSound)
        val labelSound = findViewById<TextView>(R.id.labelSound)
        val iconVib = findViewById<TextView>(R.id.iconVibration)
        val labelVib = findViewById<TextView>(R.id.labelVibration)
        iconSound.alpha = if (settings.soundEnabled) 1.0f else 0.35f
        labelSound.text = if (settings.soundEnabled) "Sound" else "Sound off"
        iconVib.alpha = if (settings.hapticEnabled) 1.0f else 0.35f
        labelVib.text = if (settings.hapticEnabled) "Vibration" else "Vibration off"
    }
}
