package com.example.animatedkeyboard.settings

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class KeyboardSettings private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE)

    companion object {
        @Volatile private var instance: KeyboardSettings? = null
        fun getInstance(context: Context): KeyboardSettings {
            return instance ?: synchronized(this) {
                instance ?: KeyboardSettings(context.applicationContext).also { instance = it }
            }
        }
    }

    var hapticEnabled: Boolean
        get() = prefs.getBoolean("haptic", true)
        set(value) = prefs.edit().putBoolean("haptic", value).apply()

    var hapticDurationMs: Long
        get() = prefs.getLong("haptic_duration_ms", 28L)
        set(value) = prefs.edit().putLong("haptic_duration_ms", value.coerceIn(1L, 100L)).apply()

    var hapticAmplitude: Int
        get() = prefs.getInt("haptic_amplitude", 170)
        set(value) = prefs.edit().putInt("haptic_amplitude", value.coerceIn(1, 255)).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound", true)
        set(value) = prefs.edit().putBoolean("sound", value).apply()

    var keyVolume: Float
        get() = prefs.getFloat("key_volume", 0.85f)
        set(value) = prefs.edit().putFloat("key_volume", value.coerceIn(0f, 1f)).apply()

    var urduEnabled: Boolean
        get() = prefs.getBoolean("urdu_enabled", false)
        set(value) = prefs.edit().putBoolean("urdu_enabled", value).apply()

    var selectedThemeId: String
        get() = prefs.getString("selected_theme_id", "rainbow") ?: "rainbow"
        set(value) = prefs.edit().putString("selected_theme_id", value).apply()

    var customThemeColor: Int
        get() = prefs.getInt("custom_theme_color", 0xFFFF6D00.toInt())
        set(value) = prefs.edit().putInt("custom_theme_color", value).apply()

    var keyboardImagePath: String?
        get() = prefs.getString("keyboard_image_path", null)
        set(value) = prefs.edit().putString("keyboard_image_path", value).apply()

    private val recentEmojiDelimiter = " "
    private val maxRecentEmojis = 30

    fun recentEmojis(): List<String> {
        val raw = prefs.getString("recent_emojis", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split(recentEmojiDelimiter).filter { it.isNotEmpty() }
    }

    fun addRecentEmoji(character: String) {
        val current = recentEmojis().toMutableList()
        current.remove(character)
        current.add(0, character)
        val trimmed = current.take(maxRecentEmojis)
        prefs.edit().putString("recent_emojis", trimmed.joinToString(recentEmojiDelimiter)).apply()
    }

    fun urduWordPreference(romanKeyLower: String): String? =
        prefs.getString("urdu_pref_$romanKeyLower", null)

    fun setUrduWordPreference(romanKeyLower: String, chosenUrduWord: String) {
        prefs.edit().putString("urdu_pref_$romanKeyLower", chosenUrduWord).apply()
    }

    fun englishWordPreference(prefixLower: String): String? =
        prefs.getString("eng_pref_$prefixLower", null)

    fun setEnglishWordPreference(prefixLower: String, chosenWord: String) {
        prefs.edit().putString("eng_pref_$prefixLower", chosenWord).apply()
    }

    fun englishWordBoost(wordLower: String): Int =
        prefs.getInt("eng_boost_$wordLower", 0)

    fun bumpEnglishWord(wordLower: String) {
        prefs.edit().putInt("eng_boost_$wordLower", englishWordBoost(wordLower) + 1).apply()
    }

    var selectedTuneIndex: Int
        get() = prefs.getInt("selected_tune_index", 0)
        set(value) = prefs.edit().putInt("selected_tune_index", value.coerceIn(0, 9)).apply()

    var ninjaModeEnabled: Boolean
        get() = prefs.getBoolean("ninja_mode_enabled", false)
        set(value) = prefs.edit().putBoolean("ninja_mode_enabled", value).apply()

    fun getClipboardEntriesRaw(): List<JSONObject> {
        val raw = prefs.getString("clipboard_entries", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getJSONObject(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setClipboardEntriesRaw(entries: List<JSONObject>) {
        val arr = JSONArray()
        for (e in entries) arr.put(e)
        prefs.edit().putString("clipboard_entries", arr.toString()).apply()
    }

    var birdyBirdHighScore: Int
        get() = prefs.getInt("birdy_bird_high_score", 0)
        set(value) = prefs.edit().putInt("birdy_bird_high_score", value).apply()

    var animationEnabled: Boolean
        get() = prefs.getBoolean("animation", true)
        set(value) = prefs.edit().putBoolean("animation", value).apply()

    var backspaceRepeatIntervalMs: Long
        get() = prefs.getLong("backspace_interval", 80L)
        set(value) = prefs.edit().putLong("backspace_interval", value.coerceIn(30L, 300L)).apply()
}
