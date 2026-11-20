package com.example.animatedkeyboard.aura

import android.graphics.Color

object AuraContext {
    enum class Category(val badge: String, val accent: Int) {
        SOCIAL("SOCIAL", Color.parseColor("#FF4081")),
        WORK("WORK", Color.parseColor("#90A4AE")),
        CREATE("CREATE", Color.parseColor("#7C4DFF")),
        BROWSE("BROWSE", Color.parseColor("#00E5FF")),
        GENERAL("AURA", Color.parseColor("#00E676"))
    }

    fun categorize(packageName: String?): Category {
        val p = packageName?.lowercase() ?: return Category.GENERAL
        return when {
            p.contains("whatsapp") || p.contains("instagram") || p.contains("facebook") ||
            p.contains("messenger") || p.contains("telegram") || p.contains("snapchat") -> Category.SOCIAL
            p.contains("gmail") || p.contains("outlook") || p.contains("slack") ||
            p.contains("teams") || p.contains("mail") -> Category.WORK
            p.contains("note") || p.contains("keep") || p.contains("writer") ||
            p.contains("docs") || p.contains("notion") -> Category.CREATE
            p.contains("chrome") || p.contains("browser") || p.contains("firefox") ||
            p.contains("search") -> Category.BROWSE
            else -> Category.GENERAL
        }
    }
}
