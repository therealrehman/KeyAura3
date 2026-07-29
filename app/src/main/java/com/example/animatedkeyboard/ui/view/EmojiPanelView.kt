package com.example.animatedkeyboard.ui.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.example.animatedkeyboard.emoji.EmojiRepository

class EmojiPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface OnEmojiPanelListener {
        fun onEmojiSelected(emoji: String)
        fun onBackToKeyboard()
        fun onSearchRequested() // NEW
    }

    private var listener: OnEmojiPanelListener? = null
    private val repository by lazy { EmojiRepository.getInstance(context) }
    private val gridView: EmojiGridCanvas

    fun setOnEmojiPanelListener(l: OnEmojiPanelListener) {
        listener = l
    }

    init {
        setBackgroundColor(Color.BLACK)
        gridView = EmojiGridCanvas(context)
        gridView.onEmojiTapped = { entry ->
            repository.recordUsage(entry)
            listener?.onEmojiSelected(entry.character)
        }
        gridView.onBackTapped = { listener?.onBackToKeyboard() }
        gridView.onSearchRequested = { listener?.onSearchRequested() } // NEW
        addView(gridView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val dm = resources.displayMetrics
        val isLandscape = dm.widthPixels > dm.heightPixels
        val desiredHeight = if (isLandscape) (dm.heightPixels * 0.30f).toInt()
        else (dm.heightPixels * 0.35f).toInt()
        super.onMeasure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(desiredHeight, View.MeasureSpec.EXACTLY)
        )
    }

    fun onPanelShown() {
        gridView.refreshRecents()
        gridView.resetToDefaultCategory()
    }

    fun clearSearchFocus() {
        // No-op now — search lives in IME, not panel
    }
}
