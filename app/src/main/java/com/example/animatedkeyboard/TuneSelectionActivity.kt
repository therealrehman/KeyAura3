package com.example.animatedkeyboard

import android.graphics.Color
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.animatedkeyboard.ads.UnityAdsManager
import com.example.animatedkeyboard.ads.UnityAdsManager.RewardType
import com.example.animatedkeyboard.audio.KeySoundEngine
import com.example.animatedkeyboard.settings.KeyboardSettings

class TuneSelectionActivity : AppCompatActivity() {

    private val settings by lazy { KeyboardSettings.getInstance(this) }
    private val ads      by lazy { UnityAdsManager.getInstance(this) }
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var previewPool: SoundPool
    private val rowViews = mutableListOf<LinearLayout>()

    private lateinit var unlockStatusView: TextView

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

        // ── Unlock status banner (injected at top) ────────────────────────
        unlockStatusView = TextView(this).apply {
            textSize = 13f
            setPadding(dp(16), dp(12), dp(16), dp(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isClickable = true
            isFocusable = true
        }
        container.addView(unlockStatusView, 0)
        updateUnlockBanner()

        for (i in KeySoundEngine.TUNE_NAMES.indices) {
            val row = buildTuneRow(i)
            rowViews.add(row)
            container.addView(row)
        }
        refreshRowStyles()
    }

    override fun onResume() {
        super.onResume()
        updateUnlockBanner()
        refreshRowStyles()
    }

    private fun updateUnlockBanner() {
        if (ads.isUnlocked(RewardType.TUNES)) {
            val h = ads.remainingHours(RewardType.TUNES)
            unlockStatusView.text = "✅ All tunes unlocked — ${h}h remaining"
            unlockStatusView.setTextColor(Color.parseColor("#00C853"))
            unlockStatusView.setOnClickListener(null)
        } else {
            unlockStatusView.text = "🔒 Tap here to watch an ad and unlock all tunes for 12 hours"
            unlockStatusView.setTextColor(Color.parseColor("#FFC400"))
            unlockStatusView.setOnClickListener { showAdDialog() }
        }
    }

    private fun showAdDialog() {
        AlertDialog.Builder(this)
            .setTitle("🎶 Unlock All Swipe Tunes")
            .setMessage("Watch a short ad to unlock all 10 swipe tunes for 12 hours.")
            .setPositiveButton("▶ Watch Ad") { _, _ ->
                ads.showRewardedAd(
                    activity   = this,
                    type       = RewardType.TUNES,
                    onRewarded = {
                        updateUnlockBanner()
                        refreshRowStyles()
                    }
                )
            }
            .setNegativeButton("Not now", null)
            .show()
    }

    private fun buildTuneRow(index: Int): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp(8)
            layoutParams = lp
            isClickable = true
            isFocusable = true
        }

        val label = TextView(this).apply {
            text = KeySoundEngine.TUNE_NAMES[index]
            textSize = 17f
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams = lp
            tag = "label"
        }

        val lockIcon = TextView(this).apply {
            text = "🔒"
            textSize = 14f
            tag = "lock"
            setPadding(dp(4), 0, dp(4), 0)
        }

        val checkmark = TextView(this).apply {
            text = "\u2713" // ✓
            textSize = 18f
            setTextColor(0xFF4488FF.toInt())
            tag = "check"
        }

        row.addView(label)
        row.addView(lockIcon)
        row.addView(checkmark)

        row.setOnClickListener {
            val isLocked = !ads.isUnlocked(RewardType.TUNES)

            if (isLocked) {
                showAdDialog()
            } else {
                settings.selectedTuneIndex = index
                refreshRowStyles()
                playPreview(index)
            }
        }
        return row
    }

    private fun refreshRowStyles() {
        val selected  = settings.selectedTuneIndex
        val unlocked  = ads.isUnlocked(RewardType.TUNES)

        for ((i, row) in rowViews.withIndex()) {
            val isSelected = i == selected
            val isLocked   = !unlocked

            row.setBackgroundResource(
                if (isSelected) R.drawable.bg_tune_row_selected else R.drawable.bg_tune_row
            )

            // Label color
            row.findViewWithTag<TextView>("label")?.apply {
                setTextColor(
                    when {
                        isSelected -> 0xFFFFFFFF.toInt()
                        isLocked   -> 0xFF555878.toInt()
                        else       -> 0xFFBBBBBB.toInt()
                    }
                )
            }

            // Lock icon
            row.findViewWithTag<TextView>("lock")?.visibility =
                if (isLocked) View.VISIBLE else View.GONE

            // Checkmark
            row.findViewWithTag<TextView>("check")?.visibility =
                if (isSelected) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun playPreview(tuneIndex: Int) {
        val resIds = KeySoundEngine.resIdsForTune(tuneIndex)
        val previewNotes = intArrayOf(0, 3, 6)
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
                } catch (_: Exception) { }
            }, delay)
            delay += 160L
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        previewPool.release()
    }
}
