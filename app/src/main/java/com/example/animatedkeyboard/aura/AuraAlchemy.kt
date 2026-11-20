package com.example.animatedkeyboard.aura

object AuraAlchemy {
    val TRIGGERS = setOf("/pro", "/fun", "/poet", "/aura")

    fun isTrigger(token: String) = token in TRIGGERS

    fun shift(text: String, trigger: String): String {
        var t = text.trim()
        if (t.isEmpty()) return t
        when (trigger) {
            "/pro" -> {
                t = t.replace(Regex("(?i)\\b(u|ur)\\b"), "you")
                    .replace(Regex("(?i)\\bgonna\\b"), "going to")
                    .replace(Regex("(?i)\\bwanna\\b"), "want to")
                    .replace(Regex("(?i)\\bthx\\b"), "thank you")
                    .replace(Regex("(?i)\\bpls\\b"), "please")
                    .replace(Regex("(?i)\\basap\\b"), "as soon as possible")
                t = t.replaceFirstChar { it.uppercase() }
                if (!t.endsWith(".")) t += "."
                t += " Regards."
            }
            "/fun" -> {
                t = t.replace(Regex("(?i)\\bhello\\b"), "Heyy")
                    .replace(Regex("(?i)\\byes\\b"), "yesss")
                    .replace(Regex("(?i)\\bno\\b"), "nope")
                if (!t.endsWith("!") && !t.endsWith("😊")) t += " 😄"
            }
            "/poet" -> {
                t = t.replace(Regex("(?i)\\bdil\\b"), "دل")
                    .replace(Regex("(?i)\\bishq\\b"), "عشق")
                    .replace(Regex("(?i)\\bzindagi\\b"), "زندگی")
                    .replace(Regex("(?i)\\bpyar\\b"), "پیار")
                    .replace(Regex("(?i)\\bdosti\\b"), "دوستی")
                t += " ✨"
            }
        }
        return t
    }

    val shayari = listOf(
        "ہزاروں خواہشیں ایسی کہ ہر خواہش پہ دم نکلے",
        "خودی کو کر بلند اتنا کہ ہر تقدیر سے پہلے",
        "ستاروں سے آگے جہاں اور بھی ہیں",
        "نہیں ہے ناامید اقبال اپنی کشتِ ویراں سے"
    )
}
