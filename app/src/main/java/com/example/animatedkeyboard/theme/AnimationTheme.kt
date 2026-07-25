package com.example.animatedkeyboard.theme

import android.graphics.Color

enum class AnimationTheme(
    val displayName: String,
    val colors: IntArray,
    val particleType: ParticleType
) {
    MASTER(
        "Master",
        intArrayOf(
            Color.parseColor("#FF5050"), Color.parseColor("#3296FF"), Color.parseColor("#FFDC00"),
            Color.parseColor("#00FF96"), Color.parseColor("#FF00DC"), Color.parseColor("#FFA000"),
            Color.parseColor("#B432FF"), Color.parseColor("#00FFFF"), Color.parseColor("#FF6464"),
            Color.parseColor("#64FF64"), Color.parseColor("#FFFF32"), Color.parseColor("#FF64C8"),
            Color.parseColor("#64C8FF"), Color.parseColor("#FFC832"), Color.parseColor("#DC64FF"),
            Color.parseColor("#32DCFF"), Color.parseColor("#FF5050"), Color.parseColor("#50FF96"),
            Color.parseColor("#FFF032"), Color.parseColor("#C864FF"), Color.parseColor("#64FFC8"),
            Color.parseColor("#FFA064"), Color.parseColor("#64A0FF"), Color.parseColor("#FFFF96"),
            Color.parseColor("#FF5096"), Color.parseColor("#50FFDC")
        ),
        ParticleType.SPARKLE
    ),
    ICE(
        "Ice",
        intArrayOf(
            Color.parseColor("#E0F7FA"), Color.parseColor("#B2EBF2"), Color.parseColor("#80DEEA"),
            Color.parseColor("#4DD0E1"), Color.parseColor("#26C6DA"), Color.parseColor("#00BCD4"),
            Color.parseColor("#00ACC1"), Color.parseColor("#0097A7"), Color.parseColor("#00838F"),
            Color.parseColor("#006064")
        ),
        ParticleType.CIRCLE
    ),
    WATER(
        "Water",
        intArrayOf(
            Color.parseColor("#1A237E"), Color.parseColor("#0D47A1"), Color.parseColor("#1565C0"),
            Color.parseColor("#1976D2"), Color.parseColor("#1E88E5"), Color.parseColor("#2196F3"),
            Color.parseColor("#42A5F5"), Color.parseColor("#64B5F6"), Color.parseColor("#90CAF9"),
            Color.parseColor("#BBDEFB")
        ),
        ParticleType.CIRCLE
    ),
    FIRE(
        "Fire",
        intArrayOf(
            Color.parseColor("#FF6F00"), Color.parseColor("#E65100"), Color.parseColor("#BF360C"),
            Color.parseColor("#D84315"), Color.parseColor("#E64A19"), Color.parseColor("#F4511E"),
            Color.parseColor("#FF5722"), Color.parseColor("#FF7043"), Color.parseColor("#FF8A65"),
            Color.parseColor("#FFAB91")
        ),
        ParticleType.CIRCLE
    ),
    LEAVES(
        "Leaves",
        intArrayOf(
            Color.parseColor("#1B5E20"), Color.parseColor("#2E7D32"), Color.parseColor("#388E3C"),
            Color.parseColor("#43A047"), Color.parseColor("#4CAF50"), Color.parseColor("#66BB6A"),
            Color.parseColor("#81C784"), Color.parseColor("#A5D6A7"), Color.parseColor("#C8E6C9"),
            Color.parseColor("#E8F5E9")
        ),
        ParticleType.LEAF
    ),
    PAKISTAN(
        "Pakistan",
        intArrayOf(
            Color.parseColor("#01411C"), Color.parseColor("#FFFFFF"), Color.parseColor("#0A3A2A"),
            Color.parseColor("#2E8B57"), Color.parseColor("#98FB98"), Color.parseColor("#006400")
        ),
        ParticleType.STAR
    ),
    JAPAN(
        "Japan",
        intArrayOf(
            Color.parseColor("#FBCFE8"), Color.parseColor("#F9A8D4"), Color.parseColor("#BE185D"),
            Color.parseColor("#FFF5F7"), Color.parseColor("#93C5FD"), Color.parseColor("#78350F")
        ),
        ParticleType.PETAL
    ),
    KOREA(
        "South Korea",
        intArrayOf(
            Color.parseColor("#D4772A"), Color.parseColor("#E8A039"), Color.parseColor("#C0392B"),
            Color.parseColor("#F1C40F"), Color.parseColor("#8B4513"), Color.parseColor("#A0522D")
        ),
        ParticleType.LEAF
    ),
    USA(
        "USA",
        intArrayOf(
            Color.parseColor("#B22234"), Color.parseColor("#FFFFFF"), Color.parseColor("#3C3B6E"),
            Color.parseColor("#FFD700"), Color.parseColor("#1E3A8A"), Color.parseColor("#DC143C")
        ),
        ParticleType.STAR
    ),
    UK(
        "UK",
        intArrayOf(
            Color.parseColor("#C8102E"), Color.parseColor("#FFFFFF"), Color.parseColor("#012169"),
            Color.parseColor("#1E3A8A"), Color.parseColor("#FFD700"), Color.parseColor("#00247D")
        ),
        ParticleType.CONFETTI
    );

    companion object {
        val valuesList: List<AnimationTheme> = values().toList()
        val default: AnimationTheme = MASTER

        fun fromIndex(index: Int): AnimationTheme {
            return valuesList.getOrElse(index) { default }
        }

        fun indexOf(theme: AnimationTheme): Int = valuesList.indexOf(theme)
    }
}
