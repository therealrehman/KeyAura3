package com.example.animatedkeyboard.aura

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.animatedkeyboard.R

class AuraStudioActivity : AppCompatActivity(), AuraEngine.Listener {

    private lateinit var prefs: AuraPrefs
    private lateinit var engine: AuraEngine
    private lateinit var orb: AuraOrbView
    private lateinit var stats: TextView
    private lateinit var art: ImageView
    private lateinit var vibeInput: EditText
    private lateinit var vibeOut: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aura_studio)
        prefs = AuraPrefs.getInstance(this)
        engine = AuraEngine(this, prefs) { true }
        engine.addListener(this)

        orb = findViewById(R.id.aura_orb)
        stats = findViewById(R.id.aura_stats)
        art = findViewById(R.id.aura_art)
        vibeInput = findViewById(R.id.aura_vibe_input)
        vibeOut = findViewById(R.id.aura_vibe_out)

        art.setImageBitmap(AuraArt.snapshot())
        refreshStats()

        findViewById<Button>(R.id.aura_save).setOnClickListener {
            val uri = AuraArt.exportUri(this)
            if (uri == null) { toast("Export failed"); return@setOnClickListener }
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(share, "Share your Aura Canvas"))
        }
        findViewById<Button>(R.id.aura_new).setOnClickListener {
            AuraArt.reset(); art.setImageBitmap(AuraArt.snapshot()); refreshStats()
        }
        findViewById<Button>(R.id.aura_refresh).setOnClickListener {
            art.setImageBitmap(AuraArt.snapshot()); refreshStats()
        }

        bindSwitch(R.id.aura_sw_sound, prefs.auraSound) { prefs.auraSound = it }
        bindSwitch(R.id.aura_sw_flow, prefs.flowMode) { prefs.flowMode = it }
        bindSwitch(R.id.aura_sw_onehand, prefs.oneHand) { prefs.oneHand = it }
        bindSwitch(R.id.aura_sw_big, prefs.bigKeys) { prefs.bigKeys = it }

        findViewById<Button>(R.id.aura_vibe_pro).setOnClickListener { vibe("/pro") }
        findViewById<Button>(R.id.aura_vibe_fun).setOnClickListener { vibe("/fun") }
        findViewById<Button>(R.id.aura_vibe_poet).setOnClickListener { vibe("/poet") }
    }

    private fun vibe(trigger: String) {
        val src = vibeInput.text.toString().ifBlank { "boss aaj nahi aa sakta tabiyat kharab hai" }
        vibeOut.text = AuraAlchemy.shift(src, trigger)
    }

    private fun bindSwitch(id: Int, initial: Boolean, setter: (Boolean) -> Unit) {
        val sw = findViewById<Switch>(id)
        sw.isChecked = initial
        sw.setOnCheckedChangeListener { _, checked -> setter(checked) }
    }

    private fun refreshStats() {
        val d = engine.dna
        stats.text = "Mood: ${d.mood.label}   ·   ${d.wpm} WPM   ·   " +
            "rhythm ${(d.variance * 100).toInt()}%   ·   ${AuraArt.strokeCount()} strokes painted"
    }

    override fun onDNA(dna: AuraDNA) {
        orb.dna = dna
        refreshStats()
    }

    override fun onResume() {
        super.onResume()
        art.setImageBitmap(AuraArt.snapshot())
        refreshStats()
    }

    override fun onDestroy() {
        engine.removeListener(this)
        engine.release()
        super.onDestroy()
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
