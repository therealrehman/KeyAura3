package com.example.animatedkeyboard.service

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import com.example.animatedkeyboard.ui.view.EmojiPanelView
import com.example.animatedkeyboard.ui.view.KeyboardView

class AnimatedKeyboardIME : InputMethodService() {

    private lateinit var rootContainer: FrameLayout
    private lateinit var keyboardView: KeyboardView
    private lateinit var emojiPanelView: EmojiPanelView
    private var currentInputEditorInfo: EditorInfo? = null

    // FIX: Force keyboard to stay at bottom, never fullscreen
    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        rootContainer = FrameLayout(this)

        keyboardView = KeyboardView(this)
        keyboardView.setBackgroundColor(0x00000000)
        keyboardView.setOnCustomKeyListener(object : KeyboardView.OnKeyListener {
            override fun onKey(code: Int, label: String) {
                val ic = currentInputConnection ?: return
                when (code) {
                    -1 -> {} // Shift handled in view
                    -5 -> ic.deleteSurroundingText(1, 0)
                    -4 -> handleSmartEnter()
                    -9 -> showEmojiPanel() // FIX: emoji key (replaces old Settings key)
                    else -> {
                        if (label == "Space") ic.commitText(" ", 1)
                        else ic.commitText(label, 1)
                    }
                }
            }
        })

        emojiPanelView = EmojiPanelView(this)
        emojiPanelView.setOnEmojiPanelListener(object : EmojiPanelView.OnEmojiPanelListener {
            override fun onEmojiSelected(emoji: String) {
                currentInputConnection?.commitText(emoji, 1)
            }
            override fun onBackToKeyboard() {
                showKeyboard()
            }
        })
        emojiPanelView.visibility = View.GONE

        rootContainer.addView(
            keyboardView,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        rootContainer.addView(
            emojiPanelView,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        return rootContainer
    }

    // FIX: Swaps the visible child within the same input view instead of
    // recreating onCreateInputView — instant, no flicker, and each view keeps
    // its own state (keyboard layout/shift state, emoji scroll/search text).
    private fun showEmojiPanel() {
        keyboardView.visibility = View.GONE
        emojiPanelView.visibility = View.VISIBLE
        emojiPanelView.onPanelShown()
    }

    private fun showKeyboard() {
        emojiPanelView.clearSearchFocus()
        emojiPanelView.visibility = View.GONE
        keyboardView.visibility = View.VISIBLE
    }

    // FIX: Store EditorInfo for Smart Enter functionality
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        currentInputEditorInfo = attribute
    }

    // FIX: Runs after the input view actually exists, so this is the reliable place
    // to tell KeyboardView which action the Return key should show/perform this time.
    // Also resets to the letter keyboard on every fresh field focus, matching how
    // system keyboards never reopen mid-conversation on the emoji tray.
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentInputEditorInfo = info
        if (::keyboardView.isInitialized) {
            keyboardView.setImeAction(resolveEditorAction(info))
        }
        if (::emojiPanelView.isInitialized && ::keyboardView.isInitialized) {
            showKeyboard()
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::keyboardView.isInitialized) {
            keyboardView.requestLayout()
            keyboardView.invalidate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::keyboardView.isInitialized) {
            keyboardView.release()
        }
    }

    // FIX: The old code checked EditorInfo.actionId first via ?:, but actionId is a
    // plain (non-null) Int that defaults to 0 for every normal app — it's only set
    // when an app defines a custom actionLabel. Since 0 isn't null, the Elvis operator
    // never fell through to check imeOptions, so real Search/Send/Go/Done requests
    // (which live in imeOptions, not actionId) were always ignored.
    private fun resolveEditorAction(info: EditorInfo?): Int {
        val imeOptions = info?.imeOptions ?: EditorInfo.IME_ACTION_UNSPECIFIED
        val noEnterAction = (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (noEnterAction) return EditorInfo.IME_ACTION_UNSPECIFIED
        return imeOptions and EditorInfo.IME_MASK_ACTION
    }

    // FIX: Smart Enter - performs Search/Send/Go/Done/Next/Previous when the field
    // asks for one (e.g. Google search box, chat send box); otherwise inserts a real
    // newline, same as a normal text field or notes app.
    private fun handleSmartEnter() {
        val ic = currentInputConnection ?: return
        val action = resolveEditorAction(currentInputEditorInfo)
        val hasRealAction = action != EditorInfo.IME_ACTION_NONE &&
            action != EditorInfo.IME_ACTION_UNSPECIFIED
        if (hasRealAction) {
            ic.performEditorAction(action)
        } else {
            ic.commitText("\n", 1)
        }
    }
}
