package com.example.animatedkeyboard.aura

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.sin

/**
 * Draws the living Aurora wave INSIDE the existing Space key rect.
 * KeyboardView.onDraw already runs a continuous invalidate loop, so no
 * extra animator is needed here.
 */
class AuraSpacebarRenderer {
    private val wave = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val wave2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val badge = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.LEFT }
    private val badgeR = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.RIGHT }
    private val path = Path()
    private val path2 = Path()
    private val clip = Path()
    private val rectF = RectF()

    fun draw(canvas: Canvas, space: Rect, dna: AuraDNA, prefs: AuraPrefs, context: Context) {
        if (space.width() <= 0 || space.height() <= 0) return
        val d = context.resources.displayMetrics.density
        val mh = space.width() * 0.05f
        val mv = space.height() * 0.05f
        rectF.set(space.left + mh, space.top + mv, space.right - mh, space.bottom - mv)
        val r = 5f * d

        clip.reset()
        clip.addRoundRect(rectF, r, r, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clip)

        val moodColor = dna.mood.color
        val ctxColor = dnaCategoryColor(prefs)
        val t = System.nanoTime() / 1_000_000_000.0
        val phase = (t * (1.2 + dna.energy * 2.4)).toFloat()
        val cy = rectF.centerY()
        val amp = rectF.height() * (0.10f + dna.energy * 0.30f)
        val freq = 0.045f + dna.variance * 0.03f

        wave.shader = LinearGradient(rectF.left, 0f, rectF.right, 0f,
            Color.argb(30, Color.red(moodColor), Color.green(moodColor), Color.blue(moodColor)),
            Color.argb(190, Color.red(moodColor), Color.green(moodColor), Color.blue(moodColor)),
            Shader.TileMode.CLAMP)
        wave2.shader = LinearGradient(rectF.left, 0f, rectF.right, 0f,
            Color.argb(20, Color.red(ctxColor), Color.green(ctxColor), Color.blue(ctxColor)),
            Color.argb(110, Color.red(ctxColor), Color.green(ctxColor), Color.blue(ctxColor)),
            Shader.TileMode.CLAMP)
        edge.color = moodColor
        edge.alpha = 160
        edge.strokeWidth = 1.6f * d

        path.reset(); path2.reset()
        path.moveTo(rectF.left, rectF.bottom); path2.moveTo(rectF.left, rectF.bottom)
        path.lineTo(rectF.left, cy); path2.lineTo(rectF.left, cy)
        var x = rectF.left
        while (x <= rectF.right) {
            path.lineTo(x, cy + sin(x * freq + phase) * amp)
            path2.lineTo(x, cy + sin(x * freq * 1.7f - phase * 0.8f) * amp * 0.6f + rectF.height() * 0.12f)
            x += 3f
        }
        path.lineTo(rectF.right, rectF.bottom); path.close()
        path2.lineTo(rectF.right, rectF.bottom); path2.close()
        canvas.drawPath(path2, wave2)
        canvas.drawPath(path, wave)
        canvas.drawPath(path, edge)

        // badges: context left, mood + wpm right
        badge.color = Color.argb(210, Color.red(ctxColor), Color.green(ctxColor), Color.blue(ctxColor))
        badge.textSize = 8.5f * d
        badgeR.color = Color.argb(220, 255, 255, 255)
        badgeR.textSize = 8.5f * d
        val by = rectF.bottom - 4f * d
        canvas.drawText(categoryBadge, rectF.left + 6f * d, by, badge)
        canvas.drawText("${dna.mood.label} · ${dna.wpm} WPM", rectF.right - 6f * d, by, badgeR)

        canvas.restore()
    }

    private var dnaCategoryColorCache = Color.CYAN
    private var categoryBadge = "AURA"
    fun setContext(cat: AuraContext.Category) {
        dnaCategoryColorCache = cat.accent
        categoryBadge = cat.badge
    }
    private fun dnaCategoryColor(prefs: AuraPrefs): Int = dnaCategoryColorCache
}
