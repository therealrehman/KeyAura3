package com.keyaura.view

import android.graphics.*
import android.view.View
import com.keyaura.engine.MoodState
import com.keyaura.engine.TypingDNA
import kotlin.math.cos
import kotlin.math.sin

class AuraCanvas(private val view: View) {

    private val path = Path()
    private val paintWave = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val paintGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
        style = Paint.Style.STROKE; strokeWidth = 4f; maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL) 
    }
    
    // Daily Aura Canvas (Generative Art)
    private val artCanvas = Canvas()
    private val artBitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
    private val artPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private var artX = 0f
    private var artY = 0f

    init {
        artCanvas.setBitmap(artBitmap)
        artCanvas.drawColor(Color.BLACK)
        artX = 540f; artY = 960f
    }

    // 1. THE AURA SPACEBAR (Reactive Wave)
    fun drawAuraSpacebar(canvas: Canvas, bounds: Rect, dna: TypingDNA, phase: Float) {
        val color = Color.valueOf(dna.mood.colorHex).toArgb()
        
        val gradient = LinearGradient(
            bounds.left.toFloat(), 0f, bounds.right.toFloat(), 0f,
            Color.argb(50, Color.red(color), Color.green(color), Color.blue(color)),
            Color.argb(220, Color.red(color), Color.green(color), Color.blue(color)),
            Shader.TileMode.CLAMP
        )
        paintWave.shader = gradient
        paintGlow.color = color

        path.reset()
        val cy = bounds.centerY().toFloat()
        val amp = 5f + (dna.burstIntensity * 25f)
        val freq = 0.05f + (dna.rhythmVariance * 0.05f)

        path.moveTo(bounds.left.toFloat(), bounds.bottom.toFloat())
        path.lineTo(bounds.left.toFloat(), cy)
        for (x in bounds.left..bounds.right step 2) {
            path.lineTo(x.toFloat(), cy + (sin(x * freq + phase) * amp).toFloat())
        }
        path.lineTo(bounds.right.toFloat(), bounds.bottom.toFloat())
        path.close()

        canvas.drawPath(path, paintWave)
        canvas.drawPath(path, paintGlow)
    }

    // 2. DAILY AURA CANVAS (Generative Art Diary)
    fun recordKeypressToCanvas(dna: TypingDNA) {
        val color = Color.valueOf(dna.mood.colorHex).toArgb()
        artPaint.color = color
        artPaint.strokeWidth = 2f + (dna.burstIntensity * 8f)
        artPaint.alpha = 150

        // Flow field algorithm based on rhythm variance
        val angle = (dna.rhythmVariance * 360 * Math.PI / 180).toFloat()
        val nextX = artX + cos(angle) * 10f
        val nextY = artY + sin(angle) * 10f

        artCanvas.drawLine(artX, artY, nextX, nextY, artPaint)
        
        // Add particles for high intensity
        if (dna.burstIntensity > 0.7f) {
            artPaint.strokeWidth = 12f
            artPaint.alpha = 50
            artCanvas.drawCircle(nextX, nextY, 15f, artPaint)
        }

        artX = nextX; artY = nextY
        
        // Wrap around screen
        if (artX > 1080) artX = 0f
        if (artY > 1920) artY = 0f
    }

    fun getDailyArtBitmap(): Bitmap = artBitmap
    
    fun resetDailyArt() {
        artCanvas.drawColor(Color.BLACK)
        artX = 540f; artY = 960f
    }
}
