package com.example.animatedkeyboard.ui.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.example.animatedkeyboard.emoji.EmojiRepository

/**
 * Full-size emoji panel — occupies the SAME footprint as KeyboardView (fixed
 * height via onMeasure below, mirroring KeyboardView's own approach) so it
 * never grows to fill the whole screen the way a plain MATCH_PARENT view
 * would inside an InputMethodService.
 *
 * Search has no real EditText (an IME can't reliably host its own text field
 * and have the system route typing back into it) — instead EmojiGridCanvas
 * draws the query text itself and provides a compact built-in mini keyboard
 * while searching. See EmojiGridCanvas for that logic.
 */
class EmojiPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface OnEmojiPanelListener {
        fun onEmojiSelected(emoji: String)
        fun onBackToKeyboard()
    }

    private var listener: OnEmojiPanelListener? = null
    private val repository by lazy { EmojiRepository.getInstance(context) }
    private val gridView: EmojiGridCanvas
    private var bottomInsetPx = 0
    private var lastContentHeight = 0

    fun setOnEmojiPanelListener(l: OnEmojiPanelListener) {
        listener = l
    }

    // FIX: only the panel's own background should extend into the nav bar
    // inset — the grid's internal tab/grid/mini-keyboard math assumes its
    // height IS the usable content area, so it keeps a fixed height instead
    // of MATCH_PARENT and simply doesn't stretch into the extra space.
    fun setBottomInset(px: Int) {
        if (bottomInsetPx != px) {
            bottomInsetPx = px
            requestLayout()
        }
    }

    init {
        setBackgroundColor(Color.BLACK)

        gridView = EmojiGridCanvas(context)
        gridView.onEmojiTapped = { entry ->
            repository.recordUsage(entry)
            listener?.onEmojiSelected(entry.character)
        }
        gridView.onBackTapped = { listener?.onBackToKeyboard() }
        addView(gridView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    // FIX: Without this, EmojiPanelView (a plain MATCH_PARENT FrameLayout) could
    // expand to fill the entire available IME window height — visibly the whole
    // screen — since nothing constrained it the way KeyboardView constrains
    // itself. Forcing the exact same fraction here guarantees the emoji panel
    // is always the same height as the keyboard, never taller (plus the nav
    // bar inset, so its background can paint all the way down like the
    // keyboard's does).
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val dm = resources.displayMetrics
        val isLandscape = dm.widthPixels > dm.heightPixels
        val desiredContentHeight = if (isLandscape) {
            (dm.heightPixels * 0.30f).toInt()
        } else {
            (dm.heightPixels * 0.35f).toInt()
        }
        lastContentHeight = desiredContentHeight
        val totalHeight = desiredContentHeight + bottomInsetPx

        val gridParams = gridView.layoutParams as LayoutParams
        if (gridParams.height != desiredContentHeight) {
            gridParams.height = desiredContentHeight
            gridParams.gravity = android.view.Gravity.TOP
            gridView.layoutParams = gridParams
        }

        super.onMeasure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(totalHeight, View.MeasureSpec.EXACTLY)
        )
    }

    /** Called by the IME each time the panel is shown, so it reflects current recents. */
    fun onPanelShown() {
        gridView.refreshRecents()
        gridView.resetToDefaultCategory()
    }

    /** Exits search mode (if active) — called right before switching back to the keyboard. */
    fun clearSearchFocus() {
        gridView.exitSearchMode()
    }
}
