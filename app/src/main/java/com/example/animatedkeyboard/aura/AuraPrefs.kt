package com.example.animatedkeyboard.aura

import android.content.Context
import android.content.SharedPreferences

class AuraPrefs private constructor(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("keyaura_aura", Context.MODE_PRIVATE)

    var auraSound: Boolean
        get() = sp.getBoolean("aura_sound", true)
        set(v) { sp.edit().putBoolean("aura_sound", v).apply() }

    var flowMode: Boolean
        get() = sp.getBoolean("flow_mode", false)
        set(v) { sp.edit().putBoolean("flow_mode", v).apply() }

    var oneHand: Boolean
        get() = sp.getBoolean("one_hand", false)
        set(v) { sp.edit().putBoolean("one_hand", v).apply() }

    var bigKeys: Boolean
        get() = sp.getBoolean("big_keys", false)
        set(v) { sp.edit().putBoolean("big_keys", v).apply() }

    var artEnabled: Boolean
        get() = sp.getBoolean("art_enabled", true)
        set(v) { sp.edit().putBoolean("art_enabled", v).apply() }

    companion object {
        @Volatile private var instance: AuraPrefs? = null
        fun getInstance(context: Context): AuraPrefs =
            instance ?: synchronized(this) {
                instance ?: AuraPrefs(context.applicationContext).also { instance = it }
            }
    }
}
