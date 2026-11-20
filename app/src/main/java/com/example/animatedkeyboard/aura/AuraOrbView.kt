package com.example.animatedkeyboard.aura

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class AuraOrbView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, def: Int = 0
) : View(context, attrs, def) {

    private val orb = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }
    var dna: AuraDNA = AuraDNA()
        set(v) { field = v; postInvalidateOnAnimation() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f; val cy = height / 2f
        val base = minOf(width, height) / 3.2f
        val t = System.nanoTime() / 1_000_000_000.0
        val pulse = 1f + dna.energy * 0.12f * sin(t * 3).toFloat()
        val col = dna.mood.color
        orb.shader = RadialGradient(cx - base * 0.3f, cy - base * 0.3f, base * pulse,
            lighten(col), col, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, base * pulse, orb)
        ring.color = col; ring.alpha = 120; ring.strokeWidth = 3f
        canvas.drawCircle(cx, cy, base * 1.35f, ring)
        dot.color = col
        for (i in 0 until 10) {
            val a = t * (0.6 + dna.energy * 2) + i * (Math.PI / 5)
            val rr = base * 1.35f
            dot.alpha = 90 + (dna.energy * 150).toInt()
            canvas.drawCircle(cx + cos(a).toFloat() * rr, cy + sin(a).toFloat() * rr,
                3f + dna.energy * 4f, dot)
        }
        txt.textSize = base * 0.34f
        canvas.drawText(dna.mood.label, cx, cy + txt.textSize / 3f, txt)
        postInvalidateOnAnimation()
    }

    private fun lighten(c: Int): Int = Color.rgb(
        (Color.red(c) + 60).coerceAtMost(255),
        (Color.green(c) + 60).coerceAtMost(255),
        (Color.blue(c) + 60).coerceAtMost(255))
}
