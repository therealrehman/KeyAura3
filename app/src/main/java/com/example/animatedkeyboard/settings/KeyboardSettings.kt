package com.example.animatedkeyboard.settings

import android.content.Context
import android.content.SharedPreferences

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

    var hapticEnabled: Boolean
        get() = prefs.getBoolean("haptic", true)
        set(value) = prefs.edit().putBoolean("haptic", value).apply()

    // FIX: Direct control over vibration timing/strength — previously the app
    // used performHapticFeedback(KEYBOARD_TAP), whose actual duration/intensity
    // is decided by the device's own haptic engine (varies wildly by OEM). This
    // lets the app fully control both, independent of device defaults.
    var hapticDurationMs: Long
        get() = prefs.getLong("haptic_duration_ms", 12L)
        set(value) = prefs.edit().putLong("haptic_duration_ms", value.coerceIn(1L, 100L)).apply()

    var hapticAmplitude: Int
        get() = prefs.getInt("haptic_amplitude", 60)
        set(value) = prefs.edit().putInt("haptic_amplitude", value.coerceIn(1, 255)).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound", true)
        set(value) = prefs.edit().putBoolean("sound", value).apply()

    var urduEnabled: Boolean
        get() = prefs.getBoolean("urdu_enabled", false)
        set(value) = prefs.edit().putBoolean("urdu_enabled", value).apply()

    // FIX: Recent emoji storage for the emoji panel's "Recently Used" tab.
    // Stored as a single delimiter-joined string (most-recent-first, capped)
    // since SharedPreferences has no native ordered-list type and the volume
    // here is small enough that a simple string is the least fragile option.
    private val recentEmojiDelimiter = "\u0000" // NUL can't appear in emoji text
    private val maxRecentEmojis = 30

    fun recentEmojis(): List<String> {
        val raw = prefs.getString("recent_emojis", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split(recentEmojiDelimiter).filter { it.isNotEmpty() }
    }

    fun addRecentEmoji(character: String) {
        val current = recentEmojis().toMutableList()
        current.remove(character) // move-to-front if already present
        current.add(0, character)
        val trimmed = current.take(maxRecentEmojis)
        prefs.edit().putString("recent_emojis", trimmed.joinToString(recentEmojiDelimiter)).apply()
    }

    // FIX: Lightweight local learning for Urdu suggestions — remembers which
    // Urdu spelling the user picked for a given roman word, so it ranks first
    // next time (same spirit as the reference app's Frequency-based ranking,
    // without needing a full SQLite database for this small dataset).
    fun urduWordPreference(romanKeyLower: String): String? =
        prefs.getString("urdu_pref_$romanKeyLower", null)

    fun setUrduWordPreference(romanKeyLower: String, chosenUrduWord: String) {
        prefs.edit().putString("urdu_pref_$romanKeyLower", chosenUrduWord).apply()
    }

    // FIX: Same lightweight learning pattern for English suggestions.
    fun englishWordPreference(prefixLower: String): String? =
        prefs.getString("eng_pref_$prefixLower", null)

    fun setEnglishWordPreference(prefixLower: String, chosenWord: String) {
        prefs.edit().putString("eng_pref_$prefixLower", chosenWord).apply()
    }

    // Which of the 5 swipe-tune instruments is active (see KeySoundEngine.TUNE_NAMES).
    var selectedTuneIndex: Int
        get() = prefs.getInt("selected_tune_index", 0)
        set(value) = prefs.edit().putInt("selected_tune_index", value.coerceIn(0, 9)).apply()

    // UI toggle only for now — the actual ninja fighting overlay feature is
    // being built later; this just persists the on/off state ahead of that.
    var ninjaModeEnabled: Boolean
        get() = prefs.getBoolean("ninja_mode_enabled", false)
        set(value) = prefs.edit().putBoolean("ninja_mode_enabled", value).apply()

    var animationEnabled: Boolean
        get() = prefs.getBoolean("animation", true)
        set(value) = prefs.edit().putBoolean("animation", value).apply()

    var backspaceRepeatIntervalMs: Long
        get() = prefs.getLong("backspace_interval", 100L)
        set(value) = prefs.edit().putLong("backspace_interval", value).apply()
}