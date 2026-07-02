package com.example.animatedkeyboard.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.animatedkeyboard.emoji.EmojiEntry
import com.example.animatedkeyboard.emoji.EmojiRepository
import kotlin.math.abs
import kotlin.math.max

/**
 * The category tabs + scrollable emoji grid beneath the search bar.
 * Kept as a plain canvas view (rather than RecyclerView) to match the rest
 * of the app's drawing style and avoid pulling in a new dependency for what
 * is, at ~630 emoji, a small enough dataset to lay out directly.
 */
class EmojiGridCanvas @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onEmojiTapped: ((EmojiEntry) -> Unit)? = null
    var onBackTapped: (() -> Unit)? = null

    private val repository by lazy { EmojiRepository.getInstance(context) }
    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    // --- Layout constants -------------------------------------------------
    private val topBarHeightDp = 44f     // shares vertical space with the search EditText above us
    private val tabBarHeightDp = 40f
    private val cellSizeDp = 44f
    private val gridPaddingDp = 6f

    private val backgroundPaint = Paint().apply { color = Color.BLACK }
    private val topBarPaint = Paint().apply { color = Color.parseColor("#0A0A0A") }
    private val tabBarPaint = Paint().apply { color = Color.parseColor("#101010") }
    private val tabIconPaint = Paint().apply { textSize = dp(18f); textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val tabActiveIndicatorPaint = Paint().apply { color = Color.parseColor("#4488FF"); isAntiAlias = true }
    private val emojiPaint = Paint().apply { textSize = dp(24f); textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val emojiPressedBgPaint = Paint().apply { color = Color.parseColor("#1E1E1E"); isAntiAlias = true }
    private val labelPaint = Paint().apply {
        color = Color.parseColor("#666666"); textSize = dp(12f); isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.LEFT
    }
    private val emptyStatePaint = Paint().apply {
        color = Color.parseColor("#666666"); textSize = dp(14f); isAntiAlias = true; textAlign = Paint.Align.CENTER
    }
    private val backButtonPaint = Paint().apply { color = Color.parseColor("#141414"); isAntiAlias = true }
    private val backIconPaint = Paint().apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = dp(2f)
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; isAntiAlias = true
    }

    // "Recently Used" is a synthetic first tab; category tabs follow.
    private val recentsLabel = "Recently Used"
    private fun tabGroups(): List<String> = listOf(recentsLabel) + repository.groups

    private fun tabGlyph(group: String): String = when (group) {
        recentsLabel -> "\uD83D\uDD52"             // 🕒
        "Smileys & Emotion" -> "\uD83D\uDE00"       // 😀
        "People & Body" -> "\uD83D\uDC4B"           // 👋
        "Animals & Nature" -> "\uD83D\uDC3B"        // 🐻
        "Food & Drink" -> "\uD83C\uDF54"            // 🍔
        "Travel & Places" -> "\u2708\uFE0F"         // ✈️
        "Activities" -> "\u26BD"                    // ⚽
        "Objects" -> "\uD83D\uDCA1"                 // 💡
        "Symbols" -> "#\uFE0F\u20E3"                // #️⃣
        "Flags" -> "\uD83C\uDFC1"                   // 🏁
        else -> "\u2022"
    }

    private var activeGroup: String = recentsLabel
    private var searchQuery: String = ""
    private var currentItems: List<EmojiEntry> = emptyList()
    private var scrollOffsetY = 0f
    private var maxScrollOffsetY = 0f

    private val cellRects = mutableListOf<Pair<Rect, EmojiEntry>>()
    private val tabRects = mutableListOf<Pair<Rect, String>>()
    private var backButtonRect = Rect()

    private var pressedCellIndex = -1
    private var pressedTab: String? = null
    private var isDraggingScroll = false
    private var dragStartY = 0f
    private var dragStartScrollOffset = 0f
    private val dragThreshold = dp(8f)

    init {
        setWillNotDraw(false)
        refreshItemsForActiveGroup()
    }

    fun resetToDefaultCategory() {
        activeGroup = if (repository.recents().isNotEmpty()) recentsLabel else (repository.groups.firstOrNull() ?: recentsLabel)
        searchQuery = ""
        scrollOffsetY = 0f
        refreshItemsForActiveGroup()
        postInvalidateOnAnimation()
    }

    fun refreshRecents() {
        if (activeGroup == recentsLabel) {
            refreshItemsForActiveGroup()
        }
        postInvalidateOnAnimation()
    }

    fun setSearchQuery(query: String) {
        searchQuery = query
        scrollOffsetY = 0f
        refreshItemsForActiveGroup()
        postInvalidateOnAnimation()
    }

    private fun showCategory(index: Int) {
        val groups = tabGroups()
        if (index in groups.indices) {
            activeGroup = groups[index]
            scrollOffsetY = 0f
            refreshItemsForActiveGroup()
            postInvalidateOnAnimation()
        }
    }

    private fun refreshItemsForActiveGroup() {
        currentItems = if (searchQuery.isNotBlank()) {
            repository.search(searchQuery)
        } else if (activeGroup == recentsLabel) {
            repository.recents()
        } else {
            repository.emojisForGroup(activeGroup)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutTabs(w)
        layoutTopBarButtons(w)
    }

    private fun layoutTabs(w: Int) {
        tabRects.clear()
        val groups = tabGroups()
        if (groups.isEmpty()) return
        val tabBarTop = dp(topBarHeightDp).toInt()
        val tabBarBottom = tabBarTop + dp(tabBarHeightDp).toInt()
        val tabWidth = w / groups.size
        for ((i, g) in groups.withIndex()) {
            val left = i * tabWidth
            val right = if (i == groups.size - 1) w else left + tabWidth
            tabRects.add(Rect(left, tabBarTop, right, tabBarBottom) to g)
        }
    }

    private fun layoutTopBarButtons(w: Int) {
        val backSize = dp(36f).toInt()
        val margin = dp(4f).toInt()
        backButtonRect = Rect(w - backSize - margin, margin, w - margin, margin + backSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width
        val h = height
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), backgroundPaint)

        val topBarPx = dp(topBarHeightDp)
        val tabBarPx = dp(tabBarHeightDp)

        // Top bar sits behind the search EditText (a real Android view layered
        // above us by the parent EmojiPanelView).
        canvas.drawRect(0f, 0f, w.toFloat(), topBarPx, topBarPaint)
        drawBackButton(canvas)

        canvas.drawRect(0f, topBarPx, w.toFloat(), topBarPx + tabBarPx, tabBarPaint)
        drawTabs(canvas)

        val gridTop = topBarPx + tabBarPx
        drawGrid(canvas, w, h, gridTop)
    }

    private fun drawBackButton(canvas: Canvas) {
        canvas.drawRoundRect(
            backButtonRect.left.toFloat(), backButtonRect.top.toFloat(),
            backButtonRect.right.toFloat(), backButtonRect.bottom.toFloat(),
            dp(8f), dp(8f), backButtonPaint
        )
        val cx = backButtonRect.exactCenterX()
        val cy = backButtonRect.exactCenterY()
        val s = dp(6f)
        canvas.drawLine(cx + s, cy - s, cx - s, cy, backIconPaint)
        canvas.drawLine(cx - s, cy, cx + s, cy + s, backIconPaint)
    }

    private fun drawTabs(canvas: Canvas) {
        for ((rect, group) in tabRects) {
            val isActive = group == activeGroup && searchQuery.isBlank()
            if (pressedTab == group) {
                canvas.drawRect(rect, emojiPressedBgPaint)
            }
            tabIconPaint.color = if (isActive) Color.WHITE else Color.parseColor("#666666")
            canvas.drawText(
                tabGlyph(group),
                rect.exactCenterX(),
                rect.exactCenterY() + tabIconPaint.textSize / 3f,
                tabIconPaint
            )
            if (isActive) {
                val indicatorHeight = dp(2.5f)
                canvas.drawRect(
                    rect.left.toFloat() + dp(6f), rect.bottom - indicatorHeight,
                    rect.right.toFloat() - dp(6f), rect.bottom.toFloat(),
                    tabActiveIndicatorPaint
                )
            }
        }
    }

    private fun drawGrid(canvas: Canvas, w: Int, h: Int, gridTop: Float) {
        cellRects.clear()
        val cellSize = dp(cellSizeDp)
        val padding = dp(gridPaddingDp)
        val columns = max(1, ((w - padding) / (cellSize + padding)).toInt())
        val rowHeight = cellSize + padding

        canvas.save()
        canvas.clipRect(0f, gridTop, w.toFloat(), h.toFloat())

        if (currentItems.isEmpty()) {
            val message = when {
                searchQuery.isNotBlank() -> "No emoji found for \"$searchQuery\""
                activeGroup == recentsLabel -> "Emojis you use will show up here"
                else -> "No emoji in this category"
            }
            canvas.drawText(message, w / 2f, gridTop + dp(60f), emptyStatePaint)
            canvas.restore()
            maxScrollOffsetY = 0f
            return
        }

        val sectionLabel = if (searchQuery.isNotBlank()) {
            "${currentItems.size} result${if (currentItems.size == 1) "" else "s"}"
        } else {
            activeGroup
        }
        val labelHeight = dp(22f)
        canvas.drawText(sectionLabel, padding, gridTop + labelHeight - scrollOffsetY, labelPaint)

        val gridStartY = gridTop + labelHeight + dp(4f)
        val totalRows = (currentItems.size + columns - 1) / columns
        val totalContentHeight = totalRows * rowHeight
        val visibleHeight = h - gridStartY
        maxScrollOffsetY = max(0f, totalContentHeight - visibleHeight)
        scrollOffsetY = scrollOffsetY.coerceIn(0f, maxScrollOffsetY)

        for ((index, entry) in currentItems.withIndex()) {
            val row = index / columns
            val col = index % columns
            val cellLeft = padding + col * (cellSize + padding)
            val cellTop = gridStartY + row * rowHeight - scrollOffsetY
            if (cellTop + cellSize < gridTop || cellTop > h) continue // cheap off-screen culling

            val rect = Rect(
                cellLeft.toInt(), cellTop.toInt(),
                (cellLeft + cellSize).toInt(), (cellTop + cellSize).toInt()
            )
            cellRects.add(rect to entry)

            if (pressedCellIndex == index) {
                canvas.drawRoundRect(
                    rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.bottom.toFloat(),
                    dp(8f), dp(8f), emojiPressedBgPaint
                )
            }
            canvas.drawText(
                entry.character,
                rect.exactCenterX(),
                rect.exactCenterY() + emojiPaint.textSize / 3f,
                emojiPaint
            )
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragStartY = event.y
                dragStartScrollOffset = scrollOffsetY
                isDraggingScroll = false

                if (backButtonRect.contains(event.x.toInt(), event.y.toInt())) {
                    postInvalidateOnAnimation()
                    return true
                }
                val tab = tabRects.firstOrNull { it.first.contains(event.x.toInt(), event.y.toInt()) }
                if (tab != null) {
                    pressedTab = tab.second
                    postInvalidateOnAnimation()
                    return true
                }
                val cell = cellRects.withIndex().firstOrNull { (_, pair) -> pair.first.contains(event.x.toInt(), event.y.toInt()) }
                if (cell != null) {
                    pressedCellIndex = cell.index
                    postInvalidateOnAnimation()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - dragStartY
                if (!isDraggingScroll && abs(dy) > dragThreshold) {
                    isDraggingScroll = true
                    pressedCellIndex = -1
                    pressedTab = null
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
                        val tab = pressedTab
                        if (tab != null) {
                            val groups = tabGroups()
                            val idx = groups.indexOf(tab)
                            if (idx >= 0) showCategory(idx)
                        } else if (pressedCellIndex in currentItems.indices) {
                            onEmojiTapped?.invoke(currentItems[pressedCellIndex])
                        }
                    }
                }
                pressedCellIndex = -1
                pressedTab = null
                postInvalidateOnAnimation()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
