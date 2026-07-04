package com.example.animatedkeyboard.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.example.animatedkeyboard.R

/**
 * Plays a bundled click sound (res/raw/key_click.wav) via SoundPool instead of
 * AudioManager.playSoundEffect(). The system version depends on the device's
 * "Touch sounds" setting being enabled AND the SYSTEM audio stream not being
 * muted — on many devices/OEM skins that's off by default, so it can silently
 * play nothing even though the code is "working". A bundled SoundPool sound
 * plays regardless of that setting, using its own audio stream.
 */
class KeySoundEngine(context: Context) {
    private val TAG = "KeySoundEngine"
    private val appContext = context.applicationContext

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var soundId = 0
    private var isLoaded = false

    init {
        try {
            soundPool.setOnLoadCompleteListener { _, _, status ->
                isLoaded = status == 0
                if (!isLoaded) {
                    Log.w(TAG, "Sound failed to load, status=$status")
                }
            }
            soundId = soundPool.load(appContext, R.raw.key_click, 1)
        } catch (e: Exception) {
            Log.w(TAG, "Could not load key_click sound: ${e.message}")
        }
    }

    fun playClick() {
        if (!isLoaded || soundId == 0) return
        try {
            soundPool.play(soundId, 0.5f, 0.5f, 1, 0, 1.0f)
        } catch (e: Exception) {
            Log.w(TAG, "Could not play sound: ${e.message}")
        }
    }

    fun release() {
        try {
            soundPool.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing SoundPool: ${e.message}")
        }
    }
}
