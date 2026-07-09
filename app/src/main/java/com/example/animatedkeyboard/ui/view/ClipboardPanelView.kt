package com.example.animatedkeyboard.ui.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.example.animatedkeyboard.clipboard.ClipboardEntry
import com.example.animatedkeyboard.clipboard.ClipboardRepository

/**
 * Full-size clipboard history panel — same fixed-height approach as
 * EmojiPanelView (screen-fraction via onMeasure) so it matches the keyboard's
 * own footprint instead of growing to fill the screen.
 */
class ClipboardPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface OnClipboardPanelListener {
        fun onClipSelected(entry: ClipboardEntry)
        fun onBackToKeyboard()
    }

    private var listener: OnClipboardPanelListener? = null
    private val repository by lazy { ClipboardRepository.getInstance(context) }
    private val gridView: ClipboardGridCanvas

    fun setOnClipboardPanelListener(l: OnClipboardPanelListener) {
        listener = l
    }

    init {
        setBackgroundColor(Color.BLACK)
        gridView = ClipboardGridCanvas(context)
        gridView.onClipTapped = { entry -> listener?.onClipSelected(entry) }
        gridView.onPinTapped = { entry -> repository.togglePin(entry.id); gridView.refresh() }
        gridView.onBackTapped = { listener?.onBackToKeyboard() }
        addView(gridView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val dm = resources.displayMetrics
        val isLandscape = dm.widthPixels > dm.heightPixels
        val desiredHeight = if (isLandscape) (dm.heightPixels * 0.30f).toInt() else (dm.heightPixels * 0.35f).toInt()
        super.onMeasure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(desiredHeight, View.MeasureSpec.EXACTLY)
        )
    }

    /** Called by the IME each time the panel is shown, so it reflects the latest clips. */
    fun onPanelShown() {
        gridView.refresh()
    }
}
