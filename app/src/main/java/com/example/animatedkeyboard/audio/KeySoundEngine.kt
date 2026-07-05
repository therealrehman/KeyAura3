package com.example.animatedkeyboard.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.example.animatedkeyboard.R
import com.example.animatedkeyboard.settings.KeyboardSettings

/**
 * Plays the tap-click sound and one note of the currently-selected swipe tune
 * (a bundled pentatonic scale, choice of 5 instrument timbres — see TUNE_NAMES)
 * via SoundPool. The active tune is read from KeyboardSettings at construction
 * time, matching whatever the user picked in the Tune screen.
 */
class KeySoundEngine(context: Context) {
    private val TAG = "KeySoundEngine"
    private val appContext = context.applicationContext
    private val settings = KeyboardSettings.getInstance(appContext)

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA) // FIX: media stream = controllable via phone volume buttons
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var clickSoundId = 0
    private var isClickLoaded = false

    private val noteResIds: IntArray
    private val noteSoundIds: IntArray
    private val noteLoaded: BooleanArray

    val noteCount: Int get() = noteResIds.size

    init {
        noteResIds = resIdsForTune(settings.selectedTuneIndex)
        noteSoundIds = IntArray(noteResIds.size)
        noteLoaded = BooleanArray(noteResIds.size)
        try {
            soundPool.setOnLoadCompleteListener { _, sampleId, status ->
                val loaded = status == 0
                if (sampleId == clickSoundId) {
                    isClickLoaded = loaded
                } else {
                    val idx = noteSoundIds.indexOf(sampleId)
                    if (idx != -1) noteLoaded[idx] = loaded
                }
            }
            clickSoundId = soundPool.load(appContext, R.raw.key_click, 1)
            for (i in noteResIds.indices) {
                noteSoundIds[i] = soundPool.load(appContext, noteResIds[i], 1)
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

    /** Plays one note of the active swipe tune; noteIndex wraps to stay in range. */
    fun playSwipeTone(noteIndex: Int) {
        if (noteCount == 0) return
        val idx = ((noteIndex % noteCount) + noteCount) % noteCount
        if (!noteLoaded[idx]) return
        try {
            soundPool.play(noteSoundIds[idx], 1.0f, 1.0f, 1, 0, 1.0f)
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

    companion object {
        // FIX: shared list of tune names + resource-id lookup, used both here
        // and by the Tune selection screen, so they never drift out of sync.
        val TUNE_NAMES = listOf("Chime", "Tabla", "Marimba", "Piano", "Flute")

        fun resIdsForTune(tuneIndex: Int): IntArray {
            val safeIndex = tuneIndex.coerceIn(0, TUNE_NAMES.size - 1)
            return when (safeIndex) {
                0 -> intArrayOf(
                    R.raw.tune_0_note_0, R.raw.tune_0_note_1, R.raw.tune_0_note_2, R.raw.tune_0_note_3, R.raw.tune_0_note_4,
                    R.raw.tune_0_note_5, R.raw.tune_0_note_6, R.raw.tune_0_note_7, R.raw.tune_0_note_8, R.raw.tune_0_note_9
                )
                1 -> intArrayOf(
                    R.raw.tune_1_note_0, R.raw.tune_1_note_1, R.raw.tune_1_note_2, R.raw.tune_1_note_3, R.raw.tune_1_note_4,
                    R.raw.tune_1_note_5, R.raw.tune_1_note_6, R.raw.tune_1_note_7, R.raw.tune_1_note_8, R.raw.tune_1_note_9
                )
                2 -> intArrayOf(
                    R.raw.tune_2_note_0, R.raw.tune_2_note_1, R.raw.tune_2_note_2, R.raw.tune_2_note_3, R.raw.tune_2_note_4,
                    R.raw.tune_2_note_5, R.raw.tune_2_note_6, R.raw.tune_2_note_7, R.raw.tune_2_note_8, R.raw.tune_2_note_9
                )
                3 -> intArrayOf(
                    R.raw.tune_3_note_0, R.raw.tune_3_note_1, R.raw.tune_3_note_2, R.raw.tune_3_note_3, R.raw.tune_3_note_4,
                    R.raw.tune_3_note_5, R.raw.tune_3_note_6, R.raw.tune_3_note_7, R.raw.tune_3_note_8, R.raw.tune_3_note_9
                )
                else -> intArrayOf(
                    R.raw.tune_4_note_0, R.raw.tune_4_note_1, R.raw.tune_4_note_2, R.raw.tune_4_note_3, R.raw.tune_4_note_4,
                    R.raw.tune_4_note_5, R.raw.tune_4_note_6, R.raw.tune_4_note_7, R.raw.tune_4_note_8, R.raw.tune_4_note_9
                )
            }
        }
    }
}
