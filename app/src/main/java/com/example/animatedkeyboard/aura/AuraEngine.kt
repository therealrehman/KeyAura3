package com.example.animatedkeyboard.aura

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import java.util.LinkedList
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class AuraMood(val label: String, val color: Int, val pitch: Float) {
    CALM("CALM", 0xFF00E5FF.toInt(), 1.0f),
    FOCUSED("FOCUSED", 0xFF7C4DFF.toInt(), 1.12f),
    RHYTHMIC("RHYTHMIC", 0xFF00E676.toInt(), 1.06f),
    EXCITED("EXCITED", 0xFFFF4081.toInt(), 1.25f),
    INTENSE("INTENSE", 0xFFFF6D00.toInt(), 1.4f)
}

data class AuraDNA(
    val wpm: Int = 0,
    val variance: Float = 0f,
    val burst: Float = 0f,
    val energy: Float = 0f,
    val mood: AuraMood = AuraMood.CALM,
    val keys: Long = 0L
)

/**
 * Typing DNA + mood + procedural pentatonic soundscape.
 * No audio assets required: notes are synthesized into AudioTrack static buffers.
 * No coroutines required: decay runs on a main-looper Handler.
 */
class AuraEngine(
    context: Context,
    private val prefs: AuraPrefs,
    private val soundAllowed: () -> Boolean
) {
    interface Listener { fun onDNA(dna: AuraDNA) }

    private val listeners = mutableListOf<Listener>()
    fun addListener(l: Listener) { listeners.add(l) }
    fun removeListener(l: Listener) { listeners.remove(l) }

    @Volatile var dna: AuraDNA = AuraDNA()
        private set

    var category: AuraContext.Category = AuraContext.Category.GENERAL

    private val intervals = LinkedList<Long>()
    private var lastKeyAt = 0L
    private var noteIndex = 0
    private val handler = Handler(Looper.getMainLooper())
    private val appContext = context.applicationContext

    // ---- procedural pentatonic synth (C D E G A) ----
    private val sampleRate = 22050
    private val freqs = doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00)
    private val tracks = arrayOfNulls<AudioTrack>(freqs.size)

    init {
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            for (i in freqs.indices) {
                val buf = synthNote(freqs[i])
                val t = AudioTrack.Builder()
                    .setAudioAttributes(attrs)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buf.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                t.write(buf, 0, buf.size)
                tracks[i] = t
            }
        } catch (_: Exception) { /* audio unavailable: silent fallback */ }
    }

    private fun synthNote(freq: Double): ByteArray {
        val n = (sampleRate * 0.22).toInt()
        val out = ByteArray(n * 2)
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val env = exp(-3.2 * t) * (1 - exp(-400 * t))
            val v = (sin(2 * PI * freq * t) * 0.6 +
                     sin(2 * PI * freq * 2 * t) * 0.18 +
                     sin(2 * PI * freq * 3 * t) * 0.06) * env
            val s = (v * 32767 * 0.5).toInt().coerceIn(-32768, 32767)
            out[i * 2] = (s and 0xFF).toByte()
            out[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
        }
        return out
    }

    fun onKey(label: String) {
        val now = System.currentTimeMillis()
        val isChar = label.length == 1
        if (lastKeyAt > 0 && isChar) {
            intervals.addLast(now - lastKeyAt)
            if (intervals.size > 40) intervals.removeFirst()
        }
        lastKeyAt = now
        analyze(isChar)
        if (isChar) playNote()
        scheduleDecay()
        notifyListeners()
    }

    private fun analyze(countKey: Boolean) {
        val keys = dna.keys + if (countKey) 1 else 0
        if (intervals.size < 4) {
            dna = dna.copy(keys = keys, energy = (dna.energy + 0.08f).coerceAtMost(1f))
            return
        }
        val avg = intervals.average()
        val varr = intervals.map { (it - avg) * (it - avg) }.average()
        val cv = if (avg > 0) kotlin.math.sqrt(varr) / avg else 0.0
        val wpm = ((60000.0 / avg) * 5).toInt().coerceIn(0, 220)
        val burst = (1.0 - (avg / 420.0)).coerceIn(0.0, 1.0).toFloat()
        val energy = burst
        val mood = when {
            avg < 105 && burst > 0.75f -> AuraMood.INTENSE
            avg < 160 -> AuraMood.EXCITED
            cv < 0.16 && avg in 150.0..320.0 -> AuraMood.RHYTHMIC
            cv < 0.38 -> AuraMood.FOCUSED
            else -> AuraMood.CALM
        }
        dna = AuraDNA(wpm, cv.toFloat(), burst, energy, mood, keys)
    }

    private fun playNote() {
        if (!prefs.auraSound || prefs.flowMode || !soundAllowed()) return
        val t = tracks[noteIndex % tracks.size] ?: return
        noteIndex++
        try {
            val vol = 0.25f + dna.burst * 0.45f
            t.setVolume(vol)
            t.playbackRate = (sampleRate * dna.mood.pitch).toInt()
            t.stop()
            t.reloadStaticData()
            t.play()
        } catch (_: Exception) {}
    }

    private val decay = object : Runnable {
        override fun run() {
            if (dna.energy > 0.01f) {
                dna = dna.copy(energy = (dna.energy - 0.04f).coerceAtLeast(0f))
                notifyListeners()
                handler.postDelayed(this, 120)
            }
        }
    }

    private fun scheduleDecay() {
        handler.removeCallbacks(decay)
        handler.postDelayed(decay, 350)
    }

    private fun notifyListeners() { listeners.forEach { it.onDNA(dna) } }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        tracks.forEach { try { it?.release() } catch (_: Exception) {} }
        listeners.clear()
    }
}
