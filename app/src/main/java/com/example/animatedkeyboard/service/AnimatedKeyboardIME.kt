package com.example.animatedkeyboard.service

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import com.example.animatedkeyboard.ui.view.KeyboardView

class AnimatedKeyboardIME : InputMethodService() {

    private lateinit var keyboardView: KeyboardView
    private var currentInputEditorInfo: EditorInfo? = null

    // FIX: Force keyboard to stay at bottom, never fullscreen
    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        keyboardView = KeyboardView(this)
        keyboardView.setBackgroundColor(0x00000000)
        keyboardView.setOnCustomKeyListener(object : KeyboardView.OnKeyListener {
            override fun onKey(code: Int, label: String) {
                val ic = currentInputConnection ?: return
                when (code) {
                    -1 -> {} // Shift handled in view
                    -5 -> ic.deleteSurroundingText(1, 0)
                    -4 -> handleSmartEnter()
                    else -> {
                        if (label == "Space") ic.commitText(" ", 1)
                        else ic.commitText(label, 1)
                    }
                }
            }
        })
        return keyboardView
    }

    // FIX: Store EditorInfo for Smart Enter functionality
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        currentInputEditorInfo = attribute
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

    // FIX: Smart Enter - checks EditorInfo action type
    private fun handleSmartEnter() {
        val ic = currentInputConnection ?: return
        val action = currentInputEditorInfo?.actionId 
            ?: currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
            ?: EditorInfo.IME_ACTION_UNSPECIFIED

        when (action) {
            EditorInfo.IME_ACTION_SEND -> ic.performEditorAction(EditorInfo.IME_ACTION_SEND)
            EditorInfo.IME_ACTION_SEARCH -> ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
            EditorInfo.IME_ACTION_GO -> ic.performEditorAction(EditorInfo.IME_ACTION_GO)
            EditorInfo.IME_ACTION_DONE -> ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
            EditorInfo.IME_ACTION_NEXT -> ic.performEditorAction(EditorInfo.IME_ACTION_NEXT)
            else -> ic.commitText("\n", 1) // Actual newline for normal text fields
        }
    }
}