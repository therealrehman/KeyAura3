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
    private val cardHeightDp = 56f
    private val cardPaddingDp = 6f

    private val backgroundPaint = Paint().apply { color = Color.BLACK }
    private val topBarPaint = Paint().apply { color = Color.parseColor("#0A0A0A") }
    private val titlePaint = Paint().apply {
        color = Color.WHITE; textSize = dp(16f); isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD
    }
    private val cardBgPaint = Paint().apply { color = Color.parseColor("#141414"); isAntiAlias = true }
    private val cardPressedBgPaint = Paint().apply { color = Color.parseColor("#222222"); isAntiAlias = true }
    private val textPreviewPaint = Paint().apply {
        color = Color.WHITE; textSize = dp(13f); isAntiAlias = true
    }
    private val emptyStatePaint = Paint().apply {
        color = Color.parseColor("#666666"); textSize = dp(14f); isAntiAlias = true; textAlign = Paint.Align.CENTER
    }
    private val pinIconPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
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

    // Truncate text for display to avoid performance issues with huge texts
    private fun getDisplayText(entry: ClipboardEntry): String {
        if (entry.type != "text") return ""
        val raw = entry.content
        return if (raw.length > 200) raw.take(197) + "…" else raw
    }

    private val bitmapCache = object : LinkedHashMap<String, Bitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean = size > 24
    }

    init {
        setWillNotDraw(false)
        refresh()
    }

    fun refresh() {
        entries = repository.getAll()
        scrollOffsetY = 0f
        postInvalidateOnAnimation()
    }

    private fun bitmapFor(path: String): Bitmap? {
        bitmapCache[path]?.let { return it }
        return try {
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            val bmp = BitmapFactory.decodeFile(path, opts) ?: return null
            bitmapCache[path] = bmp
            bmp
        } catch (_: Exception) { null }
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

        drawList(canvas, w, h, topBarPx)
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

    private fun drawList(canvas: Canvas, w: Int, h: Int, top: Float) {
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
        val rowStride = cardHeight + padding
        val totalContentHeight = entries.size * rowStride + padding
        val visibleHeight = h - top
        maxScrollOffsetY = max(0f, totalContentHeight - visibleHeight)
        scrollOffsetY = scrollOffsetY.coerceIn(0f, maxScrollOffsetY)

        var y = top + padding - scrollOffsetY
        for (entry in entries) {
            val cardTop = y
            val cardBottom = y + cardHeight
            if (cardBottom >= top && cardTop <= h) {
                val cardRect = Rect(padding.toInt(), cardTop.toInt(), (w - padding).toInt(), cardBottom.toInt())
                val pinSize = dp(28f).toInt()
                val pinRect = Rect(
                    cardRect.right - pinSize - dp(6f).toInt(), cardRect.top + dp(6f).toInt(),
                    cardRect.right - dp(6f).toInt(), cardRect.top + dp(6f).toInt() + pinSize
                )
                cardRects.add(Triple(cardRect, pinRect, entry))
                drawCard(canvas, cardRect, pinRect, entry)
            }
            y += rowStride
        }
        canvas.restore()
    }

    private fun drawCard(canvas: Canvas, cardRect: Rect, pinRect: Rect, entry: ClipboardEntry) {
        val bg = if (pressedEntryId == entry.id) cardPressedBgPaint else cardBgPaint
        canvas.drawRoundRect(cardRect.left.toFloat(), cardRect.top.toFloat(), cardRect.right.toFloat(), cardRect.bottom.toFloat(), dp(10f), dp(10f), bg)

        val contentLeft = cardRect.left + dp(10f)
        val contentRight = pinRect.left - dp(6f)

        if (entry.type == "image") {
            val bmp = bitmapFor(entry.content)
            if (bmp != null) {
                val thumbSize = cardRect.height() - dp(12f).toInt()
                val srcRect = Rect(0, 0, bmp.width, bmp.height)
                val dstRect = Rect(contentLeft.toInt(), cardRect.top + dp(6f).toInt(), contentLeft.toInt() + thumbSize, cardRect.bottom - dp(6f).toInt())
                canvas.drawBitmap(bmp, srcRect, dstRect, null)
            } else {
                canvas.drawText("[image]", contentLeft, cardRect.exactCenterY() + textPreviewPaint.textSize / 3f, textPreviewPaint)
            }
        } else {
            // Use truncated text for display
            val displayText = getDisplayText(entry)
            val available = (contentRight - contentLeft).toInt()
            val truncated = truncateToWidth(displayText.replace("\n", " "), textPreviewPaint, available)
            canvas.drawText(truncated, contentLeft, cardRect.exactCenterY() + textPreviewPaint.textSize / 3f, textPreviewPaint)
        }

        pinIconPaint.color = if (entry.pinned) Color.parseColor("#4488FF") else Color.parseColor("#444444")
        pinIconPaint.style = if (entry.pinned) Paint.Style.FILL else Paint.Style.STROKE
        pinIconPaint.strokeWidth = dp(1.5f)
        canvas.drawCircle(pinRect.exactCenterX(), pinRect.exactCenterY(), pinRect.width() / 2.4f, pinIconPaint)
    }

    private fun truncateToWidth(text: String, paint: Paint, maxWidth: Int): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "\u2026") > maxWidth) end--
        return text.substring(0, end) + "\u2026"
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragStartY = event.y
                dragStartScrollOffset = scrollOffsetY
                isDraggingScroll = false
                if (backButtonRect.contains(event.x.toInt(), event.y.toInt())) { postInvalidateOnAnimation(); return true }
                val hit = cardRects.firstOrNull { it.first.contains(event.x.toInt(), event.y.toInt()) }
                if (hit != null) { pressedEntryId = hit.third.id; postInvalidateOnAnimation() }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - dragStartY
                if (!isDraggingScroll && abs(dy) > dragThreshold) { isDraggingScroll = true; pressedEntryId = null }
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
