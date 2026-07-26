package com.example.animatedkeyboard.settings

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import com.example.animatedkeyboard.theme.AnimationTheme
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

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound", true)
        set(value) = prefs.edit().putBoolean("sound", value).apply()

    var urduEnabled: Boolean
        get() = prefs.getBoolean("urdu_enabled", false)
        set(value) = prefs.edit().putBoolean("urdu_enabled", value).apply()

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

    var ninjaModeEnabled: Boolean
        get() = prefs.getBoolean("ninja_mode_enabled", false)
        set(value) = prefs.edit().putBoolean("ninja_mode_enabled", value).apply()

    var animationEnabled: Boolean
        get() = prefs.getBoolean("animation", true)
        set(value) = prefs.edit().putBoolean("animation", value).apply()

    // FIX: Added Missing Settings Properties to prevent KeyboardView Crashes
    var keyOutlineEnabled: Boolean
        get() = prefs.getBoolean("key_outline", true)
        set(value) = prefs.edit().putBoolean("key_outline", value).apply()

    var primaryColor: Int
        get() = prefs.getInt("primary_color", Color.parseColor("#4488FF"))
        set(value) = prefs.edit().putInt("primary_color", value).apply()

    var accentColor: Int
        get() = prefs.getInt("accent_color", Color.parseColor("#FF64C8"))
        set(value) = prefs.edit().putInt("accent_color", value).apply()

    var keyShape: String
        get() = prefs.getString("key_shape", "rounded") ?: "rounded"
        set(value) = prefs.edit().putString("key_shape", value).apply()

    var floatingEnabled: Boolean
        get() = prefs.getBoolean("floating_enabled", false)
        set(value) = prefs.edit().putBoolean("floating_enabled", value).apply()

    var keySpacing: Float
        get() = prefs.getFloat("key_spacing", 1.0f)
        set(value) = prefs.edit().putFloat("key_spacing", value.coerceIn(0.8f, 1.5f)).apply()

    var backgroundOpacity: Float
        get() = prefs.getFloat("bg_opacity", 1.0f)
        set(value) = prefs.edit().putFloat("bg_opacity", value.coerceIn(0.0f, 1.0f)).apply()

    var animationType: String
        get() = prefs.getString("animation_type", "SPARKLE") ?: "SPARKLE"
        set(value) = prefs.edit().putString("animation_type", value).apply()

    var backspaceRepeatIntervalMs: Long
        get() = prefs.getLong("backspace_interval", 100L)
        set(value) = prefs.edit().putLong("backspace_interval", value).apply()

    var selectedTuneIndex: Int
        get() = prefs.getInt("selected_tune_index", 0)
        set(value) = prefs.edit().putInt("selected_tune_index", value.coerceIn(0, 9)).apply()

    var selectedThemeIndex: Int
        get() = prefs.getInt("selected_theme_index", 0)
        set(value) = prefs.edit().putInt("selected_theme_index", value.coerceIn(0, AnimationTheme.values().size - 1)).apply()

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

    // Text Expansion
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

    var birdyBirdHighScore: Int
        get() = prefs.getInt("birdy_bird_high_score", 0)
        set(value) = prefs.edit().putInt("birdy_bird_high_score", value).apply()

    // Language Preferences & Recent Emojis
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

    fun urduWordPreference(romanKeyLower: String): String? = prefs.getString("urdu_pref_$romanKeyLower", null)
    fun setUrduWordPreference(romanKeyLower: String, chosenUrduWord: String) { prefs.edit().putString("urdu_pref_$romanKeyLower", chosenUrduWord).apply() }
    fun englishWordPreference(prefixLower: String): String? = prefs.getString("eng_pref_$prefixLower", null)
    fun setEnglishWordPreference(prefixLower: String, chosenWord: String) { prefs.edit().putString("eng_pref_$prefixLower", chosenWord).apply() }
}
