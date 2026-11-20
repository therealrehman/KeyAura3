package com.keyaura.intelligence

enum class AppContext { SOCIAL, PROFESSIONAL, CREATIVE, UNKNOWN }

class SmartContext {

    // 1. CONTEXTUAL CHAMELEON MODE
    fun detectContext(packageName: String): AppContext {
        return when {
            packageName.contains("whatsapp") || packageName.contains("instagram") || packageName.contains("facebook") -> AppContext.SOCIAL
            packageName.contains("gmail") || packageName.contains("outlook") || packageName.contains("slack") -> AppContext.PROFESSIONAL
            packageName.contains("notes") || packageName.contains("keep") || packageName.contains("writer") -> AppContext.CREATIVE
            else -> AppContext.UNKNOWN
        }
    }

    fun getThemeForContext(context: AppContext): Long {
        return when (context) {
            AppContext.SOCIAL -> 0xFF7C4DFF // Vibrant Purple
            AppContext.PROFESSIONAL -> 0xFF90A4AE // Sleek Blue Grey
            AppContext.CREATIVE -> 0xFFFF4081 // Artistic Pink
            AppContext.UNKNOWN -> 0xFF00E5FF // Default Cyan
        }
    }

    // 2. LINGUISTIC ALCHEMY (Real-Time Vibe Shifting)
    // Note: Production mein isay Local LLM (TensorFlow Lite) se replace karein. Yeh heuristic engine hai.
    fun shiftVibe(rawText: String, targetVibe: Vibe): String {
        var text = rawText
        when (targetVibe) {
            Vibe.PROFESSIONAL -> {
                text = text.replace(Regex("\\b(u|ur)\\b"), "you/your")
                text = text.replace(Regex("\\b(gonna)\\b"), "going to")
                text = text.replace(Regex("\\b(wanna)\\b"), "want to")
                text = text.replace(Regex("\\b(thx)\\b"), "thank you")
                text += " Regards."
            }
            Vibe.FRIENDLY -> {
                text = text.replace(Regex("\\.$"), " 😊")
                text = text.replace(Regex("\\b(hello)\\b"), "Hey")
                text = text.replace(Regex("\\b(yes)\\b"), "Yep")
            }
            Vibe.URDU_POETIC -> {
                // Roman Urdu to Urdu Script mapping (Simplified)
                text = text.replace(Regex("\\b(dil)\\b"), "دل")
                text = text.replace(Regex("\\b(ishq)\\b"), "عشق")
                text = text.replace(Regex("\\b(zindagi)\\b"), "زندگی")
            }
        }
        return text
    }
}

enum class Vibe { PROFESSIONAL, FRIENDLY, URDU_POETIC }
