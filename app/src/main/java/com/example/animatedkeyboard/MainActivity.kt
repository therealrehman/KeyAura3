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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.animatedkeyboard.ads.UnityAdsManager
import com.example.animatedkeyboard.ads.UnityAdsManager.RewardType
import com.example.animatedkeyboard.settings.KeyboardSettings
import com.example.animatedkeyboard.theme.KeyboardTheme
import com.example.animatedkeyboard.theme.ThemeRepository
import com.example.animatedkeyboard.theme.ThemeType
import java.io.File
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private val settings by lazy { KeyboardSettings.getInstance(this) }
    private val ads     by lazy { UnityAdsManager.getInstance(this) }
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

        // ── Unity Ads: initialize SDK and load banner ─────────────────────
        ads.initialize(this)
        val bannerContainer = findViewById<FrameLayout>(R.id.bannerContainer)
        // Slight delay so SDK finishes init before requesting the banner
        bannerContainer.postDelayed({
            ads.loadBannerInto(this, bannerContainer)
        }, 2000)

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

        // ── Swipe Tune card: check unlock before opening ──────────────────
        findViewById<CardView>(R.id.btnTune).setOnClickListener {
            if (ads.isUnlocked(RewardType.TUNES)) {
                startActivity(Intent(this, TuneSelectionActivity::class.java))
            } else {
                showAdDialog(
                    title = "🎶 Unlock Swipe Tunes",
                    message = "Watch a short ad to unlock all 10 swipe tunes for 12 hours.",
                    type = RewardType.TUNES,
                    onUnlocked = {
                        updateAdStatusViews()
                        startActivity(Intent(this, TuneSelectionActivity::class.java))
                    }
                )
            }
        }

        // ── Game Unlock card ──────────────────────────────────────────────
        findViewById<CardView>(R.id.gameUnlockCard).setOnClickListener {
            if (ads.isUnlocked(RewardType.GAME)) {
                Toast.makeText(this, "Game is unlocked! Open the game from the keyboard 🎮", Toast.LENGTH_LONG).show()
            } else {
                showAdDialog(
                    title = "🎮 Unlock Birdy Bird Game",
                    message = "Watch a short ad to unlock the Birdy Bird mini-game in your keyboard for 12 hours.",
                    type = RewardType.GAME,
                    onUnlocked = {
                        updateAdStatusViews()
                        Toast.makeText(this, "Game unlocked! Open your keyboard and tap 🎮", Toast.LENGTH_LONG).show()
                    }
                )
            }
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

    // ── Ad status badges ──────────────────────────────────────────────────────

    private fun updateAdStatusViews() {
        // Animated themes status label
        val themesStatus = findViewById<TextView>(R.id.themesAdStatus)
        if (ads.isUnlocked(RewardType.THEMES)) {
            val h = ads.remainingHours(RewardType.THEMES)
            themesStatus.text = "✅ Animated themes unlocked — ${h}h remaining"
            themesStatus.setTextColor(Color.parseColor("#00C853"))
            themesStatus.visibility = View.VISIBLE
        } else {
            themesStatus.text = "🔒 Tap any animated theme to watch an ad and unlock for 12h"
            themesStatus.setTextColor(Color.parseColor("#FFC400"))
            themesStatus.visibility = View.VISIBLE
        }

        // Tunes status label
        val tuneStatus = findViewById<TextView>(R.id.tuneAdStatus)
        if (ads.isUnlocked(RewardType.TUNES)) {
            val h = ads.remainingHours(RewardType.TUNES)
            tuneStatus.text = "✅ Swipe tunes unlocked — ${h}h remaining"
            tuneStatus.setTextColor(Color.parseColor("#00C853"))
            tuneStatus.visibility = View.VISIBLE
        } else {
            tuneStatus.text = "🔒 Watch an ad to unlock all tunes for 12h"
            tuneStatus.setTextColor(Color.parseColor("#FFC400"))
            tuneStatus.visibility = View.VISIBLE
        }

        // Game status
        val gameStatus  = findViewById<TextView>(R.id.gameAdStatus)
        val gameArrow   = findViewById<TextView>(R.id.gameUnlockArrow)
        if (ads.isUnlocked(RewardType.GAME)) {
            val h = ads.remainingHours(RewardType.GAME)
            gameStatus.text = "✅ Game unlocked — ${h}h remaining. Open keyboard to play!"
            gameStatus.setTextColor(Color.parseColor("#00C853"))
            gameArrow.text = "✓"
            gameArrow.setTextColor(Color.parseColor("#00C853"))
        } else {
            gameStatus.text = "🔒 Watch an ad to unlock for 12h"
            gameStatus.setTextColor(Color.parseColor("#FFC400"))
            gameArrow.text = "›"
            gameArrow.setTextColor(Color.parseColor("#4488FF"))
        }
    }

    // ── Rewarded Ad dialog helper ─────────────────────────────────────────────

    private fun showAdDialog(
        title: String,
        message: String,
        type: RewardType,
        onUnlocked: () -> Unit
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("▶ Watch Ad") { _, _ ->
                ads.showRewardedAd(
                    activity  = this,
                    type      = type,
                    onRewarded = onUnlocked,
                    onFailed  = { /* toast already shown in UnityAdsManager */ }
                )
            }
            .setNegativeButton("Not now", null)
            .show()
    }

    // ── Theme Rows ────────────────────────────────────────────────────────────

    private fun buildThemeRows() {
        val selected = settings.selectedThemeId

        // Animated row: Aurora + 10 single-color + Photo tile
        val animRow = findViewById<LinearLayout>(R.id.animatedThemeRow)
        animRow.removeAllViews()
        val animList = listOf(ThemeRepository.defaultTheme) + ThemeRepository.animatedThemes +
                listOf(ThemeRepository.imageTheme)
        for (t in animList) animRow.addView(themeCard(t, selected == t.id, isAnimated = true))

        // Solid row: Midnight/Forest/Wine/Slate/Royal/White/Grey/Black
        val solidRow = findViewById<LinearLayout>(R.id.solidThemeRow)
        solidRow.removeAllViews()
        for (t in ThemeRepository.solidThemes) solidRow.addView(themeCard(t, selected == t.id, isAnimated = false))

        // Show/hide "Clear photo" button
        findViewById<TextView>(R.id.btnClearImage).visibility =
            if (settings.keyboardImagePath != null) View.VISIBLE else View.GONE
    }

    private fun themeCard(theme: KeyboardTheme, selected: Boolean, isAnimated: Boolean): View {
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
                // Dim animated themes that are locked
                if (isAnimated && theme.type != ThemeType.CUSTOM_IMAGE
                    && !ads.isUnlocked(RewardType.THEMES)) {
                    alpha = 0.35f
                }
            })
        }

        // Lock icon overlay for locked animated themes
        val isLockedAnimated = isAnimated
            && theme.type != ThemeType.CUSTOM_IMAGE
            && !ads.isUnlocked(RewardType.THEMES)

        if (isLockedAnimated) {
            card.addView(TextView(this).apply {
                text = "🔒"
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#FFC400"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dpi(2f) }
            })
        }

        card.addView(TextView(this).apply {
            text = theme.name; textSize = 11f
            setTextColor(
                when {
                    selected          -> theme.accentColor
                    isLockedAnimated  -> Color.parseColor("#555878")
                    else              -> Color.parseColor("#AAB0C8")
                }
            )
            gravity = Gravity.CENTER; maxLines = 1
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpi(4f) }
        })

        card.setOnClickListener {
            when {
                theme.type == ThemeType.CUSTOM_IMAGE -> pickImage.launch("image/*")
                isLockedAnimated -> {
                    // Offer rewarded ad to unlock all animated themes
                    showAdDialog(
                        title     = "✨ Unlock Animated Themes",
                        message   = "Watch a short ad to unlock all animated themes for 12 hours.",
                        type      = RewardType.THEMES,
                        onUnlocked = {
                            buildThemeRows()
                            updateAdStatusViews()
                            // Now apply the theme they wanted
                            settings.selectedThemeId = theme.id
                            buildThemeRows()
                        }
                    )
                }
                else -> {
                    settings.selectedThemeId = theme.id
                    buildThemeRows()
                }
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
