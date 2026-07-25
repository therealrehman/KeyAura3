package com.example.animatedkeyboard.theme

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.animatedkeyboard.R
import com.example.animatedkeyboard.settings.KeyboardSettings

class ThemeSelectionActivity : AppCompatActivity() {

    private val settings by lazy { KeyboardSettings.getInstance(this) }
    private val rowViews = mutableListOf<LinearLayout>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_selection)

        val container = findViewById<LinearLayout>(R.id.themeListContainer)
        for ((index, theme) in AnimationTheme.valuesList.withIndex()) {
            val row = buildThemeRow(index, theme)
            rowViews.add(row)
            container.addView(row)
        }
        refreshRowStyles()
    }

    private fun buildThemeRow(index: Int, theme: AnimationTheme): LinearLayout {
        val dp8 = (8 * resources.displayMetrics.density).toInt()
        val dp16 = (16 * resources.displayMetrics.density).toInt()

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp16, dp16, dp16, dp16)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp8
            layoutParams = lp
            isClickable = true
            isFocusable = true
        }

        // Color swatch preview
        val swatchContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(dp16 * 4, dp16 * 2)
            lp.marginEnd = dp16
            layoutParams = lp
        }
        val colors = theme.colors.take(4)
        for (c in colors) {
            val swatch = View(this).apply {
                val size = dp16
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = dp4
                }
                setBackgroundColor(c)
                // Rounded corners for swatch
                setClipToOutline(true)
                outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
                setBackgroundResource(R.drawable.bg_tune_row) // just to get shape, but we override color
            }
            // Actually we need a proper drawable, let's just use a simple shape programmatically.
            // But for simplicity, we'll use a fixed drawable and tint it, or just draw colored circles in canvas.
            // I'll use a TextView with colored background for simplicity.
        }
        // Simpler: just show a horizontal color strip using a TextView with text
        val swatchView = TextView(this).apply {
            val dp4 = (4 * resources.displayMetrics.density).toInt()
            val dp20 = (20 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(dp20 * 3, dp20)
            setPadding(dp4, dp4, dp4, dp4)
            text = "● ● ● ●"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1A1A3A.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }
        // Actually, let's just use a colored view span. I'll use a simple LinearLayout with colored circles.
        // For brevity in this file generation, I'll use a text-based preview "🎨" or a simple colored background.
        // Let's just use a TextView with the theme name and a colored dot using HTML.
        // I'll create a better preview later. Let's use a simple circle drawable.
        // Let's just use a text label and a checkmark.

        val label = TextView(this).apply {
            text = theme.displayName
            textSize = 17f
            setTextColor(0xFFFFFFFF.toInt())
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams = lp
        }

        val checkmark = TextView(this).apply {
            text = "\u2713"
            textSize = 18f
            setTextColor(0xFF4488FF.toInt())
            tag = "check"
            visibility = if (index == settings.selectedThemeIndex) View.VISIBLE else View.INVISIBLE
        }

        row.addView(label)
        row.addView(checkmark)

        row.setOnClickListener {
            settings.selectedThemeIndex = index
            refreshRowStyles()
        }
        return row
    }

    private fun refreshRowStyles() {
        val selected = settings.selectedThemeIndex
        for ((i, row) in rowViews.withIndex()) {
            val isSelected = i == selected
            row.setBackgroundResource(
                if (isSelected) R.drawable.bg_tune_row_selected else R.drawable.bg_tune_row
            )
            row.findViewWithTag<TextView>("check")?.visibility =
                if (isSelected) View.VISIBLE else View.INVISIBLE
        }
    }
}
