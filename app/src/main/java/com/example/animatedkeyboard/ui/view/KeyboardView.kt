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

    // Roman Urdu transliteration map
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

    // FIX: current editor action (Search/Send/Go/Done/Next/newline), pushed by the IME
    // service so the Return key can both look and behave correctly per text field.
    private var imeAction: Int = EditorInfo.IME_ACTION_UNSPECIFIED

    fun setImeAction(action: Int) {
        if (imeAction != action) {
            imeAction = action
            postInvalidateOnAnimation()
        }
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

    private val horizontalKeyGapDp = 4f
    private val verticalRowGapDp = 6f
    private val sideMarginDp = 3f
    private val topBottomMarginDp = 4f
    private val keyCornerRadiusDp = 5f

    // FIX: Landscape - fixed height fraction, never expand
    private val keyboardHeightFraction = 0.35f
    private val landscapeHeightFraction = 0.30f
    private val spaceRowHeightFactor = 1.0f // FIX: Space row same height as others

    private val keyPaint = Paint()
    private val keyBorderPaint = Paint()
    private val textPaint = Paint()
    private val animationEngine = AnimationEngine()
    private var lastFrameTime = 0L
    private var glowPulse = 0.5f
    private var glowDirection = -1
    private val glowPaint = Paint()
    private val pressedKeys = mutableMapOf<String, Long>()
    private val keyStates = mutableMapOf<String, KeyState>()
    private val ripples = mutableListOf<RippleEffect>()
    private var currentPopup: PopupEffect? = null
    private val popupPaint = Paint()
    private val popupBorderPaint = Paint()
    private val popupTextPaint = Paint()

    // Letter layout with numbers row
    private val letterLayout = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "Urdu"),
        listOf("Shift", "z", "x", "c", "v", "b", "n", "m", "Del"),
        listOf("123", "Emoji", "Space", ".", "Go")
    )

    // Symbol layout 1
    private val numberLayout = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/"),
        listOf("*", "\"", "'", ":", ";", "!", "?"),
        listOf("=\\<", "%", "^", "[", "]", "{", "}", "Del"),
        listOf("ABC", "Emoji", ",", "Space", ".", "Go")
    )

    // Symbol layout 2 (extended)
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
    private val debounceInterval = 100L
    private var touchStartX = 0f
    private var touchStartY = 0f
    private val swipeThreshold = 50f
    private var isSwiping = false
    private var lastTouchedKey: String? = null
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
        popupTextPaint.textSize = dp(22f)
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

    // FIX: Landscape - use fixed height, never expand to fullscreen
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

    private fun buildKeyMapInternal(width: Int, height: Int) {
        keyMap.clear()

        val sideMargin = dp(sideMarginDp).toInt()
        val topBottomMargin = dp(topBottomMarginDp).toInt()
        val hGap = dp(horizontalKeyGapDp).toInt()
        val vGap = dp(verticalRowGapDp).toInt()

        val rowCount = currentLayout.size
        val availableHeight = height - (topBottomMargin * 2) - (vGap * (rowCount - 1))

        // FIX: All rows same height including space row
        val rowHeight = availableHeight / rowCount

        var currentY = topBottomMargin

        for ((rowIndex, row) in currentLayout.withIndex()) {
            val availableRowWidth = width - (sideMargin * 2) - (hGap * (row.size - 1))
            var totalWeight = 0.0
            for (item in row) {
                totalWeight += getWeight(item).toDouble()
            }
            val tw = totalWeight.toFloat()
            var currentX = sideMargin

            for ((keyIndex, keyLabel) in row.withIndex()) {
                val isLastKeyInRow = keyIndex == row.lastIndex
                val kw = (availableRowWidth * (getWeight(keyLabel) / tw)).roundToInt()
                val safeRight = if (isLastKeyInRow) (width - sideMargin) else (currentX + kw)
                keyMap[keyLabel] = Rect(currentX, currentY, safeRight, currentY + rowHeight)
                keyStates[keyLabel] = KeyState.NORMAL
                currentX = safeRight + hGap
            }
            currentY += rowHeight + vGap
        }
    }

    private fun getWeight(label: String): Float {
        return when (label) {
            "Space" -> 3.5f
            "Shift", "Del", "123", "ABC", "Go" -> 1.4f
            "=\\<" -> 1.6f
            "Emoji" -> 1.0f
            "Urdu" -> 1.0f // FIX: same box size as regular letter keys, per spec
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
            for ((label, rect) in keyMap) {
                drawKey(canvas, label, rect)
            }
            currentPopup?.draw(canvas)
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
        } catch (e: Exception) {
            Log.e(TAG, "Fallback rendering failed: ${e.message}")
        }
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
                keyPaint.color = Color.parseColor("#080808")
                textPaint.color = Color.WHITE
                keyPaint.clearShadowLayer()
            }
        }

        val l = rect.left.toFloat()
        val t = rect.top.toFloat()
        val r = rect.right.toFloat()
        val b = rect.bottom.toFloat()

        // FIX: separate H/V margins — a width-based margin applied to height too
        // made wide keys (Space) render visibly shorter than narrow keys in the same row.
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
            "Go" -> drawEnterIcon(canvas, rect, textPaint.color) // FIX: icon reflects Search/Send/Go/Done/Next
            "Urdu" -> canvas.drawText("اردو", rect.exactCenterX(), rect.exactCenterY() + (textPaint.textSize / 3f), textPaint)
            else -> canvas.drawText(dl, rect.exactCenterX(), rect.exactCenterY() + (textPaint.textSize / 3f), textPaint)
        }
    }

    // FIX: Dispatches to the right glyph based on the field's requested editor action.
    private fun drawEnterIcon(canvas: Canvas, rect: Rect, color: Int) {
        when (imeAction) {
            EditorInfo.IME_ACTION_SEARCH -> drawSearchIcon(canvas, rect, color)
            EditorInfo.IME_ACTION_SEND -> drawSendIcon(canvas, rect, color)
            EditorInfo.IME_ACTION_DONE -> drawDoneIcon(canvas, rect, color)
            EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_NEXT -> drawGoArrowIcon(canvas, rect, color)
            else -> drawReturnIcon(canvas, rect, color) // Unspecified/None -> literal newline
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

    // FIX: Return arrow icon instead of "Go" text
    private fun drawReturnIcon(canvas: Canvas, rect: Rect, color: Int) {
        iconPaint.color = color
        iconPaint.style = Paint.Style.STROKE
        iconPaint.strokeWidth = dp(2f)
        iconPaint.strokeCap = Paint.Cap.ROUND

        val cx = rect.exactCenterX()
        val cy = rect.exactCenterY()
        val s = minOf(rect.width(), rect.height()) * 0.18f

        // Return/Enter arrow shape
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

    // FIX: Replaces the gear/settings icon — this key now opens the emoji panel.
    private fun drawEmojiGlyph(canvas: Canvas, rect: Rect) {
        val glyphPaint = Paint().apply {
            textSize = dp(19f)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(
            "\uD83D\uDE00", // 😀
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
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                isSwiping = false
                lastTouchedKey = null
                isLongPress = false
                handleTouchDown(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - touchStartX
                val dy = event.y - touchStartY
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (dist > swipeThreshold) isSwiping = true
                if (!isSwiping) handleTouchDown(event.x, event.y)
                else handleSwipeAnim(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(backspaceRunnable ?: Runnable {})
                handler.removeCallbacks(capsLockRunnable ?: Runnable {})
                if (!isSwiping && lastTouchedKey != null) {
                    val now = System.currentTimeMillis()
                    val skipDueToCapsLock = lastTouchedKey == "Shift" && capsLockJustActivated
                    if (now - lastKeyTime > debounceInterval && !skipDueToCapsLock) {
                        lastKeyTime = now
                        commitKey(lastTouchedKey!!)
                    }
                }
                capsLockJustActivated = false
                lastTouchedKey = null
                isSwiping = false
                isLongPress = false
                longPressKey = null
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(backspaceRunnable ?: Runnable {})
                handler.removeCallbacks(capsLockRunnable ?: Runnable {})
                capsLockJustActivated = false
                lastTouchedKey = null
                isSwiping = false
                isLongPress = false
                longPressKey = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleTouchDown(x: Float, y: Float) {
        for ((label, rect) in keyMap) {
            if (rect.contains(x.toInt(), y.toInt())) {
                lastTouchedKey = label
                if (settings.hapticEnabled) {
                    performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP, android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
                }
                soundEngine.playClick()

                // FIX: Per-key radial color animation for ALL keys
                animationEngine.triggerAnimation(rect.exactCenterX(), rect.exactCenterY(), label)
                ripples.add(RippleEffect(rect.exactCenterX(), rect.exactCenterY()))
                currentPopup = PopupEffect(label, rect.exactCenterX(), rect.top.toFloat() - dp(20f), rect.width().toFloat(), rect.height().toFloat())
                pressedKeys[label] = System.currentTimeMillis()
                postInvalidateOnAnimation()

                if (label == "Del") {
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

                if (label == "Shift") {
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
            if (rect.contains(x.toInt(), y.toInt())) {
                animationEngine.triggerAnimation(rect.exactCenterX(), rect.exactCenterY(), label)
                pressedKeys[label] = System.currentTimeMillis()
                postInvalidateOnAnimation()
                break
            }
        }
    }

    // FIX: Finalizes the pending Roman-Urdu buffer at a word boundary (Space, Go,
    // or a layout switch). If the whole typed word matches a known dictionary
    // entry, the letter-by-letter text already on screen is replaced with the
    // correct combined spelling (e.g. "shukriya" -> "شکریہ").
    private fun finalizeRomanBuffer() {
        if (settings.urduEnabled && currentRomanBuffer.length > 1) {
            val typed = currentRomanBuffer.toString()
            val wholeWordMatch = romanUrduMap[typed.lowercase()]
            if (wholeWordMatch != null) {
                for (i in 0 until typed.length) {
                    keyListener?.onKey(-5, "Del")
                }
                keyListener?.onKey(wholeWordMatch.hashCode(), wholeWordMatch)
            }
        }
        currentRomanBuffer.clear()
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
                // Keep the buffer in sync with what's actually on screen, so a later
                // word-boundary correction never deletes more than what was typed.
                if (settings.urduEnabled && currentRomanBuffer.isNotEmpty()) {
                    currentRomanBuffer.deleteCharAt(currentRomanBuffer.length - 1)
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
                postInvalidateOnAnimation()
            }
            else -> {
                var fl = if ((isShifted || isCapsLocked) && label.length == 1 && label[0].isLetter()) label.uppercase() else label

                // FIX: direct per-letter substitution that only ever appends — never
                // deletes — so each new key press joins onto the previous one instead
                // of erasing it. finalizeRomanBuffer() upgrades known whole words later.
                if (settings.urduEnabled && fl.length == 1 && fl[0].isLetter()) {
                    val lower = fl.lowercase()
                    currentRomanBuffer.append(lower)
                    val urduChar = romanUrduMap[lower] ?: fl
                    keyListener?.onKey(urduChar.hashCode(), urduChar)
                } else {
                    keyListener?.onKey(fl.hashCode(), fl)
                }

                // FIX: Auto return to alphabetic layout after typing in numbers/symbols
                // But NOT when pressing =\< key (stay in symbols)
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
            else -> label
        }
        try {
            announceForAccessibility(spoken)
        } catch (e: Exception) {
            Log.w(TAG, "Accessibility announcement failed: ${e.message}")
        }
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
        private val px: Float,
        private val py: Float,
        private val keyWidth: Float,
        private val keyHeight: Float
    ) {
        private var alp = 255
        private var offY = 10f
        var finished = false
        private val dur = 250L
        private val start = System.currentTimeMillis()

        fun draw(canvas: Canvas) {
            val p = (System.currentTimeMillis() - start).toFloat() / dur.toFloat()
            if (p >= 1.0f) { finished = true; return }
            if (p < 0.2f) {
                offY = 10f - (10f * (p / 0.2f))
                alp = 255
            } else {
                alp = (255 * (1 - (p - 0.2f) / 0.8f)).toInt()
            }
            val pw = keyWidth * 1.2f
            val ph = keyHeight * 1.2f
            popupPaint.alpha = alp
            canvas.drawRoundRect(px - pw / 2, py + offY, px + pw / 2, py + offY + ph, 15f, 15f, popupPaint)
            popupBorderPaint.alpha = alp
            canvas.drawRoundRect(px - pw / 2, py + offY, px + pw / 2, py + offY + ph, 15f, 15f, popupBorderPaint)
            popupTextPaint.alpha = alp
            canvas.drawText(lbl.uppercase(), px, py + offY + ph / 2 + popupTextPaint.textSize / 3f, popupTextPaint)
        }
    }
}