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
 * The search bar, category tabs / mini-keyboard, and scrollable emoji grid.
 * Kept as a plain canvas view (rather than RecyclerView + EditText) to match
 * the rest of the app's drawing style, and — importantly — because a real
 * EditText inside an IME's own view can't reliably get a keyboard shown for
 * it (this IME already IS the active keyboard, so it can't re-invoke itself
 * for its own internal field). Instead, search has its own compact built-in
 * mini keyboard that types directly into an internal query string.
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
    private val topBarHeightDp = 40f
    private val tabBarHeightDp = 40f
    private val miniKeyboardHeightDp = 78f
    private val cellSizeDp = 40f
    private val gridPaddingDp = 6f

    private val backgroundPaint = Paint().apply { color = Color.BLACK }
    private val topBarPaint = Paint().apply { color = Color.parseColor("#0A0A0A") }
    private val tabBarPaint = Paint().apply { color = Color.parseColor("#101010") }
    private val tabIconPaint = Paint().apply { textSize = dp(18f); textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val tabActiveIndicatorPaint = Paint().apply { color = Color.parseColor("#4488FF"); isAntiAlias = true }
    private val emojiPaint = Paint().apply { textSize = dp(22f); textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val emojiPressedBgPaint = Paint().apply { color = Color.parseColor("#1E1E1E"); isAntiAlias = true }
    private val labelPaint = Paint().apply {
        color = Color.parseColor("#666666"); textSize = dp(12f); isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.LEFT
    }
    private val emptyStatePaint = Paint().apply {
        color = Color.parseColor("#666666"); textSize = dp(14f); isAntiAlias = true; textAlign = Paint.Align.CENTER
    }
    private val iconButtonPaint = Paint().apply { color = Color.parseColor("#141414"); isAntiAlias = true }
    private val backIconPaint = Paint().apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = dp(2f)
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; isAntiAlias = true
    }
    private val queryTextPaint = Paint().apply {
        color = Color.WHITE; textSize = dp(15f); isAntiAlias = true; textAlign = Paint.Align.LEFT
    }
    private val queryHintPaint = Paint().apply {
        color = Color.parseColor("#666666"); textSize = dp(15f); isAntiAlias = true; textAlign = Paint.Align.LEFT
    }
    private val miniKeyPaint = Paint().apply { color = Color.parseColor("#141414"); isAntiAlias = true }
    private val miniKeyTextPaint = Paint().apply {
        color = Color.WHITE; textSize = dp(13f); isAntiAlias = true; textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
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

    // FIX: compact built-in keyboard for search — letters + backspace only,
    // three short rows so it fits the same fixed panel height as the keyboard.
    private val miniKeyboardRows = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("z", "x", "c", "v", "b", "n", "m", "Del")
    )

    private var activeGroup: String = recentsLabel
    private var searchQuery: String = ""
    private var isSearchMode = false
    private var currentItems: List<EmojiEntry> = emptyList()
    private var scrollOffsetY = 0f
    private var maxScrollOffsetY = 0f

    private val cellRects = mutableListOf<Pair<Rect, EmojiEntry>>()
    private val tabRects = mutableListOf<Pair<Rect, String>>()
    private val miniKeyRects = mutableListOf<Pair<Rect, String>>()
    private var backButtonRect = Rect()
    private var searchButtonRect = Rect()

    private var pressedCellIndex = -1
    private var pressedTab: String? = null
    private var pressedMiniKey: String? = null
    private var isDraggingScroll = false
    private var dragStartY = 0f
    private var dragStartScrollOffset = 0f
    private val dragThreshold = dp(8f)

    init {
        setWillNotDraw(false)
        refreshItemsForActiveGroup()
    }

    fun resetToDefaultCategory() {
        isSearchMode = false
        activeGroup = if (repository.recents().isNotEmpty()) recentsLabel else (repository.groups.firstOrNull() ?: recentsLabel)
        searchQuery = ""
        scrollOffsetY = 0f
        refreshItemsForActiveGroup()
        postInvalidateOnAnimation()
    }

    fun exitSearchMode() {
        if (isSearchMode) {
            isSearchMode = false
            searchQuery = ""
            scrollOffsetY = 0f
            refreshItemsForActiveGroup()
            postInvalidateOnAnimation()
        }
    }

    fun refreshRecents() {
        if (activeGroup == recentsLabel) {
            refreshItemsForActiveGroup()
        }
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
        } else if (isSearchMode) {
            emptyList() // FIX: only show searched emojis while searching — blank until typed
        } else if (activeGroup == recentsLabel) {
            repository.recents()
        } else {
            repository.emojisForGroup(activeGroup)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutTabs(w)
        layoutMiniKeyboard(w)
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

    private fun layoutMiniKeyboard(w: Int) {
        miniKeyRects.clear()
        val top = dp(topBarHeightDp).toInt()
        val rowHeight = (dp(miniKeyboardHeightDp) / miniKeyboardRows.size).toInt()
        var rowTop = top
        for (row in miniKeyboardRows) {
            val keyWidth = w / row.size
            var x = 0
            for ((i, key) in row.withIndex()) {
                val right = if (i == row.lastIndex) w else x + keyWidth
                miniKeyRects.add(Rect(x, rowTop, right, rowTop + rowHeight) to key)
                x = right
            }
            rowTop += rowHeight
        }
    }

    private fun layoutTopBarButtons(w: Int) {
        val btnSize = dp(32f).toInt()
        val margin = dp(4f).toInt()
        backButtonRect = Rect(w - btnSize - margin, margin, w - margin, margin + btnSize)
        searchButtonRect = Rect(margin, margin, margin + btnSize, margin + btnSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width
        val h = height
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), backgroundPaint)

        val topBarPx = dp(topBarHeightDp)
        val secondBandPx = if (isSearchMode) dp(miniKeyboardHeightDp) else dp(tabBarHeightDp)

        canvas.drawRect(0f, 0f, w.toFloat(), topBarPx, topBarPaint)
        drawSearchButton(canvas)
        drawBackButton(canvas)
        drawQueryText(canvas, topBarPx)

        if (isSearchMode) {
            drawMiniKeyboard(canvas)
        } else {
            canvas.drawRect(0f, topBarPx, w.toFloat(), topBarPx + secondBandPx, tabBarPaint)
            drawTabs(canvas)
        }

        val gridTop = topBarPx + secondBandPx
        drawGrid(canvas, w, h, gridTop)
    }

    private fun drawQueryText(canvas: Canvas, topBarPx: Float) {
        val textX = searchButtonRect.right.toFloat() + dp(10f)
        val textY = topBarPx / 2f + queryTextPaint.textSize / 3f
        if (isSearchMode) {
            if (searchQuery.isEmpty()) {
                canvas.drawText("Type to search\u2026", textX, textY, queryHintPaint)
            } else {
                canvas.drawText(searchQuery, textX, textY, queryTextPaint)
            }
        }
    }

    private fun drawSearchButton(canvas: Canvas) {
        canvas.drawRoundRect(
            searchButtonRect.left.toFloat(), searchButtonRect.top.toFloat(),
            searchButtonRect.right.toFloat(), searchButtonRect.bottom.toFloat(),
            dp(8f), dp(8f), iconButtonPaint
        )
        val cx = searchButtonRect.exactCenterX() - dp(1.5f)
        val cy = searchButtonRect.exactCenterY() - dp(1.5f)
        val s = dp(5f)
        val p = Paint(backIconPaint).apply {
            color = if (isSearchMode) Color.parseColor("#4488FF") else Color.WHITE
            strokeWidth = dp(1.8f)
        }
        canvas.drawCircle(cx, cy, s, p)
        canvas.drawLine(cx + s * 0.75f, cy + s * 0.75f, cx + s * 1.6f, cy + s * 1.6f, p)
    }

    private fun drawBackButton(canvas: Canvas) {
        canvas.drawRoundRect(
            backButtonRect.left.toFloat(), backButtonRect.top.toFloat(),
            backButtonRect.right.toFloat(), backButtonRect.bottom.toFloat(),
            dp(8f), dp(8f), iconButtonPaint
        )
        val cx = backButtonRect.exactCenterX()
        val cy = backButtonRect.exactCenterY()
        val s = dp(6f)
        canvas.drawLine(cx + s, cy - s, cx - s, cy, backIconPaint)
        canvas.drawLine(cx - s, cy, cx + s, cy + s, backIconPaint)
    }

    private fun drawTabs(canvas: Canvas) {
        for ((rect, group) in tabRects) {
            val isActive = group == activeGroup
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

    private fun drawMiniKeyboard(canvas: Canvas) {
        val gap = dp(2f)
        for ((rect, key) in miniKeyRects) {
            val bg = if (pressedMiniKey == key) Color.parseColor("#2A2A2A") else Color.parseColor("#141414")
            miniKeyPaint.color = bg
            canvas.drawRoundRect(
                rect.left + gap, rect.top + gap, rect.right - gap, rect.bottom - gap,
                dp(4f), dp(4f), miniKeyPaint
            )
            if (key == "Del") {
                val cx = rect.exactCenterX()
                val cy = rect.exactCenterY()
                val s = dp(5f)
                canvas.drawLine(cx - s, cy - s, cx + s, cy + s, backIconPaint)
                canvas.drawLine(cx + s, cy - s, cx - s, cy + s, backIconPaint)
            } else {
                canvas.drawText(key, rect.exactCenterX(), rect.exactCenterY() + miniKeyTextPaint.textSize / 3f, miniKeyTextPaint)
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
                isSearchMode && searchQuery.isBlank() -> "Type to search emoji"
                searchQuery.isNotBlank() -> "No emoji found for \"$searchQuery\""
                activeGroup == recentsLabel -> "Emojis you use will show up here"
                else -> "No emoji in this category"
            }
            canvas.drawText(message, w / 2f, gridTop + dp(40f), emptyStatePaint)
            canvas.restore()
            maxScrollOffsetY = 0f
            return
        }

        val sectionLabel = if (searchQuery.isNotBlank()) {
            "${currentItems.size} result${if (currentItems.size == 1) "" else "s"}"
        } else {
            activeGroup
        }
        val labelHeight = dp(18f)
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

    private fun onMiniKeyTapped(key: String) {
        if (key == "Del") {
            if (searchQuery.isNotEmpty()) searchQuery = searchQuery.dropLast(1)
        } else {
            searchQuery += key
        }
        scrollOffsetY = 0f
        refreshItemsForActiveGroup()
        postInvalidateOnAnimation()
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
                if (searchButtonRect.contains(event.x.toInt(), event.y.toInt())) {
                    postInvalidateOnAnimation()
                    return true
                }
                if (isSearchMode) {
                    val key = miniKeyRects.firstOrNull { it.first.contains(event.x.toInt(), event.y.toInt()) }
                    if (key != null) {
                        pressedMiniKey = key.second
                        postInvalidateOnAnimation()
                        return true
                    }
                } else {
                    val tab = tabRects.firstOrNull { it.first.contains(event.x.toInt(), event.y.toInt()) }
                    if (tab != null) {
                        pressedTab = tab.second
                        postInvalidateOnAnimation()
                        return true
                    }
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
                    pressedMiniKey = null
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
                    } else if (searchButtonRect.contains(event.x.toInt(), event.y.toInt())) {
                        isSearchMode = !isSearchMode
                        searchQuery = ""
                        scrollOffsetY = 0f
                        refreshItemsForActiveGroup()
                    } else {
                        val miniKey = pressedMiniKey
                        val tab = pressedTab
                        if (miniKey != null) {
                            onMiniKeyTapped(miniKey)
                        } else if (tab != null) {
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
                pressedMiniKey = null
                postInvalidateOnAnimation()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
