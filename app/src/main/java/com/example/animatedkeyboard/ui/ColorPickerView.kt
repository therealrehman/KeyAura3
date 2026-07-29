package com.example.animatedkeyboard.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class ColorPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onColorChanged: ((Int) -> Unit)? = null

    private var hue = 20f
    private var sat = 1f
    private var value = 1f

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val svPaint = Paint()
    private val svOverlayPaint = Paint()
    private val huePaint = Paint()
    private val indicatorPaint = Paint().apply {
        style = Paint.Style.STROKE; strokeWidth = dp(2.5f); color = Color.WHITE; isAntiAlias = true
    }
    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE; strokeWidth = dp(1f); color = Color.parseColor("#333340"); isAntiAlias = true
    }

    private val hueBarHeight get() = dp(28f)
    private val hueBarTopMargin get() = dp(14f)

    fun setColor(color: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hue = hsv[0]; sat = hsv[1]; value = hsv[2]
        invalidate()
    }

    fun getColor(): Int = Color.HSVToColor(floatArrayOf(hue, sat, value))

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val desired = (w * 0.72f + hueBarHeight + hueBarTopMargin + dp(8f)).toInt()
        setMeasuredDimension(w, desired)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val svHeight = height - hueBarHeight - hueBarTopMargin - dp(8f)

        val pureHue = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        svPaint.shader = LinearGradient(0f, 0f, w, 0f, Color.WHITE, pureHue, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, svHeight, svPaint)
        svOverlayPaint.shader = LinearGradient(0f, 0f, 0f, svHeight, Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, svHeight, svOverlayPaint)
        canvas.drawRect(0f, 0f, w, svHeight, borderPaint)

        val ix = sat * w
        val iy = (1f - value) * svHeight
        canvas.drawCircle(ix, iy, dp(9f), indicatorPaint)

        val hueTop = svHeight + hueBarTopMargin
        val hueColors = IntArray(7) { i -> Color.HSVToColor(floatArrayOf(i * 60f, 1f, 1f)) }
        huePaint.shader = LinearGradient(0f, hueTop, w, hueTop, hueColors, null, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(0f, hueTop, w, hueTop + hueBarHeight, dp(8f), dp(8f), huePaint)
        canvas.drawRoundRect(0f, hueTop, w, hueTop + hueBarHeight, dp(8f), dp(8f), borderPaint)

        val hx = (hue / 360f) * w
        canvas.drawCircle(hx, hueTop + hueBarHeight / 2f, dp(10f), indicatorPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.toFloat()
        val svHeight = height - hueBarHeight - hueBarTopMargin - dp(8f)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (event.y < svHeight) {
                    sat = (event.x / w).coerceIn(0f, 1f)
                    value = (1f - event.y / svHeight).coerceIn(0f, 1f)
                } else {
                    hue = ((event.x / w) * 360f).coerceIn(0f, 359.9f)
                }
                onColorChanged?.invoke(getColor())
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
