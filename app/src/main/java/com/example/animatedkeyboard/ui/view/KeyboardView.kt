package com.example.animatedkeyboard.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
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

    // Clipboard suggestion: store full text and a display-friendly truncated version
    private var pendingClipboardFull: String? = null
    private var pendingClipboardDisplay: String? = null

    fun showClipboardSuggestion(fullText: String) {
        pendingClipboardFull = fullText
        pendingClipboardDisplay = truncateForDisplay(fullText)
        createKeyMap(width, height)
    }

    private fun truncateForDisplay(text: String): String {
        // Show first word if short, else first 30 characters
        val firstWord = text.split(Regex("\\s+")).firstOrNull() ?: text
        return if (firstWord.length <= 30) firstWord else firstWord.take(30) + "…"
    }

    // Speech listening state
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

    fun setImeAction(action: Int) {
        if (imeAction != action) {
            imeAction = action
            postInvalidateOnAnimation()
        }
    }

    fun refreshSoundEngineTune() {
        soundEngine.refreshTuneIfChanged()
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
    private val spaceRowHeightFactor = 1.0f

    private val keyPaint = Paint()
    private val keyBorderPaint = Paint()
    private val stripBgPaint = Paint().apply { color = Color.parseColor("#050505"); isAntiAlias = true }
    private val textPaint = Paint()
    private val animationEngine = AnimationEngine()
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

    private val letterLayout = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("Shift", "z", "x", "c", "v", "b", "n", "m", "Del"),
        listOf("123", "Emoji", "Space", ".", "Go")
    )

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
    private val keyCodes = mutableMapOf<String, Int>()
    private var lastKeyTime = 0L
    private val debounceInterval = 25L
    private var touchStartX = 0f
    private var touchStartY = 0f
    private val swipeThreshold = 50f
    private var isSwiping = false
    private var isLongPress = false
    private var longPressKey: String? = null
    private var capsLockJustActivated = false

    init {
        setWillNotDraw(false)
        setBackgroundColor(Color.BLACK)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = "KeyAura keyboard"

        keyPaint.color = Color.parseColor("#080808")
        keyPaint.isAntiAlias = true
        keyPaint.style = Paint.Style.FILL
        keyBorderPaint.color = Color.parseColor("#1A1A1A")
        keyBorderPaint.isAntiAlias = true
        keyBorderPaint.style = Paint.Style.STROKE
        keyBorderPaint.strokeWidth = dp(1f)
        textPaint.color = Color.WHITE
        textPaint.textSize = dp(15f)
        textPaint.isAntiAlias = true
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.DEFAULT_BOLD
        popupPaint.color = Color.parseColor("#1E1E1E")
        popupPaint.isAntiAlias = true
        glowPaint.isAntiAlias = true
        popupBorderPaint.color = Color.WHITE
        popupBorderPaint.isAntiAlias = true
        popupBorderPaint.style = Paint.Style.STROKE
        popupBorderPaint.strokeWidth = dp(1.5f)
        popupTextPaint.color = Color.WHITE
        popupTextPaint.textSize = dp(30f)
        popupTextPaint.isAntiAlias = true
        popupTextPaint.textAlign = Paint.Align.CENTER
        popupTextPaint.isFakeBoldText = true

        keyCodes["Shift"] = -1
        keyCodes["Del"] = -5
        keyCodes["Go"] = -4
        keyCodes["Space"] = 32
        keyCodes["123"] = -2
        keyCodes["ABC"] = -3
        keyCodes["Emoji"] = -9
        keyCodes["=\\<"] = -7
        keyCodes["Urdu"] = -8
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val dm = context.resources.displayMetrics
        val isLandscape = dm.widthPixels > dm.heightPixels
        val desiredHeight = if (isLandscape) {
            (dm.heightPixels * landscapeHeightFraction).toInt()
        } else {
            (dm.heightPixels * keyboardHeightFraction).toInt()
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
        val hGap = dp(horizontalKeyGapDp).toInt()
        val vGap = dp(verticalRowGapDp).toInt()

        val rowCount = currentLayout.size
        val availableHeight = usableHeight - (topBottomMargin * 2) - (vGap * (rowCount - 1))
        val rowHeight = availableHeight / rowCount

        var currentY = stripHeightPx + topBottomMargin

        for ((rowIndex, row) in currentLayout.withIndex()) {
            val isHomeRow = currentLayout === letterLayout && rowIndex == 2
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

        // Urdu key
        val urduKeyWidth = dp(28f).toInt()
        val urduRight = sideMargin + urduKeyWidth
        keyMap["Urdu"] = Rect(sideMargin, stripTop, urduRight, stripBottom)
        keyStates.putIfAbsent("Urdu", KeyState.NORMAL)

        // Clipboard key
        val clipboardKeyWidth = dp(28f).toInt()
        val clipboardLeft = urduRight + hGap
        val clipboardRight = clipboardLeft + clipboardKeyWidth
        keyMap["Clipboard"] = Rect(clipboardLeft, stripTop, clipboardRight, stripBottom)
        keyStates.putIfAbsent("Clipboard", KeyState.NORMAL)

        // Game key
        val gameKeyWidth = dp(28f).toInt()
        val gameLeft = clipboardRight + hGap
        val gameRight = gameLeft + gameKeyWidth
        keyMap["Game"] = Rect(gameLeft, stripTop, gameRight, stripBottom)
        keyStates.putIfAbsent("Game", KeyState.NORMAL)

        // Mic key
        val micKeyWidth = dp(32f).toInt()
        val micLeft = width - sideMargin - micKeyWidth
        keyMap["Mic"] = Rect(micLeft, stripTop, width - sideMargin, stripBottom)
        keyStates.putIfAbsent("Mic", KeyState.NORMAL)

        // Clipboard suggestion chip (uses pendingClipboardDisplay if available)
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

        canvas.drawColor(Color.BLACK)
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
            postInvalidateOnAnimation()
        } catch (e: Exception) {
            Log.e(TAG, "Rendering error: ${e.message}")
            drawFallbackKeys(canvas)
        }
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
        val colors = intArrayOf(
            Color.argb(a1, 60, 90, 255),
            Color.argb(a2, 130, 60, 220),
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

        when (state) {
            KeyState.WHITE -> {
                keyPaint.color = Color.WHITE
                textPaint.color = Color.BLACK
                keyPaint.setShadowLayer(35f, 0f, 0f, Color.WHITE)
            }
            KeyState.PINK -> {
                keyPaint.color = Color.MAGENTA
                textPaint.color = Color.WHITE
                keyPaint.setShadowLayer(28f, 0f, 0f, Color.MAGENTA)
            }
            KeyState.FADE -> {
                keyPaint.color = Color.parseColor("#FF6400")
                textPaint.color = Color.WHITE
                keyPaint.setShadowLayer(22f, 0f, 0f, Color.parseColor("#FF6400"))
            }
            KeyState.NORMAL -> {
                if (label == "Urdu" && settings.urduEnabled) {
                    keyPaint.color = Color.parseColor("#2255CC")
                    textPaint.color = Color.WHITE
                } else if (label == "Mic" && isListeningForSpeech) {
                    keyPaint.color = Color.parseColor("#CC2244")
                    textPaint.color = Color.WHITE
                } else if (label.startsWith("sugg")) {
                    keyPaint.color = Color.parseColor("#151515")
                    textPaint.color = Color.WHITE
                } else {
                    keyPaint.color = Color.parseColor("#080808")
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
        val cornerRadius = dp(keyCornerRadiusDp)
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
                // Use the display text for the suggestion chip
                drawFittedChipText(canvas, pendingClipboardDisplay ?: "", rect)
            } else if (label.startsWith("sugg")) {
                val index = label.removePrefix("sugg").toIntOrNull()
                val word = index?.let { currentSuggestions.getOrNull(it) } ?: ""
                drawFittedChipText(canvas, word, rect)
            } else {
                canvas.drawText(dl, rect.exactCenterX(), rect.exactCenterY() + (textPaint.textSize / 3f), textPaint)
            }
        }
    }

    private fun drawClipboardIcon(canvas: Canvas, rect: Rect, color: Int) {
        val p = Paint().apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = dp(1.6f); isAntiAlias = true; strokeJoin = Paint.Join.ROUND }
        val cx = rect.exactCenterX(); val cy = rect.exactCenterY()
        val w = dp(12f); val h = dp(15f)
        val bodyRect = android.graphics.RectF(cx - w / 2, cy - h / 2 + dp(1.5f), cx + w / 2, cy + h / 2)
        canvas.drawRoundRect(bodyRect, dp(2f), dp(2f), p)
        val clipW = dp(5f)
        val clipRect = android.graphics.RectF(cx - clipW / 2, cy - h / 2 - dp(1.5f), cx + clipW / 2, cy - h / 2 + dp(2f))
        canvas.drawRoundRect(clipRect, dp(1f), dp(1f), p)
    }

    private fun drawGameIcon(canvas: Canvas, rect: Rect, color: Int) {
        val cx = rect.exactCenterX(); val cy = rect.exactCenterY()
        val r = dp(6f)
        val fillPaint = Paint().apply { this.color = color; style = Paint.Style.FILL; isAntiAlias = true }
        canvas.drawCircle(cx - dp(1f), cy, r, fillPaint)
        val beak = android.graphics.Path()
        beak.moveTo(cx + r - dp(1f), cy - dp(1.5f))
        beak.lineTo(cx + r + dp(4f), cy)
        beak.lineTo(cx + r - dp(1f), cy + dp(1.5f))
        beak.close()
        canvas.drawPath(beak, fillPaint)
        val strokePaint = Paint().apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = dp(1.4f); isAntiAlias = true; strokeCap = Paint.Cap.ROUND }
        val wing = android.graphics.Path()
        wing.moveTo(cx - r, cy)
        wing.quadTo(cx - dp(2f), cy - dp(4f), cx + dp(1f), cy - dp(1f))
        canvas.drawPath(wing, strokePaint)
    }

    private fun drawMicIcon(canvas: Canvas, rect: Rect, color: Int) {
        val p = Paint().apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = dp(1.6f); isAntiAlias = true; strokeCap = Paint.Cap.ROUND }
        val cx = rect.exactCenterX(); val cy = rect.exactCenterY()
        val capsuleW = dp(6f); val capsuleH = dp(10f)
        val capsuleRect = android.graphics.RectF(cx - capsuleW / 2, cy - capsuleH / 2 - dp(2f), cx + capsuleW / 2, cy + capsuleH / 2 - dp(2f))
        val fillPaint = Paint().apply { this.color = color; style = Paint.Style.FILL; isAntiAlias = true }
        canvas.drawRoundRect(capsuleRect, capsuleW / 2, capsuleW / 2, fillPaint)
        val archTop = cy + capsuleH / 2 - dp(2f) - dp(2f)
        val archRect = android.graphics.RectF(cx - dp(6f), archTop - dp(4f), cx + dp(6f), archTop + dp(6f))
        canvas.drawArc(archRect, 20f, 140f, false, p)
        canvas.drawLine(cx, archTop + dp(4f), cx, cy + capsuleH / 2 + dp(3f), p)
        canvas.drawLine(cx - dp(3.5f), cy + capsuleH / 2 + dp(3f), cx + dp(3.5f), cy + capsuleH / 2 + dp(3f), p)
    }

    private fun drawFittedChipText(canvas: Canvas, word: String, rect: Rect) {
        if (word.isEmpty()) return
        val maxWidth = rect.width() * 0.88f
        val maxTextSizePx = dp(14f)
        val minTextSizePx = dp(9f)
        val p = Paint(textPaint).apply { textSize = maxTextSizePx }

        var display = word
        // If the raw word is already too long, truncate it before measuring
        if (display.length > 50) display = display.take(47) + "…"

        var size = maxTextSizePx
        while (size > minTextSizePx && p.measureText(display) > maxWidth) {
            size -= dp(0.5f)
            p.textSize = size
        }

        if (p.measureText(display) > maxWidth) {
            while (display.length > 1 && p.measureText("$display\u2026") > maxWidth) {
                display = display.dropLast(1)
            }
            display += "\u2026"
        }

        canvas.drawText(display, rect.exactCenterX(), rect.exactCenterY() + (p.textSize / 3f), p)
    }

    private fun drawEnterIcon(canvas: Canvas, rect: Rect, color: Int) {
        when (imeAction) {
            EditorInfo.IME_ACTION_SEARCH -> drawSearchIcon(canvas, rect, color)
            EditorInfo.IME_ACTION_SEND -> drawSendIcon(canvas, rect, color)
            EditorInfo.IME_ACTION_DONE -> drawDoneIcon(canvas, rect, color)
            EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_NEXT -> drawGoArrowIcon(canvas, rect, color)
            else -> drawReturnIcon(canvas, rect, color)
        }
    }

    private fun drawSearchIcon(canvas: Canvas, rect: Rect, color: Int) {
        iconPaint.color = color
        iconPaint.style = Paint.Style.STROKE
        iconPaint.strokeWidth = dp(2f)
        iconPaint.strokeCap = Paint.Cap.ROUND
        val cx = rect.exactCenterX() - dp(1.5f)
        val cy = rect.exactCenterY() - dp(1.5f)
        val s = minOf(rect.width(), rect.height()) * 0.16f
        canvas.drawCircle(cx, cy, s, iconPaint)
        val handleOffset = s * 0.75f
        canvas.drawLine(
            cx + handleOffset, cy + handleOffset,
            cx + s * 1.6f, cy + s * 1.6f, iconPaint
        )
        iconPaint.style = Paint.Style.FILL
    }

    private fun drawSendIcon(canvas: Canvas, rect: Rect, color: Int) {
        iconPaint.color = color
        iconPaint.style = Paint.Style.FILL
        val cx = rect.exactCenterX()
        val cy = rect.exactCenterY()
        val s = minOf(rect.width(), rect.height()) * 0.20f
        val path = android.graphics.Path()
        path.moveTo(cx - s * 1.1f, cy - s * 0.9f)
        path.lineTo(cx + s * 1.2f, cy)
        path.lineTo(cx - s * 1.1f, cy + s * 0.9f)
        path.lineTo(cx - s * 0.5f, cy)
        path.close()
        canvas.drawPath(path, iconPaint)
    }

    private fun drawDoneIcon(canvas: Canvas, rect: Rect, color: Int) {
        iconPaint.color = color
        iconPaint.style = Paint.Style.STROKE
        iconPaint.strokeWidth = dp(2.2f)
        iconPaint.strokeCap = Paint.Cap.ROUND
        iconPaint.strokeJoin = Paint.Join.ROUND
        val cx = rect.exactCenterX()
        val cy = rect.exactCenterY()
        val s = minOf(rect.width(), rect.height()) * 0.18f
        val path = android.graphics.Path()
        path.moveTo(cx - s, cy)
        path.lineTo(cx - s * 0.2f, cy + s * 0.8f)
        path.lineTo(cx + s * 1.1f, cy - s * 0.8f)
        canvas.drawPath(path, iconPaint)
        iconPaint.style = Paint.Style.FILL
    }

    private fun drawGoArrowIcon(canvas: Canvas, rect: Rect, color: Int) {
        iconPaint.color = color
        iconPaint.style = Paint.Style.STROKE
        iconPaint.strokeWidth = dp(2f)
        iconPaint.strokeCap = Paint.Cap.ROUND
        iconPaint.strokeJoin = Paint.Join.ROUND
        val cx = rect.exactCenterX()
        val cy = rect.exactCenterY()
        val s = minOf(rect.width(), rect.height()) * 0.18f
        val path = android.graphics.Path()
        path.moveTo(cx - s, cy - s)
        path.lineTo(cx + s, cy)
        path.lineTo(cx - s, cy + s)
        canvas.drawPath(path, iconPaint)
        iconPaint.style = Paint.Style.FILL
    }

    private fun drawReturnIcon(canvas: Canvas, rect: Rect, color: Int) {
        iconPaint.color = color
        iconPaint.style = Paint.Style.STROKE
        iconPaint.strokeWidth = dp(2f)
        iconPaint.strokeCap = Paint.Cap.ROUND

        val cx = rect.exactCenterX()
        val cy = rect.exactCenterY()
        val s = minOf(rect.width(), rect.height()) * 0.18f

        val path = android.graphics.Path()
        path.moveTo(cx + s, cy - s * 0.7f)
        path.lineTo(cx - s * 0.3f, cy - s * 0.7f)
        path.lineTo(cx - s * 0.3f, cy - s * 1.2f)
        path.lineTo(cx - s, cy)
        path.lineTo(cx - s * 0.3f, cy + s * 1.2f)
        path.lineTo(cx - s * 0.3f, cy + s * 0.7f)
        path.lineTo(cx + s, cy + s * 0.7f)
        canvas.drawPath(path, iconPaint)

        iconPaint.style = Paint.Style.FILL
    }

    private val iconPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }

    private fun drawEmojiGlyph(canvas: Canvas, rect: Rect) {
        val glyphPaint = Paint().apply {
            textSize = dp(19f)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(
            "\uD83D\uDE00",
            rect.exactCenterX(),
            rect.exactCenterY() + glyphPaint.textSize / 3f,
            glyphPaint
        )
    }

    private fun drawShiftIcon(canvas: Canvas, rect: Rect, color: Int) {
        iconPaint.color = if (isCapsLocked) Color.WHITE else if (isShifted) Color.WHITE else Color.parseColor("#AAAAAA")
        val cx = rect.exactCenterX()
        val cy = rect.exactCenterY()
        val s = minOf(rect.width(), rect.height()) * 0.22f
        val path = android.graphics.Path()
        path.moveTo(cx, cy - s * 1.3f)
        path.lineTo(cx + s, cy)
        path.lineTo(cx + s * 0.45f, cy)
        path.lineTo(cx + s * 0.45f, cy + s * 0.9f)
        path.lineTo(cx - s * 0.45f, cy + s * 0.9f)
        path.lineTo(cx - s * 0.45f, cy)
        path.lineTo(cx - s, cy)
        path.close()
        canvas.drawPath(path, iconPaint)
    }

    private fun drawBackspaceIcon(canvas: Canvas, rect: Rect, color: Int) {
        iconPaint.color = color
        iconPaint.style = Paint.Style.STROKE
        iconPaint.strokeWidth = dp(1.8f)
        iconPaint.strokeCap = Paint.Cap.ROUND
        val cx = rect.exactCenterX()
        val cy = rect.exactCenterY()
        val s = minOf(rect.width(), rect.height()) * 0.20f
        val bodyPath = android.graphics.Path()
        bodyPath.moveTo(cx - s * 1.3f, cy)
        bodyPath.lineTo(cx - s * 0.5f, cy - s)
        bodyPath.lineTo(cx + s * 1.1f, cy - s)
        bodyPath.lineTo(cx + s * 1.1f, cy + s)
        bodyPath.lineTo(cx - s * 0.5f, cy + s)
        bodyPath.close()
        canvas.drawPath(bodyPath, iconPaint)
        val xOffset = s * 0.35f
        canvas.drawLine(cx - xOffset, cy - xOffset * 0.7f, cx + xOffset, cy + xOffset * 0.7f, iconPaint)
        canvas.drawLine(cx + xOffset, cy - xOffset * 0.7f, cx - xOffset, cy + xOffset * 0.7f, iconPaint)
        iconPaint.style = Paint.Style.FILL
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                primaryPointerId = event.getPointerId(0)
                touchStartX = event.x
                touchStartY = event.y
                isSwiping = false
                isLongPress = false
                lastSwipeKeyLabel = null
                handleTouchDown(primaryPointerId, event.x, event.y, isPrimary = true)
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val pid = event.getPointerId(idx)
                handleTouchDown(pid, event.getX(idx), event.getY(idx), isPrimary = false)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val primaryIndex = event.findPointerIndex(primaryPointerId)
                if (primaryIndex != -1) {
                    val dx = event.getX(primaryIndex) - touchStartX
                    val dy = event.getY(primaryIndex) - touchStartY
                    val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    if (dist > swipeThreshold) {
                        if (!isSwiping) {
                            pointerPopups.remove(primaryPointerId)?.release()
                            pendingClickRunnables.remove(primaryPointerId)?.let { handler.removeCallbacks(it) }
                        }
                        isSwiping = true
                    }
                    if (!isSwiping) {
                        handleTouchDown(primaryPointerId, event.getX(primaryIndex), event.getY(primaryIndex), isPrimary = true)
                    } else {
                        handleSwipeAnim(event.getX(primaryIndex), event.getY(primaryIndex))
                    }
                }
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val idx = event.actionIndex
                val pid = event.getPointerId(idx)
                commitPointerKey(pid)
                if (pid == primaryPointerId) {
                    for (i in 0 until event.pointerCount) {
                        val candidateId = event.getPointerId(i)
                        if (candidateId != pid) {
                            primaryPointerId = candidateId
                            touchStartX = event.getX(i)
                            touchStartY = event.getY(i)
                            isSwiping = false
                            lastSwipeKeyLabel = null
                            break
                        }
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(backspaceRunnable ?: Runnable {})
                handler.removeCallbacks(capsLockRunnable ?: Runnable {})
                if (!isSwiping) {
                    commitPointerKey(primaryPointerId)
                } else {
                    activePointers.remove(primaryPointerId)
                    pointerPopups.remove(primaryPointerId)?.release()
                }
                capsLockJustActivated = false
                isSwiping = false
                isLongPress = false
                longPressKey = null
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(backspaceRunnable ?: Runnable {})
                handler.removeCallbacks(capsLockRunnable ?: Runnable {})
                for (pid in activePointers.keys.toList()) {
                    pointerPopups.remove(pid)?.release()
                    pendingClickRunnables.remove(pid)?.let { handler.removeCallbacks(it) }
                }
                activePointers.clear()
                capsLockJustActivated = false
                isSwiping = false
                isLongPress = false
                longPressKey = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun isPreviewEligible(label: String): Boolean {
        return label.length == 1
    }

    private val touchSlopPx by lazy { dp(2f).toInt() }
    private val hitTestRect = Rect()

    private fun commitPointerKey(pointerId: Int) {
        val label = activePointers.remove(pointerId)
        pointerPopups.remove(pointerId)?.release()
        pendingClickRunnables.remove(pointerId)?.let {
            handler.removeCallbacks(it)
            if (settings.soundEnabled) soundEngine.playClick()
        }
        if (label != null) {
            val now = System.currentTimeMillis()
            val skipDueToCapsLock = label == "Shift" && capsLockJustActivated
            if (now - lastKeyTime > debounceInterval && !skipDueToCapsLock) {
                lastKeyTime = now
                commitKey(label)
            }
        }
    }

    private fun handleTouchDown(pointerId: Int, x: Float, y: Float, isPrimary: Boolean) {
        for ((label, rect) in keyMap) {
            hitTestRect.set(rect)
            hitTestRect.inset(-touchSlopPx, -touchSlopPx)
            if (hitTestRect.contains(x.toInt(), y.toInt())) {
                if (activePointers[pointerId] == label) return
                activePointers[pointerId] = label
                if (settings.hapticEnabled) {
                    triggerKeyHaptic()
                }
                if (settings.soundEnabled) {
                    pendingClickRunnables.remove(pointerId)?.let { handler.removeCallbacks(it) }
                    val runnable = Runnable {
                        pendingClickRunnables.remove(pointerId)
                        soundEngine.playClick()
                    }
                    pendingClickRunnables[pointerId] = runnable
                    handler.postDelayed(runnable, clickSoundDelayMs)
                }

                animationEngine.triggerAnimation(rect.exactCenterX(), rect.exactCenterY(), label)
                ripples.add(RippleEffect(rect.exactCenterX(), rect.exactCenterY()))
                if (isPreviewEligible(label)) {
                    pointerPopups[pointerId] = PopupEffect(label, rect.top.toFloat(), rect.exactCenterX(), rect.width().toFloat(), rect.height().toFloat())
                } else {
                    pointerPopups.remove(pointerId)?.release()
                }
                pressedKeys[label] = System.currentTimeMillis()
                postInvalidateOnAnimation()

                if (isPrimary && label == "Del") {
                    isLongPress = true
                    longPressKey = label
                    backspaceRunnable = object : Runnable {
                        override fun run() {
                            if (isLongPress && longPressKey == "Del") {
                                keyListener?.onKey(-5, "Del")
                                handler.postDelayed(this, settings.backspaceRepeatIntervalMs)
                            }
                        }
                    }
                    handler.postDelayed(backspaceRunnable!!, 500)
                }

                if (isPrimary && label == "Shift") {
                    isLongPress = true
                    longPressKey = label
                    capsLockRunnable = Runnable {
                        if (isLongPress && longPressKey == "Shift") {
                            isCapsLocked = true
                            isShifted = true
                            capsLockJustActivated = true
                            postInvalidateOnAnimation()
                        }
                    }
                    handler.postDelayed(capsLockRunnable!!, 400)
                }
                break
            }
        }
    }

    private fun handleSwipeAnim(x: Float, y: Float) {
        for ((label, rect) in keyMap) {
            hitTestRect.set(rect)
            hitTestRect.inset(-touchSlopPx, -touchSlopPx)
            if (hitTestRect.contains(x.toInt(), y.toInt())) {
                animationEngine.triggerAnimation(rect.exactCenterX(), rect.exactCenterY(), label)
                pressedKeys[label] = System.currentTimeMillis()
                postInvalidateOnAnimation()

                if (settings.soundEnabled && label != lastSwipeKeyLabel && width > 0) {
                    lastSwipeKeyLabel = label
                    val noteIndex = ((rect.exactCenterX() / width.toFloat()) * soundEngine.noteCount)
                        .toInt().coerceIn(0, soundEngine.noteCount - 1)
                    soundEngine.playSwipeTone(noteIndex)
                }
                break
            }
        }
    }

    private fun finalizeRomanBuffer() {
        val typed = currentRomanBuffer.toString()
        if (typed.isNotEmpty()) {
            if (settings.urduEnabled) {
                val match = urduRepo.phraseMatch(typed.uppercase()) ?: run {
                    if (typed.length > 1) urduRepo.bestWordMatch(typed.lowercase()) else null
                }
                if (match != null) {
                    for (i in 0 until typed.length) {
                        keyListener?.onKey(-5, "Del")
                    }
                    keyListener?.onKey(match.hashCode(), match)
                    urduRepo.recordUsage(typed.lowercase(), match)
                }
            }
        }
        currentRomanBuffer.clear()
        if (currentSuggestions.isNotEmpty()) {
            currentSuggestions = emptyList()
            createKeyMap(width, height)
        }
    }

    private fun updateSuggestions() {
        val prefix = currentRomanBuffer.toString().lowercase()
        currentSuggestions = when {
            prefix.isEmpty() -> emptyList()
            settings.urduEnabled -> urduRepo.candidatesForPrefix(prefix)
            else -> englishRepo.candidatesForPrefix(prefix)
        }
        createKeyMap(width, height)
    }

    private fun commitSuggestion(index: Int) {
        val word = currentSuggestions.getOrNull(index) ?: return
        val typedPrefix = currentRomanBuffer.toString().lowercase()
        val typedLength = currentRomanBuffer.length
        for (i in 0 until typedLength) {
            keyListener?.onKey(-5, "Del")
        }
        keyListener?.onKey(word.hashCode(), word)
        if (settings.urduEnabled) {
            urduRepo.recordUsage(typedPrefix, word)
        } else {
            englishRepo.recordUsage(typedPrefix, word)
        }
        currentRomanBuffer.clear()
        currentSuggestions = emptyList()
        createKeyMap(width, height)
    }

    private fun commitKey(label: String) {
        announceKeyForAccessibility(label)
        when (label) {
            "Shift" -> {
                if (isCapsLocked) {
                    isCapsLocked = false
                    isShifted = false
                } else {
                    isShifted = !isShifted
                }
                postInvalidateOnAnimation()
            }
            "Del" -> {
                if (currentRomanBuffer.isNotEmpty()) {
                    currentRomanBuffer.deleteCharAt(currentRomanBuffer.length - 1)
                    updateSuggestions()
                }
                keyListener?.onKey(-5, "Del")
            }
            "Go" -> {
                finalizeRomanBuffer()
                keyListener?.onKey(-4, "Go")
            }
            "Space" -> {
                finalizeRomanBuffer()
                keyListener?.onKey(32, "Space")
            }
            "123" -> {
                finalizeRomanBuffer()
                currentLayout = numberLayout
                createKeyMap(width, height)
                postInvalidateOnAnimation()
            }
            "ABC" -> {
                finalizeRomanBuffer()
                currentLayout = letterLayout
                createKeyMap(width, height)
                postInvalidateOnAnimation()
            }
            "=\\<" -> {
                finalizeRomanBuffer()
                currentLayout = extendedSymbolLayout
                createKeyMap(width, height)
                postInvalidateOnAnimation()
            }
            "Emoji" -> {
                finalizeRomanBuffer()
                keyListener?.onKey(-9, "Emoji")
            }
            "Urdu" -> {
                finalizeRomanBuffer()
                settings.urduEnabled = !settings.urduEnabled
                createKeyMap(width, height)
                postInvalidateOnAnimation()
            }
            "Clipboard" -> {
                finalizeRomanBuffer()
                keyListener?.onKey(-10, "Clipboard")
            }
            "Game" -> {
                finalizeRomanBuffer()
                keyListener?.onKey(-14, "Game")
            }
            "Mic" -> {
                finalizeRomanBuffer()
                keyListener?.onKey(-11, "Mic")
            }
            else -> {
                // Clipboard suggestion chip
                if (label == "clipSugg") {
                    val fullText = pendingClipboardFull
                    if (fullText != null) {
                        // Paste the full text, then add a space
                        keyListener?.onKey(fullText.hashCode(), fullText)
                        keyListener?.onKey(32, "Space")
                    }
                    // Clear the suggestion after use
                    pendingClipboardFull = null
                    pendingClipboardDisplay = null
                    createKeyMap(width, height)
                    return
                }

                // Suggestion chip
                if (label.startsWith("sugg")) {
                    val index = label.removePrefix("sugg").toIntOrNull()
                    if (index != null) commitSuggestion(index)
                    return
                }

                var fl = if ((isShifted || isCapsLocked) && label.length == 1 && label[0].isLetter()) label.uppercase() else label

                if (fl.length == 1 && fl[0].isLetter()) {
                    val lower = fl.lowercase()
                    currentRomanBuffer.append(lower)
                    if (settings.urduEnabled) {
                        val urduChar = romanUrduMap[lower] ?: fl
                        keyListener?.onKey(urduChar.hashCode(), urduChar)
                    } else {
                        keyListener?.onKey(fl.hashCode(), fl)
                    }
                    updateSuggestions()
                } else if (settings.urduEnabled && urduPunctuationMap.containsKey(fl)) {
                    finalizeRomanBuffer()
                    val urduPunct = urduPunctuationMap.getValue(fl)
                    keyListener?.onKey(urduPunct.hashCode(), urduPunct)
                } else {
                    keyListener?.onKey(fl.hashCode(), fl)
                }

                if (currentLayout == numberLayout && label.length == 1 && label != "=\\<") {
                    currentLayout = letterLayout
                    createKeyMap(width, height)
                    postInvalidateOnAnimation()
                }

                if (isShifted && !isCapsLocked && label.isNotEmpty() && label[0].isLetter()) {
                    isShifted = false
                    postInvalidateOnAnimation()
                }
            }
        }
    }

    private fun announceKeyForAccessibility(label: String) {
        if (!isAccessibilityLiveRegionRelevant()) return
        val spoken = when (label) {
            "Del" -> "Backspace"
            "Go" -> when (imeAction) {
                EditorInfo.IME_ACTION_SEARCH -> "Search"
                EditorInfo.IME_ACTION_SEND -> "Send"
                EditorInfo.IME_ACTION_DONE -> "Done"
                EditorInfo.IME_ACTION_GO -> "Go"
                EditorInfo.IME_ACTION_NEXT -> "Next"
                EditorInfo.IME_ACTION_PREVIOUS -> "Previous"
                else -> "Enter"
            }
            "Space" -> "Space"
            "Shift" -> if (isShifted) "Shift off" else "Shift on"
            "123" -> "Numbers"
            "ABC" -> "Letters"
            "Emoji" -> "Emoji"
            "Urdu" -> if (settings.urduEnabled) "Urdu typing off" else "Urdu typing on"
            else -> if (label.startsWith("sugg")) {
                val idx = label.removePrefix("sugg").toIntOrNull()
                idx?.let { currentSuggestions.getOrNull(it) } ?: label
            } else label
        }
        try {
            announceForAccessibility(spoken)
        } catch (_: Exception) { }
    }

    private fun isAccessibilityLiveRegionRelevant(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
        return am?.isEnabled == true
    }

    private inner class RippleEffect(private val cx: Float, private val cy: Float) {
        private var radius = 0f
        private var alp = 255
        var finished = false
        private val maxR = 100f
        private val dur = 500L
        private val start = System.currentTimeMillis()

        fun update(dt: Long) {
            val p = (System.currentTimeMillis() - start).toFloat() / dur.toFloat()
            if (p >= 1.0f) { finished = true; return }
            radius = maxR * p
            alp = (255 * (1 - p)).toInt()
        }

        fun draw(canvas: Canvas) {
            val pt = Paint()
            pt.isAntiAlias = true
            pt.color = Color.argb(alp, 255, 255, 255)
            canvas.drawCircle(cx, cy, radius, pt)
        }
    }

    private inner class PopupEffect(
        private val lbl: String,
        private val keyTop: Float,
        private val px: Float,
        private val keyWidth: Float,
        private val keyHeight: Float
    ) {
        private var alp = 255
        var finished = false
        private var released = false
        private var releaseStart = 0L
        private val fadeDur = 100L

        fun release() {
            if (!released) {
                released = true
                releaseStart = System.currentTimeMillis()
            }
        }

        fun draw(canvas: Canvas) {
            if (released) {
                val p = (System.currentTimeMillis() - releaseStart).toFloat() / fadeDur.toFloat()
                if (p >= 1.0f) { finished = true; return }
                alp = (255 * (1 - p)).toInt()
            } else {
                alp = 255
            }

            val pw = keyWidth * 1.19f
            val ph = keyHeight * 1.47f
            val bubbleBottom = keyTop + keyHeight * 0.35f
            val bubbleTop = bubbleBottom - ph
            val radius = dp(14f)

            popupPaint.alpha = alp
            canvas.drawRoundRect(px - pw / 2, bubbleTop, px + pw / 2, bubbleBottom, radius, radius, popupPaint)
            popupBorderPaint.alpha = alp
            canvas.drawRoundRect(px - pw / 2, bubbleTop, px + pw / 2, bubbleBottom, radius, radius, popupBorderPaint)
            popupTextPaint.alpha = alp
            canvas.drawText(lbl.uppercase(), px, bubbleTop + ph * 0.42f, popupTextPaint)
        }
    }
}
