package com.example.animatedkeyboard.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.example.animatedkeyboard.R

/**
 * Plays two kinds of sound, both via bundled SoundPool assets (not
 * AudioManager.playSoundEffect(), which silently depends on the device's
 * "Touch sounds" setting and system volume stream — unreliable across devices):
 *
 *  - playClick(): the normal single tap sound (res/raw/key_click.wav)
 *  - playSwipeTone(noteIndex): one note of a bundled C major pentatonic scale
 *    (res/raw/swipe_note_0..9.wav), triggered continuously as a finger glides
 *    across keys, so swiping the keyboard sounds like running a finger across
 *    a wind chime / kalimba rather than a string of identical clicks. The
 *    pentatonic scale is used specifically because any combination/order of
 *    its notes is consonant — there's no "wrong" sequence to worry about.
 */
class KeySoundEngine(context: Context) {
    private val TAG = "KeySoundEngine"
    private val appContext = context.applicationContext

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6) // a few clicks/notes can legitimately overlap during fast swipes
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var clickSoundId = 0
    private var isClickLoaded = false

    // FIX: C major pentatonic scale (2 octaves) for the swipe glide sound.
    private val swipeNoteResIds = intArrayOf(
        R.raw.swipe_note_0, R.raw.swipe_note_1, R.raw.swipe_note_2, R.raw.swipe_note_3,
        R.raw.swipe_note_4, R.raw.swipe_note_5, R.raw.swipe_note_6, R.raw.swipe_note_7,
        R.raw.swipe_note_8, R.raw.swipe_note_9
    )
    private val swipeNoteSoundIds = IntArray(swipeNoteResIds.size)
    private val swipeNoteLoaded = BooleanArray(swipeNoteResIds.size)

    val noteCount: Int get() = swipeNoteResIds.size

    init {
        try {
            soundPool.setOnLoadCompleteListener { _, sampleId, status ->
                val loaded = status == 0
                if (sampleId == clickSoundId) {
                    isClickLoaded = loaded
                } else {
                    val idx = swipeNoteSoundIds.indexOf(sampleId)
                    if (idx != -1) swipeNoteLoaded[idx] = loaded
                }
            }
            clickSoundId = soundPool.load(appContext, R.raw.key_click, 1)
            for (i in swipeNoteResIds.indices) {
                swipeNoteSoundIds[i] = soundPool.load(appContext, swipeNoteResIds[i], 1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load sounds: ${e.message}")
        }
    }

    fun playClick() {
        if (!isClickLoaded || clickSoundId == 0) return
        try {
            soundPool.play(clickSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        } catch (e: Exception) {
            Log.w(TAG, "Could not play click: ${e.message}")
        }
    }

    /** Plays one note of the pentatonic scale; noteIndex wraps to stay in range. */
    fun playSwipeTone(noteIndex: Int) {
        val idx = ((noteIndex % noteCount) + noteCount) % noteCount // safe wrap for negatives too
        if (!swipeNoteLoaded[idx]) return
        try {
            soundPool.play(swipeNoteSoundIds[idx], 1.0f, 1.0f, 1, 0, 1.0f)
        } catch (e: Exception) {
            Log.w(TAG, "Could not play swipe tone: ${e.message}")
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
