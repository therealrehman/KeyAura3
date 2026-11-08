package com.example.animatedkeyboard.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.animatedkeyboard.settings.KeyboardSettings
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * A wide/short flappy-bird-style endless game, shaped to fit the keyboard's
 * own landscape-strip footprint rather than a tall phone-screen layout: the
 * bird holds a fixed X position, gravity pulls it down within the panel's
 * limited height, and pipes scroll in from the right with a vertical gap.
 */
class BirdyBirdCanvas @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onBackTapped: (() -> Unit)? = null

    private val settings by lazy { KeyboardSettings.getInstance(context) }
    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private enum class GameState { READY, PLAYING, GAME_OVER }
    private var state = GameState.READY

    // --- Bird physics ---
    private var birdY = 0f
    private var birdVelocity = 0f
    private val gravity = 1400f       // px/s^2
    private val flapImpulse = -420f   // px/s
    private var birdX = 0f
    private val birdRadiusDp = 9f

    // --- Pipes ---
    private data class Pipe(var x: Float, val gapCenterY: Float, val gapHeight: Float, var scored: Boolean = false)
    private val pipes = mutableListOf<Pipe>()
    private val pipeWidthDp = 22f
    private val pipeSpacingDp = 130f
    private val scrollSpeedDpPerSec = 110f

    private var score = 0
    private var highScore = 0
    private var lastFrameTime = 0L
    private var backButtonRect = Rect()

    // --- Paints ---
    private val bgPaint = Paint().apply { color = Color.parseColor("#05050F") }
    private val glowPaint = Paint().apply { isAntiAlias = true }
    private val birdPaint = Paint().apply { isAntiAlias = true }
    private val pipePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val pipeBorderPaint = Paint().apply {
        isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = dp(1.5f); color = Color.parseColor("#4488FF")
    }
    private val scorePaint = Paint().apply {
        color = Color.WHITE; isAntiAlias = true; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val hintPaint = Paint().apply {
        color = Color.parseColor("#AAAAEE"); isAntiAlias = true; textAlign = Paint.Align.CENTER
    }
    private val backIconPaint = Paint().apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = dp(2f)
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; isAntiAlias = true
    }
    private val iconButtonPaint = Paint().apply { color = Color.parseColor("#141414"); isAntiAlias = true }
    private val frameStrokePaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = dp(3f); isAntiAlias = true }
    private val groundPaint = Paint().apply { isAntiAlias = true }
    private val grassPaint = Paint().apply { color = Color.parseColor("#3CCB6B"); isAntiAlias = true }
    private val cloudPaint = Paint().apply { color = Color.parseColor("#1A2350"); isAntiAlias = true }
    private val starPaint = Paint().apply { color = Color.WHITE; isAntiAlias = true }
    private val rivetPaint = Paint().apply { color = Color.parseColor("#4488FF"); isAntiAlias = true }
    private val backBtnPaint = Paint().apply { color = Color.parseColor("#1E2A4A"); isAntiAlias = true }
    private val backBtnBorderPaint = Paint().apply {
        color = Color.parseColor("#4488FF"); style = Paint.Style.STROKE; strokeWidth = dp(1f); isAntiAlias = true
    }
    private val backTextPaint = Paint().apply {
        color = Color.WHITE; textSize = dp(12f); isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD
    }

    private var isRunning = false

    init {
        setWillNotDraw(false)
        highScore = settings.birdyBirdHighScore
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        birdX = w * 0.28f
        birdY = h / 2f
        val btnW = dp(84f).toInt()
        val btnH = dp(32f).toInt()
        val margin = dp(8f).toInt()
        backButtonRect = Rect(w - btnW - margin, margin, w - margin, margin + btnH)
        resetGame()
    }

    private fun resetGame() {
        birdY = height / 2f
        birdVelocity = 0f
        pipes.clear()
        score = 0
        state = GameState.READY
        seedPipes()
    }

    private fun seedPipes() {
        if (width == 0 || height == 0) return
        var x = width * 1.1f
        repeat(4) {
            pipes.add(makePipe(x))
            x += dp(pipeSpacingDp)
        }
    }

    private fun makePipe(x: Float): Pipe {
        val minGapHeight = height * 0.42f
        val margin = height * 0.14f
        val gapCenter = Random.nextFloat() * (height - margin * 2) + margin
        return Pipe(x, gapCenter.coerceIn(margin, height - margin), minGapHeight)
    }

    fun onShown() {
        isRunning = true
        lastFrameTime = System.currentTimeMillis()
        if (width > 0) resetGame()
        postInvalidateOnAnimation()
    }

    fun onHidden() {
        isRunning = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.currentTimeMillis()
        val dt = ((now - lastFrameTime).coerceIn(0, 50)) / 1000f
        lastFrameTime = now

        if (state == GameState.PLAYING && isRunning) {
            update(dt)
        }

        drawBackground(canvas)
        drawFrame(canvas)
        drawPipes(canvas)
        drawBird(canvas)
        drawHud(canvas)
        drawBackButton(canvas)

        if (isRunning) postInvalidateOnAnimation()
    }

    /** Game framing: gradient border, corner rivets, stars, clouds and a grassy ground strip. */
    private fun drawFrame(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // stars
        for (i in 0 until 22) {
            val x = ((i * 97) % 100) / 100f * w
            val y = ((i * 53) % 55) / 100f * h
            starPaint.alpha = 70 + (i % 4) * 45
            canvas.drawCircle(x, y, dp(0.7f + (i % 3) * 0.45f), starPaint)
        }

        // clouds
        cloudPaint.alpha = 255
        for ((fx, fy, s) in listOf(Triple(0.18f, 0.22f, 1.0f), Triple(0.72f, 0.36f, 0.8f), Triple(0.46f, 0.12f, 0.6f))) {
            val cx = fx * w
            val cy = fy * h
            val rx = dp(40f) * s
            val ry = dp(14f) * s
            canvas.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, cloudPaint)
            canvas.drawOval(cx - rx * 0.55f, cy - ry * 1.5f, cx + rx * 0.35f, cy + ry * 0.4f, cloudPaint)
        }

        // ground strip with a grass line on top
        val groundTop = h * 0.86f
        groundPaint.shader = LinearGradient(0f, groundTop, 0f, h,
            Color.parseColor("#1B3D2F"), Color.parseColor("#0B1F17"), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, groundTop, w, h, groundPaint)
        canvas.drawRect(0f, groundTop, w, groundTop + dp(3f), grassPaint)

        // gradient frame + corner rivets
        val inset = dp(3f)
        val rect = RectF(inset, inset, w - inset, h - inset)
        frameStrokePaint.shader = LinearGradient(0f, 0f, w, h,
            Color.parseColor("#4488FF"), Color.parseColor("#B05CFF"), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, dp(14f), dp(14f), frameStrokePaint)
        val r = dp(2.5f)
        val p = dp(12f)
        for ((x, y) in listOf(p to p, w - p to p, p to h - p, w - p to h - p)) {
            canvas.drawCircle(x, y, r, rivetPaint)
        }
    }

    private fun update(dt: Float) {
        birdVelocity += gravity * dt
        birdY += birdVelocity * dt

        val scrollPx = dp(scrollSpeedDpPerSec) * dt
        for (pipe in pipes) pipe.x -= scrollPx

        // Recycle pipes that scrolled off-screen to the left.
        val leftmost = pipes.minByOrNull { it.x }
        pipes.removeAll { it.x < -dp(pipeWidthDp) - dp(10f) }
        while (pipes.size < 4) {
            val rightmostX = pipes.maxOfOrNull { it.x } ?: (width * 1.1f)
            pipes.add(makePipe(rightmostX + dp(pipeSpacingDp)))
        }

        // Score: passed a pipe's center once.
        for (pipe in pipes) {
            if (!pipe.scored && pipe.x + dp(pipeWidthDp) < birdX) {
                pipe.scored = true
                score++
            }
        }

        checkCollisions()
    }

    private fun checkCollisions() {
        val r = dp(birdRadiusDp)
        if (birdY - r <= 0f || birdY + r >= height) {
            gameOver()
            return
        }
        for (pipe in pipes) {
            val pipeLeft = pipe.x
            val pipeRight = pipe.x + dp(pipeWidthDp)
            if (birdX + r > pipeLeft && birdX - r < pipeRight) {
                val gapTop = pipe.gapCenterY - pipe.gapHeight / 2f
                val gapBottom = pipe.gapCenterY + pipe.gapHeight / 2f
                if (birdY - r < gapTop || birdY + r > gapBottom) {
                    gameOver()
                    return
                }
            }
        }
    }

    private fun gameOver() {
        if (state != GameState.PLAYING) return
        state = GameState.GAME_OVER
        if (score > highScore) {
            highScore = score
            settings.birdyBirdHighScore = highScore
        }
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        val cx = width * 0.7f; val cy = height * 0.3f
        val radius = max(width, height) * 0.6f
        glowPaint.shader = RadialGradient(
            cx, cy, radius,
            intArrayOf(Color.argb(60, 100, 70, 255), Color.argb(30, 255, 80, 190), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glowPaint)
    }

    private fun drawBird(canvas: Canvas) {
        val r = dp(birdRadiusDp)
        birdPaint.shader = LinearGradient(
            birdX - r, birdY - r, birdX + r, birdY + r,
            intArrayOf(Color.parseColor("#4488FF"), Color.parseColor("#B060FF"), Color.parseColor("#FF64C8")),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(birdX, birdY, r, birdPaint)
        // simple beak
        val beakPaint = Paint().apply { color = Color.parseColor("#FFCC66"); isAntiAlias = true }
        val path = android.graphics.Path()
        path.moveTo(birdX + r * 0.7f, birdY - r * 0.2f)
        path.lineTo(birdX + r * 1.5f, birdY)
        path.lineTo(birdX + r * 0.7f, birdY + r * 0.2f)
        path.close()
        canvas.drawPath(path, beakPaint)
    }

    private fun drawPipes(canvas: Canvas) {
        val w = dp(pipeWidthDp)
        for (pipe in pipes) {
            val gapTop = pipe.gapCenterY - pipe.gapHeight / 2f
            val gapBottom = pipe.gapCenterY + pipe.gapHeight / 2f
            pipePaint.shader = LinearGradient(
                pipe.x, 0f, pipe.x + w, 0f,
                intArrayOf(Color.parseColor("#1A2A6A"), Color.parseColor("#2A1A6A")),
                null, Shader.TileMode.CLAMP
            )
            // top pipe segment
            canvas.drawRect(pipe.x, 0f, pipe.x + w, gapTop, pipePaint)
            canvas.drawRect(pipe.x, 0f, pipe.x + w, gapTop, pipeBorderPaint)
            // bottom pipe segment
            canvas.drawRect(pipe.x, gapBottom, pipe.x + w, height.toFloat(), pipePaint)
            canvas.drawRect(pipe.x, gapBottom, pipe.x + w, height.toFloat(), pipeBorderPaint)
        }
    }

    private fun drawHud(canvas: Canvas) {
        scorePaint.textSize = dp(20f)
        canvas.drawText(score.toString(), width / 2f, dp(28f), scorePaint)

        when (state) {
            GameState.READY -> {
                hintPaint.textSize = dp(13f)
                canvas.drawText("Tap to fly", width / 2f, height / 2f + dp(28f), hintPaint)
            }
            GameState.GAME_OVER -> {
                hintPaint.textSize = dp(14f)
                canvas.drawText("Game Over — Best: $highScore", width / 2f, height / 2f, hintPaint)
                hintPaint.textSize = dp(12f)
                canvas.drawText("Tap to try again", width / 2f, height / 2f + dp(20f), hintPaint)
            }
            GameState.PLAYING -> {}
        }
    }

    private fun drawBackButton(canvas: Canvas) {
        val r = dp(16f)
        canvas.drawRoundRect(backButtonRect.left.toFloat(), backButtonRect.top.toFloat(),
            backButtonRect.right.toFloat(), backButtonRect.bottom.toFloat(), r, r, backBtnPaint)
        canvas.drawRoundRect(backButtonRect.left.toFloat(), backButtonRect.top.toFloat(),
            backButtonRect.right.toFloat(), backButtonRect.bottom.toFloat(), r, r, backBtnBorderPaint)
        val cy = backButtonRect.exactCenterY()
        val chevX = backButtonRect.left + dp(14f)
        val s = dp(5f)
        canvas.drawLine(chevX + s, cy - s, chevX, cy, backIconPaint)
        canvas.drawLine(chevX, cy, chevX + s, cy + s, backIconPaint)
        canvas.drawText("Back", chevX + dp(10f), cy + backTextPaint.textSize / 3f, backTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (backButtonRect.contains(event.x.toInt(), event.y.toInt())) {
                onBackTapped?.invoke()
                return true
            }
            when (state) {
                GameState.READY -> {
                    state = GameState.PLAYING
                    birdVelocity = flapImpulse
                }
                GameState.PLAYING -> {
                    birdVelocity = flapImpulse
                }
                GameState.GAME_OVER -> {
                    resetGame()
                    state = GameState.PLAYING
                    birdVelocity = flapImpulse
                }
            }
            postInvalidateOnAnimation()
            return true
        }
        return super.onTouchEvent(event)
    }
}
