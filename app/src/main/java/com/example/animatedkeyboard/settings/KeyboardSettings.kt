package com.example.animatedkeyboard.settings

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class KeyboardSettings private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var instance: KeyboardSettings? = null

        fun getInstance(context: Context): KeyboardSettings {
            return instance ?: synchronized(this) {
                instance ?: KeyboardSettings(context.applicationContext).also { instance = it }
            }
        }
    }

    // Basic settings
    var hapticEnabled: Boolean
        get() = prefs.getBoolean("haptic", true)
        set(value) = prefs.edit().putBoolean("haptic", value).apply()

    var hapticDurationMs: Long
        get() = prefs.getLong("haptic_duration_ms", 30L)
        set(value) = prefs.edit().putLong("haptic_duration_ms", value.coerceIn(1L, 100L)).apply()

    var hapticAmplitude: Int
        get() = prefs.getInt("haptic_amplitude", 160)
        set(value) = prefs.edit().putInt("haptic_amplitude", value.coerceIn(1, 255)).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound", true)
        set(value) = prefs.edit().putBoolean("sound", value).apply()

    var urduEnabled: Boolean
        get() = prefs.getBoolean("urdu_enabled", false)
        set(value) = prefs.edit().putBoolean("urdu_enabled", value).apply()

    // New settings
    var gestureTypingEnabled: Boolean
        get() = prefs.getBoolean("gesture_typing", false)
        set(value) = prefs.edit().putBoolean("gesture_typing", value).apply()

    var numberRowEnabled: Boolean
        get() = prefs.getBoolean("number_row", true)
        set(value) = prefs.edit().putBoolean("number_row", value).apply()

    var keyboardHeightPercent: Int
        get() = prefs.getInt("keyboard_height_percent", 35)
        set(value) = prefs.edit().putInt("keyboard_height_percent", value.coerceIn(30, 50)).apply()

    var textExpansionEnabled: Boolean
        get() = prefs.getBoolean("text_expansion", true)
        set(value) = prefs.edit().putBoolean("text_expansion", value).apply()

    var cursorSwipeEnabled: Boolean
        get() = prefs.getBoolean("cursor_swipe", true)
        set(value) = prefs.edit().putBoolean("cursor_swipe", value).apply()

    // Theme
    var selectedThemeIndex: Int
        get() = prefs.getInt("selected_theme_index", 0)
        set(value) = prefs.edit().putInt("selected_theme_index", value.coerceIn(0, 9)).apply()

    // Recent emoji
    private val recentEmojiDelimiter = "\u0000"
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

    // Language preferences
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

    // Tune
    var selectedTuneIndex: Int
        get() = prefs.getInt("selected_tune_index", 0)
        set(value) = prefs.edit().putInt("selected_tune_index", value.coerceIn(0, 9)).apply()

    var ninjaModeEnabled: Boolean
        get() = prefs.getBoolean("ninja_mode_enabled", false)
        set(value) = prefs.edit().putBoolean("ninja_mode_enabled", value).apply()

    // Clipboard
    fun getClipboardEntriesRaw(): List<JSONObject> {
        val raw = prefs.getString("clipboard_entries", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getJSONObject(it) }
        } catch (_: Exception) { emptyList() }
    }

    fun setClipboardEntriesRaw(entries: List<JSONObject>) {
        val arr = JSONArray()
        for (e in entries) arr.put(e)
        prefs.edit().putString("clipboard_entries", arr.toString()).apply()
    }

    // Birdy Bird high score
    var birdyBirdHighScore: Int
        get() = prefs.getInt("birdy_bird_high_score", 0)
        set(value) = prefs.edit().putInt("birdy_bird_high_score", value).apply()

    var animationEnabled: Boolean
        get() = prefs.getBoolean("animation", true)
        set(value) = prefs.edit().putBoolean("animation", value).apply()

    var backspaceRepeatIntervalMs: Long
        get() = prefs.getLong("backspace_interval", 100L)
        set(value) = prefs.edit().putLong("backspace_interval", value).apply()

    // Text Expansion Shortcuts (stored as JSON)
    var textShortcuts: Map<String, String>
        get() {
            val json = prefs.getString("text_shortcuts", "{}") ?: "{}"
            return try {
                val obj = JSONObject(json)
                val map = mutableMapOf<String, String>()
                obj.keys().forEach { key -> map[key] = obj.getString(key) }
                map
            } catch (_: Exception) { emptyMap() }
        }
        set(value) {
            val obj = JSONObject()
            value.forEach { (k, v) -> obj.put(k, v) }
            prefs.edit().putString("text_shortcuts", obj.toString()).apply()
        }

    fun addTextShortcut(shortcut: String, expansion: String) {
        val current = textShortcuts.toMutableMap()
        current[shortcut] = expansion
        textShortcuts = current
    }

    fun removeTextShortcut(shortcut: String) {
        val current = textShortcuts.toMutableMap()
        current.remove(shortcut)
        textShortcuts = current
    }
}
