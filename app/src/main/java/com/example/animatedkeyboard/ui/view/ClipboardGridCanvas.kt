package com.example.animatedkeyboard.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.animatedkeyboard.clipboard.ClipboardEntry
import com.example.animatedkeyboard.clipboard.ClipboardRepository
import kotlin.math.abs
import kotlin.math.max

class ClipboardGridCanvas @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onClipTapped: ((ClipboardEntry) -> Unit)? = null
    var onPinTapped: ((ClipboardEntry) -> Unit)? = null
    var onBackTapped: (() -> Unit)? = null

    private val repository by lazy { ClipboardRepository.getInstance(context) }
    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val topBarHeightDp = 40f
    private val cardHeightDp = 84f
    private val cardPaddingDp = 6f
    private val columns = 2

    private val backgroundPaint = Paint().apply { color = Color.BLACK }
    private val topBarPaint = Paint().apply { color = Color.parseColor("#0A0A0A") }
    private val titlePaint = Paint().apply {
        color = Color.WHITE; textSize = dp(16f); isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD
    }
    private val cardBgPaint = Paint().apply { color = Color.parseColor("#141419"); isAntiAlias = true }
    private val cardPressedBgPaint = Paint().apply { color = Color.parseColor("#232330"); isAntiAlias = true }
    private val cardBorderPaint = Paint().apply {
        color = Color.parseColor("#23232E"); style = Paint.Style.STROKE
        strokeWidth = dp(1f); isAntiAlias = true
    }
    private val textPreviewPaint = Paint().apply {
        color = Color.parseColor("#E8E8EE"); textSize = dp(12f); isAntiAlias = true
    }
    private val timePaint = Paint().apply {
        color = Color.parseColor("#5A5F70"); textSize = dp(10f); isAntiAlias = true
    }
    private val emptyStatePaint = Paint().apply {
        color = Color.parseColor("#666666"); textSize = dp(14f); isAntiAlias = true; textAlign = Paint.Align.CENTER
    }
    private val pinIconPaint = Paint().apply { isAntiAlias = true }
    private val backIconPaint = Paint().apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = dp(2f)
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; isAntiAlias = true
    }
    private val iconButtonPaint = Paint().apply { color = Color.parseColor("#141414"); isAntiAlias = true }

    private var entries: List<ClipboardEntry> = emptyList()
    private var scrollOffsetY = 0f
    private var maxScrollOffsetY = 0f

    private val cardRects = mutableListOf<Triple<Rect, Rect, ClipboardEntry>>()
    private var backButtonRect = Rect()

    private var pressedEntryId: String? = null
    private var isDraggingScroll = false
    private var dragStartY = 0f
    private var dragStartScrollOffset = 0f
    private val dragThreshold = dp(8f)

    private val bitmapCache = object : LinkedHashMap<String, Bitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean = size > 24
    }

    init {
        setWillNotDraw(false)
        refresh()
    }

    fun refresh() {
        entries = repository.getAll()
        postInvalidateOnAnimation()
    }

    private fun bitmapFor(path: String): Bitmap? {
        bitmapCache[path]?.let { return it }
        return try {
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            val bmp = BitmapFactory.decodeFile(path, opts) ?: return null
            bitmapCache[path] = bmp
            bmp
        } catch (e: Exception) {
            null
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val btnSize = dp(32f).toInt()
        val margin = dp(4f).toInt()
        backButtonRect = Rect(w - btnSize - margin, margin, w - margin, margin + btnSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width
        val h = height
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), backgroundPaint)

        val topBarPx = dp(topBarHeightDp)
        canvas.drawRect(0f, 0f, w.toFloat(), topBarPx, topBarPaint)
        canvas.drawText("Clipboard", dp(14f), topBarPx / 2f + titlePaint.textSize / 3f, titlePaint)
        drawBackButton(canvas)

        drawGrid(canvas, w, h, topBarPx)
    }

    private fun drawBackButton(canvas: Canvas) {
        canvas.drawRoundRect(
            backButtonRect.left.toFloat(), backButtonRect.top.toFloat(),
            backButtonRect.right.toFloat(), backButtonRect.bottom.toFloat(),
            dp(8f), dp(8f), iconButtonPaint
        )
        val cx = backButtonRect.exactCenterX(); val cy = backButtonRect.exactCenterY(); val s = dp(6f)
        canvas.drawLine(cx + s, cy - s, cx - s, cy, backIconPaint)
        canvas.drawLine(cx - s, cy, cx + s, cy + s, backIconPaint)
    }

    private fun drawGrid(canvas: Canvas, w: Int, h: Int, top: Float) {
        cardRects.clear()
        canvas.save()
        canvas.clipRect(0f, top, w.toFloat(), h.toFloat())

        if (entries.isEmpty()) {
            canvas.drawText("Copied text and images will show up here", w / 2f, top + dp(50f), emptyStatePaint)
            canvas.restore()
            maxScrollOffsetY = 0f
            return
        }

        val padding = dp(cardPaddingDp)
        val cardHeight = dp(cardHeightDp)
        val cardWidth = (w - padding * (columns + 1)) / columns
        val rowStride = cardHeight + padding
        val totalRows = (entries.size + columns - 1) / columns
        val totalContentHeight = totalRows * rowStride + padding
        val visibleHeight = h - top
        maxScrollOffsetY = max(0f, totalContentHeight - visibleHeight)
        scrollOffsetY = scrollOffsetY.coerceIn(0f, maxScrollOffsetY)

        for ((index, entry) in entries.withIndex()) {
            val row = index / columns
            val col = index % columns
            val left = padding + col * (cardWidth + padding)
            val cardTop = top + padding + row * rowStride - scrollOffsetY
            val cardBottom = cardTop + cardHeight
            if (cardBottom < top || cardTop > h) continue

            val cardRect = Rect(left.toInt(), cardTop.toInt(), (left + cardWidth).toInt(), cardBottom.toInt())
            val pinSize = dp(22f).toInt()
            val pinRect = Rect(
                cardRect.right - pinSize - dp(4f).toInt(), cardRect.top + dp(4f).toInt(),
                cardRect.right - dp(4f).toInt(), cardRect.top + dp(4f).toInt() + pinSize
            )
            cardRects.add(Triple(cardRect, pinRect, entry))
            drawCard(canvas, cardRect, pinRect, entry)
        }
        canvas.restore()
    }

    private fun drawCard(canvas: Canvas, cardRect: Rect, pinRect: Rect, entry: ClipboardEntry) {
        val bg = if (pressedEntryId == entry.id) cardPressedBgPaint else cardBgPaint
        val radius = dp(12f)
        canvas.drawRoundRect(cardRect.left.toFloat(), cardRect.top.toFloat(),
            cardRect.right.toFloat(), cardRect.bottom.toFloat(), radius, radius, bg)
        canvas.drawRoundRect(cardRect.left.toFloat(), cardRect.top.toFloat(),
            cardRect.right.toFloat(), cardRect.bottom.toFloat(), radius, radius, cardBorderPaint)

        val contentLeft = cardRect.left + dp(10f)
        val contentRight = cardRect.right - dp(10f)

        if (entry.type == "image") {
            val bmp = bitmapFor(entry.content)
            if (bmp != null) {
                val thumbH = cardRect.height() - dp(26f).toInt()
                val thumbW = thumbH * bmp.width / max(1, bmp.height)
                val dst = Rect(contentLeft.toInt(), cardRect.top + dp(8f).toInt(),
                    contentLeft.toInt() + minOf(thumbW, cardRect.width() / 2), cardRect.top + dp(8f).toInt() + thumbH)
                canvas.drawBitmap(bmp, Rect(0, 0, bmp.width, bmp.height), dst, null)
            } else {
                canvas.drawText("🖼 image", contentLeft, cardRect.exactCenterY(), textPreviewPaint)
            }
        } else {
            // FIX: preview sirf 3 lines / ~90 chars — poora text memory me hai, draw nahi hota
            val preview = entry.content.replace(Regex("\\s+"), " ").trim().take(90)
            drawWrappedText(canvas, preview, contentLeft, cardRect.top + dp(16f),
                contentRight, dp(14f), 3, textPreviewPaint)
        }

        // Relative time
        canvas.drawText(relativeTime(entry.timestamp), contentLeft,
            cardRect.bottom - dp(6f), timePaint)

        // Pin dot
        pinIconPaint.color = if (entry.pinned) Color.parseColor("#4488FF") else Color.parseColor("#3A3A44")
        pinIconPaint.style = if (entry.pinned) Paint.Style.FILL else Paint.Style.STROKE
        pinIconPaint.strokeWidth = dp(1.5f)
        canvas.drawCircle(pinRect.exactCenterX(), pinRect.exactCenterY(), pinRect.width() / 2.6f, pinIconPaint)
    }

    private fun drawWrappedText(
        canvas: Canvas, text: String, left: Float, top: Float,
        right: Float, lineHeight: Float, maxLines: Int, paint: Paint
    ) {
        var remaining = text
        var y = top + paint.textSize
        var line = 0
        while (remaining.isNotEmpty() && line < maxLines) {
            val count = paint.breakText(remaining, true, right - left, null)
            if (count <= 0) break
            var part = remaining.take(count)
            if (line == maxLines - 1 && count < remaining.length) {
                while (part.isNotEmpty() && paint.measureText("$part…") > right - left) {
                    part = part.dropLast(1)
                }
                part += "…"
            }
            canvas.drawText(part, left, y, paint)
            remaining = remaining.drop(count).trimStart()
            y += lineHeight
            line++
        }
    }

    private fun relativeTime(ts: Long): String {
        val diff = System.currentTimeMillis() - ts
        val min = diff / 60000
        val hr = min / 60
        val day = hr / 24
        return when {
            min < 1 -> "just now"
            min < 60 -> "${min}m ago"
            hr < 24 -> "${hr}h ago"
            else -> "${day}d ago"
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragStartY = event.y
                dragStartScrollOffset = scrollOffsetY
                isDraggingScroll = false
                if (backButtonRect.contains(event.x.toInt(), event.y.toInt())) {
                    postInvalidateOnAnimation(); return true
                }
                val hit = cardRects.firstOrNull { it.first.contains(event.x.toInt(), event.y.toInt()) }
                if (hit != null) { pressedEntryId = hit.third.id; postInvalidateOnAnimation() }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - dragStartY
                if (!isDraggingScroll && abs(dy) > dragThreshold) {
                    isDraggingScroll = true; pressedEntryId = null
                }
                if (isDraggingScroll) {
                    scrollOffsetY = (dragStartScrollOffset - dy).coerceIn(0f, maxScrollOffsetY)
                    postInvalidateOnAnimation()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasDragging = isDraggingScroll
                isDraggingScroll = false
                if (!wasDragging) {
                    if (backButtonRect.contains(event.x.toInt(), event.y.toInt())) {
                        onBackTapped?.invoke()
                    } else {
                        val hit = cardRects.firstOrNull { it.first.contains(event.x.toInt(), event.y.toInt()) }
                        if (hit != null) {
                            val (_, pinRect, entry) = hit
                            if (pinRect.contains(event.x.toInt(), event.y.toInt())) {
                                onPinTapped?.invoke(entry)
                            } else {
                                onClipTapped?.invoke(entry)
                            }
                        }
                    }
                }
                pressedEntryId = null
                postInvalidateOnAnimation()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
