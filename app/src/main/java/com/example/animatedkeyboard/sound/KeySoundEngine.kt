package com.example.animatedkeyboard.sound

import android.content.Context
import android.media.AudioManager
import android.media.AudioTrack
import android.os.SystemClock
import android.util.Log
import com.example.animatedkeyboard.settings.KeyboardSettings
import java.util.concurrent.Executors
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

class KeySoundEngine(context: Context) {

    private val settings = KeyboardSettings.getInstance(context.applicationContext)
    private val executor = Executors.newSingleThreadExecutor()

    @Volatile private var appVolume: Float = settings.keyVolume
    @Volatile private var builtTuneIndex = -1

    private var clickTrack: AudioTrack? = null
    private val noteTracks = ArrayList<AudioTrack?>()
    private var lastPlayAt = 0L

    init {
        executor.execute {
            try {
                clickTrack = makeStaticTrack(renderClick())
                buildTune(settings.selectedTuneIndex)
            } catch (e: Exception) {
                Log.w("KeySoundEngine", "init failed: ${e.message}")
            }
        }
    }

    fun playClick() {
        if (!settings.soundEnabled) return
        if (!rateLimit()) return
        replay(clickTrack)
    }

    fun playSwipeTone(noteIndex: Int) {
        if (!settings.soundEnabled) return
        if (!rateLimit()) return
        val size = noteTracks.size
        if (size == 0) return
        val idx = ((noteIndex % size) + size) % size
        replay(noteTracks[idx])
    }

    fun refreshTuneIfChanged() {
        val wanted = settings.selectedTuneIndex
        if (wanted != builtTuneIndex) {
            executor.execute { buildTune(wanted) }
        }
    }

    fun refreshVolume() {
        appVolume = settings.keyVolume
    }

    fun release() {
        executor.execute {
            releaseTrack(clickTrack); clickTrack = null
            for (t in noteTracks) releaseTrack(t)
            noteTracks.clear()
        }
        executor.shutdown()
    }

    private fun rateLimit(): Boolean {
        val now = SystemClock.uptimeMillis()
        if (now - lastPlayAt < 32L) return false
        lastPlayAt = now
        return true
    }

    private fun replay(track: AudioTrack?) {
        track ?: return
        try {
            @Suppress("DEPRECATION")
            track.setVolume(appVolume)
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.pause()
            track.setPlaybackHeadPosition(0)
            track.play()
        } catch (e: Exception) {
            Log.w("KeySoundEngine", "Replay failed: ${e.message}")
        }
    }

    private fun releaseTrack(track: AudioTrack?) {
        track ?: return
        try { track.pause() } catch (_: Exception) {}
        try { track.release() } catch (_: Exception) {}
    }

    private fun buildTune(index: Int) {
        for (t in noteTracks) releaseTrack(t)
        noteTracks.clear()
        val scale = SCALES[index.coerceIn(SCALES.indices)]
        for (semi in scale.semitones) {
            val freq = scale.baseFreq * 2.0.pow(semi / 12.0)
            noteTracks.add(makeStaticTrack(renderTone(freq.toFloat(), scale.brightness)))
        }
        builtTuneIndex = index
    }

    private fun renderClick(): ShortArray {
        val n = (SAMPLE_RATE * 0.014).toInt()
        val out = ShortArray(n)
        var noise = 0
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 300.0)
            noise = (noise * 3 + (Math.random() * 65535 - 32768).toInt()) / 4
            val blip = sin(2 * Math.PI * 2200.0 * t) * 0.45
            val v = (noise / 32768.0 * 0.55 + blip) * env
            out[i] = (v * 32767 * 0.8).toInt().toShort()
        }
        return out
    }

    private fun renderTone(freq: Float, brightness: Double): ShortArray {
        val n = (SAMPLE_RATE * 0.22).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 14.0) * (1.0 - exp(-t * 400.0))
            val fundamental = sin(2 * Math.PI * freq * t)
            val harmonic = sin(2 * Math.PI * freq * 2.0 * t) * brightness
            val v = (fundamental * 0.75 + harmonic) * env
            out[i] = (v * 32767 * 0.85).toInt().toShort()
        }
        return out
    }

    @Suppress("DEPRECATION")
    private fun makeStaticTrack(pcm: ShortArray): AudioTrack? {
        return try {
            val bytes = pcm.size * 2
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, android.media.AudioFormat.CHANNEL_OUT_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT
            )
            val track = AudioTrack(
                AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                android.media.AudioFormat.CHANNEL_OUT_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                maxOf(bytes, minBuf), AudioTrack.MODE_STATIC
            )
            track.write(pcm, 0, pcm.size)
            track
        } catch (e: Exception) {
            Log.w("KeySoundEngine", "Track create failed: ${e.message}")
            null
        }
    }

    private data class Scale(val baseFreq: Double, val semitones: IntArray, val brightness: Double)

    private companion object {
        const val SAMPLE_RATE = 22050

        val SCALES = listOf(
            Scale(261.63, intArrayOf(0, 2, 4, 7, 9, 12, 14, 16, 19, 21), 0.30),
            Scale(220.00, intArrayOf(0, 3, 5, 7, 10, 12, 15, 17, 19, 22), 0.25),
            Scale(246.94, intArrayOf(0, 2, 3, 7, 9, 12, 14, 15, 19, 21), 0.35),
            Scale(293.66, intArrayOf(0, 2, 4, 6, 7, 11, 12, 14, 16, 18), 0.40),
            Scale(329.63, intArrayOf(0, 2, 3, 7, 8, 12, 14, 15, 19, 20), 0.30),
            Scale(233.08, intArrayOf(0, 1, 4, 5, 7, 8, 11, 12, 13, 16), 0.35),
            Scale(207.65, intArrayOf(0, 2, 4, 6, 8, 10, 12, 14, 16, 18), 0.45),
            Scale(196.00, intArrayOf(0, 3, 5, 6, 7, 10, 12, 15, 17, 18), 0.30),
            Scale(523.25, intArrayOf(0, 2, 4, 5, 7, 9, 11, 12, 14, 16), 0.50),
            Scale(130.81, intArrayOf(0, 2, 3, 5, 7, 8, 10, 12, 14, 15), 0.20)
        )
    }
}
