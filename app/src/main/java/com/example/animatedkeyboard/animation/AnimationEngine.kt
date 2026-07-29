package com.example.animatedkeyboard.animation

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class AnimationEngine {

    var singleThemeColor: Int? = null
    var themeAnimationsEnabled: Boolean = true

    private val blooms = ArrayList<Bloom>(MAX_BLOOMS)
    private var lastFrameAt = 0L

    private val ringPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }
    private val fillPaint = Paint().apply { isAntiAlias = true }
    private val particlePaint = Paint().apply { isAntiAlias = true }

    private val rainbowCache = HashMap<Int, IntArray>()
    private val singleCache = HashMap<Int, IntArray>()

    private class Particle {
        var x = 0f; var y = 0f
        var vx = 0f; var vy = 0f
        var life = 0f; var maxLife = 1f
        var size = 4f
        var color = Color.WHITE
    }

    private class Bloom {
        var x = 0f; var y = 0f
        var age = 0f
        var maxRadius = 120f
        var colors = IntArray(5)
        val particles = Array(PARTICLE_COUNT) { Particle() }

        fun reset(nx: Float, ny: Float, palette: IntArray, density: Float) {
            x = nx; y = ny; age = 0f
            maxRadius = 130f * density
            System.arraycopy(palette, 0, colors, 0, minOf(palette.size, colors.size))
            for (p in particles) {
                val angle = Random.nextFloat() * (Math.PI * 2).toFloat()
                val speed = (120f + Random.nextFloat() * 260f) * density
                p.x = nx; p.y = ny
                p.vx = cos(angle) * speed
                p.vy = sin(angle) * speed
                p.maxLife = 0.35f + Random.nextFloat() * 0.3f
                p.life = p.maxLife
                p.size = (2.5f + Random.nextFloat() * 4f) * density
                p.color = palette[Random.nextInt(palette.size - 1)]
            }
        }
    }

    fun triggerAnimation(x: Float, y: Float, keyLabel: String) {
        if (!themeAnimationsEnabled) return
        val density = xResourcesDensity
        val base = singleThemeColor
        val palette = if (base != null) gradientFor(base) else rainbowFor(keyLabel)

        val bloom = if (blooms.size < MAX_BLOOMS) {
            Bloom().also { blooms.add(it) }
        } else {
            blooms.removeAt(0).also { blooms.add(it) }
        }
        bloom.reset(x, y, palette, density)
    }

    private var xResourcesDensity = 2.5f

    fun triggerWithDensity(x: Float, y: Float, keyLabel: String, density: Float) {
        xResourcesDensity = density
        triggerAnimation(x, y, keyLabel)
    }

    fun draw(canvas: Canvas): Boolean {
        if (blooms.isEmpty()) return false
        val now = SystemClock.uptimeMillis()
        val dt = if (lastFrameAt == 0L) 0.016f else (now - lastFrameAt) / 1000f
        lastFrameAt = now

        val it = blooms.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.age += dt
            if (b.age >= LIFE) { it.remove(); continue }

            val t = (b.age / LIFE).coerceIn(0f, 1f)
            val ease = 1f - (1f - t) * (1f - t) * (1f - t)
            val radius = b.maxRadius * ease
            val alpha = ((1f - t) * 110).toInt().coerceIn(0, 255)

            if (radius > 1f && alpha > 4) {
                val c0 = withAlpha(b.colors[0], alpha)
                val c1 = withAlpha(b.colors[1], (alpha * 0.55f).toInt())
                fillPaint.shader = RadialGradient(
                    b.x, b.y, radius, c0, c1, Shader.TileMode.CLAMP
                )
                canvas.drawCircle(b.x, b.y, radius, fillPaint)
                fillPaint.shader = null

                ringPaint.color = withAlpha(b.colors[0], (alpha * 0.8f).toInt())
                ringPaint.strokeWidth = (1f - t) * 6f + 1f
                canvas.drawCircle(b.x, b.y, radius * 0.72f, ringPaint)
            }

            for (p in b.particles) {
                if (p.life <= 0f) continue
                p.life -= dt
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.vx *= 0.92f
                p.vy *= 0.92f
                val pa = ((p.life / p.maxLife) * 200).toInt().coerceIn(0, 255)
                if (pa > 4) {
                    particlePaint.color = withAlpha(p.color, pa)
                    canvas.drawCircle(p.x, p.y, p.size * (p.life / p.maxLife), particlePaint)
                }
            }
        }
        if (blooms.isEmpty()) lastFrameAt = 0L
        return blooms.isNotEmpty()
    }

    fun clear() {
        blooms.clear()
        lastFrameAt = 0L
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    private fun rainbowFor(label: String): IntArray {
        val key = label.lowercase().hashCode()
        rainbowCache[key]?.let { return it }
        val hue = ((key % 360) + 360) % 360
        val arr = gradientForHue(hue.toFloat())
        rainbowCache[key] = arr
        return arr
    }

    private fun gradientFor(base: Int): IntArray {
        singleCache[base]?.let { return it }
        val hsv = FloatArray(3)
        Color.colorToHSV(base, hsv)
        val arr = gradientForHue(hsv[0], hsv[1])
        singleCache[base] = arr
        return arr
    }

    private fun gradientForHue(hue: Float, sat: Float = 0.95f): IntArray {
        return intArrayOf(
            Color.HSVToColor(floatArrayOf(hue, (sat * 0.55f).coerceIn(0f, 1f), 1f)),
            Color.HSVToColor(floatArrayOf(hue, sat, 0.95f)),
            Color.HSVToColor(floatArrayOf((hue + 18f) % 360f, sat, 0.75f)),
            Color.HSVToColor(floatArrayOf((hue + 340f) % 360f, sat, 0.55f)),
            Color.TRANSPARENT
        )
    }

    private companion object {
        const val MAX_BLOOMS = 24
        const val PARTICLE_COUNT = 10
        const val LIFE = 0.55f
    }
}
