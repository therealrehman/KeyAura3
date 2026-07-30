package com.example.animatedkeyboard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.animatedkeyboard.settings.KeyboardSettings
import com.example.animatedkeyboard.theme.KeyboardTheme
import com.example.animatedkeyboard.theme.ThemeRepository
import com.example.animatedkeyboard.theme.ThemeType

class StaticThemesActivity : AppCompatActivity() {

    private val settings by lazy { KeyboardSettings.getInstance(this) }
    private fun dp(v: Float) = v * resources.displayMetrics.density
    private fun dpi(v: Float) = dp(v).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_static_themes)

        val container = findViewById<LinearLayout>(R.id.themesContainer)
        val themes = ThemeRepository.solidThemes()

        for (theme in themes) {
            container.addView(themeCard(theme))
        }
    }

    private fun themeCard(theme: KeyboardTheme): View {
        val card = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpi(80f)
            ).apply { bottomMargin = dpi(12f) }
            radius = dp(16f)
            cardElevation = dp(4f)
            setCardBackgroundColor(Color.parseColor("#12162A"))
            isClickable = true
            isFocusable = true
            foreground = getDrawable(android.R.attr.selectableItemBackground)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpi(20f), 0, dpi(20f), 0)
        }

        val preview = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpi(48f), dpi(48f)).apply {
                marginEnd = dpi(16f)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(theme.accentColor)
            }
        }

        val nameText = TextView(this).apply {
            text = theme.name
            textSize = 16f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val check = TextView(this).apply {
            text = if (settings.selectedThemeId == theme.id) "✓" else ""
            textSize = 20f
            setTextColor(Color.parseColor("#4488FF"))
        }

        layout.addView(preview)
        layout.addView(nameText)
        layout.addView(check)
        card.addView(layout)

        card.setOnClickListener {
            settings.selectedThemeId = theme.id
            finish()
        }

        return card
    }
}
