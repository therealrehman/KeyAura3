package com.example.animatedkeyboard

import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import com.example.animatedkeyboard.settings.KeyboardSettings

class SettingsActivity : AppCompatActivity() {

    private val settings by lazy { KeyboardSettings.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Version
        val tvVersion = findViewById<TextView>(R.id.tvVersion)
        try {
            val pkgInfo = packageManager.getPackageInfo(packageName, 0)
            tvVersion.text = pkgInfo.versionName
        } catch (_: Exception) {
            tvVersion.text = "1.0.0"
        }

        // Navigate
        findViewById<CardView>(R.id.btnAbout).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
        findViewById<CardView>(R.id.btnContactUs).setOnClickListener {
            startActivity(Intent(this, ContactUsActivity::class.java))
        }
        findViewById<CardView>(R.id.btnPrivacyPolicy).setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }

        // Switches
        val switchGesture = findViewById<SwitchCompat>(R.id.switchGestureTyping)
        switchGesture.isChecked = settings.gestureTypingEnabled
        switchGesture.setOnCheckedChangeListener { _, isChecked ->
            settings.gestureTypingEnabled = isChecked
        }

        val switchNumberRow = findViewById<SwitchCompat>(R.id.switchNumberRow)
        switchNumberRow.isChecked = settings.numberRowEnabled
        switchNumberRow.setOnCheckedChangeListener { _, isChecked ->
            settings.numberRowEnabled = isChecked
        }

        val switchTextExpansion = findViewById<SwitchCompat>(R.id.switchTextExpansion)
        switchTextExpansion.isChecked = settings.textExpansionEnabled
        switchTextExpansion.setOnCheckedChangeListener { _, isChecked ->
            settings.textExpansionEnabled = isChecked
        }

        val switchCursorSwipe = findViewById<SwitchCompat>(R.id.switchCursorSwipe)
        switchCursorSwipe.isChecked = settings.cursorSwipeEnabled
        switchCursorSwipe.setOnCheckedChangeListener { _, isChecked ->
            settings.cursorSwipeEnabled = isChecked
        }

        // Keyboard height slider
        val seekBar = findViewById<SeekBar>(R.id.seekBarHeight)
        val tvHeight = findViewById<TextView>(R.id.tvHeightValue)
        val currentPercent = settings.keyboardHeightPercent
        seekBar.progress = currentPercent - 30 // 30% base
        tvHeight.text = "$currentPercent%"
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val percent = progress + 30
                tvHeight.text = "$percent%"
                if (fromUser) settings.keyboardHeightPercent = percent
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
}
