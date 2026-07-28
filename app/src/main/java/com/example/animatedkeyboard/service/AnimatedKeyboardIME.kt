package com.example.animatedkeyboard.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.example.animatedkeyboard.clipboard.ClipboardEntry
import com.example.animatedkeyboard.clipboard.ClipboardRepository
import com.example.animatedkeyboard.ui.view.ClipboardPanelView
import com.example.animatedkeyboard.ui.view.EmojiPanelView
import com.example.animatedkeyboard.ui.view.GamePanelView
import com.example.animatedkeyboard.ui.view.KeyboardView

class AnimatedKeyboardIME : InputMethodService() {

    private lateinit var rootContainer: FrameLayout
    private lateinit var keyboardView: KeyboardView
    private lateinit var emojiPanelView: EmojiPanelView
    private lateinit var clipboardPanelView: ClipboardPanelView
    private lateinit var gamePanelView: GamePanelView
    private var currentInputEditorInfo: EditorInfo? = null

    private val clipboardRepo by lazy { ClipboardRepository.getInstance(this) }
    private var clipboardManager: ClipboardManager? = null
    private var clipListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private var isFirstClipCallback = true // FIX: registering the listener fires once immediately with whatever's already on the clipboard — skip that first spurious callback

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    // FIX: Force keyboard to stay at bottom, never fullscreen
    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        rootContainer = FrameLayout(this)

        window?.window?.let { w ->
            w.navigationBarColor = android.graphics.Color.BLACK
        }
        window?.setVolumeControlStream(AudioManager.STREAM_MUSIC)

        keyboardView = KeyboardView(this)
        keyboardView.setBackgroundColor(0x00000000)
        keyboardView.setOnCustomKeyListener(object : KeyboardView.OnKeyListener {
            override fun onKey(code: Int, label: String) {
                val ic = currentInputConnection ?: return
                when (code) {
                    -1 -> {} // Shift handled in view
                    -5 -> {
                        val selected = ic.getSelectedText(0)
                        if (!selected.isNullOrEmpty()) {
                            ic.commitText("", 1)
                        } else {
                            val beforeCursor = ic.getTextBeforeCursor(2, 0)
                            val deleteLength = if (beforeCursor != null && beforeCursor.length == 2 &&
                                Character.isSurrogatePair(beforeCursor[0], beforeCursor[1])
                            ) 2 else 1
                            ic.deleteSurroundingText(deleteLength, 0)
                        }
                    }
                    -4 -> handleSmartEnter()
                    -9 -> showEmojiPanel()
                    -10 -> showClipboardPanel() // FIX: Clipboard key
                    -11 -> toggleSpeechRecognition() // FIX: Mic key
                    -14 -> showGamePanel() // FIX: Game key
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

        clipboardPanelView = ClipboardPanelView(this)
        clipboardPanelView.setOnClipboardPanelListener(object : ClipboardPanelView.OnClipboardPanelListener {
            override fun onClipSelected(entry: ClipboardEntry) {
                pasteClip(entry)
                showKeyboard()
            }
            override fun onBackToKeyboard() {
                showKeyboard()
            }
        })
        clipboardPanelView.visibility = View.GONE

        gamePanelView = GamePanelView(this)
        gamePanelView.setOnGamePanelListener(object : GamePanelView.OnGamePanelListener {
            override fun onBackToKeyboard() {
                showKeyboard()
            }
        })
        gamePanelView.visibility = View.GONE

        rootContainer.addView(keyboardView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        rootContainer.addView(emojiPanelView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        rootContainer.addView(clipboardPanelView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        rootContainer.addView(gamePanelView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        registerClipboardListener()
        return rootContainer
    }

    // FIX: watches the system clipboard the whole time the IME process is alive
    // (not just while a panel is open) so a copy made in another app is already
    // waiting — as a quick-paste suggestion and in the Clipboard panel — by the
    // time the user switches back to type.
    private fun registerClipboardListener() {
        if (clipListener != null) return
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            if (isFirstClipCallback) {
                isFirstClipCallback = false
                return@OnPrimaryClipChangedListener
            }
            onSystemClipChanged()
        }
        clipListener = listener
        clipboardManager?.addPrimaryClipChangedListener(listener)
    }

    private fun onSystemClipChanged() {
        val clip = clipboardManager?.primaryClip ?: return
        if (clip.itemCount == 0) return
        val item = clip.getItemAt(0)
        val description = clip.description

        val isImage = description.hasMimeType("image/*") && item.uri != null
        if (isImage) {
            item.uri?.let { uri ->
                clipboardRepo.addImage(uri, contentResolver)
            }
        } else {
            val text = item.coerceToText(this)?.toString()
            if (!text.isNullOrBlank()) {
                clipboardRepo.addText(text)
                if (::keyboardView.isInitialized) {
                    keyboardView.showClipboardSuggestion(text)
                }
            }
        }
    }

    private fun pasteClip(entry: ClipboardEntry) {
        val ic = currentInputConnection ?: return
        if (entry.type == "image") {
            try {
                // FIX: a raw file:// URI into our private storage can't be read by
                // any other app on API 24+ — FileProvider issues a content:// URI
                // the receiving app can actually open, and the GRANT_READ flag
                // gives it temporary read access to that specific file.
                val file = java.io.File(entry.content)
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    this, "$packageName.fileprovider", file
                )
                val editorInfo = currentInputEditorInfo
                val mimeTypes = editorInfo?.contentMimeTypes
                val mimeType = mimeTypes?.firstOrNull { it.startsWith("image/") } ?: "image/png"
                val contentInfo = InputContentInfoCompat(uri, android.content.ClipDescription("clip image", arrayOf(mimeType)), null)
                InputConnectionCompat.commitContent(
                    ic, editorInfo ?: EditorInfo(), contentInfo,
                    InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, null
                )
            } catch (e: Exception) {
                // Some apps' text fields simply don't support rich content commit at
                // all (an Android platform limitation, not something an IME can force).
            }
        } else {
            ic.commitText(entry.content, 1)
        }
    }

    // FIX: checks RECORD_AUDIO permission first — an IME (Service) can't show
    // the system permission dialog itself, so it hands off to a tiny transparent
    // Activity that can. First tap after install just grants permission; the
    // user taps the mic again to actually start listening (kept simple/robust
    // rather than trying to auto-resume listening across the Service/Activity boundary).
    private fun toggleSpeechRecognition() {
        if (isListening) {
            stopSpeechRecognition()
            return
        }
        val granted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) {
            val intent = Intent(this, com.example.animatedkeyboard.SpeechPermissionActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
            return
        }
        startSpeechRecognition()
    }

    private fun startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        val recognizer = speechRecognizer ?: SpeechRecognizer.createSpeechRecognizer(this).also { speechRecognizer = it }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
                if (::keyboardView.isInitialized) keyboardView.setListeningState(false)
            }
            override fun onError(error: Int) {
                isListening = false
                if (::keyboardView.isInitialized) keyboardView.setListeningState(false)
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                if (::keyboardView.isInitialized) keyboardView.setListeningState(false)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    currentInputConnection?.commitText("$text ", 1)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        try {
            recognizer.startListening(intent)
            isListening = true
            if (::keyboardView.isInitialized) keyboardView.setListeningState(true)
        } catch (e: Exception) {
            isListening = false
        }
    }

    private fun stopSpeechRecognition() {
        speechRecognizer?.stopListening()
        isListening = false
        if (::keyboardView.isInitialized) keyboardView.setListeningState(false)
    }

    private fun showEmojiPanel() {
        keyboardView.visibility = View.GONE
        clipboardPanelView.visibility = View.GONE
        gamePanelView.visibility = View.GONE
        gamePanelView.onPanelHidden()
        emojiPanelView.visibility = View.VISIBLE
        emojiPanelView.onPanelShown()
    }

    private fun showClipboardPanel() {
        keyboardView.visibility = View.GONE
        emojiPanelView.visibility = View.GONE
        gamePanelView.visibility = View.GONE
        gamePanelView.onPanelHidden()
        clipboardPanelView.visibility = View.VISIBLE
        clipboardPanelView.onPanelShown()
    }

    private fun showGamePanel() {
        keyboardView.visibility = View.GONE
        emojiPanelView.visibility = View.GONE
        clipboardPanelView.visibility = View.GONE
        gamePanelView.visibility = View.VISIBLE
        gamePanelView.onPanelShown()
    }

    private fun showKeyboard() {
        emojiPanelView.clearSearchFocus()
        emojiPanelView.visibility = View.GONE
        clipboardPanelView.visibility = View.GONE
        gamePanelView.visibility = View.GONE
        gamePanelView.onPanelHidden()
        keyboardView.visibility = View.VISIBLE
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        currentInputEditorInfo = attribute
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentInputEditorInfo = info
        window?.window?.let { w -> w.navigationBarColor = android.graphics.Color.BLACK }
        if (::keyboardView.isInitialized) {
            keyboardView.refreshSoundEngineTune()
            keyboardView.refreshTheme()
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
        clipListener?.let { clipboardManager?.removePrimaryClipChangedListener(it) }
        speechRecognizer?.destroy()
    }

    private fun resolveEditorAction(info: EditorInfo?): Int {
        val imeOptions = info?.imeOptions ?: EditorInfo.IME_ACTION_UNSPECIFIED
        val noEnterAction = (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (noEnterAction) return EditorInfo.IME_ACTION_UNSPECIFIED
        return imeOptions and EditorInfo.IME_MASK_ACTION
    }

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
