package com.example.animatedkeyboard.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import android.view.View

class KeySoundEngine(context: Context) {
    private val soundPool: SoundPool
    private var clickSoundId: Int = 0
    private val TAG = "KeySoundEngine"
    private val view = View(context)

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        // Load default system click sound
        try {
            clickSoundId = soundPool.load(context, android.R.raw.keypress_standard, 1)
        } catch (e: Exception) {
            Log.w(TAG, "Could not load sound: ${e.message}")
            clickSoundId = 0
        }
    }

    fun playClick() {
        if (clickSoundId != 0) {
            try {
                soundPool.play(clickSoundId, 0.5f, 0.5f, 0, 0, 1.0f)
            } catch (e: Exception) {
                Log.w(TAG, "Could not play sound: ${e.message}")
            }
        }
    }

    fun release() {
        try {
            soundPool.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing sound pool: ${e.message}")
        }
    }
}