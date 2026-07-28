package com.example.animatedkeyboard.theme

import android.graphics.Color
import com.example.animatedkeyboard.settings.KeyboardSettings

enum class ThemeType { SOLID, ANIMATED_SINGLE, ANIMATED_MULTI, CUSTOM_COLOR, CUSTOM_IMAGE }

data class KeyboardTheme(
    val id: String,
    val name: String,
    val type: ThemeType,
    val accentColor: Int,
    val keyColor: Int,
    val textColor: Int,
    val bgColor: Int
)

object ThemeRepository {

    val presetThemes: List<KeyboardTheme> = listOf(
        KeyboardTheme(
            id = "rainbow", name = "Rainbow", type = ThemeType.ANIMATED_MULTI,
            accentColor = Color.parseColor("#FF50C8"),
            keyColor = Color.argb(0xD2, 0x14, 0x14, 0x18),
            textColor = Color.parseColor("#F5F0F5"),
            bgColor = Color.BLACK
        ),
        KeyboardTheme(
            id = "inferno", name = "Inferno", type = ThemeType.ANIMATED_SINGLE,
            accentColor = Color.parseColor("#FF6400"),
            keyColor = Color.argb(0xD2, 0x18, 0x10, 0x08),
            textColor = Color.parseColor("#F5E8D8"),
            bgColor = Color.BLACK
        ),
        KeyboardTheme(
            id = "ocean", name = "Ocean", type = ThemeType.ANIMATED_SINGLE,
            accentColor = Color.parseColor("#3296FF"),
            keyColor = Color.argb(0xD2, 0x08, 0x10, 0x18),
            textColor = Color.parseColor("#E8F2FF"),
            bgColor = Color.BLACK
        ),
        KeyboardTheme(
            id = "emerald", name = "Emerald", type = ThemeType.ANIMATED_SINGLE,
            accentColor = Color.parseColor("#00DC96"),
            keyColor = Color.argb(0xD2, 0x08, 0x16, 0x10),
            textColor = Color.parseColor("#E8FFF2"),
            bgColor = Color.BLACK
        ),
        KeyboardTheme(
            id = "violet", name = "Violet", type = ThemeType.ANIMATED_SINGLE,
            accentColor = Color.parseColor("#B432FF"),
            keyColor = Color.argb(0xD2, 0x14, 0x08, 0x18),
            textColor = Color.parseColor("#F5E8FF"),
            bgColor = Color.BLACK
        ),
        KeyboardTheme(
            id = "midnight", name = "Midnight", type = ThemeType.SOLID,
            accentColor = Color.parseColor("#5C7CFA"),
            keyColor = Color.argb(0xD2, 0x0C, 0x0E, 0x18),
            textColor = Color.parseColor("#E8EAFF"),
            bgColor = Color.BLACK
        )
    )

    /** Fallback used before any theme has been resolved and if a saved id no longer matches. */
    val defaultTheme: KeyboardTheme get() = presetThemes.first()

    /** Turns the user's saved selection into a concrete theme, building the
     *  custom color / custom image variants on the fly since those depend on
     *  values the user can change at any time (unlike the fixed presets). */
    fun resolve(settings: KeyboardSettings): KeyboardTheme {
        return when (settings.selectedThemeId) {
            "custom_color" -> customColorTheme(settings.customThemeColor)
            "custom_image" -> customImageTheme()
            else -> presetThemes.find { it.id == settings.selectedThemeId } ?: defaultTheme
        }
    }

    /** Full list shown in the MainActivity theme row: presets + the two "build your own" tiles. */
    fun selectableThemes(settings: KeyboardSettings): List<KeyboardTheme> {
        return presetThemes + customColorTheme(settings.customThemeColor) + customImageTheme()
    }

    private fun customColorTheme(color: Int): KeyboardTheme = KeyboardTheme(
        id = "custom_color", name = "Custom", type = ThemeType.CUSTOM_COLOR,
        accentColor = color,
        keyColor = darken(color, 0.85f),
        textColor = Color.WHITE,
        bgColor = Color.BLACK
    )

    private fun customImageTheme(): KeyboardTheme = KeyboardTheme(
        id = "custom_image", name = "Photo", type = ThemeType.CUSTOM_IMAGE,
        accentColor = Color.parseColor("#CCCCCC"),
        keyColor = Color.argb(0x66, 0x00, 0x00, 0x00),
        textColor = Color.WHITE,
        bgColor = Color.BLACK
    )

    /** Blends a color toward white by [factor] (0 = unchanged, 1 = white), preserving hue. */
    fun lighten(color: Int, factor: Float): Int {
        val f = factor.coerceIn(0f, 1f)
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = (hsv[1] * (1f - f)).coerceIn(0f, 1f)
        hsv[2] = (hsv[2] + (1f - hsv[2]) * f).coerceIn(0f, 1f)
        return Color.HSVToColor(Color.alpha(color), hsv)
    }

    /** Blends a color toward black by [factor] (0 = unchanged, 1 = black), preserving hue. */
    fun darken(color: Int, factor: Float): Int {
        val f = factor.coerceIn(0f, 1f)
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * (1f - f)).coerceIn(0f, 1f)
        return Color.HSVToColor(Color.alpha(color), hsv)
    }
}
