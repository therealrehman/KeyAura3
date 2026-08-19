package com.example.animatedkeyboard.settings

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
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

    var hapticEnabled: Boolean
        get() = prefs.getBoolean("haptic", true)
        set(value) = prefs.edit().putBoolean("haptic", value).apply()

    // FIX: Direct control over vibration timing/strength — previously the app
    // used performHapticFeedback(KEYBOARD_TAP), whose actual duration/intensity
    // is decided by the device's own haptic engine (varies wildly by OEM). This
    // lets the app fully control both, independent of device defaults.
    // FIX: 12ms/60 was too weak/short to be physically felt on many vibration
    // motors (they need a brief spin-up before producing real force).
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
    fun englishWordBoost(word: String): Int = prefs.getInt("eng_boost_$word", 0)

    fun bumpEnglishWord(word: String) { val c = prefs.getInt("eng_boost_$word", 0); prefs.edit().putInt("eng_boost_$word", c + 1).apply() }

    fun englishWordPreference(prefixLower: String): String? =
        prefs.getString("eng_pref_$prefixLower", null)

    fun setEnglishWordPreference(prefixLower: String, chosenWord: String) {
        prefs.edit().putString("eng_pref_$prefixLower", chosenWord).apply()
    }

    // Which of the 10 swipe-tune instruments is active (see KeySoundEngine.TUNE_NAMES).
    // -1 = no tune (default for fresh installs; tunes are locked behind a rewarded ad).
    var selectedTuneIndex: Int
        get() = prefs.getInt("selected_tune_index", -1)
        set(value) = prefs.edit().putInt("selected_tune_index", value.coerceIn(-1, 9)).apply()

    // UI toggle only for now — the actual ninja fighting overlay feature is
    // being built later; this just persists the on/off state ahead of that.
    var ninjaModeEnabled: Boolean
        get() = prefs.getBoolean("ninja_mode_enabled", false)
        set(value) = prefs.edit().putBoolean("ninja_mode_enabled", value).apply()

    // FIX: Clipboard history storage — structured JSON list (id/type/content/
    // pinned/timestamp) since a plain string list (like recentEmojis) can't
    // carry the pin flag or distinguish text vs image entries.
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

    // ---------- Theming (custom color / photo background) ----------

    // Which theme is active: a preset id ("rainbow", "inferno", ...) or the
    // special ids "custom_color" / "custom_image" — see ThemeRepository.resolve().
    // Default for new installs is "solid_slate" — a static theme that needs no ad unlock.
    // Animated themes (rainbow, etc.) are unlocked via rewarded ad; KeyboardView enforces this at runtime.
    var selectedThemeId: String
        get() = prefs.getString("selected_theme_id", "solid_slate") ?: "solid_slate"
        set(value) = prefs.edit().putString("selected_theme_id", value).apply()

    // User-picked color from the color wheel dialog (MainActivity.showColorPickerDialog).
    var customThemeColor: Int
        get() = prefs.getInt("custom_theme_color", Color.parseColor("#4488FF"))
        set(value) = prefs.edit().putInt("custom_theme_color", value).apply()

    // Absolute path to the copied keyboard background photo, or null if none picked yet.
    var keyboardImagePath: String?
        get() = prefs.getString("keyboard_image_path", null)
        set(value) = prefs.edit().putString("keyboard_image_path", value).apply()

    // Key click / tune playback volume, 0f (silent) .. 1f (full).
    var keyVolume: Float
        get() = prefs.getFloat("key_volume", 1.0f)
        set(value) = prefs.edit().putFloat("key_volume", value.coerceIn(0f, 1f)).apply()
    // Last animated theme user selected — used to re-apply after 3hr unlock expires.
    var lastAnimatedThemeId: String?
        get() = prefs.getString("last_animated_theme_id", null)
        set(value) = prefs.edit().putString("last_animated_theme_id", value).apply()

    // Fallback static theme shown when animated themes are locked.
    val staticFallbackThemeId: String get() = "solid_slate"
}