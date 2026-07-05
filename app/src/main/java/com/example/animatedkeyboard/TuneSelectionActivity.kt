package com.example.animatedkeyboard

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.animatedkeyboard.audio.KeySoundEngine
import com.example.animatedkeyboard.settings.KeyboardSettings

class TuneSelectionActivity : AppCompatActivity() {

    private val settings by lazy { KeyboardSettings.getInstance(this) }
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var previewPool: SoundPool
    private val rowViews = mutableListOf<LinearLayout>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tune_selection)

        previewPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()

        val container = findViewById<LinearLayout>(R.id.tuneListContainer)
        for (i in KeySoundEngine.TUNE_NAMES.indices) {
            val row = buildTuneRow(i)
            rowViews.add(row)
            container.addView(row)
        }
        refreshRowStyles()
    }

    private fun buildTuneRow(index: Int): LinearLayout {
        val dp8 = (8 * resources.displayMetrics.density).toInt()
        val dp16 = (16 * resources.displayMetrics.density).toInt()

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp16, dp16, dp16, dp16)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp8
            layoutParams = lp
            isClickable = true
            isFocusable = true
        }

        val label = TextView(this).apply {
            text = KeySoundEngine.TUNE_NAMES[index]
            textSize = 17f
            setTextColor(0xFFFFFFFF.toInt())
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams = lp
        }

        val checkmark = TextView(this).apply {
            text = "\u2713" // ✓
            textSize = 18f
            setTextColor(0xFF4488FF.toInt())
            tag = "check"
        }

        row.addView(label)
        row.addView(checkmark)

        row.setOnClickListener {
            settings.selectedTuneIndex = index
            refreshRowStyles()
            playPreview(index)
        }
        return row
    }

    private fun refreshRowStyles() {
        val selected = settings.selectedTuneIndex
        for ((i, row) in rowViews.withIndex()) {
            val isSelected = i == selected
            row.setBackgroundResource(if (isSelected) R.drawable.bg_tune_row_selected else R.drawable.bg_tune_row)
            row.findViewWithTag<TextView>("check")?.visibility = if (isSelected) android.view.View.VISIBLE else android.view.View.INVISIBLE
        }
    }

    // FIX: short 3-note arpeggio preview so the user can hear a tune before
    // committing to it, without needing a full KeySoundEngine reload.
    private fun playPreview(tuneIndex: Int) {
        val resIds = KeySoundEngine.resIdsForTune(tuneIndex)
        val previewNotes = intArrayOf(0, 3, 6) // a short, pleasant ascending snippet
        var delay = 0L
        for (noteIdx in previewNotes) {
            val resId = resIds[noteIdx]
            handler.postDelayed({
                try {
                    val soundId = previewPool.load(this, resId, 1)
                    previewPool.setOnLoadCompleteListener { pool, sampleId, status ->
                        if (status == 0 && sampleId == soundId) {
                            pool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
                        }
                    }
                } catch (_: Exception) {
                }
            }, delay)
            delay += 160L
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        previewPool.release()
    }
}
