package com.example.animatedkeyboard.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.animatedkeyboard.theme.AnimationTheme
import kotlin.math.pow
import kotlin.random.Random

class AnimationEngine {
    private val activeAnimations = mutableListOf<GradientAnimation>()
    private val random = Random(System.currentTimeMillis())
    private var currentTheme: AnimationTheme = AnimationTheme.MASTER

    fun setTheme(theme: AnimationTheme) {
        currentTheme = theme
    }

    fun triggerAnimation(x: Float, y: Float, keyLabel: String) {
        val colors = getGradientColorsForKey(keyLabel)
        activeAnimations.add(GradientAnimation(x, y, colors))
    }

    fun update(elapsedTimeMs: Long) {
        activeAnimations.removeAll { anim ->
            anim.update(elapsedTimeMs)
            anim.isFinished
        }
    }

    fun draw(canvas: Canvas) {
        for (animation in activeAnimations) {
            animation.draw(canvas)
        }
    }

    fun hasActiveAnimations(): Boolean {
        return activeAnimations.isNotEmpty()
    }

    private fun getGradientColorsForKey(key: String): IntArray {
        val themeColors = currentTheme.colors
        val selected = mutableListOf<Int>()
        // Pick 4 random colors from the theme palette
        repeat(4) {
            selected.add(themeColors[random.nextInt(themeColors.size)])
        }
        // Add transparent at the end
        selected.add(Color.TRANSPARENT)
        return selected.toIntArray()
    }

    private class GradientAnimation(
        private val centerX: Float,
        private val centerY: Float,
        private val colors: IntArray
    ) {
        var radius = 0f
            private set
        var isFinished = false
            private set

        private val maxRadius = 800f
        private val durationMs = 800L
        private var startTime = System.currentTimeMillis()

        fun update(elapsedTimeMs: Long): Boolean {
            val progress = (System.currentTimeMillis() - startTime).toFloat() / durationMs.toFloat()
            if (progress >= 1.0f) {
                isFinished = true
                return false
            }
            radius = maxRadius * (1 - (1 - progress).toDouble().pow(2.0)).toFloat()
            return true
        }

        fun draw(canvas: Canvas) {
            if (radius <= 0) return
            val paint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(
                    centerX, centerY, radius,
                    colors,
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(centerX, centerY, radius, paint)
        }
    }
}
