package com.example.animatedkeyboard.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import com.example.animatedkeyboard.animation.AnimationEngine
import com.example.animatedkeyboard.settings.KeyboardSettings
import com.example.animatedkeyboard.sound.KeySoundEngine
import com.example.animatedkeyboard.suggest.SuggestionEngine
import com.example.animatedkeyboard.theme.KeyboardTheme
import com.example.animatedkeyboard.theme.ThemeRepository
import com.example.animatedkeyboard.theme.ThemeType
import com.example.animatedkeyboard.urdu.RomanUrduEngine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnKeyListener {
        fun onKey(code: Int, label: String)
    }

    companion object {
        const val CODE_SHIFT = -1
        const val CODE_SYMBOLS = -2
        const val CODE_ALPHA = -3
        const val CODE_ENTER = -4
        const val CODE_DELETE = -5
        const val CODE_URDU_TOGGLE = -8
        const val CODE_EMOJI = -9
        const val CODE_CLIPBOARD = -10
        const val CODE_MIC = -11
        const val CODE_URDU_LAYOUT = -12
        const val CODE_GAME = -14
        const val CODE_COMMIT_TEXT = 0
        const val CODE_REPLACE_BASE = -1000
    }

    var searchMode = false

    private val settings = KeyboardSettings.getInstance(context)
    private val suggestionEngine = SuggestionEngine.getInstance(context)
    private val urduEngine = RomanUrduEngine.getInstance(context)
    private val animationEngine = AnimationEngine()
    private val soundEngine = KeySoundEngine(context)

    private var keyListener: OnKeyListener? = null
    fun setOnCustomKeyListener(l: OnKeyListener) { keyListener = l }

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val handler = Handler(Looper.getMainLooper())

    private val qwertyLayout = listOf(
        listOf("q","w","e","r","t","y","u","i","o","p"),
        listOf("a","s","d","f","g","h","j","k","l"),
        listOf("Shift","z","x","c","v","b","n","m","Del"),
        listOf("123","Emoji","Space",".","Go")
    )

    private val numberLayout = listOf(
        listOf("1","2","3","4","5","6","7","8","9","0"),
        listOf("@","#","$","_","&","-","+","(",")","/"),
        listOf("=/<","*","\"","'",":",";","!","?","Del"),
        listOf("ABC","اردو","Emoji",",","Space",".","Go")
    )

    private val extendedSymbolLayout = listOf(
        listOf("~","`","|","•","√","π","÷","×","¶","∆"),
        listOf("£","¢","€","¥","^","°","=","{","}","\\"),
        listOf("123","%","©","®","™","✓","[","]","Del"),
        listOf("ABC","اردو","Emoji",",","Space",".","Go")
    )

    private val urduLayout = listOf(
        listOf("۱","۲","۳","۴","۵","۶","۷","۸","۹","۰"),
        listOf("ق","و","ع","ر","ت","ے","ء","ی","ہ","پ"),
        listOf("ا","س","د","ف","گ","ھ","ج","ک","ل"),
        listOf("Shift","ز","ش","چ","ط","ب","ن","م","Del"),
        listOf("ABC","123","Emoji","Space","۔","Go")
    )

    private val urduShiftLayout = listOf(
        listOf("!","ٍ","؛","٪","؞","۔","،","(",")","؟"),
        listOf("ؤ","ئ","آ","ٹ","ڈ","خ","ص","ض","ژ","غ"),
        listOf("ث","ذ","ظ","ڑ","ں","ّ","ٰ","أ","إ"),
        listOf("Shift","ـ","ٓ","٬","٫","€","¥","£","Del"),
        listOf("ABC","123","Emoji","Space","۔","Go")
    )

    private var currentLayout: List<List<String>> = qwertyLayout
    private var isShifted = false
    private var isCapsLocked = false
    private var lastShiftTapAt = 0L
    private var imeAction = EditorInfo.IME_ACTION_UNSPECIFIED

    private class Key {
        var label = ""
        var code = 0
        var rect = RectF()
        var isAction = false
    }

    private enum class StripKind { SUGGESTION, CLIP_CHIP, CLIP_CLOSE, QUICK }

    private class StripCell {
        var kind = StripKind.SUGGESTION
        var text = ""
        var code = 0
        var rect = RectF()
    }

    private val keys = ArrayList<Key>(64)
    private val stripCells = ArrayList<StripCell>(8)

    private var stripHeight = 0f
    private var keyboardTop = 0f

    private val romanBuffer = StringBuilder()
    private var suggestions: List<String> = emptyList()

    private var pendingClipboardFull: String? = null
    private var pendingClipboardPreview = ""

    private var activePointerId = -1
    private var downKey: Key? = null
    private var downStripCell: StripCell? = null
    private val pressStartTimes = HashMap<Key, Long>()

    private var isLongPressDel = false
    private var backspaceRunnable: Runnable? = null

    private var isGliding = false
    private val glideBuffer = StringBuilder()
    private var glideNoteIndex = 0
    private var lastGlideKey: Key? = null
    private val trailPoints = ArrayList<Float>(96)
    private val trailPath = Path()

    private var popupKey: Key? = null
    private var popupShownAt = 0L

    private var isListeningForSpeech = false

    private var activeTheme: KeyboardTheme = ThemeRepository.defaultTheme
    private var kbBgBitmap: Bitmap? = null
    private var kbBgPath: String? = null

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    private val stripBgPaint = Paint().apply { color = Color.parseColor("#060609") }
    private val stripDividerPaint = Paint().apply { color = Color.parseColor("#1B1B22") }
    private val suggestionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val chipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#15161C") }
    private val popupBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val popupTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
    }
    private val scratchRect = RectF()

    private val vibrator by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                    as? android.os.VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        }
    }
    private var lastHapticAt = 0L

    init {
        setWillNotDraw(false)
        refreshTheme()
    }

    fun setImeAction(action: Int) {
        imeAction = action
        rebuildKeys()
        postInvalidateOnAnimation()
    }

    fun setListeningState(listening: Boolean) {
        isListeningForSpeech = listening
        postInvalidateOnAnimation()
    }

    fun showClipboardSuggestion(text: String) {
        pendingClipboardFull = text
        val oneLine = text.trim().replace(Regex("\\s+"), " ")
        pendingClipboardPreview = if (oneLine.length <= 22) oneLine
        else oneLine.take(22).trimEnd() + "…"
        rebuildKeys()
        postInvalidateOnAnimation()
    }

    fun refreshSoundEngineTune() {
        soundEngine.refreshTuneIfChanged()
        soundEngine.refreshVolume()
    }

    fun refreshTheme() {
        activeTheme = ThemeRepository.resolve(settings)
        when (activeTheme.type) {
            ThemeType.ANIMATED_MULTI -> {
                animationEngine.singleThemeColor = null
                animationEngine.themeAnimationsEnabled = settings.animationEnabled
            }
            ThemeType.ANIMATED_SINGLE -> {
                animationEngine.singleThemeColor = activeTheme.accentColor
                animationEngine.themeAnimationsEnabled = settings.animationEnabled
            }
            ThemeType.SOLID -> {
                animationEngine.themeAnimationsEnabled = false
            }
            ThemeType.CUSTOM_COLOR -> {
                animationEngine.singleThemeColor = settings.customThemeColor
                animationEngine.themeAnimationsEnabled = settings.animationEnabled
            }
            ThemeType.CUSTOM_IMAGE -> {
                animationEngine.singleThemeColor = null
                animationEngine.themeAnimationsEnabled = settings.animationEnabled
            }
        }
        loadBgBitmapIfNeeded()
        stripBgPaint.color = if (activeTheme.type == ThemeType.SOLID)
            ThemeRepository.darken(activeTheme.bgColor, 0.6f)
        else Color.parseColor("#060609")
        rebuildKeys()
        postInvalidateOnAnimation()
    }

    fun release() {
        soundEngine.release()
        kbBgBitmap?.recycle()
        kbBgBitmap = null
        handler.removeCallbacksAndMessages(null)
    }

    private fun loadBgBitmapIfNeeded() {
        val path = if (activeTheme.type == ThemeType.CUSTOM_IMAGE) settings.keyboardImagePath else null
        if (path == kbBgPath) return
        kbBgPath = path
        kbBgBitmap?.recycle()
        kbBgBitmap = null
        if (path != null) {
            kbBgBitmap = try {
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeFile(path, opts)
            } catch (e: Exception) { null }
        }
    }

    private fun codeFor(label: String): Int = when (label) {
        "Shift" -> CODE_SHIFT
        "Del" -> CODE_DELETE
        "Go" -> CODE_ENTER
        "123" -> CODE_SYMBOLS
        "ABC" -> CODE_ALPHA
        "=/<" -> CODE_SYMBOLS
        "Emoji" -> CODE_EMOJI
        "Mic" -> CODE_MIC
        "Clip" -> CODE_CLIPBOARD
        "Game" -> CODE_GAME
        "UrduT" -> CODE_URDU_TOGGLE
        "اردو" -> CODE_URDU_LAYOUT
        else -> CODE_COMMIT_TEXT
    }

    private fun weightFor(label: String): Float = when (label) {
        "Space" -> 4.6f
        "Shift", "Del" -> 1.45f
        "123", "ABC", "=/<", "Go" -> 1.35f
        "اردو" -> 1.35f
        "Emoji", ",", "." -> 1.0f
        "۔" -> 1.0f
        else -> 1.0f
    }

    private fun rebuildKeys() {
        keys.clear()
        stripCells.clear()
        if (width == 0 || height == 0) return

        stripHeight = dp(46f)
        keyboardTop = stripHeight + dp(2f)

        val rows = currentLayout
        val sidePad = dp(3f)
        val vGap = dp(5f)
        val bottomPad = dp(6f)
        val usableH = height - keyboardTop - bottomPad
        val rowH = (usableH - vGap * (rows.size - 1)) / rows.size

        for (r in rows.indices) {
            val row = rows[r]
            var totalWeight = 0f
            for (label in row) totalWeight += weightFor(label)
            val rowWidth = width - sidePad * 2
            var x = sidePad
            val y = keyboardTop + r * (rowH + vGap)
            if (currentLayout === qwertyLayout && r == 1) x += dp(10f)

            for (label in row) {
                val w = rowWidth * weightFor(label) / totalWeight - dp(3f)
                val key = Key()
                key.label = label
                key.code = codeFor(label)
                key.isAction = key.code < 0
                key.rect = RectF(x, y, x + w, y + rowH)
                keys.add(key)
                x += w + dp(3f)
            }
        }
        rebuildStrip()
    }

    private fun rebuildStrip() {
        stripCells.clear()
        if (width == 0) return
        val y0 = 0f
        val y1 = stripHeight
        val pad = dp(6f)

        when {
            pendingClipboardFull != null -> {
                val closeW = dp(38f)
                val chip = StripCell()
                chip.kind = StripKind.CLIP_CHIP
                chip.text = if (settings.ninjaModeEnabled) "Paste ••••••" else pendingClipboardPreview
                chip.code = CODE_COMMIT_TEXT
                chip.rect = RectF(pad, y0 + dp(6f), width - closeW - pad, y1 - dp(6f))
                stripCells.add(chip)

                val close = StripCell()
                close.kind = StripKind.CLIP_CLOSE
                close.text = "✕"
                close.rect = RectF(width - closeW, y0, width.toFloat(), y1)
                stripCells.add(close)
            }
            suggestions.isNotEmpty() -> {
                val cellW = (width - pad * 2) / 3f
                for (i in 0 until 3) {
                    val cell = StripCell()
                    cell.kind = StripKind.SUGGESTION
                    cell.text = suggestions.getOrNull(i) ?: ""
                    cell.code = CODE_COMMIT_TEXT
                    cell.rect = RectF(pad + i * cellW, y0, pad + (i + 1) * cellW, y1)
                    stripCells.add(cell)
                }
            }
            else -> {
                val quick = listOf(
                    Triple("📋", CODE_CLIPBOARD, 1f),
                    Triple("🎤", CODE_MIC, 1f),
                    Triple("🎮", CODE_GAME, 1f),
                    Triple("اردو", CODE_URDU_TOGGLE, 1f)
                )
                val cellW = (width - pad * 2) / quick.size
                for ((i, q) in quick.withIndex()) {
                    val cell = StripCell()
                    cell.kind = StripKind.QUICK
                    cell.text = q.first
                    cell.code = q.second
                    cell.rect = RectF(pad + i * cellW + dp(3f), y0 + dp(5f),
                        pad + (i + 1) * cellW - dp(3f), y1 - dp(5f))
                    stripCells.add(cell)
                }
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildKeys()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackground(canvas)
        drawStrip(canvas)
        val pressing = drawKeys(canvas)
        drawGlideTrail(canvas)
        drawPopup(canvas)
        val animActive = animationEngine.draw(canvas)
        if (animActive || pressing || isListeningForSpeech) {
            postInvalidateOnAnimation()
        }
    }

    private fun drawBackground(canvas: Canvas) {
        val bmp = kbBgBitmap
        if (activeTheme.type == ThemeType.CUSTOM_IMAGE && bmp != null) {
            val vw = width.toFloat(); val vh = height.toFloat()
            val scale = maxOf(vw / bmp.width, vh / bmp.height)
            val dw = bmp.width * scale; val dh = bmp.height * scale
            val left = (vw - dw) / 2f; val top = (vh - dh) / 2f
            canvas.drawBitmap(bmp, null, RectF(left, top, left + dw, top + dh), null)
            canvas.drawColor(Color.argb(135, 0, 0, 0))
        } else {
            canvas.drawColor(activeTheme.bgColor)
        }
        if (activeTheme.type != ThemeType.SOLID) drawAmbientGlow(canvas)
    }

    private fun drawAmbientGlow(canvas: Canvas) {
        val acc = activeTheme.accentColor
        val cx = width * 0.5f
        val cy = height * 0.85f
        val radius = width * 0.75f
        if (radius <= 0f) return
        glowPaint.shader = RadialGradient(
            cx, cy, radius,
            Color.argb(26, Color.red(acc), Color.green(acc), Color.blue(acc)),
            Color.TRANSPARENT, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glowPaint)
        glowPaint.shader = null
    }

    private fun drawStrip(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), stripHeight, stripBgPaint)
        canvas.drawRect(0f, stripHeight, width.toFloat(), stripHeight + dp(1f), stripDividerPaint)

        for (cell in stripCells) {
            when (cell.kind) {
                StripKind.SUGGESTION -> {
                    if (cell.text.isEmpty()) continue
                    suggestionPaint.color = activeTheme.textColor
                    suggestionPaint.textSize = dp(15f)
                    var text = cell.text
                    while (text.length > 2 && suggestionPaint.measureText(text) > cell.rect.width() - dp(16f)) {
                        text = text.dropLast(1)
                    }
                    if (text != cell.text) text = text.dropLast(1) + "…"
                    canvas.drawText(text, cell.rect.centerX(),
                        cell.rect.centerY() + suggestionPaint.textSize / 3f, suggestionPaint)
                    if (cell.rect.left > dp(10f)) {
                        canvas.drawRect(cell.rect.left, cell.rect.top + dp(12f),
                            cell.rect.left + dp(1f), cell.rect.bottom - dp(12f), stripDividerPaint)
                    }
                }
                StripKind.CLIP_CHIP -> {
                    canvas.drawRoundRect(cell.rect, dp(12f), dp(12f), chipBgPaint)
                    suggestionPaint.color = activeTheme.accentColor
                    suggestionPaint.textSize = dp(13f)
                    val prefix = "📋 "
                    var text = cell.text
                    while (text.length > 2 &&
                        suggestionPaint.measureText(prefix + text) > cell.rect.width() - dp(20f)) {
                        text = text.dropLast(1)
                    }
                    if (text != cell.text) text = text.dropLast(1) + "…"
                    canvas.drawText(prefix + text, cell.rect.centerX(),
                        cell.rect.centerY() + suggestionPaint.textSize / 3f, suggestionPaint)
                }
                StripKind.CLIP_CLOSE -> {
                    iconPaint.strokeWidth = dp(2f)
                    iconPaint.color = Color.parseColor("#777C8C")
                    val cx = cell.rect.centerX(); val cy = cell.rect.centerY(); val s = dp(6f)
                    canvas.drawLine(cx - s, cy - s, cx + s, cy + s, iconPaint)
                    canvas.drawLine(cx + s, cy - s, cx - s, cy + s, iconPaint)
                }
                StripKind.QUICK -> {
                    val active = cell.code == CODE_URDU_TOGGLE && settings.urduEnabled
                    val micActive = cell.code == CODE_MIC && isListeningForSpeech
                    if (active || micActive) {
                        chipBgPaint.color = if (micActive) Color.parseColor("#3A1420")
                        else Color.parseColor("#12233F")
                        canvas.drawRoundRect(cell.rect, dp(12f), dp(12f), chipBgPaint)
                    }
                    suggestionPaint.textSize = if (cell.text == "اردو") dp(14f) else dp(18f)
                    suggestionPaint.color = if (active) activeTheme.accentColor else activeTheme.textColor
                    canvas.drawText(cell.text, cell.rect.centerX(),
                        cell.rect.centerY() + suggestionPaint.textSize / 3f, suggestionPaint)
                }
            }
        }
        chipBgPaint.color = Color.parseColor("#15161C")
    }

    private fun drawKeys(canvas: Canvas): Boolean {
        var anyPressActive = false
        val now = SystemClock.uptimeMillis()

        for (key in keys) {
            val start = pressStartTimes[key]
            var scale = 1f
            var pressT = 1f
            if (start != null) {
                val elapsed = now - start
                pressT = (elapsed / 260f).coerceIn(0f, 1f)
                scale = if (pressT < 0.3f) 1f - (pressT / 0.3f) * 0.06f
                else 0.94f + ((pressT - 0.3f) / 0.7f) * 0.06f
                if (pressT < 1f) anyPressActive = true
                else pressStartTimes.remove(key)
            }

            canvas.save()
            canvas.scale(scale, scale, key.rect.centerX(), key.rect.centerY())
            drawSingleKey(canvas, key, pressT)
            canvas.restore()
        }
        return anyPressActive
    }

    private fun drawSingleKey(canvas: Canvas, key: Key, pressT: Float) {
        val r = key.rect
        val radius = dp(9f)
        val acc = activeTheme.accentColor

        keyPaint.clearShadowLayer()
        keyPaint.color = keyBgColor(key)
        canvas.drawRoundRect(r, radius, radius, keyPaint)

        if (pressT < 1f) {
            val glowA = ((1f - pressT) * 90).toInt().coerceIn(0, 255)
            if (glowA > 4) {
                keyPaint.color = Color.argb(glowA, Color.red(acc), Color.green(acc), Color.blue(acc))
                keyPaint.setShadowLayer(24f * (1f - pressT), 0f, 0f, acc)
                canvas.drawRoundRect(r, radius, radius, keyPaint)
                keyPaint.clearShadowLayer()
            }
        }

        drawKeyLabel(canvas, key)
    }

    private fun keyBgColor(key: Key): Int {
        val theme = activeTheme
        return when {
            key.label == "Go" && imeAction != EditorInfo.IME_ACTION_NONE &&
                    imeAction != EditorInfo.IME_ACTION_UNSPECIFIED ->
                ThemeRepository.darken(theme.accentColor, 0.75f)
            key.label == "Shift" && (isShifted || isCapsLocked) ->
                ThemeRepository.darken(theme.accentColor, 0.7f)
            key.isAction -> ThemeRepository.darken(theme.keyColor, 0.8f)
            else -> theme.keyColor
        }
    }

    private fun drawKeyLabel(canvas: Canvas, key: Key) {
        val r = key.rect
        val display = displayLabel(key)
        textPaint.color = activeTheme.textColor
        textPaint.textSize = when {
            key.label == "Space" -> dp(13f)
            display.length > 2 -> dp(12f)
            display.length == 2 -> dp(15f)
            key.isAction -> dp(15f)
            else -> dp(20f)
        }
        val baseline = r.centerY() + textPaint.textSize / 3f

        when (key.label) {
            "Shift" -> drawShiftIcon(canvas, r)
            "Del" -> drawDeleteIcon(canvas, r)
            "Go" -> drawGoLabel(canvas, r)
            else -> canvas.drawText(display, r.centerX(), baseline, textPaint)
        }
    }

    private fun displayLabel(key: Key): String {
        val l = key.label
        if (currentLayout === qwertyLayout && l.length == 1 && l[0].isLetter()) {
            return if (isShifted || isCapsLocked) l.uppercase() else l
        }
        return when (l) {
            "123" -> "123"
            "ABC" -> "ABC"
            "=/<" -> "=\\<"
            "Space" -> "KeyAura"
            "Emoji" -> "😀"
            else -> l
        }
    }

    private fun drawShiftIcon(canvas: Canvas, r: RectF) {
        val active = isShifted || isCapsLocked
        iconPaint.color = if (active) activeTheme.accentColor else activeTheme.textColor
        iconPaint.strokeWidth = dp(2.2f)
        val cx = r.centerX(); val cy = r.centerY(); val s = dp(7f)
        canvas.drawLine(cx, cy - s, cx - s * 0.8f, cy + s * 0.3f, iconPaint)
        canvas.drawLine(cx, cy - s, cx + s * 0.8f, cy + s * 0.3f, iconPaint)
        canvas.drawLine(cx, cy - s, cx, cy + s * 0.9f, iconPaint)
        if (isCapsLocked) {
            canvas.drawLine(cx - s * 0.6f, cy + s * 1.15f, cx + s * 0.6f, cy + s * 1.15f, iconPaint)
        }
    }

    private fun drawDeleteIcon(canvas: Canvas, r: RectF) {
        iconPaint.color = activeTheme.textColor
        iconPaint.strokeWidth = dp(1.8f)
        val cx = r.centerX(); val cy = r.centerY()
        val w = dp(10f); val h = dp(7f)
        val path = Path()
        path.moveTo(cx - w, cy)
        path.lineTo(cx - w * 0.35f, cy - h)
        path.lineTo(cx + w, cy - h)
        path.lineTo(cx + w, cy + h)
        path.lineTo(cx - w * 0.35f, cy + h)
        path.close()
        canvas.drawPath(path, iconPaint)
        val xs = dp(3.2f)
        canvas.drawLine(cx + dp(1f) - xs, cy - xs, cx + dp(1f) + xs, cy + xs, iconPaint)
        canvas.drawLine(cx + dp(1f) + xs, cy - xs, cx + dp(1f) - xs, cy + xs, iconPaint)
    }

    private fun drawGoLabel(canvas: Canvas, r: RectF) {
        val (glyph, colored) = when (imeAction) {
            EditorInfo.IME_ACTION_SEARCH -> "🔍" to true
            EditorInfo.IME_ACTION_SEND -> "➤" to true
            EditorInfo.IME_ACTION_GO -> "→" to true
            EditorInfo.IME_ACTION_NEXT -> "⇥" to true
            EditorInfo.IME_ACTION_DONE -> "✓" to true
            else -> "⏎" to false
        }
        textPaint.textSize = dp(19f)
        textPaint.color = if (colored) Color.WHITE else activeTheme.textColor
        canvas.drawText(glyph, r.centerX(), r.centerY() + textPaint.textSize / 3f, textPaint)
    }

    private fun drawGlideTrail(canvas: Canvas) {
        if (trailPoints.size < 4) return
        trailPath.reset()
        trailPath.moveTo(trailPoints[0], trailPoints[1])
        for (i in 2 until trailPoints.size - 1 step 2) {
            trailPath.lineTo(trailPoints[i], trailPoints[i + 1])
        }
        val acc = activeTheme.accentColor
        trailPaint.color = Color.argb(130, Color.red(acc), Color.green(acc), Color.blue(acc))
        trailPaint.strokeWidth = dp(5f)
        canvas.drawPath(trailPath, trailPaint)
    }

    private fun drawPopup(canvas: Canvas) {
        if (settings.ninjaModeEnabled) return
        val key = popupKey ?: return
        if (key.label.length != 1) return
        val elapsed = SystemClock.uptimeMillis() - popupShownAt
        if (elapsed > 220) return

        val w = dp(38f); val h = dp(46f)
        val cx = key.rect.centerX()
        val left = (cx - w / 2f).coerceIn(dp(2f), width - w - dp(2f))
        val top = key.rect.top - h - dp(8f)
        if (top < stripHeight) return
        scratchRect.set(left, top, left + w, top + h)

        val alpha = ((1f - elapsed / 220f) * 255).toInt().coerceIn(0, 255)
        popupBgPaint.color = Color.argb(alpha, 26, 27, 34)
        canvas.drawRoundRect(scratchRect, dp(10f), dp(10f), popupBgPaint)
        popupTextPaint.color = Color.argb(alpha, 255, 255, 255)
        popupTextPaint.textSize = dp(24f)
        canvas.drawText(displayLabel(key), scratchRect.centerX(),
            scratchRect.centerY() + popupTextPaint.textSize / 3f, popupTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                handleDown(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val idx = event.findPointerIndex(activePointerId)
                if (idx >= 0) handleMove(event.getX(idx), event.getY(idx))
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val idx = event.findPointerIndex(activePointerId)
                if (idx >= 0) handleUp(event.getX(idx), event.getY(idx))
                else handleUp(-1f, -1f)
                activePointerId = -1
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findKey(x: Float, y: Float): Key? {
        for (key in keys) {
            if (key.rect.contains(x, y)) return key
        }
        return null
    }

    private fun findStripCell(x: Float, y: Float): StripCell? {
        for (cell in stripCells) {
            if (cell.rect.contains(x, y)) return cell
        }
        return null
    }

    private fun handleDown(x: Float, y: Float) {
        if (y < stripHeight) {
            downStripCell = findStripCell(x, y)
            downKey = null
            return
        }
        downStripCell = null
        val key = findKey(x, y) ?: return
        downKey = key
        lastGlideKey = key
        pressStartTimes[key] = SystemClock.uptimeMillis()

        if (settings.hapticEnabled) triggerKeyHaptic()
        soundEngine.playClick()
        animationEngine.triggerWithDensity(key.rect.centerX(), key.rect.centerY(), key.label, density)

        if (key.label.length == 1) {
            popupKey = key
            popupShownAt = SystemClock.uptimeMillis()
        }

        if (key.label == "Shift" && currentLayout !== urduLayout && currentLayout !== urduShiftLayout) {
            val now = SystemClock.uptimeMillis()
            if (now - lastShiftTapAt < 400) {
                isCapsLocked = true
                isShifted = true
                lastShiftTapAt = 0L
            } else {
                lastShiftTapAt = now
            }
        }

        if (key.label == "Del") {
            isLongPressDel = false
            val runnable = object : Runnable {
                override fun run() {
                    if (downKey?.label == "Del") {
                        isLongPressDel = true
                        performDelete()
                        if (settings.hapticEnabled) triggerKeyHaptic()
                        handler.postDelayed(this, settings.backspaceRepeatIntervalMs)
                    }
                }
            }
            backspaceRunnable = runnable
            handler.postDelayed(runnable, 400)
        }

        isGliding = false
        glideBuffer.setLength(0)
        glideNoteIndex = 0
        trailPoints.clear()

        postInvalidateOnAnimation()
    }

    private fun handleMove(x: Float, y: Float) {
        val startKey = downKey ?: return
        if (y < stripHeight) return
        if (startKey.label.length != 1 || !startKey.label[0].isLetter()) return
        if (currentLayout !== qwertyLayout) return

        val over = findKey(x, y) ?: return
        if (over === lastGlideKey) return
        if (over.label.length == 1 && over.label[0].isLetter()) {
            isGliding = true
            lastGlideKey = over
            pressStartTimes[over] = SystemClock.uptimeMillis()
            glideBuffer.append(over.label)
            soundEngine.playSwipeTone(glideNoteIndex++)
            animationEngine.triggerWithDensity(over.rect.centerX(), over.rect.centerY(), over.label, density)
            trailPoints.add(x); trailPoints.add(y)
            if (trailPoints.size > 96) {
                trailPoints.removeAt(0); trailPoints.removeAt(0)
            }
            postInvalidateOnAnimation()
        }
    }

    private fun handleUp(x: Float, y: Float) {
        backspaceRunnable?.let { handler.removeCallbacks(it) }
        backspaceRunnable = null

        val stripCell = downStripCell
        downStripCell = null
        if (stripCell != null) {
            if (x >= 0 && stripCell.rect.contains(x, y)) activateStripCell(stripCell)
            resetTouchVisuals()
            return
        }

        val key = downKey ?: run { resetTouchVisuals(); return }
        downKey = null

        if (isLongPressDel) {
            resetTouchVisuals()
            return
        }

        if (isGliding && glideBuffer.length >= 2) {
            romanBuffer.setLength(0)
            romanBuffer.append(glideBuffer.toString())
            updateSuggestions()
            resetTouchVisuals()
            return
        }

        if (x >= 0 && key.rect.contains(x, y)) {
            commitKey(key)
        }
        resetTouchVisuals()
    }

    private fun resetTouchVisuals() {
        isGliding = false
        trailPoints.clear()
        popupKey = null
        postInvalidateOnAnimation()
    }

    private fun activateStripCell(cell: StripCell) {
        when (cell.kind) {
            StripKind.CLIP_CHIP -> {
                val full = pendingClipboardFull ?: return
                pendingClipboardFull = null
                clearRomanBuffer()
                keyListener?.onKey(CODE_COMMIT_TEXT, full)
                rebuildKeys()
                postInvalidateOnAnimation()
            }
            StripKind.CLIP_CLOSE -> {
                pendingClipboardFull = null
                rebuildKeys()
                postInvalidateOnAnimation()
            }
            StripKind.SUGGESTION -> {
                if (cell.text.isNotEmpty()) commitSuggestion(cell.text)
            }
            StripKind.QUICK -> {
                when (cell.code) {
                    CODE_URDU_TOGGLE -> {
                        settings.urduEnabled = !settings.urduEnabled
                        updateSuggestions()
                        postInvalidateOnAnimation()
                    }
                    else -> keyListener?.onKey(cell.code, cell.text)
                }
            }
        }
    }

    private fun commitKey(key: Key) {
        val label = key.label

        if (searchMode) {
            when (key.code) {
                CODE_SHIFT, CODE_SYMBOLS, CODE_ALPHA -> handleLayoutKey(label)
                else -> keyListener?.onKey(key.code, if (label == "Space") "Space" else displayLabel(key))
            }
            return
        }

        when (key.code) {
            CODE_SHIFT -> handleShift()
            CODE_SYMBOLS -> {
                finalizeRomanBuffer()
                currentLayout = if (currentLayout === numberLayout) extendedSymbolLayout else numberLayout
                isShifted = false
                rebuildKeys()
            }
            CODE_ALPHA -> {
                finalizeRomanBuffer()
                currentLayout = qwertyLayout
                isShifted = false
                rebuildKeys()
            }
            CODE_URDU_LAYOUT -> {
                finalizeRomanBuffer()
                currentLayout = urduLayout
                isShifted = false
                rebuildKeys()
            }
            CODE_DELETE -> performDelete()
            CODE_ENTER -> {
                finalizeRomanBuffer()
                keyListener?.onKey(CODE_ENTER, "Go")
            }
            CODE_EMOJI, CODE_CLIPBOARD, CODE_MIC, CODE_GAME -> {
                finalizeRomanBuffer()
                keyListener?.onKey(key.code, label)
            }
            else -> commitCharKey(key)
        }
        postInvalidateOnAnimation()
    }

    private fun handleLayoutKey(label: String) {
        when (label) {
            "Shift" -> handleShift()
            "123" -> { currentLayout = numberLayout; rebuildKeys() }
            "ABC" -> { currentLayout = qwertyLayout; isShifted = false; rebuildKeys() }
        }
    }

    private fun handleShift() {
        if (currentLayout === urduLayout || currentLayout === urduShiftLayout) {
            currentLayout = if (currentLayout === urduLayout) urduShiftLayout else urduLayout
            isShifted = currentLayout === urduShiftLayout
            rebuildKeys()
            return
        }
        if (currentLayout !== qwertyLayout) {
            isShifted = !isShifted
            return
        }
        if (isCapsLocked) {
            isCapsLocked = false
            isShifted = false
        } else {
            isShifted = !isShifted
        }
    }

    private fun commitCharKey(key: Key) {
        val label = key.label

        if (currentLayout === urduLayout || currentLayout === urduShiftLayout) {
            keyListener?.onKey(CODE_COMMIT_TEXT, label)
            if (currentLayout === urduShiftLayout && label.length == 1) {
                currentLayout = urduLayout
                isShifted = false
                rebuildKeys()
            }
            return
        }

        if (label == "Space") {
            finalizeRomanBuffer()
            keyListener?.onKey(CODE_COMMIT_TEXT, " ")
            return
        }

        val isLetter = label.length == 1 && label[0].isLetter()
        val isUrduScript = label.isNotEmpty() && label[0] in '؀'..'ۿ'

        if (isUrduScript) {
            keyListener?.onKey(CODE_COMMIT_TEXT, label)
            return
        }

        if (currentLayout === qwertyLayout && isLetter) {
            val c = if (isShifted || isCapsLocked) label.uppercase() else label
            keyListener?.onKey(CODE_COMMIT_TEXT, c)
            romanBuffer.append(label.lowercase())
            pendingClipboardFull = null
            updateSuggestions()
            if (isShifted && !isCapsLocked) {
                isShifted = false
            }
        } else {
            finalizeRomanBuffer()
            keyListener?.onKey(CODE_COMMIT_TEXT, label)
        }
    }

    private fun performDelete() {
        if (romanBuffer.isNotEmpty()) {
            romanBuffer.deleteCharAt(romanBuffer.length - 1)
            keyListener?.onKey(CODE_DELETE, "Del")
            updateSuggestions()
        } else {
            keyListener?.onKey(CODE_DELETE, "Del")
        }
    }

    private fun updateSuggestions() {
        if (searchMode) return
        suggestions = if (romanBuffer.isEmpty() || settings.ninjaModeEnabled) {
            emptyList()
        } else if (settings.urduEnabled) {
            urduEngine.suggest(romanBuffer.toString())
        } else {
            suggestionEngine.suggest(romanBuffer.toString())
        }
        rebuildStrip()
        postInvalidateOnAnimation()
    }

    private fun commitSuggestion(word: String) {
        val deleteCount = romanBuffer.length
        val isUrdu = word.isNotEmpty() && word[0] in '؀'..'ۿ'
        val display = if (!isUrdu && (isShifted || isCapsLocked)) {
            word.replaceFirstChar { it.uppercase() }
        } else word

        if (deleteCount > 0) {
            keyListener?.onKey(CODE_REPLACE_BASE - deleteCount, "$display ")
        } else {
            keyListener?.onKey(CODE_COMMIT_TEXT, "$display ")
        }

        if (!settings.ninjaModeEnabled) {
            if (isUrdu || settings.urduEnabled) {
                urduEngine.learn(romanBuffer.toString(), word)
            } else {
                suggestionEngine.learn(romanBuffer.toString(), word)
            }
        }
        clearRomanBuffer()
        if (isShifted && !isCapsLocked) isShifted = false
    }

    private fun finalizeRomanBuffer() {
        clearRomanBuffer()
    }

    private fun clearRomanBuffer() {
        romanBuffer.setLength(0)
        if (suggestions.isNotEmpty()) {
            suggestions = emptyList()
        }
        rebuildStrip()
        postInvalidateOnAnimation()
    }

    private fun triggerKeyHaptic() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val now = SystemClock.uptimeMillis()
        if (now - lastHapticAt < 20L) return
        lastHapticAt = now
        try {
            v.cancel()
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
        } catch (_: Exception) {
        }
    }
}
