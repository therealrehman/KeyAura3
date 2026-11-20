package com.keyaura.accessibility

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.accessibility.AccessibilityManager

/**
 * FlowAndAccessibilityManager: Handles Flow Mode (Zen typing) 
 * and Accessibility features (Dyslexia, One-Hand, Haptics).
 */
class FlowAndAccessibilityManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("keyaura_settings", Context.MODE_PRIVATE)
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // --- FLOW MODE (Zen / Distraction Free) ---
    var isFlowModeEnabled: Boolean
        get() = prefs.getBoolean("flow_mode", false)
        set(value) { prefs.edit().putBoolean("flow_mode", value).apply() }

    fun getFlowModeSettings(): FlowSettings {
        return if (isFlowModeEnabled) {
            FlowSettings(
                showKeyPreview = false,      // No popup on keypress
                showSound = false,           // Silent typing
                showVibration = false,       // No haptics
                keyOpacity = 0.05f,          // Ultra minimal keys
                hideSuggestions = true       // No distraction bar
            )
        } else {
            FlowSettings(
                showKeyPreview = true,
                showSound = prefs.getBoolean("sound_on", true),
                showVibration = prefs.getBoolean("vibration_on", true),
                keyOpacity = 0.15f,
                hideSuggestions = false
            )
        }
    }

    // --- ACCESSIBILITY FEATURES ---
    var isDyslexiaFriendly: Boolean
        get() = prefs.getBoolean("dyslexia_friendly", false)
        set(value) { prefs.edit().putBoolean("dyslexia_friendly", value).apply() }

    var isOneHandMode: Boolean
        get() = prefs.getBoolean("one_hand_mode", false)
        set(value) { prefs.edit().putBoolean("one_hand_mode", value).apply() }

    var hapticIntensity: Int // 0 to 100
        get() = prefs.getInt("haptic_intensity", 50)
        set(value) { prefs.edit().putInt("haptic_intensity", value.coerceIn(0, 100)).apply() }

    fun getDyslexiaFontName(): String {
        return if (isDyslexiaFriendly) "sans-serif-medium" else "sans-serif" // Fallback to bold sans-serif if OpenDyslexic not bundled
    }

    fun getKeyHeightMultiplier(): Float {
        return if (isDyslexiaFriendly) 1.3f else 1.0f // 30% taller keys for motor impairment
    }

    fun getKeyboardWidthPercent(): Float {
        return if (isOneHandMode) 0.75f else 1.0f // 75% width for one-hand use
    }

    // --- HAPTIC FEEDBACK ENGINE ---
    fun triggerHapticFeedback(isSpacebar: Boolean = false) {
        if (!getFlowModeSettings().showVibration) return
        if (vibrator == null || !vibrator.hasVibrator()) return

        val intensity = hapticIntensity / 100f
        val amplitude = (255 * intensity).toInt().coerceIn(1, 255)
        val duration = if (isSpacebar) 15L else 10L // Spacebar gets a slightly longer tap

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    // --- SYSTEM ACCESSIBILITY CHECK ---
    fun isSystemAccessibilityEnabled(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.isEnabled
    }

    data class FlowSettings(
        val showKeyPreview: Boolean,
        val showSound: Boolean,
        val showVibration: Boolean,
        val keyOpacity: Float,
        val hideSuggestions: Boolean
    )
}
