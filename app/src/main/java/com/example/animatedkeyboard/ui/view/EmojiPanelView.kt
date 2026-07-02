package com.example.animatedkeyboard.ui.view

import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import com.example.animatedkeyboard.emoji.EmojiRepository

/**
 * Full-size emoji panel — occupies the same footprint as KeyboardView so
 * switching between the two feels like a single continuous keyboard surface
 * (the way system keyboards swap in an emoji tray) rather than a small popup.
 *
 * Layout, top to bottom:
 *   [ search bar                                    ] [Back]
 *   [ category tabs: recents smileys people animals food travel activities objects symbols flags ]
 *   [ scrolling grid of emoji for the active tab/search ]
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

    fun setOnEmojiPanelListener(l: OnEmojiPanelListener) {
        listener = l
    }

    // Real EditText hosts the search field so we get a proper system text
    // cursor/IME-independent input surface without reinventing text editing;
    // it's styled to match the panel's canvas-drawn dark look.
    private val searchInput: EditText
    private val gridView: EmojiGridCanvas

    init {
        setBackgroundColor(Color.BLACK)

        gridView = EmojiGridCanvas(context)
        gridView.onEmojiTapped = { entry ->
            repository.recordUsage(entry)
            listener?.onEmojiSelected(entry.character)
        }
        gridView.onBackTapped = { listener?.onBackToKeyboard() }
        addView(gridView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        searchInput = EditText(context).apply {
            hint = "Search emoji"
            setHintTextColor(Color.parseColor("#666666"))
            setTextColor(Color.WHITE)
            textSize = 15f
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            setBackgroundColor(Color.parseColor("#141414"))
            setPadding(dp(14f).toInt(), dp(8f).toInt(), dp(14f).toInt(), dp(8f).toInt())
        }
        val searchParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(40f).toInt())
        searchParams.leftMargin = dp(8f).toInt()
        searchParams.rightMargin = dp(56f).toInt() // room for the canvas-drawn Back button
        searchParams.topMargin = dp(6f).toInt()
        addView(searchInput, searchParams)

        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                gridView.setSearchQuery(s?.toString().orEmpty())
            }
        })
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    /** Called by the IME each time the panel is shown, so it reflects current recents. */
    fun onPanelShown() {
        gridView.refreshRecents()
        searchInput.setText("")
        gridView.resetToDefaultCategory()
    }

    /** Drops keyboard focus from the search field, e.g. right before switching back. */
    fun clearSearchFocus() {
        searchInput.clearFocus()
    }
}
