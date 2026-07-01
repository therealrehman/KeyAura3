package com.example.animatedkeyboard.audio

import android.content.Context
import android.media.AudioManager
import android.util.Log

class KeySoundEngine(context: Context) {
    private val audioManager =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val TAG = "KeySoundEngine"

    fun playClick() {
        try {
            audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
        } catch (e: Exception) {
            Log.w(TAG, "Could not play sound: ${e.message}")
        }
    }

    fun release() {
        // No SoundPool resources to release; AudioManager sound effects are stateless.
    }
}