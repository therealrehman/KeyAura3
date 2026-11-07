package com.example.animatedkeyboard.theme

import android.graphics.Color
import com.example.animatedkeyboard.settings.KeyboardSettings

enum class ThemeType {
    ANIMATED_MULTI,
    ANIMATED_SINGLE,
    SOLID,
    CUSTOM_IMAGE
}

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

    val defaultTheme = KeyboardTheme(
        "rainbow", "Aurora", ThemeType.ANIMATED_MULTI,
        Color.parseColor("#FF6400"), Color.parseColor("#0A0A0E"),
        Color.WHITE, Color.parseColor("#030308")
    )

    val animatedThemes = listOf(
        KeyboardTheme("anim_orange",  "Ember",   ThemeType.ANIMATED_SINGLE, Color.parseColor("#FF6D00"), Color.parseColor("#0D0A08"), Color.WHITE, Color.parseColor("#040303")),
        KeyboardTheme("anim_blue",    "Ocean",   ThemeType.ANIMATED_SINGLE, Color.parseColor("#2979FF"), Color.parseColor("#080A10"), Color.WHITE, Color.parseColor("#030408")),
        KeyboardTheme("anim_green",   "Emerald", ThemeType.ANIMATED_SINGLE, Color.parseColor("#00C853"), Color.parseColor("#08100A"), Color.WHITE, Color.parseColor("#030503")),
        KeyboardTheme("anim_purple",  "Nebula",  ThemeType.ANIMATED_SINGLE, Color.parseColor("#AA00FF"), Color.parseColor("#0D0812"), Color.WHITE, Color.parseColor("#050308")),
        KeyboardTheme("anim_red",     "Crimson", ThemeType.ANIMATED_SINGLE, Color.parseColor("#FF1744"), Color.parseColor("#120808"), Color.WHITE, Color.parseColor("#060303")),
        KeyboardTheme("anim_cyan",    "Glacier", ThemeType.ANIMATED_SINGLE, Color.parseColor("#00E5FF"), Color.parseColor("#081012"), Color.WHITE, Color.parseColor("#030606")),
        KeyboardTheme("anim_pink",    "Blossom", ThemeType.ANIMATED_SINGLE, Color.parseColor("#FF4081"), Color.parseColor("#12080D"), Color.WHITE, Color.parseColor("#060305")),
        KeyboardTheme("anim_gold",    "Solar",   ThemeType.ANIMATED_SINGLE, Color.parseColor("#FFC400"), Color.parseColor("#121008"), Color.WHITE, Color.parseColor("#060503")),
        KeyboardTheme("anim_teal",    "Lagoon",  ThemeType.ANIMATED_SINGLE, Color.parseColor("#00BFA5"), Color.parseColor("#081210"), Color.WHITE, Color.parseColor("#030605")),
        KeyboardTheme("anim_magenta", "Plasma",  ThemeType.ANIMATED_SINGLE, Color.parseColor("#D500F9"), Color.parseColor("#100812"), Color.WHITE, Color.parseColor("#050306"))
    )

    val solidThemes = listOf(
        KeyboardTheme("solid_midnight", "Midnight", ThemeType.SOLID, Color.parseColor("#3D5AFE"), Color.parseColor("#1B2544"), Color.WHITE,                  Color.parseColor("#0D1326")),
        KeyboardTheme("solid_forest",   "Forest",   ThemeType.SOLID, Color.parseColor("#00C853"), Color.parseColor("#17351F"), Color.WHITE,                  Color.parseColor("#0B1F10")),
        KeyboardTheme("solid_wine",     "Wine",     ThemeType.SOLID, Color.parseColor("#FF5252"), Color.parseColor("#3E1A24"), Color.WHITE,                  Color.parseColor("#220D13")),
        KeyboardTheme("solid_slate",    "Slate",    ThemeType.SOLID, Color.parseColor("#90A4AE"), Color.parseColor("#2A323A"), Color.WHITE,                  Color.parseColor("#161C22")),
        KeyboardTheme("solid_royal",    "Royal",    ThemeType.SOLID, Color.parseColor("#B388FF"), Color.parseColor("#2A1D4E"), Color.WHITE,                  Color.parseColor("#160F2C")),
        KeyboardTheme("solid_white",    "White",    ThemeType.SOLID, Color.parseColor("#CCCCCC"), Color.parseColor("#E8E8E8"), Color.parseColor("#111111"),  Color.parseColor("#F0F0F0")),
        KeyboardTheme("solid_grey",     "Grey",     ThemeType.SOLID, Color.parseColor("#888888"), Color.parseColor("#444444"), Color.WHITE,                  Color.parseColor("#222222")),
        KeyboardTheme("solid_black",    "Black",    ThemeType.SOLID, Color.parseColor("#444444"), Color.parseColor("#1A1A1A"), Color.WHITE,                  Color.parseColor("#000000"))
    )

    val imageTheme = KeyboardTheme(
        "custom_image", "Photo", ThemeType.CUSTOM_IMAGE,
        Color.parseColor("#8C9EFF"), Color.parseColor("#CC16161C"),
        Color.WHITE, Color.BLACK
    )

    fun selectableThemes(settings: KeyboardSettings): List<KeyboardTheme> =
        listOf(defaultTheme) + animatedThemes + solidThemes + listOf(imageTheme)

    fun resolve(settings: KeyboardSettings): KeyboardTheme =
        selectableThemes(settings).firstOrNull { it.id == settings.selectedThemeId } ?: defaultTheme

    fun lighten(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = (hsv[1] * (1f - factor * 0.5f)).coerceIn(0f, 1f)
        hsv[2] = (hsv[2] + factor * (1f - hsv[2])).coerceIn(0f, 1f)
        return Color.HSVToColor(hsv)
    }

    fun darken(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * factor).coerceIn(0.06f, 1f)
        return Color.HSVToColor(hsv)
    }
}
