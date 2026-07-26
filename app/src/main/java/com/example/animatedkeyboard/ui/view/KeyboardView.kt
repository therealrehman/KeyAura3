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
import com.example.animatedkeyboard.theme.AnimationTheme
import com.example.animatedkeyboard.utils.AnimationEngine
import kotlin.math.roundToInt

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

    private val settings by lazy { KeyboardSettings.getInstance(context) }
    private val soundEngine by lazy { KeySoundEngine(context) }

    private var animationEngine: AnimationEngine = AnimationEngine()

    // FIX: Missing setTheme function added
    fun setTheme(theme: AnimationTheme) {
        animationEngine.setTheme(theme)
        // Also update primary and accent colors based on theme to match UI
        settings.primaryColor = theme.colors.firstOrNull() ?: Color.parseColor("#4488FF")
        settings.accentColor = theme.colors.getOrElse(1) { Color.parseColor("#FF64C8") }
        postInvalidateOnAnimation()
    }

    fun setImeAction(action: Int) { postInvalidateOnAnimation() }
    fun refreshSoundEngineTune() { soundEngine.refreshTuneIfChanged() }
    fun setListeningState(listening: Boolean) { postInvalidateOnAnimation() }
    fun showClipboardSuggestion(fullText: String) { postInvalidateOnAnimation() }

    fun refreshSettings() {
        animationEngine = AnimationEngine()
        val themeIndex = settings.selectedThemeIndex
        animationEngine.setTheme(AnimationTheme.fromIndex(themeIndex))
        createKeyMap(width, height)
        soundEngine.refreshTuneIfChanged()
        postInvalidateOnAnimation()
    }

    fun setOnCustomKeyListener(listener: OnKeyListener) { this.keyListener = listener }
    fun release() {
        handler.removeCallbacks(backspaceRunnable ?: Runnable {})
        soundEngine.release()
    }

    private val density = context.resources.displayMetrics.density
    private fun dp(value: Float): Float = value * density

    private val keyPaint = Paint()
    private val keyBorderPaint = Paint()
    private val textPaint = Paint()
    private val glowPaint = Paint()
    private val pressedKeys = mutableMapOf<String, Long>()
    private val keyStates = mutableMapOf<String, KeyState>()
    private val keyMap = mutableMapOf<String, Rect>()
    private val activePointers = mutableMapOf<Int, String>()

    private val baseLetterLayout = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("Shift", "z", "x", "c", "v", "b", "n", "m", "Del"),
        listOf("123", "Emoji", "Space", ".", "Go")
    )

    private val numberRow = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    private val letterLayout: List<List<String>>
        get() = if (settings.numberRowEnabled) listOf(numberRow) + baseLetterLayout else baseLetterLayout

    private var currentLayout = letterLayout
    private var isShifted = false
    private var isSwiping = false

    init {
        setWillNotDraw(false)
        setBackgroundColor(Color.BLACK)
        keyPaint.isAntiAlias = true
        keyBorderPaint.isAntiAlias = true
        keyBorderPaint.style = Paint.Style.STROKE
        textPaint.color = Color.WHITE
        textPaint.isAntiAlias = true
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.DEFAULT_BOLD
        glowPaint.isAntiAlias = true
        refreshSettings()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val dm = context.resources.displayMetrics
        val isLandscape = dm.widthPixels > dm.heightPixels
        val heightPercent = settings.keyboardHeightPercent / 100f
        val desiredHeight = if (isLandscape) (dm.heightPixels * 0.30f).toInt() else (dm.heightPixels * heightPercent).toInt()
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
        if (width <= 0 || height <= 0) return
        keyMap.clear()
        val rowCount = currentLayout.size
        val rowHeight = height / rowCount
        var currentY = 0

        for (row in currentLayout) {
            var currentX = 0
            for (keyLabel in row) {
                val kw = width / row.size
                keyMap[keyLabel] = Rect(currentX, currentY, currentX + kw, currentY + rowHeight)
                keyStates[keyLabel] = KeyState.NORMAL
                currentX += kw
            }
            currentY += rowHeight
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bgAlpha = (255 * settings.backgroundOpacity).toInt()
        canvas.drawColor(Color.argb(bgAlpha, 0, 0, 0))

        try {
            for ((label, rect) in keyMap) {
                drawKey(canvas, label, rect)
            }
            postInvalidateOnAnimation()
        } catch (e: Exception) {
            Log.e("KeyboardView", "Rendering error: ${e.message}")
        }
    }

    private fun drawKey(canvas: Canvas, label: String, rect: Rect) {
        val state = keyStates[label] ?: KeyState.NORMAL
        val primary = settings.primaryColor
        val accent = settings.accentColor

        when (state) {
            KeyState.WHITE -> { keyPaint.color = Color.WHITE; textPaint.color = Color.BLACK }
            KeyState.PINK -> { keyPaint.color = accent; textPaint.color = Color.WHITE }
            KeyState.FADE -> { keyPaint.color = primary; textPaint.color = Color.WHITE }
            KeyState.NORMAL -> { keyPaint.color = Color.parseColor("#08080F"); textPaint.color = Color.WHITE }
        }

        val l = rect.left.toFloat(); val t = rect.top.toFloat()
        val r = rect.right.toFloat(); val b = rect.bottom.toFloat()
        val cornerRadius = dp(5f)

        canvas.drawRoundRect(l, t, r, b, cornerRadius, cornerRadius, keyPaint)

        // FIX: Keys Outline Enable/Disable function added
        if (settings.keyOutlineEnabled) {
            keyBorderPaint.color = primary
            keyBorderPaint.strokeWidth = dp(1.5f)
            canvas.drawRoundRect(l, t, r, b, cornerRadius, cornerRadius, keyBorderPaint)
        }

        val dl = if (isShifted && label.length == 1 && label[0].isLetter()) label.uppercase() else label
        textPaint.textSize = dp(15f)
        canvas.drawText(dl, rect.exactCenterX(), rect.exactCenterY() + (textPaint.textSize / 3f), textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val key = findKeyAt(x, y) ?: return true
                activePointers[pointerId] = key
                pressedKeys[key] = System.currentTimeMillis()
                keyStates[key] = KeyState.WHITE
                triggerKeyHaptic()
                if (settings.soundEnabled) soundEngine.playClick()
                postInvalidateOnAnimation()
            }
            MotionEvent.ACTION_MOVE -> {
                val key = findKeyAt(x, y)
                if (key != null && key != activePointers[pointerId]) {
                    isSwiping = true
                    activePointers[pointerId] = key
                    pressedKeys[key] = System.currentTimeMillis()
                    keyStates[key] = KeyState.WHITE
                    postInvalidateOnAnimation()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val key = activePointers[pointerId] ?: return true
                activePointers.remove(pointerId)
                if (!isSwiping) keyListener?.onKey(keyCodeForLabel(key), key)
                keyStates[key] = KeyState.NORMAL
                isSwiping = false
                postInvalidateOnAnimation()
            }
            MotionEvent.ACTION_CANCEL -> {
                activePointers.clear()
                isSwiping = false
                postInvalidateOnAnimation()
            }
        }
        return true
    }

    private fun findKeyAt(x: Float, y: Float): String? {
        for ((label, rect) in keyMap) {
            if (rect.contains(x.toInt(), y.toInt())) return label
        }
        return null
    }

    private fun keyCodeForLabel(label: String): Int {
        return when (label) {
            "Space" -> 32; "Shift" -> -1; "Del" -> -5; "Go" -> -4
            "123" -> -2; "ABC" -> -3; "Emoji" -> -9
            else -> if (label.length == 1) label[0].toInt() else 0
        }
    }

    private fun triggerKeyHaptic() {
        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator ?: return
        if (!v.hasVibrator() || !settings.hapticEnabled) return
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                v.vibrate(android.os.VibrationEffect.createOneShot(30L, 160))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(30L)
            }
        } catch (_: Exception) { }
    }
}
