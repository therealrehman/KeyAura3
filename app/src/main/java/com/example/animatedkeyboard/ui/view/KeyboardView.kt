package com.example.animatedkeyboard.ui.view

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import com.example.animatedkeyboard.audio.KeySoundEngine
import com.example.animatedkeyboard.settings.KeyboardSettings
import com.example.animatedkeyboard.urdu.UrduSuggestionRepository
import com.example.animatedkeyboard.english.EnglishSuggestionRepository
import com.example.animatedkeyboard.utils.AnimationEngine
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class KeyState { NORMAL, WHITE, PINK, FADE }

class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnKeyListener {
        fun onKey(code: Int, label: String)
    }

    private var keyListener: OnKeyListener? = null
    private val handler = Handler(Looper.getMainLooper())
    private var backspaceRunnable: Runnable? = null
    private var capsLockRunnable: Runnable? = null

    private val settings by lazy { KeyboardSettings.getInstance(context) }
    private val soundEngine by lazy { KeySoundEngine(context) }
    private val urduRepo by lazy { UrduSuggestionRepository.getInstance(context) }
    private val englishRepo by lazy { EnglishSuggestionRepository.getInstance(context) }
    private val clipboardRepo by lazy { com.example.animatedkeyboard.clipboard.ClipboardRepository.getInstance(context) }

    // Animation engine – recreated on settings change
    private var animationEngine: AnimationEngine = AnimationEngine(settings.animationType)

    private var pendingClipboardFull: String? = null
    private var pendingClipboardDisplay: String? = null

    fun showClipboardSuggestion(fullText: String) {
        pendingClipboardFull = fullText
        pendingClipboardDisplay = truncateForDisplay(fullText)
        createKeyMap(width, height)
    }

    private fun truncateForDisplay(text: String): String {
        val firstWord = text.split(Regex("\\s+")).firstOrNull() ?: text
        return if (firstWord.length <= 30) firstWord else firstWord.take(30) + "…"
    }

    private var isListeningForSpeech = false

    fun setListeningState(listening: Boolean) {
        if (isListeningForSpeech != listening) {
            isListeningForSpeech = listening
            postInvalidateOnAnimation()
        }
    }

    private val vibrator by lazy {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
    }

    private fun triggerKeyHaptic() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (!settings.hapticEnabled) return
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                v.vibrate(
                    android.os.VibrationEffect.createOneShot(
                        settings.hapticDurationMs,
                        settings.hapticAmplitude
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(settings.hapticDurationMs)
            }
        } catch (_: Exception) { }
    }

    private val urduPunctuationMap = mapOf(
        "," to "،", "." to "۔", ";" to "؛", "?" to "؟"
    )

    private val romanUrduMap = mapOf(
        "a" to "ا", "b" to "ب", "c" to "چ", "d" to "د",
        "e" to "ے", "f" to "ف", "g" to "گ", "h" to "ح",
        "i" to "ی", "j" to "ج", "k" to "ک", "l" to "ل",
        "m" to "م", "n" to "ن", "o" to "و", "p" to "پ",
        "q" to "ق", "r" to "ر", "s" to "س", "t" to "ت",
        "u" to "ء", "v" to "ط", "w" to "و", "x" to "ش",
        "y" to "ے", "z" to "ز",
        "aa" to "آ", "ae" to "ع", "ai" to "ئی",
        "ch" to "چ", "gh" to "غ", "kh" to "خ", "ph" to "ف",
        "sh" to "ش", "th" to "ث", "zh" to "ژ",
        "ba" to "با", "be" to "بے", "bi" to "بی", "bo" to "بو", "bu" to "بو",
        "ta" to "تا", "te" to "تے", "ti" to "تی", "to" to "تو", "tu" to "تو",
        "ja" to "جا", "je" to "جے", "ji" to "جی", "jo" to "جو", "ju" to "جو",
        "ha" to "حا", "he" to "حے", "hi" to "حی", "ho" to "حو", "hu" to "حو",
        "da" to "دا", "de" to "دے", "di" to "دی", "do" to "دو", "du" to "دو",
        "ra" to "را", "re" to "رے", "ri" to "ری", "ro" to "رو", "ru" to "رو",
        "sa" to "سا", "se" to "سے", "si" to "سی", "so" to "سو", "su" to "سو",
        "na" to "نا", "ne" to "نے", "ni" to "نی", "no" to "نو", "nu" to "نو",
        "la" to "لا", "le" to "لے", "li" to "لی", "lo" to "لو", "lu" to "لو",
        "ma" to "ما", "me" to "مے", "mi" to "می", "mo" to "مو", "mu" to "مو",
        "ka" to "کا", "ke" to "کے", "ki" to "کی", "ko" to "کو", "ku" to "کو",
        "ga" to "گا", "ge" to "گے", "gi" to "گی", "go" to "گو", "gu" to "گو",
        "fa" to "فا", "fe" to "فے", "fi" to "فی", "fo" to "فو", "fu" to "فو",
        "pa" to "پا", "pe" to "پے", "pi" to "پی", "po" to "پو", "pu" to "پو",
        "wa" to "وا", "we" to "وے", "wi" to "وی", "wo" to "وو", "wu" to "وو",
        "ya" to "یا", "ye" to "یے", "yi" to "یی", "yo" to "یو", "yu" to "یو",
        "haan" to "ہاں", "nahi" to "نہیں", "theek" to "ٹھیک", "bilkul" to "بالکل",
        "kya" to "کیا", "kaise" to "کیسے", "kab" to "کب", "kahan" to "کہاں",
        "kaun" to "کون", "kyun" to "کیوں", "kitna" to "کتنا", "kaisa" to "کیسا",
        "mera" to "میرا", "tera" to "تیرا", "uska" to "اسکا", "hamara" to "ہمارا",
        "tumhara" to "تمہارا", "apka" to "آپکا",
        "mein" to "میں", "tum" to "تم", "aap" to "آپ", "wo" to "وہ",
        "hum" to "ہم", "yeh" to "یہ", "woh" to "وہ", "koi" to "کوئی",
        "sab" to "سب", "kuch" to "کچھ", "bahut" to "بہت", "thora" to "تھوڑا",
        "acha" to "اچھا", "bura" to "برا", "bara" to "بڑا", "chota" to "چھوٹا",
        "naya" to "نیا", "purana" to "پرانا", "sasta" to "سستا", "mehnga" to "مہنگا",
        "garam" to "گرم", "thanda" to "ٹھنڈا", "tez" to "تیز", "dheema" to "دھیما",
        "aana" to "آنا", "jana" to "جانا", "khana" to "کھانا", "peena" to "پینا",
        "sona" to "سونا", "uthna" to "اٹھنا", "baithna" to "بیٹھنا", "chalna" to "چلنا",
        "daina" to "دینا", "lena" to "لینا", "karna" to "کرنا", "hona" to "ہونا",
        "dekho" to "دیکھو", "sunno" to "سنو", "bolo" to "بولو", "chup" to "چپ",
        "shukriya" to "شکریہ", "meherbani" to "مہربانی", "maaf" to "معاف",
        "khuda" to "خدا", "hafiz" to "حافظ", "adab" to "ادب", "salam" to "سلام"
    )

    private var currentRomanBuffer = StringBuilder()
    private var currentSuggestions: List<String> = emptyList()
    private var imeAction: Int = EditorInfo.IME_ACTION_UNSPECIFIED

    // Track the last word typed for next-word prediction
    private var lastTypedWord: String = ""

    fun setImeAction(action: Int) {
        if (imeAction != action) {
            imeAction = action
            postInvalidateOnAnimation()
        }
    }

    fun refreshSoundEngineTune() {
        soundEngine.refreshTuneIfChanged()
    }

    /** Refresh all settings from SharedPreferences and rebuild the keyboard. */
    fun refreshSettings() {
        animationEngine = AnimationEngine(settings.animationType)
        createKeyMap(width, height)
        soundEngine.refreshTuneIfChanged()
        postInvalidateOnAnimation()
    }

    fun setOnCustomKeyListener(listener: OnKeyListener) {
        this.keyListener = listener
    }

    fun release() {
        handler.removeCallbacks(backspaceRunnable ?: Runnable {})
        handler.removeCallbacks(capsLockRunnable ?: Runnable {})
        soundEngine.release()
    }

    companion object {
        private const val TAG = "KeyboardView"
    }

    private val density = context.resources.displayMetrics.density
    private fun dp(value: Float): Float = value * density

    private val horizontalKeyGapDp = 3.2f
    private val verticalRowGapDp = 6f
    private val sideMarginDp = 3f
    private val topBottomMarginDp = 4f
    private val keyCornerRadiusDp = 5f

    private val keyboardHeightFraction = 0.35f
    private val landscapeHeightFraction = 0.30f

    private val keyPaint = Paint()
    private val keyBorderPaint = Paint()
    private val stripBgPaint = Paint().apply { color = Color.parseColor("#05050F"); isAntiAlias = true }
    private val textPaint = Paint()
    private var lastFrameTime = 0L
    private var glowPulse = 0.5f
    private var glowDirection = -1
    private val glowPaint = Paint()
    private val pressedKeys = mutableMapOf<String, Long>()
    private val keyStates = mutableMapOf<String, KeyState>()
    private val ripples = mutableListOf<RippleEffect>()
    private val activePointers = mutableMapOf<Int, String>()
    private val pointerPopups = mutableMapOf<Int, PopupEffect>()
    private var primaryPointerId = -1
    private var lastSwipeKeyLabel: String? = null
    private val pendingClickRunnables = mutableMapOf<Int, Runnable>()
    private val clickSoundDelayMs = 40L
    private val popupPaint = Paint()
    private val popupBorderPaint = Paint()
    private val popupTextPaint = Paint()

    // FIX: Keyboard layouts – Number Row toggle now controls whether row 0 is shown
    private val baseLetterLayout = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("Shift", "z", "x", "c", "v", "b", "n", "m", "Del"),
        listOf("123", "Emoji", "Space", ".", "Go")
    )

    private val numberRow = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

    private val letterLayout: List<List<String>>
        get() = if (settings.numberRowEnabled) {
            listOf(numberRow) + baseLetterLayout
        } else {
            baseLetterLayout
        }

    private val numberLayout = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/"),
        listOf("*", "\"", "'", ":", ";", "!", "?"),
        listOf("=\\<", "%", "^", "[", "]", "{", "}", "Del"),
        listOf("ABC", "Emoji", ",", "Space", ".", "Go")
    )

    private val extendedSymbolLayout = listOf(
        listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "Δ"),
        listOf("£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\"),
        listOf("©", "®", "™", "✓", "[", "]", "<", ">"),
        listOf("123", "_", "-", "+", "(", ")", "/", "Del"),
        listOf("ABC", "Emoji", ",", "Space", ".", "Go")
    )

    private var currentLayout = letterLayout
    private var isShifted = false
    private var isCapsLocked = false
    private val keyMap = mutableMapOf<String, Rect>()
    private var lastKeyTime = 0L
    private val debounceInterval = 25L
    private var touchStartX = 0f
    private var touchStartY = 0f
    private val swipeThreshold = 50f
    private var isSwiping = false
    private var isLongPress = false
    private var longPressKey: String? = null
    private var capsLockJustActivated = false

    // Floating keyboard
    private var floatingOffsetX = 0f
    private var floatingOffsetY = 0f
    private var isFloatingDragging = false
    private var dragStartFloatingX = 0f
    private var dragStartFloatingY = 0f

    init {
        setWillNotDraw(false)
        setBackgroundColor(Color.BLACK)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = "KeyAura keyboard"

        keyPaint.color = Color.parseColor("#08080F")
        keyPaint.isAntiAlias = true
        keyPaint.style = Paint.Style.FILL
        keyBorderPaint.color = Color.parseColor("#1A1A2A")
        keyBorderPaint.isAntiAlias = true
        keyBorderPaint.style = Paint.Style.STROKE
        keyBorderPaint.strokeWidth = dp(1.5f)
        textPaint.color = Color.WHITE
        textPaint.textSize = dp(15f)
        textPaint.isAntiAlias = true
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.DEFAULT_BOLD
        popupPaint.color = Color.parseColor("#1E1E30")
        popupPaint.isAntiAlias = true
        glowPaint.isAntiAlias = true
        popupBorderPaint.color = Color.WHITE
        popupBorderPaint.isAntiAlias = true
        popupBorderPaint.style = Paint.Style.STROKE
        popupBorderPaint.strokeWidth = dp(2f)
        popupTextPaint.color = Color.WHITE
        popupTextPaint.textSize = dp(30f)
        popupTextPaint.isAntiAlias = true
        popupTextPaint.textAlign = Paint.Align.CENTER
        popupTextPaint.isFakeBoldText = true

        refreshSettings()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val dm = context.resources.displayMetrics
        val isLandscape = dm.widthPixels > dm.heightPixels

        // FIX: Apply keyboard height from settings
        val heightPercent = settings.keyboardHeightPercent / 100f
        val desiredHeight = if (isLandscape) {
            (dm.heightPixels * landscapeHeightFraction).toInt()
        } else {
            (dm.heightPixels * heightPercent).toInt()
        }
        super.onMeasure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(desiredHeight, View.MeasureSpec.EXACTLY)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        createKeyMap(w, h)
    }

    private fun createKeyMap(width: Int, height: Int) {
        if (width <= 0 || height <= 0 || currentLayout.isEmpty()) {
            Log.w(TAG, "createKeyMap skipped: invalid dimensions ($width x $height)")
            return
        }
        try {
            buildKeyMapInternal(width, height)
        } catch (e: Exception) {
            Log.e(TAG, "createKeyMap failed: ${e.message}")
        }
    }

    private val suggestionStripHeightFraction = 0.15f
    private var lastStripHeightPx = 0

    private fun buildKeyMapInternal(width: Int, height: Int) {
        keyMap.clear()

        val stripHeightPx = (height * suggestionStripHeightFraction).toInt()
        lastStripHeightPx = stripHeightPx
        val usableHeight = height - stripHeightPx

        val sideMargin = dp(sideMarginDp).toInt()
        val topBottomMargin = dp(topBottomMarginDp).toInt()

        // Apply key spacing from settings
        val spacingMultiplier = settings.keySpacing
        val hGap = (dp(horizontalKeyGapDp) * spacingMultiplier).toInt()
        val vGap = (dp(verticalRowGapDp) * spacingMultiplier).toInt()

        val rowCount = currentLayout.size
        val availableHeight = usableHeight - (topBottomMargin * 2) - (vGap * (rowCount - 1))
        val rowHeight = availableHeight / rowCount

        var currentY = stripHeightPx + topBottomMargin

        for ((rowIndex, row) in currentLayout.withIndex()) {
            val isHomeRow = currentLayout == letterLayout && rowIndex == (if (settings.numberRowEnabled) 2 else 1)
            val rowSideMargin = if (isHomeRow) dp(sideMarginDp * 4f).toInt() else sideMargin
            val availableRowWidth = width - (rowSideMargin * 2) - (hGap * (row.size - 1))
            var totalWeight = 0.0
            for (item in row) {
                totalWeight += getWeight(item).toDouble()
            }
            val tw = totalWeight.toFloat()
            var currentX = rowSideMargin

            for ((keyIndex, keyLabel) in row.withIndex()) {
                val isLastKeyInRow = keyIndex == row.lastIndex
                val kw = (availableRowWidth * (getWeight(keyLabel) / tw)).roundToInt()
                val safeRight = if (isLastKeyInRow) (width - rowSideMargin) else (currentX + kw)
                keyMap[keyLabel] = Rect(currentX, currentY, safeRight, currentY + rowHeight)
                keyStates[keyLabel] = KeyState.NORMAL
                currentX = safeRight + hGap
            }
            currentY += rowHeight + vGap
        }

        layoutSuggestionStrip(width, stripHeightPx)
    }

    private fun layoutSuggestionStrip(width: Int, stripHeightPx: Int) {
        val sideMargin = dp(sideMarginDp).toInt()
        val hGap = dp(horizontalKeyGapDp).toInt()
        val stripTop = dp(2f).toInt()
        val stripBottom = stripHeightPx - dp(2f).toInt()

        val urduKeyWidth = dp(28f).toInt()
        val urduRight = sideMargin + urduKeyWidth
        keyMap["Urdu"] = Rect(sideMargin, stripTop, urduRight, stripBottom)
        keyStates.putIfAbsent("Urdu", KeyState.NORMAL)

        val clipboardKeyWidth = dp(28f).toInt()
        val clipboardLeft = urduRight + hGap
        val clipboardRight = clipboardLeft + clipboardKeyWidth
        keyMap["Clipboard"] = Rect(clipboardLeft, stripTop, clipboardRight, stripBottom)
        keyStates.putIfAbsent("Clipboard", KeyState.NORMAL)

        val gameKeyWidth = dp(28f).toInt()
        val gameLeft = clipboardRight + hGap
        val gameRight = gameLeft + gameKeyWidth
        keyMap["Game"] = Rect(gameLeft, stripTop, gameRight, stripBottom)
        keyStates.putIfAbsent("Game", KeyState.NORMAL)

        val micKeyWidth = dp(32f).toInt()
        val micLeft = width - sideMargin - micKeyWidth
        keyMap["Mic"] = Rect(micLeft, stripTop, width - sideMargin, stripBottom)
        keyStates.putIfAbsent("Mic", KeyState.NORMAL)

        val chipsLeft = gameRight + hGap
        val chipsAvailableWidth = micLeft - hGap - chipsLeft
        if (chipsAvailableWidth <= 0) return

        val clipDisplay = pendingClipboardDisplay
        if (clipDisplay != null) {
            keyMap["clipSugg"] = Rect(chipsLeft, stripTop, chipsLeft + chipsAvailableWidth, stripBottom)
            keyStates.putIfAbsent("clipSugg", KeyState.NORMAL)
            return
        }

        val suggestions = currentSuggestions
        if (suggestions.isEmpty()) return
        val chipWidth = (chipsAvailableWidth - hGap * (suggestions.size - 1)) / suggestions.size
        var x = chipsLeft
        for (i in suggestions.indices) {
            val label = "sugg$i"
            keyMap[label] = Rect(x, stripTop, x + chipWidth, stripBottom)
            keyStates.putIfAbsent(label, KeyState.NORMAL)
            x += chipWidth + hGap
        }
    }

    private fun getWeight(label: String): Float {
        return when (label) {
            "Space" -> 3.5f
            "Shift", "Del", "123", "ABC", "Go" -> 1.4f
            "=\\<" -> 1.6f
            "Emoji" -> 1.0f
            "Urdu" -> 1.0f
            else -> 1.0f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.currentTimeMillis()
        val dt = if (lastFrameTime == 0L) 16 else now - lastFrameTime
        lastFrameTime = now

        // Apply background opacity from settings
        val bgAlpha = (255 * settings.backgroundOpacity).toInt()
        canvas.drawColor(Color.argb(bgAlpha, 0, 0, 0))

        // Apply floating keyboard offset if enabled
        if (settings.floatingEnabled) {
            canvas.save()
            canvas.translate(floatingOffsetX, floatingOffsetY)
        }

        drawCoolGlow(canvas)

        try {
            animationEngine.update(dt)
            animationEngine.draw(canvas)
            updateRipples(canvas, dt)
            updateKeyStates()
            if (lastStripHeightPx > 0) {
                canvas.drawRect(0f, 0f, width.toFloat(), lastStripHeightPx.toFloat(), stripBgPaint)
            }
            for ((label, rect) in keyMap) {
                drawKey(canvas, label, rect)
            }
            for (popup in pointerPopups.values) popup.draw(canvas)
            if (settings.floatingEnabled) {
                canvas.restore()
                drawFloatingHandle(canvas)
            }
            postInvalidateOnAnimation()
        } catch (e: Exception) {
            Log.e(TAG, "Rendering error: ${e.message}")
            drawFallbackKeys(canvas)
            if (settings.floatingEnabled) canvas.restore()
        }
    }

    private fun drawFloatingHandle(canvas: Canvas) {
        val handlePaint = Paint().apply {
            color = Color.parseColor("#4488FF")
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val handleWidth = dp(40f)
        val handleHeight = dp(4f)
        val cx = width / 2f
        val cy = dp(8f)
        canvas.drawRoundRect(
            cx - handleWidth / 2, cy,
            cx + handleWidth / 2, cy + handleHeight,
            dp(2f), dp(2f), handlePaint
        )
    }

    private fun drawFallbackKeys(canvas: Canvas) {
        try {
            for ((label, rect) in keyMap) {
                canvas.drawRect(rect, keyPaint)
                canvas.drawText(
                    label, rect.exactCenterX(),
                    rect.exactCenterY() + (textPaint.textSize / 3f), textPaint
                )
            }
        } catch (_: Exception) { }
    }

    private fun drawCoolGlow(canvas: Canvas) {
        glowPulse += glowDirection * 0.004f
        if (glowPulse <= 0.25f || glowPulse >= 0.55f) {
            glowDirection *= -1
        }
        val cx = width / 2f
        val cy = height.toFloat()
        val a1 = (70 * glowPulse).toInt()
        val a2 = (35 * glowPulse).toInt()

        // Apply theme colors to glow
        val primary = settings.primaryColor
        val accent = settings.accentColor
        val colors = intArrayOf(
            Color.argb(a1, Color.red(primary), Color.green(primary), Color.blue(primary)),
            Color.argb(a2, Color.red(accent), Color.green(accent), Color.blue(accent)),
            Color.TRANSPARENT
        )
        val pos = floatArrayOf(0f, 0.55f, 1f)
        glowPaint.shader = android.graphics.RadialGradient(
            cx, cy, width * 0.75f, colors, pos, android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glowPaint)
    }

    private fun updateRipples(canvas: Canvas, dt: Long) {
        val it = ripples.iterator()
        while (it.hasNext()) {
            val r = it.next()
            r.update(dt)
            r.draw(canvas)
            if (r.finished) it.remove()
        }
    }

    private fun updateKeyStates() {
        val now = System.currentTimeMillis()
        val entries = pressedKeys.entries.toList()
        for (entry in entries) {
            val elapsed = now - entry.value
            val ns = when {
                elapsed < 70 -> KeyState.WHITE
                elapsed < 140 -> KeyState.PINK
                elapsed < 210 -> KeyState.PINK
                elapsed < 410 -> KeyState.FADE
                else -> KeyState.NORMAL
            }
            keyStates[entry.key] = ns
            if (elapsed >= 410) pressedKeys.remove(entry.key)
        }
    }

    private fun drawKey(canvas: Canvas, label: String, rect: Rect) {
        val state = keyStates[label] ?: KeyState.NORMAL

        // Apply theme colors to key background
        val primary = settings.primaryColor
        val accent = settings.accentColor

        when (state) {
            KeyState.WHITE -> {
                keyPaint.color = Color.WHITE
                textPaint.color = Color.BLACK
                keyPaint.setShadowLayer(35f, 0f, 0f, Color.WHITE)
            }
            KeyState.PINK -> {
                keyPaint.color = accent
                textPaint.color = Color.WHITE
                keyPaint.setShadowLayer(28f, 0f, 0f, accent)
            }
            KeyState.FADE -> {
                keyPaint.color = primary
                textPaint.color = Color.WHITE
                keyPaint.setShadowLayer(22f, 0f, 0f, primary)
            }
            KeyState.NORMAL -> {
                if (label == "Urdu" && settings.urduEnabled) {
                    keyPaint.color = Color.parseColor("#2255CC")
                    textPaint.color = Color.WHITE
                } else if (label == "Mic" && isListeningForSpeech) {
                    keyPaint.color = Color.parseColor("#CC2244")
                    textPaint.color = Color.WHITE
                } else if (label.startsWith("sugg")) {
                    keyPaint.color = Color.parseColor("#15152A")
                    textPaint.color = Color.WHITE
                } else {
                    keyPaint.color = Color.parseColor("#08080F")
                    textPaint.color = Color.WHITE
                }
                keyPaint.clearShadowLayer()
            }
        }

        val l = rect.left.toFloat()
        val t = rect.top.toFloat()
        val r = rect.right.toFloat()
        val b = rect.bottom.toFloat()

        val keyMarginH = ((r - l) * 0.05f)
        val keyMarginV = ((b - t) * 0.05f)

        // Apply key shape from settings
        val cornerRadius = when (settings.keyShape) {
            "squircle" -> dp(keyCornerRadiusDp * 1.5f)
            "circle" -> minOf(rect.width(), rect.height()) / 2f
            "diamond" -> dp(keyCornerRadiusDp * 0.3f)
            else -> dp(keyCornerRadiusDp) // rounded
        }

        // Apply theme colors to key border
        keyBorderPaint.color = settings.primaryColor

        canvas.drawRoundRect(l + keyMarginH, t + keyMarginV, r - keyMarginH, b - keyMarginV, cornerRadius, cornerRadius, keyPaint)
        canvas.drawRoundRect(l + keyMarginH, t + keyMarginV, r - keyMarginH, b - keyMarginV, cornerRadius, cornerRadius, keyBorderPaint)

        val dl = if (isShifted && label.length == 1 && label[0].isLetter()) label.uppercase() else label

        when (label) {
            "Shift" -> drawShiftIcon(canvas, rect, textPaint.color)
            "Del" -> drawBackspaceIcon(canvas, rect, textPaint.color)
            "Emoji" -> drawEmojiGlyph(canvas, rect)
            "Go" -> drawEnterIcon(canvas, rect, textPaint.color)
            "Urdu" -> {
                val p = Paint(textPaint).apply { textSize = dp(10f) }
                canvas.drawText("اردو", rect.exactCenterX(), rect.exactCenterY() + (p.textSize / 3f), p)
            }
            "Clipboard" -> drawClipboardIcon(canvas, rect, textPaint.color)
            "Game" -> drawGameIcon(canvas, rect, textPaint.color)
            "Mic" -> drawMicIcon(canvas, rect, textPaint.color)
            else -> if (label == "clipSugg") {
                drawFittedChipText(canvas, pendingClipboardDisplay ?: "", rect)
            } else if (label.startsWith("sugg")) {
                val index = label.removePrefix("sugg").toIntOrNull()
                if (index != null && index < currentSuggestions.size) {
                    drawFittedChipText(canvas, currentSuggestions[index], rect)
                } else {
                    canvas.drawText(dl, rect.exactCenterX(), rect.exactCenterY() + (textPaint.textSize / 3f), textPaint)
                }
            } else {
                canvas.drawText(dl, rect.exactCenterX(), rect.exactCenterY() + (textPaint.textSize / 3f), textPaint)
            }
        }
    }

    // ==================== Icon drawing helpers ====================

    private fun drawShiftIcon(canvas: Canvas, rect: Rect, color: Int) {
        val cx = rect.exactCenterX()
        val cy = rect.exactCenterY()
        val p = Paint().apply {
            this.color = color
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = dp(2.5f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val s = dp(8f)
        // Up arrow
        canvas.drawLine(cx, cy + s, cx, cy - s, p)
        canvas.drawLine(cx - s, cy, cx, cy - s, p)
        canvas.drawLine(cx + s, cy, cx, cy - s, p)
        // If caps lock, add underline
        if (isCapsLocked) {
            p.strokeWidth = dp(2f)
            canvas.drawLine(cx - s * 0.7f, cy + s * 1.3f, cx + s * 0.7f, cy + s * 1.3f, p)
        }
    }

    private fun drawBackspaceIcon(canvas: Canvas, rect: Rect, color: Int) {
        val cx = rect.exactCenterX()
        val cy = rect.exactCenterY()
        val p = Paint().apply {
            this.color = color
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = dp(2.5f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val s = dp(9f)
        // Backspace shape: left-pointing arrow with cross
        val path = Path()
        path.moveTo(cx + s, cy - s * 0.6f)
        path.lineTo(cx - s * 0.5f, cy - s * 0.6f)
        path.lineTo(cx - s * 0.5f, cy - s * 1.1f)
        path.lineTo(cx - s * 1.2f, cy)
        path.lineTo(cx - s * 0.5f, cy + s * 1.1f)
        path.lineTo(cx - s * 0.5f, cy + s * 0.6f)
        path.lineTo(cx + s, cy + s * 0.6f)
        path.close()
        canvas.drawPath(path, p)
        // Cross
        p.strokeWidth = dp(2f)
        canvas.drawLine(cx + s * 0.3f, cy - s * 0.4f, cx + s * 0.9f, cy + s * 0.4f, p)
        canvas.drawLine(cx + s * 0.9f, cy - s * 0.4f, cx + s * 0.3f, cy + s * 0.4f, p)
    }

    private fun drawEmojiGlyph(canvas: Canvas, rect: Rect) {
        val cx = rect.exactCenterX()
        val cy = rect.exactCenterY()
        val p = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            textSize = dp(18f)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("\uD83D\uDE00", cx, cy + p.textSize / 3f, p) // grinning face
    }

    private fun drawEnterIcon(canvas: Canvas, rect: Rect, color: Int) {
        val cx = rect.exactCenterX()
        val cy = rect.exactCenterY()
        val p = Paint().apply {
            this.color = color
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = dp(2.5f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val s = dp(8f)
        // Carriage return with arrow
        canvas.drawLine(cx - s, cy - s, cx - s, cy + s * 0.5f, p)
        canvas.drawLine(cx - s, cy + s * 0.5f, cx + s * 0.5f, cy + s * 0.5f, p)
        canvas.drawLine(cx + s * 0.5f, cy + s * 0.5f, cx + s * 0.5f, cy - s * 0.5f, p)
        // Arrow tip
        canvas.drawLine(cx + s * 0.5f, cy - s * 0.5f, cx + s * 0.2f, cy - s * 0.2f, p)
        canvas.drawLine(cx + s * 0.5f, cy - s * 0.5f, cx + s * 0.8f, cy - s * 0.2f, p)
    }

    private fun drawClipboardIcon(canvas: Canvas, rect: Rect, color: Int) {
        val cx = rect.exactCenterX()
        val cy = rect.exactCenterY()
        val p = Paint().apply {
            this.color = color
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = dp(2f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val s = dp(8f)
        // Rectangular clipboard with top tab
        canvas.drawRect(cx - s * 0.8f, cy - s * 0.6f, cx + s * 0.8f, cy + s * 0.8f, p)
        // Top tab
        canvas.drawRect(cx - s * 0.3f, cy - s * 0.9f, cx + s * 0.3f, cy - s * 0.6f, p)
        // Small handle
        canvas.drawCircle(cx, cy - s * 0.9f, dp(2f), p)
    }

    private fun drawGameIcon(canvas: Canvas, rect: Rect, color: Int) {
        val cx = rect.exactCenterX()
        val cy = rect.exactCenterY()
        val p = Paint().apply {
            this.color = color
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = dp(2f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val s = dp(8f)
        // D-pad or gamepad cross
        canvas.drawLine(cx - s, cy, cx + s, cy, p)
        canvas.drawLine(cx, cy - s, cx, cy + s, p)
        // Round corners
        canvas.drawCircle(cx - s, cy, dp(2f), p)
        canvas.drawCircle(cx + s, cy, dp(2f), p)
        canvas.drawCircle(cx, cy - s, dp(2f), p)
        canvas.drawCircle(cx, cy + s, dp(2f), p)
    }

    private fun drawMicIcon(canvas: Canvas, rect: Rect, color: Int) {
        val cx = rect.exactCenterX()
        val cy = rect.exactCenterY()
        val p = Paint().apply {
            this.color = color
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = dp(2.5f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val s = dp(7f)
        // Mic body: oval
        canvas.drawOval(cx - s * 0.5f, cy - s * 0.9f, cx + s * 0.5f, cy + s * 0.5f, p)
        // Bottom line
        canvas.drawLine(cx, cy + s * 0.5f, cx, cy + s * 0.9f, p)
        // Side lines
        canvas.drawLine(cx - s * 0.8f, cy + s * 0.2f, cx + s * 0.8f, cy + s * 0.2f, p)
        // If listening, fill color
        if (isListeningForSpeech) {
            p.style = Paint.Style.FILL
            p.alpha = 80
            canvas.drawOval(cx - s * 0.5f, cy - s * 0.9f, cx + s * 0.5f, cy + s * 0.5f, p)
        }
    }

    private fun drawFittedChipText(canvas: Canvas, text: String, rect: Rect) {
        val p = Paint(textPaint).apply {
            textSize = dp(13f)
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        val maxWidth = (rect.width() - dp(8f)).toFloat()
        val display = if (p.measureText(text) > maxWidth) {
            var end = text.length
            while (end > 1 && p.measureText(text.substring(0, end) + "\u2026") > maxWidth) end--
            text.substring(0, end) + "\u2026"
        } else {
            text
        }
        canvas.drawText(display, rect.exactCenterX(), rect.exactCenterY() + p.textSize / 3f, p)
    }

    // ==================== Ripple and Popup classes ====================

    private inner class RippleEffect(
        private val centerX: Float,
        private val centerY: Float,
        private val maxRadius: Float = 150f,
        private val durationMs: Long = 400L
    ) {
        var finished = false
        private var startTime = System.currentTimeMillis()
        private val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.parseColor("#4488FF")
        }

        fun update(dt: Long) {
            val progress = (System.currentTimeMillis() - startTime) / durationMs.toFloat()
            if (progress >= 1f) {
                finished = true
            }
        }

        fun draw(canvas: Canvas) {
            if (finished) return
            val progress = (System.currentTimeMillis() - startTime) / durationMs.toFloat()
            val radius = maxRadius * progress
            val alpha = ((1 - progress) * 255).toInt()
            paint.alpha = alpha
            canvas.drawCircle(centerX, centerY, radius, paint)
        }
    }

    private inner class PopupEffect(
        private val centerX: Float,
        private val centerY: Float,
        private val label: String
    ) {
        var finished = false
        private var startTime = System.currentTimeMillis()
        private val durationMs = 300L

        fun update(dt: Long) {
            if (System.currentTimeMillis() - startTime > durationMs) {
                finished = true
            }
        }

        fun draw(canvas: Canvas) {
            if (finished) return
            val progress = (System.currentTimeMillis() - startTime) / durationMs.toFloat()
            val alpha = ((1 - progress) * 255).toInt()
            val scale = 1f + 0.2f * (1 - progress)
            canvas.save()
            canvas.translate(centerX, centerY)
            canvas.scale(scale, scale)
            val bgRect = RectF(-dp(30f), -dp(20f), dp(30f), dp(20f))
            canvas.drawRoundRect(bgRect, dp(8f), dp(8f), popupPaint)
            canvas.drawRoundRect(bgRect, dp(8f), dp(8f), popupBorderPaint)
            popupTextPaint.alpha = alpha
            canvas.drawText(label, 0f, popupTextPaint.textSize / 3f, popupTextPaint)
            canvas.restore()
        }
    }

    // ==================== Touch handling ====================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (settings.floatingEnabled && y < dp(20f)) {
                    isFloatingDragging = true
                    dragStartFloatingX = x - floatingOffsetX
                    dragStartFloatingY = y - floatingOffsetY
                    return true
                }
                val key = findKeyAt(x, y) ?: return true
                activePointers[pointerId] = key
                if (primaryPointerId == -1) primaryPointerId = pointerId
                if (key == "Shift") {
                    // Handle long press for caps lock
                    handler.postDelayed({
                        if (activePointers.containsKey(pointerId) && activePointers[pointerId] == "Shift") {
                            isCapsLocked = !isCapsLocked
                            isShifted = isCapsLocked
                            capsLockJustActivated = true
                            postInvalidateOnAnimation()
                            triggerKeyHaptic()
                        }
                    }, 400)
                } else if (key == "Del") {
                    // Long press for continuous delete
                    handler.postDelayed({
                        if (activePointers.containsKey(pointerId) && activePointers[pointerId] == "Del") {
                            // Start repeating delete
                            backspaceRunnable = object : Runnable {
                                override fun run() {
                                    if (activePointers.containsKey(pointerId) && activePointers[pointerId] == "Del") {
                                        keyListener?.onKey(-5, "Del")
                                        handler.postDelayed(this, 100)
                                    }
                                }
                            }
                            handler.post(backspaceRunnable!!)
                        }
                    }, 300)
                }
                pressedKeys[key] = System.currentTimeMillis()
                keyStates[key] = KeyState.WHITE
                postInvalidateOnAnimation()
                triggerKeyHaptic()
                // Play sound
                if (settings.soundEnabled) {
                    soundEngine.playClick()
                }
                // Trigger animation
                animationEngine.triggerAnimation(x, y, key)
                // Show popup
                if (!key.startsWith("sugg") && !key.startsWith("clipSugg")) {
                    pointerPopups[pointerId] = PopupEffect(x, y - dp(40f), key)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isFloatingDragging) {
                    floatingOffsetX = x - dragStartFloatingX
                    floatingOffsetY = y - dragStartFloatingY
                    postInvalidateOnAnimation()
                    return true
                }
                // Gesture detection for swipe typing
                val key = findKeyAt(x, y)
                if (key != null && key != activePointers[pointerId]) {
                    // Check if swipe distance is enough
                    if (pointerId == primaryPointerId) {
                        isSwiping = true
                    }
                    activePointers[pointerId] = key
                    pressedKeys[key] = System.currentTimeMillis()
                    keyStates[key] = KeyState.WHITE
                    postInvalidateOnAnimation()
                    // Play swipe tone
                    if (settings.soundEnabled) {
                        val noteIndex = key.hashCode() % 10
                        soundEngine.playSwipeTone(noteIndex)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (isFloatingDragging) {
                    isFloatingDragging = false
                    return true
                }
                val key = activePointers[pointerId] ?: return true
                activePointers.remove(pointerId)
                if (pointerId == primaryPointerId) {
                    primaryPointerId = -1
                    isSwiping = false
                }
                // Cancel long press if not triggered
                handler.removeCallbacksAndMessages(null)
                // If key is Shift and caps lock not just activated, toggle shift
                if (key == "Shift" && !capsLockJustActivated) {
                    isShifted = !isShifted
                    if (!isShifted) isCapsLocked = false
                    postInvalidateOnAnimation()
                }
                capsLockJustActivated = false
                // If not swiping, trigger key press
                if (!isSwiping) {
                    keyListener?.onKey(keyCodeForLabel(key), key)
                }
                // Remove popup
                pointerPopups.remove(pointerId)
                // Clear pressed state after delay
                handler.postDelayed({
                    keyStates[key] = KeyState.NORMAL
                    postInvalidateOnAnimation()
                }, 200)
                // Reset swipe flag
                isSwiping = false
            }
            MotionEvent.ACTION_CANCEL -> {
                isFloatingDragging = false
                activePointers.clear()
                primaryPointerId = -1
                isSwiping = false
                handler.removeCallbacksAndMessages(null)
                pointerPopups.clear()
                postInvalidateOnAnimation()
            }
        }
        return true
    }

    private fun findKeyAt(x: Float, y: Float): String? {
        for ((label, rect) in keyMap) {
            if (rect.contains(x.toInt(), y.toInt())) {
                return label
            }
        }
        return null
    }

    private fun keyCodeForLabel(label: String): Int {
        return when (label) {
            "Space" -> 32
            "Shift" -> -1
            "Del" -> -5
            "Go" -> -4
            "123" -> -2
            "ABC" -> -3
            "Emoji" -> -9
            "Clipboard" -> -10
            "Mic" -> -11
            "Game" -> -14
            "Urdu" -> -12
            else -> if (label.length == 1) label[0].toInt() else 0
        }
    }
}
