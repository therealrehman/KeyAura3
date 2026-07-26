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
            Color.parseColor("#00FF96"), Color.parseColor("#FF00DC"), Color.parseColor("#FFA000")
        ),
        ParticleType.SPARKLE
    ),
    INFERNO(
        "Inferno",
        intArrayOf(
            Color.parseColor("#FF4400"), Color.parseColor("#FFAA00"), Color.parseColor("#FF5500"),
            Color.parseColor("#FF8800"), Color.parseColor("#FF2200"), Color.parseColor("#000000")
        ),
        ParticleType.SPARKLE
    ),
    CYBERPUNK(
        "Cyberpunk",
        intArrayOf(
            Color.parseColor("#FF00FF"), Color.parseColor("#00FFFF"), Color.parseColor("#AA00CC"),
            Color.parseColor("#1A0033"), Color.parseColor("#FF44FF"), Color.parseColor("#0A0014")
        ),
        ParticleType.NEON_GLOW
    ),
    OCEAN(
        "Ocean",
        intArrayOf(
            Color.parseColor("#00AAFF"), Color.parseColor("#00CCFF"), Color.parseColor("#0066AA"),
            Color.parseColor("#001122"), Color.parseColor("#88DDFF"), Color.parseColor("#000D1A")
        ),
        ParticleType.CIRCLE
    ),
    MATRIX(
        "Matrix",
        intArrayOf(
            Color.parseColor("#00FF41"), Color.parseColor("#008822"), Color.parseColor("#00FF00"),
            Color.parseColor("#003300"), Color.parseColor("#88FFAA"), Color.parseColor("#000500")
        ),
        ParticleType.SPARKLE
    ),
    SUNSET(
        "Sunset",
        intArrayOf(
            Color.parseColor("#FF6B9D"), Color.parseColor("#AA3366"), Color.parseColor("#FF88AA"),
            Color.parseColor("#1A0008"), Color.parseColor("#FF99BB"), Color.parseColor("#220011")
        ),
        ParticleType.CIRCLE
    ),
    ICE(
        "Ice",
        intArrayOf(
            Color.parseColor("#AEEEFF"), Color.parseColor("#4488AA"), Color.parseColor("#CCFFFF"),
            Color.parseColor("#001122"), Color.parseColor("#88DDFF"), Color.parseColor("#002233")
        ),
        ParticleType.CIRCLE
    ),
    NEON(
        "Neon",
        intArrayOf(
            Color.parseColor("#39FF14"), Color.parseColor("#228811"), Color.parseColor("#88FF66"),
            Color.parseColor("#050505"), Color.parseColor("#00FF00"), Color.parseColor("#111111")
        ),
        ParticleType.NEON_GLOW
    ),
    AURORA(
        "Aurora",
        intArrayOf(
            Color.parseColor("#00FFAA"), Color.parseColor("#008855"), Color.parseColor("#88FFCC"),
            Color.parseColor("#00110A"), Color.parseColor("#00FFEE"), Color.parseColor("#001A11")
        ),
        ParticleType.CIRCLE
    ),
    VOLCANIC(
        "Volcanic",
        intArrayOf(
            Color.parseColor("#FF2200"), Color.parseColor("#882200"), Color.parseColor("#FF4400"),
            Color.parseColor("#0A0000"), Color.parseColor("#FF6633"), Color.parseColor("#1A0000")
        ),
        ParticleType.SPARKLE
    ),
    GALAXY(
        "Galaxy",
        intArrayOf(
            Color.parseColor("#AA66FF"), Color.parseColor("#6644AA"), Color.parseColor("#DDAAFF"),
            Color.parseColor("#080014"), Color.parseColor("#8844DD"), Color.parseColor("#140033")
        ),
        ParticleType.SPARKLE
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
