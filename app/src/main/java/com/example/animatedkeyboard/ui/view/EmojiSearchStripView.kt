package com.example.animatedkeyboard.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.animatedkeyboard.emoji.EmojiEntry
import com.example.animatedkeyboard.emoji.EmojiRepository
import kotlin.math.abs
import kotlin.math.max

class EmojiSearchStripView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onEmojiTapped: ((EmojiEntry) -> Unit)? = null
    var onClose: (() -> Unit)? = null

    private val repository by lazy { EmojiRepository.getInstance(context) }
    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private var query = ""
    private var results: List<EmojiEntry> = emptyList()
    private var scrollX = 0f
    private var maxScrollX = 0f

    private val cellRects = mutableListOf<Pair<Rect, EmojiEntry>>()
    private var closeRect = Rect()
    private var resultsLeft = 0f

    private var downX = 0f
    private var startScrollX = 0f
    private var dragging = false
    private var pressed: EmojiEntry? = null

    private val bgPaint = Paint().apply { color = Color.parseColor("#0A0A10") }
    private val dividerPaint = Paint().apply { color = Color.parseColor("#1C1D26") }
    private val queryPaint = Paint().apply {
        color = Color.parseColor("#9AA0B0"); textSize = dp(13f); isAntiAlias = true
    }
    private val emojiPaint = Paint().apply {
        textSize = dp(22f); textAlign = Paint.Align.CENTER; isAntiAlias = true
    }
    private val pressedPaint = Paint().apply { color = Color.parseColor("#23242E"); isAntiAlias = true }
    private val closePaint = Paint().apply {
        color = Color.parseColor("#9AA0B0"); style = Paint.Style.STROKE
        strokeWidth = dp(2f); strokeCap = Paint.Cap.ROUND; isAntiAlias = true
    }
    private val hintPaint = Paint().apply {
        color = Color.parseColor("#555A6A"); textSize = dp(12f); isAntiAlias = true
    }

    init { setWillNotDraw(false) }

    fun setQuery(q: String) {
        query = q
        results = if (q.isBlank()) repository.recents().take(24)
        else repository.search(q).take(48)
        scrollX = 0f
        postInvalidateOnAnimation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val btn = dp(38f).toInt()
        closeRect = Rect(w - btn, 0, w, h)
        resultsLeft = dp(120f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        canvas.drawRect(0f, h - 1f, w, h, dividerPaint)

        val displayQuery = if (query.isEmpty()) "Search emoji…" else "🔍 $query"
        var q = displayQuery
        val maxQW = resultsLeft - dp(20f)
        while (q.length > 3 && queryPaint.measureText(q) > maxQW) q = q.dropLast(1)
        if (q != displayQuery) q = q.dropLast(1) + "…"
        canvas.drawText(q, dp(10f), h / 2f + queryPaint.textSize / 3f, queryPaint)

        val cx = closeRect.exactCenterX(); val cy = closeRect.exactCenterY(); val s = dp(6f)
        canvas.drawLine(cx - s, cy - s, cx + s, cy + s, closePaint)
        canvas.drawLine(cx + s, cy - s, cx - s, cy + s, closePaint)

        cellRects.clear()
        val cellSize = dp(40f)
        val right = closeRect.left.toFloat()
        canvas.save()
        canvas.clipRect(resultsLeft, 0f, right, h)

        if (results.isEmpty()) {
            val msg = if (query.isBlank()) "Recent emojis" else "No emoji for \"$query\""
            canvas.drawText(msg, resultsLeft + dp(8f), h / 2f + hintPaint.textSize / 3f, hintPaint)
        } else {
            val totalW = results.size * cellSize
            maxScrollX = max(0f, totalW - (right - resultsLeft))
            scrollX = scrollX.coerceIn(0f, maxScrollX)
            for ((i, entry) in results.withIndex()) {
                val left = resultsLeft + i * cellSize - scrollX
                if (left + cellSize < resultsLeft || left > right) continue
                val rect = Rect(left.toInt(), dp(4f).toInt(), (left + cellSize).toInt(), (h - dp(4f)).toInt())
                cellRects.add(rect to entry)
                if (pressed == entry) {
                    canvas.drawRoundRect(rect.left.toFloat(), rect.top.toFloat(),
                        rect.right.toFloat(), rect.bottom.toFloat(), dp(8f), dp(8f), pressedPaint)
                }
                canvas.drawText(entry.character, rect.exactCenterX(),
                    rect.exactCenterY() + emojiPaint.textSize / 3f, emojiPaint)
            }
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                startScrollX = scrollX
                dragging = false
                pressed = cellRects.firstOrNull { it.first.contains(event.x.toInt(), event.y.toInt()) }?.second
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                if (!dragging && abs(dx) > dp(8f)) { dragging = true; pressed = null }
                if (dragging) {
                    scrollX = (startScrollX - dx).coerceIn(0f, maxScrollX)
                    postInvalidateOnAnimation()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasDragging = dragging
                dragging = false
                if (!wasDragging) {
                    if (closeRect.contains(event.x.toInt(), event.y.toInt())) {
                        onClose?.invoke()
                    } else if (pressed != null &&
                        cellRects.firstOrNull { it.second == pressed }?.first
                            ?.contains(event.x.toInt(), event.y.toInt()) == true
                    ) {
                        onEmojiTapped?.invoke(pressed!!)
                    }
                }
                pressed = null
                postInvalidateOnAnimation()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
