package com.keyaura.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import com.keyaura.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.LinkedList

data class TypingDNA(
    val wpm: Int,
    val rhythmVariance: Float, // 0.0 (Robot) to 1.0 (Chaotic)
    val burstIntensity: Float, // 0.0 to 1.0
    val mood: MoodState
)

enum class MoodState(val colorHex: Long, val soundScale: Float) {
    CALM(0xFF00E5FF, 1.0f),      // Cyan
    FOCUSED(0xFF7C4DFF, 1.2f),   // Purple
    EXCITED(0xFFFF4081, 1.5f),   // Pink
    INTENSE(0xFFFF6D00, 1.8f),   // Orange
    RHYTHMIC(0xFF00E676, 1.1f)   // Green
}

class AuraEngine(private val context: Context) {

    private val _typingDNA = MutableStateFlow(TypingDNA(0, 0f, 0f, MoodState.CALM))
    val typingDNA: StateFlow<TypingDNA> = _typingDNA

    private val keyIntervals = LinkedList<Long>()
    private var lastKeyTime: Long = 0L
    private var totalChars: Int = 0
    private var sessionStartTime: Long = System.currentTimeMillis()

    // TypeMusic (Synesthetic Soundscapes)
    private val soundPool: SoundPool
    private val noteIds = IntArray(5)
    private var noteIndex = 0

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(10).setAudioAttributes(attrs).build()
        
        // Load Pentatonic Scale (C, D, E, G, A) for pleasing melodies
        noteIds[0] = soundPool.load(context, R.raw.note_c, 1)
        noteIds[1] = soundPool.load(context, R.raw.note_d, 1)
        noteIds[2] = soundPool.load(context, R.raw.note_e, 1)
        noteIds[3] = soundPool.load(context, R.raw.note_g, 1)
        noteIds[4] = soundPool.load(context, R.raw.note_a, 1)
    }

    fun processKeypress(charCode: Int) {
        val now = System.currentTimeMillis()
        totalChars++

        if (lastKeyTime > 0) {
            val interval = now - lastKeyTime
            keyIntervals.addLast(interval)
            if (keyIntervals.size > 50) keyIntervals.removeFirst() // Keep rolling window

            analyzeDNA()
            playSynestheticSound(interval)
        }
        lastKeyTime = now
    }

    private fun analyzeDNA() {
        if (keyIntervals.size < 5) return

        val avg = keyIntervals.average()
        val variance = keyIntervals.map { (it - avg) * (it - avg) }.average()
        val stdDev = Math.sqrt(variance)
        val cv = if (avg > 0) stdDev / avg else 0.0 // Coefficient of Variation (Rhythm)

        val wpm = ((60000.0 / avg) * 5).toInt()
        val burst = if (avg < 120) 1.0f else (1.0f - (avg / 500f)).coerceIn(0f, 1f)

        val mood = when {
            cv < 0.15 && wpm > 40 -> MoodState.RHYTHMIC
            avg < 100 && burst > 0.8 -> MoodState.INTENSE
            avg < 150 -> MoodState.EXCITED
            cv < 0.3 -> MoodState.FOCUSED
            else -> MoodState.CALM
        }

        _typingDNA.value = TypingDNA(wpm, cv.toFloat(), burst, mood)
    }

    private fun playSynestheticSound(interval: Long) {
        // Map typing speed to pitch and volume
        val mood = _typingDNA.value.mood
        val volume = 0.3f + (_typingDNA.value.burstIntensity * 0.5f)
        val pitch = mood.soundScale * (1.0f + (Math.random() * 0.1))

        soundPool.play(noteIds[noteIndex % 5], volume, volume, 1, 0, pitch)
        noteIndex++
    }

    fun release() {
        soundPool.release()
    }
}
