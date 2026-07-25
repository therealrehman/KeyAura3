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
    ),
    GERMANY(
        "Germany",
        intArrayOf(
            Color.parseColor("#000000"), Color.parseColor("#DD0000"), Color.parseColor("#FFCC00"),
            Color.parseColor("#333333"), Color.parseColor("#EEEEEE"), Color.parseColor("#FFD700")
        ),
        ParticleType.SPARKLE
    ),
    FRANCE(
        "France",
        intArrayOf(
            Color.parseColor("#002395"), Color.parseColor("#FFFFFF"), Color.parseColor("#ED2939"),
            Color.parseColor("#1A3B8A"), Color.parseColor("#C00A1E"), Color.parseColor("#F5F5F5")
        ),
        ParticleType.RIBBON
    ),
    UAE(
        "UAE",
        intArrayOf(
            Color.parseColor("#FF0000"), Color.parseColor("#00732F"), Color.parseColor("#FFFFFF"),
            Color.parseColor("#000000"), Color.parseColor("#FFD700"), Color.parseColor("#D2A679")
        ),
        ParticleType.SPARKLE
    ),
    SAUDI(
        "Saudi Arabia",
        intArrayOf(
            Color.parseColor("#006C35"), Color.parseColor("#FFFFFF"), Color.parseColor("#000000"),
            Color.parseColor("#2E8B57"), Color.parseColor("#F5F5F5"), Color.parseColor("#0A3A2A")
        ),
        ParticleType.GEOMETRIC
    );

    companion object {
        val valuesList: List<AnimationTheme> = values().toList()
        val default: AnimationTheme = MASTER
        fun fromIndex(index: Int): AnimationTheme = valuesList.getOrElse(index) { default }
    }
}

enum class ParticleType {
    SPARKLE, PETAL, LEAF, STAR, CONFETTI, RIBBON, GEOMETRIC
}
