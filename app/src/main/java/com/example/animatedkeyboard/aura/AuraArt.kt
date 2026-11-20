package com.example.animatedkeyboard.aura

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin

/**
 * Daily Aura Canvas: every keystroke paints a flow-field stroke.
 * Export = PNG in cacheDir/aura, shared via the app's existing FileProvider.
 */
object AuraArt {
    private const val W = 720
    private const val H = 1280
    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private var x = W / 2f
    private var y = H / 2f
    private var strokes = 0L

    private fun ensure() {
        if (bitmap == null) {
            val b = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            c.drawColor(Color.parseColor("#0F0C29"))
            bitmap = b; canvas = c
        }
    }

    fun record(dna: AuraDNA) {
        ensure()
        val c = canvas ?: return
        val col = dna.mood.color
        paint.color = col
        paint.alpha = 90 + (dna.burst * 120).toInt()
        paint.strokeWidth = 1.5f + dna.burst * 6f
        val angle = (sin(x * 0.011f) + cos(y * 0.013f) + dna.variance * 2f) * 1.8f
        val step = 6f + dna.burst * 14f
        val nx = x + cos(angle) * step
        val ny = y + sin(angle) * step
        c.drawLine(x, y, nx, ny, paint)
        if (dna.burst > 0.72f) {
            paint.alpha = 40
            c.drawCircle(nx, ny, 10f + dna.burst * 14f, paint)
        }
        x = nx; y = ny
        if (x < 0 || x > W) x = W - x.coerceIn(0f, W.toFloat())
        if (y < 0 || y > H) y = H - y.coerceIn(0f, H.toFloat())
        strokes++
    }

    fun snapshot(): Bitmap { ensure(); return bitmap!! }
    fun strokeCount(): Long = strokes

    fun reset() {
        bitmap?.recycle(); bitmap = null; canvas = null
        x = W / 2f; y = H / 2f; strokes = 0
    }

    fun exportUri(context: Context): Uri? {
        return try {
            val dir = File(context.cacheDir, "aura").apply { mkdirs() }
            val f = File(dir, "keyaura_canvas_${System.currentTimeMillis()}.png")
            FileOutputStream(f).use { snapshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
        } catch (_: Exception) { null }
    }
}
