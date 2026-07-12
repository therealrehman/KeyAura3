package com.example.animatedkeyboard.ui.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

/**
 * Wraps BirdyBirdCanvas — same fixed-height approach as the Emoji/Clipboard
 * panels (screen-fraction via onMeasure) so the game occupies exactly the
 * keyboard's own footprint, never the full screen.
 */
class GamePanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface OnGamePanelListener {
        fun onBackToKeyboard()
    }

    private var listener: OnGamePanelListener? = null
    private val gameCanvas: BirdyBirdCanvas

    fun setOnGamePanelListener(l: OnGamePanelListener) {
        listener = l
    }

    init {
        setBackgroundColor(Color.BLACK)
        gameCanvas = BirdyBirdCanvas(context)
        gameCanvas.onBackTapped = { listener?.onBackToKeyboard() }
        addView(gameCanvas, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
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

    /** Called by the IME each time the panel is shown — (re)starts the game loop. */
    fun onPanelShown() {
        gameCanvas.onShown()
    }

    /** Called right before switching back to the keyboard, so the game loop stops cleanly. */
    fun onPanelHidden() {
        gameCanvas.onHidden()
    }
}
